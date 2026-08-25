#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ENCAR FULL RECURSIVE CATALOG SCANNER

Goal
----
Build one machine-readable catalog for the Encar search hierarchy:
CarType -> Manufacturer -> ModelGroup -> Model(generation) -> Grade ->
BadgeGroup -> Badge -> BadgeDetail, while also recording technical facets
needed by the search application (Year, FuelType, SellType, Category, etc.).

The scanner is deliberately dynamic: it discovers manufacturers and model
families from Encar itself. No brand/model list is hardcoded.

Output (UTF-8 JSON Lines)
-------------------------
  catalog.jsonl    - exact hierarchy values/actions/counts/metadata
  technical.jsonl  - technical/search facets attached to each hierarchy scope
  samples.jsonl    - one sample SearchResult from each fetched hierarchy scope
  errors.jsonl     - permanent request/parse errors
  manifest.json    - final summary and schema information
  checkpoint.json  - queue/fetched state for automatic resume
  node_names.json  - every iNav node name seen + frequency

Why JSONL?
----------
Every line is an independent JSON object. The file can be hundreds of MB and
still be streamed, searched and processed without loading it all into memory.
"""

from __future__ import annotations

import argparse
import collections
import datetime as dt
import json
import os
import random
import signal
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Set, Tuple

API = "https://api.encar.com/search/car/list/general"
USER_AGENT = (
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
    "Chrome/126 Mobile Safari/537.36"
)
SCHEMA_VERSION = 3

# Exact Encar hierarchy that the application needs.
# Grade is kept explicitly because some Encar branches expose technical
# engine/fuel/drive selection at this level.
HIERARCHY_LEVELS: Dict[str, Tuple[str, int]] = {
    "Manufacturer": ("manufacturer", 1),
    "ModelGroup": ("model_group", 2),
    "Model": ("model", 3),
    "Grade": ("grade", 4),
    "BadgeGroup": ("badge_group", 5),
    "Badge": ("badge", 6),
    "BadgeDetail": ("badge_detail", 7),
}

# Capture the search/technical facets that can affect a voice query or future
# search rules. Unknown names are counted in node_names.json, so we can audit
# Encar schema changes without dumping every cosmetic/region facet repeatedly.
TECHNICAL_NODE_NAMES: Set[str] = {
    "Year",
    "FormYear",
    "FuelType",
    "SellType",
    "Category",
    "GreenType",
    "ModelCarType",
    "Transmission",
    "DriveType",
    "Displacement",
    "EngineDisplacement",
    "SeatingCapacity",
    "Seating",
    "BodyType",
    "VehicleType",
    "UseType",
    "AttributeType",
}

ROOT_TASKS = [
    {
        "action": "(And.Hidden.N._.CarType.Y.)",
        "context": {"car_type": "Y"},
        "reason": "ROOT_DOMESTIC",
    },
    {
        "action": "(And.Hidden.N._.CarType.N.)",
        "context": {"car_type": "N"},
        "reason": "ROOT_IMPORTED",
    },
]

STOP_REQUESTED = False


def _request_stop(signum: int, frame: Any) -> None:
    global STOP_REQUESTED
    STOP_REQUESTED = True


signal.signal(signal.SIGINT, _request_stop)
signal.signal(signal.SIGTERM, _request_stop)


def now_iso() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def json_compact(obj: Any) -> str:
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def append_jsonl(path: Path, obj: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8", newline="\n") as f:
        f.write(json_compact(obj))
        f.write("\n")
        f.flush()


def safe_metadata(value: Any) -> Any:
    """Keep JSON-compatible metadata, but avoid accidental gigantic blobs."""
    if isinstance(value, (str, int, float, bool)) or value is None:
        return value
    if isinstance(value, list):
        return [safe_metadata(v) for v in value]
    if isinstance(value, dict):
        return {str(k): safe_metadata(v) for k, v in value.items()}
    return str(value)


def deepest_level(context: Dict[str, Any]) -> int:
    best = 0
    for _, (field, level) in HIERARCHY_LEVELS.items():
        if context.get(field) not in (None, ""):
            best = max(best, level)
    return best


def expected_deeper_names(node_name: str) -> Set[str]:
    current = HIERARCHY_LEVELS.get(node_name)
    if current is None:
        return set()
    level = current[1]
    return {
        name
        for name, (_, other_level) in HIERARCHY_LEVELS.items()
        if other_level > level
    }


def has_deeper_hierarchy(obj: Any, node_name: str) -> bool:
    wanted = expected_deeper_names(node_name)
    if not wanted:
        return False

    if isinstance(obj, dict):
        n = obj.get("Name")
        if isinstance(n, str) and n in wanted:
            return True
        return any(has_deeper_hierarchy(v, node_name) for v in obj.values())
    if isinstance(obj, list):
        return any(has_deeper_hierarchy(v, node_name) for v in obj)
    return False


@dataclass
class Task:
    action: str
    context: Dict[str, Any]
    reason: str


class Scanner:
    def __init__(
        self,
        out_dir: Path,
        delay: float,
        max_requests: int,
        max_runtime_minutes: float,
        resume: bool,
        checkpoint_every: int,
    ) -> None:
        self.out_dir = out_dir
        self.out_dir.mkdir(parents=True, exist_ok=True)
        self.delay = max(0.0, delay)
        self.max_requests = max_requests
        self.max_runtime_seconds = max_runtime_minutes * 60.0
        self.checkpoint_every = max(1, checkpoint_every)
        self.started_monotonic = time.monotonic()
        self.started_at = now_iso()

        self.catalog_path = self.out_dir / "catalog.jsonl"
        self.technical_path = self.out_dir / "technical.jsonl"
        self.samples_path = self.out_dir / "samples.jsonl"
        self.errors_path = self.out_dir / "errors.jsonl"
        self.checkpoint_path = self.out_dir / "checkpoint.json"
        self.manifest_path = self.out_dir / "manifest.json"
        self.node_names_path = self.out_dir / "node_names.json"
        self.bundle_path = self.out_dir / "ENCAR_FULL_CATALOG.zip"

        self.queue: collections.deque[Task] = collections.deque()
        self.queued_actions: Set[str] = set()
        self.fetched_actions: Set[str] = set()
        self.emitted_catalog_actions: Set[Tuple[str, str]] = set()
        self.node_name_counts: collections.Counter[str] = collections.Counter()
        self.request_count = 0
        self.http_error_count = 0
        self.parse_error_count = 0
        self.hierarchy_counts: collections.Counter[str] = collections.Counter()
        self.technical_count = 0
        self.sample_count = 0

        if resume and self.checkpoint_path.exists():
            self._load_checkpoint()
        else:
            self._fresh_start()

    def _fresh_start(self) -> None:
        # Do not accidentally append a new scan to old data.
        for p in (
            self.catalog_path,
            self.technical_path,
            self.samples_path,
            self.errors_path,
            self.manifest_path,
            self.node_names_path,
            self.bundle_path,
            self.checkpoint_path,
        ):
            if p.exists():
                p.unlink()

        for raw in ROOT_TASKS:
            self.enqueue(Task(**raw))

        append_jsonl(
            self.catalog_path,
            {
                "record": "scan_header",
                "schema_version": SCHEMA_VERSION,
                "started_at": self.started_at,
                "api": API,
                "search_order": [
                    "CarType",
                    "Manufacturer",
                    "ModelGroup",
                    "Model",
                    "Grade",
                    "BadgeGroup",
                    "Badge",
                    "BadgeDetail",
                ],
                "note": "Year/Fuel are recorded as facets and applied only after generation selection.",
            },
        )
        self.save_checkpoint(force=True)

    def _load_checkpoint(self) -> None:
        with self.checkpoint_path.open("r", encoding="utf-8") as f:
            state = json.load(f)

        self.request_count = int(state.get("request_count", 0))
        self.http_error_count = int(state.get("http_error_count", 0))
        self.parse_error_count = int(state.get("parse_error_count", 0))
        self.fetched_actions = set(state.get("fetched_actions", []))
        self.node_name_counts.update(state.get("node_name_counts", {}))
        self.hierarchy_counts.update(state.get("hierarchy_counts", {}))
        self.technical_count = int(state.get("technical_count", 0))
        self.sample_count = int(state.get("sample_count", 0))

        for raw in state.get("queue", []):
            task = Task(
                action=raw["action"],
                context=raw.get("context", {}),
                reason=raw.get("reason", "RESUME"),
            )
            if task.action not in self.fetched_actions:
                self.queue.append(task)
                self.queued_actions.add(task.action)

        # In case the old checkpoint had an empty queue but was incomplete.
        if not self.queue and not state.get("complete", False):
            for raw in ROOT_TASKS:
                if raw["action"] not in self.fetched_actions:
                    self.enqueue(Task(**raw))

    def enqueue(self, task: Task) -> None:
        action = (task.action or "").strip()
        if not action:
            return
        if action in self.fetched_actions or action in self.queued_actions:
            return
        self.queue.append(task)
        self.queued_actions.add(action)

    def fetch(self, q: str, limit: int = 1) -> Dict[str, Any]:
        params = {
            "count": "true",
            "q": q,
            "sr": f"|ModifiedDate|0|{limit}",
            "inav": "|Metadata|Sort",
        }
        url = API + "?" + urllib.parse.urlencode(params)
        headers = {
            "User-Agent": USER_AGENT,
            "Accept": "application/json,text/plain,*/*",
            "Referer": "https://m.encar.com/",
            "Origin": "https://m.encar.com",
        }

        last_error: Optional[Exception] = None
        for attempt in range(1, 6):
            try:
                req = urllib.request.Request(url, headers=headers, method="GET")
                with urllib.request.urlopen(req, timeout=35) as resp:
                    status = getattr(resp, "status", 200)
                    body = resp.read().decode("utf-8", errors="replace")
                if status < 200 or status >= 300:
                    raise RuntimeError(f"HTTP {status}: {body[:500]}")
                return json.loads(body)
            except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError,
                    ConnectionError, json.JSONDecodeError, RuntimeError) as exc:
                last_error = exc
                if attempt >= 5:
                    break
                # Backoff; 429/temporary network errors are common in long scans.
                time.sleep(min(20.0, 1.5 * (2 ** (attempt - 1))) + random.uniform(0.0, 0.5))

        assert last_error is not None
        raise last_error

    def emit_hierarchy(
        self,
        node: Dict[str, Any],
        facet: Dict[str, Any],
        context: Dict[str, Any],
        child_context: Dict[str, Any],
        source_action: str,
    ) -> None:
        name = str(node.get("Name", ""))
        action = str(facet.get("Action", ""))
        unique = (name, action)
        if unique in self.emitted_catalog_actions:
            return
        self.emitted_catalog_actions.add(unique)

        record = {
            "record": "hierarchy",
            "schema_version": SCHEMA_VERSION,
            "level": name,
            "value": facet.get("Value"),
            "display_value": facet.get("DisplayValue"),
            "count": facet.get("Count", 0),
            "action": action,
            "expression": facet.get("Expression"),
            "metadata": safe_metadata(facet.get("Metadata", {})),
            "context": child_context,
            "parent_context": context,
            "source_action": source_action,
        }
        append_jsonl(self.catalog_path, record)
        self.hierarchy_counts[name] += 1

    def emit_technical(
        self,
        node: Dict[str, Any],
        facet: Dict[str, Any],
        context: Dict[str, Any],
        source_action: str,
    ) -> None:
        record = {
            "record": "technical_facet",
            "schema_version": SCHEMA_VERSION,
            "node": node.get("Name"),
            "node_display_name": node.get("DisplayName"),
            "node_type": node.get("Type"),
            "multi_select_mode": node.get("MultiSelectMode"),
            "value": facet.get("Value"),
            "display_value": facet.get("DisplayValue"),
            "count": facet.get("Count", 0),
            "action": facet.get("Action"),
            "expression": facet.get("Expression"),
            "metadata": safe_metadata(facet.get("Metadata", {})),
            "context": context,
            "source_action": source_action,
        }
        append_jsonl(self.technical_path, record)
        self.technical_count += 1

    def emit_sample(self, root: Dict[str, Any], context: Dict[str, Any], source_action: str) -> None:
        results = root.get("SearchResults")
        if not isinstance(results, list) or not results:
            return
        car = results[0]
        if not isinstance(car, dict):
            return
        # Preserve the full result object. This is one sample per fetched scope,
        # not a crawl of every ad, so size stays reasonable.
        append_jsonl(
            self.samples_path,
            {
                "record": "sample_car",
                "schema_version": SCHEMA_VERSION,
                "context": context,
                "source_action": source_action,
                "car": car,
            },
        )
        self.sample_count += 1

    def _process_hierarchy_node(
        self,
        node: Dict[str, Any],
        context: Dict[str, Any],
        source_action: str,
    ) -> None:
        node_name = str(node.get("Name", ""))
        field, level = HIERARCHY_LEVELS[node_name]
        facets = node.get("Facets")
        if not isinstance(facets, list):
            return

        depth = deepest_level(context)

        # If this is an ancestor already selected in the task context, follow
        # ONLY the selected facet. This prevents a Sorento request from suddenly
        # branching back into every Kia/Hyundai/Mercedes model again.
        if level <= depth and context.get(field) not in (None, ""):
            selected_value = str(context[field])
            for facet in facets:
                if not isinstance(facet, dict):
                    continue
                if str(facet.get("Value", "")) == selected_value:
                    refinements = facet.get("Refinements")
                    if refinements is not None:
                        self.walk(refinements, context, source_action)
                    return
            return

        # Do not introduce a lower/equal hierarchy level that is not part of
        # the current selected path.
        if level <= depth:
            return

        for facet in facets:
            if not isinstance(facet, dict):
                continue
            value = str(facet.get("Value", "")).strip()
            action = str(facet.get("Action", "")).strip()
            if not value or not action:
                continue

            child = dict(context)
            child[field] = value

            if node_name == "Manufacturer":
                metadata = facet.get("Metadata")
                if isinstance(metadata, dict):
                    eng = metadata.get("EngName")
                    code = metadata.get("Code")
                    if isinstance(eng, list) and eng:
                        child["manufacturer_en"] = eng[0]
                    if isinstance(code, list) and code:
                        child["manufacturer_code"] = code[0]

            self.emit_hierarchy(node, facet, context, child, source_action)

            refinements = facet.get("Refinements")
            deeper_inline = False
            if refinements is not None:
                deeper_inline = has_deeper_hierarchy(refinements, node_name)
                self.walk(refinements, child, source_action)

            # Count==0 is still catalog information, but there is nothing to
            # discover below it from the live inventory.
            try:
                count = int(facet.get("Count", 0) or 0)
            except Exception:
                count = 0

            if count > 0 and expected_deeper_names(node_name) and not deeper_inline:
                self.enqueue(
                    Task(
                        action=action,
                        context=child,
                        reason=f"EXPAND_{node_name}",
                    )
                )

    def _process_technical_node(
        self,
        node: Dict[str, Any],
        context: Dict[str, Any],
        source_action: str,
    ) -> None:
        facets = node.get("Facets")
        if not isinstance(facets, list):
            return
        for facet in facets:
            if isinstance(facet, dict):
                self.emit_technical(node, facet, context, source_action)

    def walk(self, obj: Any, context: Dict[str, Any], source_action: str) -> None:
        if isinstance(obj, list):
            for item in obj:
                self.walk(item, context, source_action)
            return

        if not isinstance(obj, dict):
            return

        if "Name" in obj and "Facets" in obj:
            node_name = str(obj.get("Name", ""))
            if node_name:
                self.node_name_counts[node_name] += 1

            if node_name in HIERARCHY_LEVELS:
                self._process_hierarchy_node(obj, context, source_action)
                # Do NOT generic-recurse this node again; _process_hierarchy_node
                # already follows refinements with the correct child context.
                return

            if node_name in TECHNICAL_NODE_NAMES:
                self._process_technical_node(obj, context, source_action)
                # Side-facet refinements are not part of the vehicle hierarchy.
                return

        # Generic traversal to reach Nodes/iNav containers.
        for value in obj.values():
            if isinstance(value, (dict, list)):
                self.walk(value, context, source_action)

    def process_task(self, task: Task) -> None:
        self.queued_actions.discard(task.action)
        if task.action in self.fetched_actions:
            return

        if self.delay > 0:
            time.sleep(self.delay + random.uniform(0.0, min(0.15, self.delay / 3.0)))

        self.request_count += 1
        try:
            root = self.fetch(task.action, limit=1)
            self.emit_sample(root, task.context, task.action)
            self.walk(root.get("iNav", root), task.context, task.action)
            self.fetched_actions.add(task.action)
        except Exception as exc:
            self.http_error_count += 1
            append_jsonl(
                self.errors_path,
                {
                    "record": "error",
                    "time": now_iso(),
                    "request_number": self.request_count,
                    "action": task.action,
                    "context": task.context,
                    "reason": task.reason,
                    "error": repr(exc),
                },
            )
            # Mark it fetched after the internal five retries. This avoids an
            # infinite loop; errors remain explicit in errors.jsonl.
            self.fetched_actions.add(task.action)

    def should_stop(self) -> Optional[str]:
        if STOP_REQUESTED:
            return "signal"
        if self.request_count >= self.max_requests:
            return "max_requests"
        if time.monotonic() - self.started_monotonic >= self.max_runtime_seconds:
            return "max_runtime"
        return None

    def save_checkpoint(self, force: bool = False, complete: bool = False) -> None:
        if not force and self.request_count % self.checkpoint_every != 0:
            return
        tmp = self.checkpoint_path.with_suffix(".json.tmp")
        state = {
            "schema_version": SCHEMA_VERSION,
            "saved_at": now_iso(),
            "complete": complete,
            "request_count": self.request_count,
            "http_error_count": self.http_error_count,
            "parse_error_count": self.parse_error_count,
            "fetched_actions": sorted(self.fetched_actions),
            "queue": [asdict(task) for task in self.queue],
            "node_name_counts": dict(self.node_name_counts),
            "hierarchy_counts": dict(self.hierarchy_counts),
            "technical_count": self.technical_count,
            "sample_count": self.sample_count,
        }
        with tmp.open("w", encoding="utf-8") as f:
            json.dump(state, f, ensure_ascii=False, separators=(",", ":"))
        os.replace(tmp, self.checkpoint_path)

    def write_manifest(self, complete: bool, stop_reason: Optional[str]) -> None:
        elapsed = time.monotonic() - self.started_monotonic
        manifest = {
            "schema_version": SCHEMA_VERSION,
            "api": API,
            "started_at_this_run": self.started_at,
            "finished_at": now_iso(),
            "complete": complete,
            "stop_reason": stop_reason,
            "requests": self.request_count,
            "http_errors": self.http_error_count,
            "parse_errors": self.parse_error_count,
            "queue_remaining": len(self.queue),
            "elapsed_seconds_this_run": round(elapsed, 2),
            "hierarchy_counts": dict(self.hierarchy_counts),
            "technical_facets": self.technical_count,
            "sample_cars": self.sample_count,
            "node_names_seen": dict(self.node_name_counts),
            "files": {
                "catalog": self.catalog_path.name,
                "technical": self.technical_path.name,
                "samples": self.samples_path.name,
                "errors": self.errors_path.name,
                "checkpoint": self.checkpoint_path.name,
            },
            "search_order": "Manufacturer -> ModelGroup -> Model(generation) -> Year -> Fuel -> Results",
            "technical_depth": "Also captures Grade -> BadgeGroup -> Badge -> BadgeDetail when Encar exposes them.",
        }
        with self.manifest_path.open("w", encoding="utf-8") as f:
            json.dump(manifest, f, ensure_ascii=False, indent=2)
        with self.node_names_path.open("w", encoding="utf-8") as f:
            json.dump(dict(self.node_name_counts), f, ensure_ascii=False, indent=2, sort_keys=True)

    def make_bundle(self) -> None:
        with zipfile.ZipFile(self.bundle_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
            for p in (
                self.catalog_path,
                self.technical_path,
                self.samples_path,
                self.errors_path,
                self.manifest_path,
                self.checkpoint_path,
                self.node_names_path,
            ):
                if p.exists():
                    z.write(p, arcname=p.name)

    def run(self) -> int:
        stop_reason: Optional[str] = None

        while self.queue:
            stop_reason = self.should_stop()
            if stop_reason is not None:
                break

            task = self.queue.popleft()
            self.process_task(task)
            self.save_checkpoint()

            if self.request_count % 25 == 0:
                print(
                    f"requests={self.request_count} queue={len(self.queue)} "
                    f"catalog={sum(self.hierarchy_counts.values())} "
                    f"tech={self.technical_count} errors={self.http_error_count}",
                    flush=True,
                )

        complete = not self.queue and stop_reason is None
        self.save_checkpoint(force=True, complete=complete)
        self.write_manifest(complete=complete, stop_reason=stop_reason)
        self.make_bundle()

        print("=" * 72)
        print(f"COMPLETE={complete}")
        print(f"STOP_REASON={stop_reason}")
        print(f"REQUESTS={self.request_count}")
        print(f"QUEUE_REMAINING={len(self.queue)}")
        print(f"CATALOG_RECORDS={sum(self.hierarchy_counts.values())}")
        print(f"TECHNICAL_RECORDS={self.technical_count}")
        print(f"SAMPLES={self.sample_count}")
        print(f"ERRORS={self.http_error_count}")
        print(f"BUNDLE={self.bundle_path}")
        print("=" * 72)

        # Incomplete is not treated as a hard failure. GitHub will save/cache
        # the checkpoint and a rerun continues automatically.
        return 0


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--out", default="scan_output")
    p.add_argument("--delay", type=float, default=0.55)
    p.add_argument("--max-requests", type=int, default=60000)
    p.add_argument("--max-runtime-minutes", type=float, default=300.0)
    p.add_argument("--checkpoint-every", type=int, default=10)
    p.add_argument("--resume", action="store_true")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    scanner = Scanner(
        out_dir=Path(args.out),
        delay=args.delay,
        max_requests=args.max_requests,
        max_runtime_minutes=args.max_runtime_minutes,
        resume=args.resume,
        checkpoint_every=args.checkpoint_every,
    )
    return scanner.run()


if __name__ == "__main__":
    raise SystemExit(main())
