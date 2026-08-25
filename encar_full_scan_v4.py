#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ENCAR FULL MAP SCANNER v4

What it maps
------------
CarType -> Manufacturer -> ModelGroup -> Model(generation) -> Grade ->
BadgeGroup -> Badge -> BadgeDetail -> RESULTS sorted by PRICE -> FIRST AD ->
real Encar detail page.

Important behavior
------------------
* Discovers brands/models dynamically from Encar. Nothing is hardcoded.
* Walks the real iNav Action chain and stores exact Action values.
* At every terminal vehicle branch it requests the result list sorted by price,
  takes the first (cheapest) result, records its carId and opens the real ad.
* Never reports complete=True if requests failed or if no real catalog/results
  were discovered.
* Failed catalog/probe tasks are saved in checkpoint.json and retried on resume.
* By default ignores HTTP_PROXY/HTTPS_PROXY environment variables. Use
  --use-env-proxy only when your runtime genuinely requires them.

Output
------
  catalog.jsonl       hierarchy/action map
  technical.jsonl     Year/Fuel/SellType/etc. facets
  samples.jsonl       one raw SearchResult sample per catalog request
  first_ads.jsonl     Price-sorted terminal result + first opened ad metadata
  errors.jsonl        catalog/probe errors
  checkpoint.json     automatic resume state
  manifest.json       final summary / validation
  node_names.json     all iNav node names seen
  detail_pages/*.html optional, only with --save-detail-html
  ENCAR_FULL_MAP.zip  bundle of the scan
"""

from __future__ import annotations

import argparse
import collections
import datetime as dt
import hashlib
import html as html_lib
import json
import os
import random
import re
import signal
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Deque, Dict, Iterable, List, Optional, Set, Tuple

API = "https://api.encar.com/search/car/list/general"
MODERN_DETAIL = "https://car.encar.com/cars/detail/{car_id}"
LEGACY_DETAIL = "https://www.encar.com/dc/dc_cardetailview.do?carid={car_id}"
USER_AGENT = (
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
    "Chrome/126 Mobile Safari/537.36"
)
SCHEMA_VERSION = 4

HIERARCHY_LEVELS: Dict[str, Tuple[str, int]] = {
    "Manufacturer": ("manufacturer", 1),
    "ModelGroup": ("model_group", 2),
    "Model": ("model", 3),
    "Grade": ("grade", 4),
    "BadgeGroup": ("badge_group", 5),
    "Badge": ("badge", 6),
    "BadgeDetail": ("badge_detail", 7),
}

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


def int_or_zero(value: Any) -> int:
    try:
        return int(value or 0)
    except Exception:
        return 0


def result_price(result: Dict[str, Any]) -> float:
    try:
        return float(result.get("Price", 0) or 0)
    except Exception:
        return 0.0


def result_id(result: Dict[str, Any]) -> str:
    return str(result.get("Id", "") or "").strip()


def preview_car(result: Dict[str, Any]) -> Dict[str, Any]:
    car_id = result_id(result)
    return {
        "id": car_id,
        "price_manwon": result.get("Price"),
        "price_won": int(result_price(result) * 10_000) if result_price(result) > 0 else 0,
        "manufacturer": result.get("Manufacturer"),
        "model_group": result.get("ModelGroup"),
        "model": result.get("Model"),
        "grade": result.get("Grade"),
        "badge": result.get("Badge"),
        "badge_detail": result.get("BadgeDetail"),
        "form_year": result.get("FormYear"),
        "year": result.get("Year"),
        "mileage": result.get("Mileage"),
        "fuel_type": result.get("FuelType"),
        "sell_type": result.get("SellType"),
        "detail_url": MODERN_DETAIL.format(car_id=car_id) if car_id else None,
    }


def extract_html_map(html: str) -> Dict[str, Any]:
    title = None
    m = re.search(r"<title[^>]*>(.*?)</title>", html, flags=re.I | re.S)
    if m:
        title = html_lib.unescape(re.sub(r"\s+", " ", m.group(1)).strip())

    script_srcs: List[str] = []
    for src in re.findall(r"<script[^>]+src=[\"']([^\"']+)[\"']", html, flags=re.I):
        if src not in script_srcs:
            script_srcs.append(src)
        if len(script_srcs) >= 100:
            break

    jsonld: List[Any] = []
    for raw in re.findall(
        r"<script[^>]+type=[\"']application/ld\+json[\"'][^>]*>(.*?)</script>",
        html,
        flags=re.I | re.S,
    ):
        try:
            jsonld.append(json.loads(html_lib.unescape(raw).strip()))
        except Exception:
            continue
        if len(jsonld) >= 10:
            break

    interesting_urls: List[str] = []
    for u in re.findall(r"https?://[^\"'<>\\\s]+", html):
        if any(k in u.lower() for k in ("encar", "api", "car", "vehicle", "detail")):
            clean = html_lib.unescape(u)
            if clean not in interesting_urls:
                interesting_urls.append(clean)
        if len(interesting_urls) >= 100:
            break

    return {
        "title": title,
        "script_srcs": script_srcs,
        "jsonld": safe_metadata(jsonld),
        "interesting_urls": interesting_urls,
    }


@dataclass
class Task:
    action: str
    context: Dict[str, Any]
    reason: str


@dataclass
class ProbeTask:
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
        use_env_proxy: bool,
        probe_preview_count: int,
        save_detail_html: bool,
        max_detail_bytes: int,
    ) -> None:
        self.out_dir = out_dir
        self.out_dir.mkdir(parents=True, exist_ok=True)
        self.delay = max(0.0, delay)
        self.max_requests = max(1, max_requests)
        self.max_runtime_seconds = max_runtime_minutes * 60.0
        self.checkpoint_every = max(1, checkpoint_every)
        self.use_env_proxy = use_env_proxy
        self.probe_preview_count = max(1, min(20, probe_preview_count))
        self.save_detail_html = save_detail_html
        self.max_detail_bytes = max(100_000, max_detail_bytes)
        self.started_monotonic = time.monotonic()
        self.started_at = now_iso()

        self.catalog_path = self.out_dir / "catalog.jsonl"
        self.technical_path = self.out_dir / "technical.jsonl"
        self.samples_path = self.out_dir / "samples.jsonl"
        self.first_ads_path = self.out_dir / "first_ads.jsonl"
        self.errors_path = self.out_dir / "errors.jsonl"
        self.checkpoint_path = self.out_dir / "checkpoint.json"
        self.manifest_path = self.out_dir / "manifest.json"
        self.node_names_path = self.out_dir / "node_names.json"
        self.detail_pages_dir = self.out_dir / "detail_pages"
        self.bundle_path = self.out_dir / "ENCAR_FULL_MAP.zip"

        if self.use_env_proxy:
            self.opener = urllib.request.build_opener()
        else:
            self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

        self.queue: Deque[Task] = collections.deque()
        self.probe_queue: Deque[ProbeTask] = collections.deque()
        self.queued_actions: Set[str] = set()
        self.queued_probe_actions: Set[str] = set()
        self.fetched_actions: Set[str] = set()
        self.probed_actions: Set[str] = set()
        self.failed_tasks: List[Task] = []
        self.failed_probes: List[ProbeTask] = []
        self.emitted_catalog_actions: Set[Tuple[str, str]] = set()
        self.node_name_counts: collections.Counter[str] = collections.Counter()
        self.hierarchy_counts: collections.Counter[str] = collections.Counter()
        self.request_count = 0
        self.http_error_count = 0
        self.parse_error_count = 0
        self.technical_count = 0
        self.sample_count = 0
        self.first_ad_count = 0
        self.detail_open_count = 0
        self.detail_reused_count = 0
        self.opened_car_ids: Dict[str, Dict[str, Any]] = {}

        # Per-catalog-task state used to detect true terminal branches.
        self._task_start_depth = 0
        self._task_max_hierarchy_level = 0

        if resume and self.checkpoint_path.exists():
            self._load_checkpoint()
        else:
            self._fresh_start()

    def _fresh_start(self) -> None:
        for p in (
            self.catalog_path,
            self.technical_path,
            self.samples_path,
            self.first_ads_path,
            self.errors_path,
            self.manifest_path,
            self.node_names_path,
            self.bundle_path,
            self.checkpoint_path,
        ):
            if p.exists():
                p.unlink()

        if self.detail_pages_dir.exists():
            for p in self.detail_pages_dir.glob("*.html"):
                p.unlink()
        self.detail_pages_dir.mkdir(parents=True, exist_ok=True)

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
                    "PriceAsc",
                    "FirstAd",
                    "DetailPage",
                ],
                "note": (
                    "Technical Year/Fuel/SellType/etc. facets are recorded at each scope. "
                    "Terminal branches are probed with sr=|Price|0|N, then the first ad is opened."
                ),
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
        self.probed_actions = set(state.get("probed_actions", []))
        self.node_name_counts.update(state.get("node_name_counts", {}))
        self.hierarchy_counts.update(state.get("hierarchy_counts", {}))
        self.technical_count = int(state.get("technical_count", 0))
        self.sample_count = int(state.get("sample_count", 0))
        self.first_ad_count = int(state.get("first_ad_count", 0))
        self.detail_open_count = int(state.get("detail_open_count", 0))
        self.detail_reused_count = int(state.get("detail_reused_count", 0))
        self.opened_car_ids = dict(state.get("opened_car_ids", {}))

        # Normal pending catalog tasks.
        for raw in state.get("queue", []):
            task = Task(
                action=raw["action"],
                context=raw.get("context", {}),
                reason=raw.get("reason", "RESUME"),
            )
            self.enqueue(task)

        # Retry catalog failures from the previous run.
        for raw in state.get("failed_tasks", []):
            task = Task(
                action=raw["action"],
                context=raw.get("context", {}),
                reason="RETRY_FAILED_" + raw.get("reason", "TASK"),
            )
            self.enqueue(task)

        # Normal pending terminal probes.
        for raw in state.get("probe_queue", []):
            probe = ProbeTask(
                action=raw["action"],
                context=raw.get("context", {}),
                reason=raw.get("reason", "RESUME_PROBE"),
            )
            self.schedule_probe(probe)

        # Retry probe failures from previous run.
        for raw in state.get("failed_probes", []):
            probe = ProbeTask(
                action=raw["action"],
                context=raw.get("context", {}),
                reason="RETRY_FAILED_" + raw.get("reason", "PROBE"),
            )
            self.schedule_probe(probe)

        # Old/bad checkpoints sometimes had nothing pending while incomplete.
        if not self.queue and not self.probe_queue and not state.get("complete", False):
            for raw in ROOT_TASKS:
                if raw["action"] not in self.fetched_actions:
                    self.enqueue(Task(**raw))

        # Failures have been converted back into pending work for this run.
        self.failed_tasks = []
        self.failed_probes = []

    def enqueue(self, task: Task) -> None:
        action = (task.action or "").strip()
        if not action:
            return
        if action in self.fetched_actions or action in self.queued_actions:
            return
        self.queue.append(task)
        self.queued_actions.add(action)

    def schedule_probe(self, probe: ProbeTask) -> None:
        action = (probe.action or "").strip()
        if not action:
            return
        # ModelGroup can be the generation when Encar has no separate Model node.
        if deepest_level(probe.context) < 2:
            return
        if action in self.probed_actions or action in self.queued_probe_actions:
            return
        self.probe_queue.append(probe)
        self.queued_probe_actions.add(action)

    def _sleep_before_request(self) -> None:
        if self.delay > 0:
            time.sleep(self.delay + random.uniform(0.0, min(0.15, self.delay / 3.0)))

    def _check_network_budget(self) -> None:
        if self.request_count >= self.max_requests:
            raise RuntimeError("REQUEST_BUDGET_EXHAUSTED")
        if time.monotonic() - self.started_monotonic >= self.max_runtime_seconds:
            raise RuntimeError("RUNTIME_BUDGET_EXHAUSTED")
        if STOP_REQUESTED:
            raise RuntimeError("STOP_REQUESTED")

    def _open_request(
        self,
        url: str,
        headers: Dict[str, str],
        timeout: int,
        max_bytes: Optional[int] = None,
    ) -> Tuple[int, str, bytes, Dict[str, str]]:
        last_error: Optional[Exception] = None

        for attempt in range(1, 6):
            self._check_network_budget()
            self._sleep_before_request()
            self.request_count += 1
            try:
                req = urllib.request.Request(url, headers=headers, method="GET")
                with self.opener.open(req, timeout=timeout) as resp:
                    status = int(getattr(resp, "status", 200))
                    final_url = str(resp.geturl())
                    if max_bytes is None:
                        body = resp.read()
                    else:
                        body = resp.read(max_bytes + 1)
                    response_headers = {str(k): str(v) for k, v in resp.headers.items()}

                if status < 200 or status >= 300:
                    raise RuntimeError(f"HTTP {status}: {body[:500]!r}")
                return status, final_url, body, response_headers

            except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError,
                    ConnectionError, RuntimeError) as exc:
                last_error = exc
                text = repr(exc)

                # 407 is not a transient Encar error. Retrying it five times only
                # wastes time, so stop immediately and keep the task failed.
                if "407" in text or "REQUEST_BUDGET_EXHAUSTED" in text or \
                        "RUNTIME_BUDGET_EXHAUSTED" in text or "STOP_REQUESTED" in text:
                    break

                if attempt >= 5:
                    break
                time.sleep(min(20.0, 1.5 * (2 ** (attempt - 1))) + random.uniform(0.0, 0.5))

        assert last_error is not None
        raise last_error

    def build_api_url(
        self,
        q: str,
        sort_field: str = "ModifiedDate",
        start: int = 0,
        limit: int = 1,
    ) -> str:
        params = {
            "count": "true",
            "q": q,
            "sr": f"|{sort_field}|{start}|{limit}",
            "inav": "|Metadata|Sort",
        }
        return API + "?" + urllib.parse.urlencode(params)

    def fetch_json(
        self,
        q: str,
        sort_field: str = "ModifiedDate",
        start: int = 0,
        limit: int = 1,
    ) -> Tuple[Dict[str, Any], str]:
        url = self.build_api_url(q, sort_field=sort_field, start=start, limit=limit)
        headers = {
            "User-Agent": USER_AGENT,
            "Accept": "application/json,text/plain,*/*",
            "Referer": "https://m.encar.com/",
            "Origin": "https://m.encar.com",
            "Accept-Language": "ko-KR,ko;q=0.9,en;q=0.7",
        }
        _, _, body, _ = self._open_request(url, headers, timeout=35)
        try:
            return json.loads(body.decode("utf-8", errors="replace")), url
        except json.JSONDecodeError:
            self.parse_error_count += 1
            raise

    def fetch_detail(self, car_id: str) -> Dict[str, Any]:
        if car_id in self.opened_car_ids:
            self.detail_reused_count += 1
            cached = dict(self.opened_car_ids[car_id])
            cached["reused"] = True
            return cached

        headers = {
            "User-Agent": USER_AGENT,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Referer": "https://car.encar.com/",
            "Accept-Language": "ko-KR,ko;q=0.9,en;q=0.7",
        }

        errors: List[str] = []
        for template in (MODERN_DETAIL, LEGACY_DETAIL):
            url = template.format(car_id=car_id)
            try:
                status, final_url, body, resp_headers = self._open_request(
                    url,
                    headers,
                    timeout=35,
                    max_bytes=self.max_detail_bytes,
                )
                truncated = len(body) > self.max_detail_bytes
                if truncated:
                    body = body[: self.max_detail_bytes]
                text = body.decode("utf-8", errors="replace")
                mapped = extract_html_map(text)
                record = {
                    "opened": True,
                    "reused": False,
                    "requested_url": url,
                    "final_url": final_url,
                    "status": status,
                    "content_type": resp_headers.get("Content-Type"),
                    "bytes_captured": len(body),
                    "truncated": truncated,
                    "sha256": hashlib.sha256(body).hexdigest(),
                    **mapped,
                }

                if self.save_detail_html:
                    detail_path = self.detail_pages_dir / f"{car_id}.html"
                    detail_path.write_bytes(body)
                    record["saved_html"] = str(detail_path.relative_to(self.out_dir))

                self.detail_open_count += 1
                self.opened_car_ids[car_id] = dict(record)
                return record
            except Exception as exc:
                errors.append(f"{url}: {repr(exc)}")

        raise RuntimeError("DETAIL_OPEN_FAILED | " + " | ".join(errors))

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

        append_jsonl(
            self.catalog_path,
            {
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
            },
        )
        self.hierarchy_counts[name] += 1

    def emit_technical(
        self,
        node: Dict[str, Any],
        facet: Dict[str, Any],
        context: Dict[str, Any],
        source_action: str,
    ) -> None:
        append_jsonl(
            self.technical_path,
            {
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
            },
        )
        self.technical_count += 1

    def emit_sample(
        self,
        root: Dict[str, Any],
        context: Dict[str, Any],
        source_action: str,
    ) -> None:
        results = root.get("SearchResults")
        if not isinstance(results, list) or not results:
            return
        car = results[0]
        if not isinstance(car, dict):
            return
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
        self._task_max_hierarchy_level = max(self._task_max_hierarchy_level, level)

        facets = node.get("Facets")
        if not isinstance(facets, list):
            return

        depth = deepest_level(context)

        # For an ancestor already selected in the task context, follow only the
        # selected facet. This prevents branching back to unrelated brands/models.
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

            count = int_or_zero(facet.get("Count", 0))
            if count <= 0:
                continue

            deeper_names = expected_deeper_names(node_name)
            if not deeper_names:
                # True BadgeDetail leaf: next step is PriceAsc -> first ad.
                self.schedule_probe(
                    ProbeTask(
                        action=action,
                        context=child,
                        reason=f"TERMINAL_{node_name}",
                    )
                )
            elif not deeper_inline:
                # Fetch this exact branch to discover the next hierarchy level.
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
                return

            if node_name in TECHNICAL_NODE_NAMES:
                self._process_technical_node(obj, context, source_action)
                return

        for value in obj.values():
            if isinstance(value, (dict, list)):
                self.walk(value, context, source_action)

    def process_catalog_task(self, task: Task) -> None:
        self.queued_actions.discard(task.action)
        if task.action in self.fetched_actions:
            return

        self._task_start_depth = deepest_level(task.context)
        self._task_max_hierarchy_level = self._task_start_depth

        try:
            root, _ = self.fetch_json(task.action, sort_field="ModifiedDate", start=0, limit=1)
            self.emit_sample(root, task.context, task.action)
            self.walk(root.get("iNav", root), task.context, task.action)

            # If a selected ModelGroup/Model/Grade/etc. opens and Encar exposes
            # no deeper hierarchy at all, that selected action itself is the leaf.
            if self._task_start_depth >= 2 and self._task_max_hierarchy_level <= self._task_start_depth:
                count = int_or_zero(root.get("Count", 0))
                if count > 0:
                    self.schedule_probe(
                        ProbeTask(
                            action=task.action,
                            context=task.context,
                            reason="NO_DEEPER_HIERARCHY",
                        )
                    )

            self.fetched_actions.add(task.action)

        except Exception as exc:
            self.http_error_count += 1
            self.failed_tasks.append(task)
            append_jsonl(
                self.errors_path,
                {
                    "record": "catalog_error",
                    "time": now_iso(),
                    "request_number": self.request_count,
                    "action": task.action,
                    "context": task.context,
                    "reason": task.reason,
                    "error": repr(exc),
                },
            )
            # DO NOT mark fetched. It must be retried on --resume.

    def _sorted_price_response(
        self,
        action: str,
    ) -> Tuple[Dict[str, Any], str, str, bool, List[Dict[str, Any]]]:
        last_root: Optional[Dict[str, Any]] = None
        last_url = ""
        last_sort = "Price"
        last_preview: List[Dict[str, Any]] = []

        # Price is the known Encar API sort field. The fallbacks are used only
        # if a response is visibly not ascending, making the scanner self-auditing.
        for sort_field in ("Price", "MobilePriceAsc", "PriceAsc"):
            root, api_url = self.fetch_json(
                action,
                sort_field=sort_field,
                start=0,
                limit=self.probe_preview_count,
            )
            results = root.get("SearchResults")
            if not isinstance(results, list):
                results = []
            dict_results = [r for r in results if isinstance(r, dict)]
            preview = [preview_car(r) for r in dict_results]
            prices = [result_price(r) for r in dict_results if result_price(r) > 0]
            ascending = len(prices) <= 1 or all(a <= b for a, b in zip(prices, prices[1:]))

            last_root = root
            last_url = api_url
            last_sort = sort_field
            last_preview = preview

            if ascending:
                return root, api_url, sort_field, True, preview

        assert last_root is not None
        return last_root, last_url, last_sort, False, last_preview

    def process_probe(self, probe: ProbeTask) -> None:
        self.queued_probe_actions.discard(probe.action)
        if probe.action in self.probed_actions:
            return

        try:
            root, api_url, sort_field, sort_verified, preview = self._sorted_price_response(
                probe.action
            )
            results = root.get("SearchResults")
            if not isinstance(results, list):
                results = []
            results = [r for r in results if isinstance(r, dict)]

            if not results:
                append_jsonl(
                    self.first_ads_path,
                    {
                        "record": "terminal_no_results",
                        "schema_version": SCHEMA_VERSION,
                        "time": now_iso(),
                        "context": probe.context,
                        "terminal_action": probe.action,
                        "reason": probe.reason,
                        "sort_field": sort_field,
                        "sort_verified_ascending": sort_verified,
                        "api_url": api_url,
                        "count": int_or_zero(root.get("Count", 0)),
                        "results_preview": preview,
                    },
                )
                self.probed_actions.add(probe.action)
                return

            first = results[0]

            # If the API ever returns a non-ascending batch despite the requested
            # sort, keep the anomaly visible. We still open the first result from
            # the requested sorted list, exactly matching the user's requirement.
            car_id = result_id(first)
            if not car_id:
                raise RuntimeError("FIRST_RESULT_HAS_NO_CAR_ID")

            detail = self.fetch_detail(car_id)

            append_jsonl(
                self.first_ads_path,
                {
                    "record": "first_cheapest_opened",
                    "schema_version": SCHEMA_VERSION,
                    "time": now_iso(),
                    "context": probe.context,
                    "terminal_action": probe.action,
                    "reason": probe.reason,
                    "sort": {
                        "requested": "PRICE_ASC",
                        "api_sort_field_used": sort_field,
                        "start": 0,
                        "limit": self.probe_preview_count,
                        "verified_non_decreasing_in_preview": sort_verified,
                        "api_url": api_url,
                    },
                    "total_results": int_or_zero(root.get("Count", 0)),
                    "results_preview": preview,
                    "first_ad": {
                        "car_id": car_id,
                        "detail_url": MODERN_DETAIL.format(car_id=car_id),
                        "price_manwon": first.get("Price"),
                        "price_won": int(result_price(first) * 10_000) if result_price(first) > 0 else 0,
                        "raw_search_result": first,
                    },
                    "opened_detail": detail,
                },
            )
            self.first_ad_count += 1
            self.probed_actions.add(probe.action)

        except Exception as exc:
            self.http_error_count += 1
            self.failed_probes.append(probe)
            append_jsonl(
                self.errors_path,
                {
                    "record": "probe_error",
                    "time": now_iso(),
                    "request_number": self.request_count,
                    "action": probe.action,
                    "context": probe.context,
                    "reason": probe.reason,
                    "error": repr(exc),
                },
            )
            # DO NOT mark probed. It must be retried on --resume.

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
            "probed_actions": sorted(self.probed_actions),
            "queue": [asdict(task) for task in self.queue],
            "probe_queue": [asdict(probe) for probe in self.probe_queue],
            "failed_tasks": [asdict(task) for task in self.failed_tasks],
            "failed_probes": [asdict(probe) for probe in self.failed_probes],
            "node_name_counts": dict(self.node_name_counts),
            "hierarchy_counts": dict(self.hierarchy_counts),
            "technical_count": self.technical_count,
            "sample_count": self.sample_count,
            "first_ad_count": self.first_ad_count,
            "detail_open_count": self.detail_open_count,
            "detail_reused_count": self.detail_reused_count,
            "opened_car_ids": self.opened_car_ids,
        }
        with tmp.open("w", encoding="utf-8") as f:
            json.dump(state, f, ensure_ascii=False, separators=(",", ":"))
        os.replace(tmp, self.checkpoint_path)

    def validation(self) -> Dict[str, Any]:
        manufacturer_ok = self.hierarchy_counts.get("Manufacturer", 0) > 0
        model_group_ok = self.hierarchy_counts.get("ModelGroup", 0) > 0
        opened_ad_ok = self.first_ad_count > 0
        no_pending = not self.queue and not self.probe_queue
        no_failures = not self.failed_tasks and not self.failed_probes
        return {
            "manufacturer_found": manufacturer_ok,
            "model_group_found": model_group_ok,
            "at_least_one_price_sorted_first_ad_opened": opened_ad_ok,
            "no_pending_tasks": no_pending,
            "no_failed_tasks": no_failures,
            "valid_complete_scan": (
                manufacturer_ok and model_group_ok and opened_ad_ok and no_pending and no_failures
            ),
        }

    def write_manifest(self, complete: bool, stop_reason: Optional[str]) -> None:
        elapsed = time.monotonic() - self.started_monotonic
        manifest = {
            "schema_version": SCHEMA_VERSION,
            "api": API,
            "started_at_this_run": self.started_at,
            "finished_at": now_iso(),
            "complete": complete,
            "stop_reason": stop_reason,
            "validation": self.validation(),
            "network_requests": self.request_count,
            "http_errors": self.http_error_count,
            "parse_errors": self.parse_error_count,
            "catalog_queue_remaining": len(self.queue),
            "probe_queue_remaining": len(self.probe_queue),
            "failed_catalog_tasks": len(self.failed_tasks),
            "failed_probe_tasks": len(self.failed_probes),
            "elapsed_seconds_this_run": round(elapsed, 2),
            "hierarchy_counts": dict(self.hierarchy_counts),
            "technical_facets": self.technical_count,
            "sample_cars": self.sample_count,
            "first_cheapest_ads_opened": self.first_ad_count,
            "unique_detail_pages_opened": self.detail_open_count,
            "detail_reuses": self.detail_reused_count,
            "node_names_seen": dict(self.node_name_counts),
            "proxy_mode": "environment" if self.use_env_proxy else "direct_no_env_proxy",
            "files": {
                "catalog": self.catalog_path.name,
                "technical": self.technical_path.name,
                "samples": self.samples_path.name,
                "first_ads": self.first_ads_path.name,
                "errors": self.errors_path.name,
                "checkpoint": self.checkpoint_path.name,
                "node_names": self.node_names_path.name,
            },
            "search_order": (
                "Manufacturer -> ModelGroup -> Model(generation) -> Grade -> BadgeGroup -> "
                "Badge -> BadgeDetail -> PriceAsc -> FirstAd -> DetailPage"
            ),
        }
        with self.manifest_path.open("w", encoding="utf-8") as f:
            json.dump(manifest, f, ensure_ascii=False, indent=2)
        with self.node_names_path.open("w", encoding="utf-8") as f:
            json.dump(dict(self.node_name_counts), f, ensure_ascii=False, indent=2, sort_keys=True)

    def make_bundle(self) -> None:
        with zipfile.ZipFile(
            self.bundle_path,
            "w",
            compression=zipfile.ZIP_DEFLATED,
            compresslevel=9,
        ) as z:
            for p in (
                self.catalog_path,
                self.technical_path,
                self.samples_path,
                self.first_ads_path,
                self.errors_path,
                self.manifest_path,
                self.checkpoint_path,
                self.node_names_path,
            ):
                if p.exists():
                    z.write(p, arcname=p.name)

            if self.save_detail_html and self.detail_pages_dir.exists():
                for p in sorted(self.detail_pages_dir.glob("*.html")):
                    z.write(p, arcname=str(p.relative_to(self.out_dir)))

    def print_progress(self) -> None:
        print(
            f"requests={self.request_count} "
            f"catalogQ={len(self.queue)} probeQ={len(self.probe_queue)} "
            f"catalog={sum(self.hierarchy_counts.values())} "
            f"firstAds={self.first_ad_count} details={self.detail_open_count} "
            f"errors={self.http_error_count}",
            flush=True,
        )

    def run(self) -> int:
        stop_reason: Optional[str] = None
        last_progress_bucket = -1

        # 1) Build the full hierarchy/action map.
        while self.queue:
            stop_reason = self.should_stop()
            if stop_reason is not None:
                break
            task = self.queue.popleft()
            self.process_catalog_task(task)
            self.save_checkpoint()

            bucket = self.request_count // 25
            if bucket != last_progress_bucket:
                last_progress_bucket = bucket
                self.print_progress()

        # 2) For every terminal branch: Price Asc -> first ad -> open detail.
        if stop_reason is None:
            while self.probe_queue:
                stop_reason = self.should_stop()
                if stop_reason is not None:
                    break
                probe = self.probe_queue.popleft()
                self.process_probe(probe)
                self.save_checkpoint()

                bucket = self.request_count // 25
                if bucket != last_progress_bucket:
                    last_progress_bucket = bucket
                    self.print_progress()

        validation = self.validation()
        complete = bool(stop_reason is None and validation["valid_complete_scan"])
        if stop_reason is None and not complete:
            stop_reason = "errors_or_validation_failed"

        self.save_checkpoint(force=True, complete=complete)
        self.write_manifest(complete=complete, stop_reason=stop_reason)
        self.make_bundle()

        print("=" * 78)
        print(f"COMPLETE={complete}")
        print(f"STOP_REASON={stop_reason}")
        print(f"NETWORK_REQUESTS={self.request_count}")
        print(f"CATALOG_QUEUE_REMAINING={len(self.queue)}")
        print(f"PROBE_QUEUE_REMAINING={len(self.probe_queue)}")
        print(f"FAILED_CATALOG_TASKS={len(self.failed_tasks)}")
        print(f"FAILED_PROBES={len(self.failed_probes)}")
        print(f"CATALOG_RECORDS={sum(self.hierarchy_counts.values())}")
        print(f"TECHNICAL_RECORDS={self.technical_count}")
        print(f"FIRST_CHEAPEST_ADS_OPENED={self.first_ad_count}")
        print(f"UNIQUE_DETAIL_PAGES_OPENED={self.detail_open_count}")
        print(f"ERRORS={self.http_error_count}")
        print(f"BUNDLE={self.bundle_path}")
        print("=" * 78)

        # Return 0 so CI can still upload the ZIP/checkpoint even when the run is
        # incomplete. The manifest/COMPLETE flag is the source of truth.
        return 0


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--out", default="scan_output")
    p.add_argument("--delay", type=float, default=0.45)
    p.add_argument("--max-requests", type=int, default=60000)
    p.add_argument("--max-runtime-minutes", type=float, default=300.0)
    p.add_argument("--checkpoint-every", type=int, default=10)
    p.add_argument("--resume", action="store_true")
    p.add_argument(
        "--use-env-proxy",
        action="store_true",
        help="Use HTTP_PROXY/HTTPS_PROXY from the environment. Default: bypass them.",
    )
    p.add_argument(
        "--probe-preview-count",
        type=int,
        default=5,
        help="How many price-sorted results to inspect before opening the first one.",
    )
    p.add_argument(
        "--save-detail-html",
        action="store_true",
        help="Also save captured detail HTML files. Off by default to keep the ZIP small.",
    )
    p.add_argument(
        "--max-detail-bytes",
        type=int,
        default=2_000_000,
        help="Maximum HTML bytes captured per unique detail page.",
    )
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
        use_env_proxy=args.use_env_proxy,
        probe_preview_count=args.probe_preview_count,
        save_detail_html=args.save_detail_html,
        max_detail_bytes=args.max_detail_bytes,
    )
    return scanner.run()


if __name__ == "__main__":
    raise SystemExit(main())

