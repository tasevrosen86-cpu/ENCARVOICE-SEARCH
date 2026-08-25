package com.encarvoicesearch;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String API = "https://api.encar.com/search/car/list/general";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36";

    private static final int DELAY_MS = 400;
    private static final int MAX_API_REQUESTS = 3000;
    private static final int COPY_CHUNK = 45000;

    private static final int LEVEL_BRAND = 0;
    private static final int LEVEL_MODEL_GROUP = 1;
    private static final int LEVEL_MODEL = 2;
    private static final int LEVEL_GRADE = 3;
    private static final int LEVEL_BADGE_GROUP = 4;
    private static final int LEVEL_BADGE = 5;
    private static final int LEVEL_BADGE_DETAIL = 6;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean stopRequested = false;

    private Spinner brandSpinner;
    private TextView status;
    private TextView preview;
    private Button startButton;
    private Button stopButton;
    private Button copyButton;
    private Button resetCopyButton;

    private final StringBuilder out = new StringBuilder();
    private int copyCursor = 0;
    private int apiRequests = 0;
    private int detailRequests = 0;
    private int cheapestOpened = 0;
    private int catalogRecords = 0;
    private int errors = 0;
    private String lastDetailUrl = "";

    private static class BrandSpec {
        final String label;
        final String manufacturer;
        final String carType;

        BrandSpec(String label, String manufacturer, String carType) {
            this.label = label;
            this.manufacturer = manufacturer;
            this.carType = carType;
        }
    }

    private static final BrandSpec[] BRANDS = new BrandSpec[]{
            new BrandSpec("KIA", "기아", "Y"),
            new BrandSpec("HYUNDAI", "현대", "Y"),
            new BrandSpec("MERCEDES", "벤츠", "N"),
            new BrandSpec("BMW", "BMW", "N"),
            new BrandSpec("AUDI", "아우디", "N")
    };

    private static class Task {
        BrandSpec brand;
        String action;
        int level;
        String modelGroup = "";
        String model = "";
        String grade = "";
        String badgeGroup = "";
        String badge = "";
        String badgeDetail = "";

        Task copy() {
            Task t = new Task();
            t.brand = brand;
            t.action = action;
            t.level = level;
            t.modelGroup = modelGroup;
            t.model = model;
            t.grade = grade;
            t.badgeGroup = badgeGroup;
            t.badge = badge;
            t.badgeDetail = badgeDetail;
            return t;
        }
    }

    private static class Facet {
        String value;
        String action;
        int count;
    }

    private static class ApiResponse {
        int http;
        JSONObject json;
        String url;
    }

    private static class DetailResponse {
        int http;
        String finalUrl;
        int bytes;
        String title;
    }

    private static class Car {
        String id;
        String manufacturer;
        String model;
        String badge;
        String fuel;
        String formYear;
        int yearRaw;
        int mileage;
        int priceManWon;
        long priceWon;
        String sellType;
        String region;
        String detailUrl;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("ENCAR DIRECT FULL MAP SCANNER");
        title.setTextSize(20);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Телефон → Encar API → каталог → Price ASC → първа обява → detail page");
        root.addView(info);

        brandSpinner = new Spinner(this);
        String[] options = {"KIA", "HYUNDAI", "MERCEDES", "BMW", "AUDI", "ALL 5"};
        brandSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options
        ));
        root.addView(brandSpinner);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        startButton = new Button(this);
        startButton.setText("START SCAN");
        row1.addView(startButton, new LinearLayout.LayoutParams(0, dp(52), 1));

        stopButton = new Button(this);
        stopButton.setText("STOP");
        stopButton.setEnabled(false);
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(0, dp(52), 1);
        stopLp.setMargins(dp(6), 0, 0, 0);
        row1.addView(stopButton, stopLp);
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        copyButton = new Button(this);
        copyButton.setText("COPY NEXT PART");
        row2.addView(copyButton, new LinearLayout.LayoutParams(0, dp(52), 1));

        resetCopyButton = new Button(this);
        resetCopyButton.setText("COPY FROM START");
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, dp(52), 1);
        resetLp.setMargins(dp(6), 0, 0, 0);
        row2.addView(resetCopyButton, resetLp);
        root.addView(row2);

        status = new TextView(this);
        status.setText("Готов за сканиране.");
        status.setTextSize(14);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status);

        ScrollView scroller = new ScrollView(this);
        preview = new TextView(this);
        preview.setTextSize(11);
        preview.setTextIsSelectable(true);
        preview.setPadding(dp(8), dp(8), dp(8), dp(8));
        scroller.addView(preview);
        root.addView(scroller, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(root);

        startButton.setOnClickListener(v -> startScan());
        stopButton.setOnClickListener(v -> {
            stopRequested = true;
            status.setText("STOP поискан — приключвам текущата заявка...");
        });
        copyButton.setOnClickListener(v -> copyNextPart());
        resetCopyButton.setOnClickListener(v -> {
            copyCursor = 0;
            Toast.makeText(this, "Следващото COPY започва от началото.", Toast.LENGTH_SHORT).show();
        });
    }

    private void startScan() {
        if (!startButton.isEnabled()) return;

        stopRequested = false;
        out.setLength(0);
        copyCursor = 0;
        apiRequests = 0;
        detailRequests = 0;
        cheapestOpened = 0;
        catalogRecords = 0;
        errors = 0;
        lastDetailUrl = "";

        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        preview.setText("");

        int selected = brandSpinner.getSelectedItemPosition();

        executor.submit(() -> {
            log("===== ENCAR DIRECT FULL MAP SCAN =====");
            log("MODE=ANDROID_PHONE_DIRECT");
            log("API=" + API);
            log("DELAY_MS=" + DELAY_MS);
            log("MAX_API_REQUESTS=" + MAX_API_REQUESTS);
            log("SEARCH_END=PRICE_ASC -> FIRST_CAR -> DETAIL_PAGE");
            log("======================================");

            if (selected == 5) {
                for (BrandSpec b : BRANDS) {
                    if (shouldStop()) break;
                    scanBrand(b);
                }
            } else {
                scanBrand(BRANDS[selected]);
            }

            log("");
            log("===== SCAN FINISHED =====");
            log("STOP_REQUESTED=" + stopRequested);
            log("API_REQUESTS=" + apiRequests);
            log("DETAIL_REQUESTS=" + detailRequests);
            log("CATALOG_RECORDS=" + catalogRecords);
            log("CHEAPEST_ADS_OPENED=" + cheapestOpened);
            log("ERRORS=" + errors);
            log("LAST_DETAIL_URL=" + clean(lastDetailUrl));
            log("=========================");

            runOnUiThread(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                status.setText(
                        "Готово. API=" + apiRequests +
                                " | catalog=" + catalogRecords +
                                " | ads=" + cheapestOpened +
                                " | errors=" + errors +
                                "\nНатисни COPY NEXT PART и ми изпрати текста тук."
                );
                refreshPreview(true);
            });
        });
    }

    private void scanBrand(BrandSpec brand) {
        log("");
        log("===== BRAND START|" + brand.label + " =====");

        String rootAction = "(And.Hidden.N._.CarType." + brand.carType +
                "._.Manufacturer." + brand.manufacturer + ".)";

        Task root = new Task();
        root.brand = brand;
        root.action = rootAction;
        root.level = LEVEL_BRAND;

        ArrayDeque<Task> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        Set<String> probedGenerations = new HashSet<>();
        queue.add(root);
        seen.add(rootAction);

        while (!queue.isEmpty() && !shouldStop()) {
            Task task = queue.removeFirst();

            try {
                ApiResponse response = fetchJson(task.action, "ModifiedDate", 0, 1);
                JSONObject json = response.json;
                int total = json.optInt("Count", 0);

                log("SCOPE|BRAND=" + task.brand.label +
                        "|LEVEL=" + levelName(task.level) +
                        context(task) +
                        "|COUNT=" + total +
                        "|HTTP=" + response.http +
                        "|ACTION=" + clean(task.action));

                if (task.level == LEVEL_MODEL || task.level == LEVEL_GRADE) {
                    emitTechnical(task, json);
                }

                String specificNextName = nextImmediateNodeName(task.level);
                JSONObject specificNextNode = specificNextName == null
                        ? null
                        : findNodeRecursive(json.optJSONObject("iNav"), specificNextName);

                if (task.level == LEVEL_MODEL_GROUP && specificNextNode == null) {
                    // Some Encar branches have ModelGroup as the generation scope.
                    String key = task.action;
                    if (!probedGenerations.contains(key)) {
                        probedGenerations.add(key);
                        log("GENERATION_FALLBACK" + context(task) + "|ACTION=" + clean(task.action));
                        probeCheapest(task);
                    }
                }

                if (task.level == LEVEL_MODEL) {
                    String key = task.action;
                    if (!probedGenerations.contains(key)) {
                        probedGenerations.add(key);
                        probeCheapest(task);
                    }
                }

                JSONObject nextNode = findNextHierarchyNode(json, task.level);
                if (nextNode == null) {
                    continue;
                }

                String nodeName = nextNode.optString("Name", "");
                int childLevel = nodeLevel(nodeName);
                JSONArray facets = nextNode.optJSONArray("Facets");
                if (facets == null) continue;

                for (int i = 0; i < facets.length(); i++) {
                    JSONObject f = facets.optJSONObject(i);
                    if (f == null) continue;

                    String value = f.optString("Value", "").trim();
                    String action = f.optString("Action", "").trim();
                    int count = f.optInt("Count", 0);

                    if (value.isEmpty() || action.isEmpty() || count <= 0) continue;
                    if (isPlaceholderValue(value, nodeName)) continue;

                    Task child = makeChild(task, childLevel, value, action);
                    catalogRecords++;

                    log("CATALOG|BRAND=" + task.brand.label +
                            context(child) +
                            "|FOUND_TYPE=" + clean(nodeName) +
                            "|VALUE=" + clean(value) +
                            "|COUNT=" + count +
                            "|ACTION=" + clean(action));

                    if (seen.add(action)) {
                        queue.addLast(child);
                    }
                }

            } catch (Exception e) {
                errors++;
                log("ERROR|BRAND=" + task.brand.label +
                        context(task) +
                        "|LEVEL=" + levelName(task.level) +
                        "|MESSAGE=" + clean(e.toString()) +
                        "|ACTION=" + clean(task.action));
            }
        }

        log("===== BRAND END|" + brand.label +
                "|QUEUE_LEFT=" + queue.size() +
                "|STOP=" + stopRequested + " =====");
    }

    private void probeCheapest(Task task) {
        if (shouldStop()) return;

        try {
            String priceAction = addFilters(task.action, "SellType.일반");
            ApiResponse response = fetchJson(priceAction, "Price", 0, 5);
            JSONArray arr = response.json.optJSONArray("SearchResults");

            if (arr == null || arr.length() == 0) {
                log("PRICE_RESULT" + context(task) +
                        "|COUNT=0|ACTION=" + clean(priceAction));
                return;
            }

            List<Car> cars = new ArrayList<>();
            boolean apiAscending = true;
            int previous = Integer.MIN_VALUE;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Car c = parseCar(o);
                if (c == null || c.id.isEmpty() || c.priceManWon <= 0) continue;

                if (previous != Integer.MIN_VALUE && c.priceManWon < previous) {
                    apiAscending = false;
                }
                previous = c.priceManWon;
                cars.add(c);

                log("PRICE_TOP|BRAND=" + task.brand.label +
                        context(task) +
                        "|POS=" + (i + 1) +
                        "|ID=" + clean(c.id) +
                        "|PRICE_MANWON=" + c.priceManWon +
                        "|PRICE_KRW=" + c.priceWon +
                        "|YEAR=" + clean(c.formYear) +
                        "|MILEAGE=" + c.mileage +
                        "|FUEL=" + clean(c.fuel) +
                        "|BADGE=" + clean(c.badge));
            }

            if (cars.isEmpty()) {
                log("PRICE_RESULT" + context(task) + "|VALID_CARS=0");
                return;
            }

            Collections.sort(cars, Comparator.comparingInt((Car a) -> a.priceManWon));
            Car first = cars.get(0);

            log("PRICE_CHECK|BRAND=" + task.brand.label +
                    context(task) +
                    "|API_ASCENDING=" + apiAscending +
                    "|RETURNED=" + cars.size() +
                    "|SORT=Price|START=0|LIMIT=5");

            log("CHEAPEST|BRAND=" + task.brand.label +
                    context(task) +
                    "|ID=" + clean(first.id) +
                    "|PRICE_MANWON=" + first.priceManWon +
                    "|PRICE_KRW=" + first.priceWon +
                    "|MODEL=" + clean(first.model) +
                    "|BADGE=" + clean(first.badge) +
                    "|FUEL=" + clean(first.fuel) +
                    "|YEAR=" + clean(first.formYear) +
                    "|MILEAGE=" + first.mileage +
                    "|URL=" + clean(first.detailUrl));

            DetailResponse detail = openDetail(first.detailUrl);
            lastDetailUrl = detail.finalUrl;
            if (detail.http >= 200 && detail.http < 400) {
                cheapestOpened++;
            }

            log("DETAIL_OPEN|BRAND=" + task.brand.label +
                    context(task) +
                    "|ID=" + clean(first.id) +
                    "|HTTP=" + detail.http +
                    "|BYTES=" + detail.bytes +
                    "|TITLE=" + clean(detail.title) +
                    "|FINAL_URL=" + clean(detail.finalUrl));

        } catch (Exception e) {
            errors++;
            log("PRICE_OR_DETAIL_ERROR|BRAND=" + task.brand.label +
                    context(task) +
                    "|MESSAGE=" + clean(e.toString()));
        }
    }

    private void emitTechnical(Task task, JSONObject json) {
        String[] names = {
                "Year", "FormYear", "FuelType", "SellType", "GreenType",
                "Transmission", "DriveType", "Displacement", "EngineDisplacement",
                "SeatingCapacity", "Seating", "BodyType", "VehicleType",
                "Category", "ModelCarType", "AttributeType"
        };

        for (String name : names) {
            JSONObject node = findNodeRecursive(json.optJSONObject("iNav"), name);
            if (node == null) continue;

            JSONArray facets = node.optJSONArray("Facets");
            if (facets == null) continue;

            for (int i = 0; i < facets.length(); i++) {
                JSONObject f = facets.optJSONObject(i);
                if (f == null) continue;
                String value = f.optString("Value", "").trim();
                String action = f.optString("Action", "").trim();
                int count = f.optInt("Count", 0);
                if (value.isEmpty() || count <= 0) continue;

                log("TECH|BRAND=" + task.brand.label +
                        context(task) +
                        "|TYPE=" + clean(name) +
                        "|VALUE=" + clean(value) +
                        "|COUNT=" + count +
                        "|ACTION=" + clean(action));
            }
        }
    }

    private ApiResponse fetchJson(String action, String sort, int start, int limit) throws Exception {
        if (apiRequests >= MAX_API_REQUESTS) {
            stopRequested = true;
            throw new IllegalStateException("MAX_API_REQUESTS reached");
        }
        if (stopRequested) throw new InterruptedException("STOP requested");

        String query = "count=true" +
                "&q=" + enc(action) +
                "&sr=" + enc("|" + sort + "|" + start + "|" + limit) +
                "&inav=" + enc("|Metadata|Sort");

        String url = API + "?" + query;
        Exception last = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            if (stopRequested) throw new InterruptedException("STOP requested");
            apiRequests++;

            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setUseCaches(false);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", USER_AGENT);
                c.setRequestProperty("Accept", "application/json,text/plain,*/*");
                c.setRequestProperty("Referer", "https://m.encar.com/");
                c.setRequestProperty("Origin", "https://m.encar.com");
                c.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");

                int code = c.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                        ? c.getInputStream()
                        : c.getErrorStream();
                String body = readText(stream, 6_000_000);

                if (code < 200 || code >= 300) {
                    throw new RuntimeException("HTTP " + code + " body=" + clean(shorten(body, 500)));
                }

                ApiResponse r = new ApiResponse();
                r.http = code;
                r.json = new JSONObject(body);
                r.url = url;

                sleepDelay();
                updateStatus();
                return r;

            } catch (Exception e) {
                last = e;
                if (attempt < 3) {
                    Thread.sleep(900L * attempt);
                }
            } finally {
                if (c != null) c.disconnect();
            }
        }

        throw last == null ? new RuntimeException("Unknown API error") : last;
    }

    private DetailResponse openDetail(String detailUrl) throws Exception {
        detailRequests++;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(detailUrl).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setInstanceFollowRedirects(true);
            c.setUseCaches(false);
            c.setRequestProperty("User-Agent", USER_AGENT);
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8");
            c.setRequestProperty("Referer", "https://car.encar.com/");
            c.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");

            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 400
                    ? c.getInputStream()
                    : c.getErrorStream();
            byte[] data = readBytes(stream, 900_000);
            String html = new String(data, StandardCharsets.UTF_8);

            DetailResponse r = new DetailResponse();
            r.http = code;
            r.finalUrl = c.getURL().toString();
            r.bytes = data.length;
            r.title = extractTitle(html);
            Thread.sleep(250);
            return r;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private Car parseCar(JSONObject o) {
        String id = o.optString("Id", "").trim();
        if (id.isEmpty()) return null;

        Car c = new Car();
        c.id = id;
        c.manufacturer = o.optString("Manufacturer", "");
        c.model = o.optString("Model", "");
        c.badge = o.optString("Badge", "");
        c.fuel = o.optString("FuelType", "");
        c.formYear = o.optString("FormYear", "");
        c.yearRaw = (int) Math.round(o.optDouble("Year", 0));
        c.mileage = (int) Math.round(o.optDouble("Mileage", 0));
        c.priceManWon = (int) Math.round(o.optDouble("Price", 0));
        c.priceWon = c.priceManWon * 10000L;
        c.sellType = o.optString("SellType", "");
        c.region = o.optString("OfficeCityState", "");
        c.detailUrl = "https://car.encar.com/cars/detail/" + id;
        return c;
    }

    private JSONObject findNextHierarchyNode(JSONObject json, int currentLevel) {
        String[] names = {"ModelGroup", "Model", "Grade", "BadgeGroup", "Badge", "BadgeDetail"};
        for (String name : names) {
            int level = nodeLevel(name);
            if (level <= currentLevel) continue;
            JSONObject n = findNodeRecursive(json.optJSONObject("iNav"), name);
            if (n != null && hasUsableFacet(n, name)) return n;
        }
        return null;
    }

    private boolean hasUsableFacet(JSONObject node, String nodeName) {
        JSONArray facets = node.optJSONArray("Facets");
        if (facets == null) return false;
        for (int i = 0; i < facets.length(); i++) {
            JSONObject f = facets.optJSONObject(i);
            if (f == null) continue;
            String value = f.optString("Value", "").trim();
            String action = f.optString("Action", "").trim();
            int count = f.optInt("Count", 0);
            if (!value.isEmpty() && !action.isEmpty() && count > 0 && !isPlaceholderValue(value, nodeName)) {
                return true;
            }
        }
        return false;
    }

    private JSONObject findNodeRecursive(Object obj, String wantedName) {
        if (obj == null) return null;
        if (obj instanceof JSONObject) {
            JSONObject jo = (JSONObject) obj;
            if (wantedName.equals(jo.optString("Name", ""))) {
                return jo;
            }
            JSONArray names = jo.names();
            if (names == null) return null;
            for (int i = 0; i < names.length(); i++) {
                String key = names.optString(i);
                Object child = jo.opt(key);
                if (child instanceof JSONObject || child instanceof JSONArray) {
                    JSONObject found = findNodeRecursive(child, wantedName);
                    if (found != null) return found;
                }
            }
        } else if (obj instanceof JSONArray) {
            JSONArray ja = (JSONArray) obj;
            for (int i = 0; i < ja.length(); i++) {
                Object child = ja.opt(i);
                if (child instanceof JSONObject || child instanceof JSONArray) {
                    JSONObject found = findNodeRecursive(child, wantedName);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private Task makeChild(Task parent, int childLevel, String value, String action) {
        Task child = parent.copy();
        child.level = childLevel;
        child.action = action;

        switch (childLevel) {
            case LEVEL_MODEL_GROUP:
                child.modelGroup = value;
                child.model = "";
                child.grade = "";
                child.badgeGroup = "";
                child.badge = "";
                child.badgeDetail = "";
                break;
            case LEVEL_MODEL:
                child.model = value;
                child.grade = "";
                child.badgeGroup = "";
                child.badge = "";
                child.badgeDetail = "";
                break;
            case LEVEL_GRADE:
                child.grade = value;
                child.badgeGroup = "";
                child.badge = "";
                child.badgeDetail = "";
                break;
            case LEVEL_BADGE_GROUP:
                child.badgeGroup = value;
                child.badge = "";
                child.badgeDetail = "";
                break;
            case LEVEL_BADGE:
                child.badge = value;
                child.badgeDetail = "";
                break;
            case LEVEL_BADGE_DETAIL:
                child.badgeDetail = value;
                break;
        }
        return child;
    }

    private String context(Task t) {
        return "|MANUFACTURER=" + clean(t.brand.manufacturer) +
                "|MODEL_GROUP=" + clean(t.modelGroup) +
                "|MODEL=" + clean(t.model) +
                "|GRADE=" + clean(t.grade) +
                "|BADGE_GROUP=" + clean(t.badgeGroup) +
                "|BADGE=" + clean(t.badge) +
                "|BADGE_DETAIL=" + clean(t.badgeDetail);
    }

    private String nextImmediateNodeName(int level) {
        switch (level) {
            case LEVEL_BRAND: return "ModelGroup";
            case LEVEL_MODEL_GROUP: return "Model";
            case LEVEL_MODEL: return "Grade";
            case LEVEL_GRADE: return "BadgeGroup";
            case LEVEL_BADGE_GROUP: return "Badge";
            case LEVEL_BADGE: return "BadgeDetail";
            default: return null;
        }
    }

    private int nodeLevel(String name) {
        if ("ModelGroup".equals(name)) return LEVEL_MODEL_GROUP;
        if ("Model".equals(name)) return LEVEL_MODEL;
        if ("Grade".equals(name)) return LEVEL_GRADE;
        if ("BadgeGroup".equals(name)) return LEVEL_BADGE_GROUP;
        if ("Badge".equals(name)) return LEVEL_BADGE;
        if ("BadgeDetail".equals(name)) return LEVEL_BADGE_DETAIL;
        return -1;
    }

    private String levelName(int level) {
        switch (level) {
            case LEVEL_BRAND: return "BRAND";
            case LEVEL_MODEL_GROUP: return "MODEL_GROUP";
            case LEVEL_MODEL: return "MODEL";
            case LEVEL_GRADE: return "GRADE";
            case LEVEL_BADGE_GROUP: return "BADGE_GROUP";
            case LEVEL_BADGE: return "BADGE";
            case LEVEL_BADGE_DETAIL: return "BADGE_DETAIL";
            default: return "UNKNOWN";
        }
    }

    private boolean isPlaceholderValue(String value, String nodeName) {
        String v = value.trim();
        if (v.isEmpty()) return true;
        if ("ModelGroup".equals(nodeName) && ("모델그룹".equals(v) || "ModelGroup".equalsIgnoreCase(v))) return true;
        if ("Model".equals(nodeName) && "모델".equals(v)) return true;
        if ("Grade".equals(nodeName) && "등급".equals(v)) return true;
        if ("BadgeGroup".equals(nodeName) && "세부등급".equals(v)) return true;
        return false;
    }

    private String addFilters(String baseAction, String... filters) {
        int close = baseAction.lastIndexOf(')');
        if (close < 0) return baseAction;

        String prefix = baseAction.substring(0, close);
        String suffix = baseAction.substring(close);
        StringBuilder added = new StringBuilder();

        for (String f : filters) {
            String x = f == null ? "" : f.trim();
            while (x.startsWith(".")) x = x.substring(1);
            while (x.endsWith(".")) x = x.substring(0, x.length() - 1);
            if (!x.isEmpty()) {
                added.append("_.").append(x).append(".");
            }
        }
        return prefix + added + suffix;
    }

    private boolean shouldStop() {
        if (stopRequested) return true;
        if (apiRequests >= MAX_API_REQUESTS) {
            stopRequested = true;
            log("STOP|REASON=MAX_API_REQUESTS");
            return true;
        }
        return false;
    }

    private void sleepDelay() throws InterruptedException {
        Thread.sleep(DELAY_MS);
    }

    private String enc(String s) throws Exception {
        return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
    }

    private String readText(InputStream in, int maxBytes) throws Exception {
        if (in == null) return "";
        byte[] data = readBytes(in, maxBytes);
        return new String(data, StandardCharsets.UTF_8);
    }

    private byte[] readBytes(InputStream in, int maxBytes) throws Exception {
        if (in == null) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            int allowed = Math.min(n, maxBytes - total);
            if (allowed > 0) out.write(buffer, 0, allowed);
            total += allowed;
            if (total >= maxBytes) break;
        }
        in.close();
        return out.toByteArray();
    }

    private String extractTitle(String html) {
        if (html == null || html.isEmpty()) return "";
        Matcher m = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);
        if (!m.find()) return "";
        return m.group(1).replaceAll("\\s+", " ").trim();
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("|", "/")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private synchronized void log(String line) {
        out.append(line == null ? "" : line).append('\n');
        if ((catalogRecords + apiRequests + detailRequests) % 20 == 0) {
            runOnUiThread(() -> refreshPreview(false));
        }
    }

    private void updateStatus() {
        runOnUiThread(() -> status.setText(
                "Сканирам... API=" + apiRequests +
                        " | catalog=" + catalogRecords +
                        " | detail=" + detailRequests +
                        " | errors=" + errors
        ));
    }

    private void refreshPreview(boolean force) {
        String text;
        synchronized (this) {
            text = out.toString();
        }
        int keep = force ? 60000 : 18000;
        if (text.length() > keep) {
            text = "... LAST " + keep + " CHARS ...\n" + text.substring(text.length() - keep);
        }
        preview.setText(text);
    }

    private void copyNextPart() {
        String text;
        synchronized (this) {
            text = out.toString();
        }

        if (text.isEmpty()) {
            Toast.makeText(this, "Още няма резултат.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (copyCursor >= text.length()) {
            Toast.makeText(this, "Копирано е всичко. Натисни COPY FROM START за начало.", Toast.LENGTH_LONG).show();
            return;
        }

        int end = Math.min(copyCursor + COPY_CHUNK, text.length());
        if (end < text.length()) {
            int newline = text.lastIndexOf('\n', end);
            if (newline > copyCursor + 1000) end = newline + 1;
        }

        String chunk = text.substring(copyCursor, end);
        int partNumber = copyCursor / COPY_CHUNK + 1;

        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("ENCAR_SCAN_PART_" + partNumber, chunk));

        copyCursor = end;
        Toast.makeText(
                this,
                "Копирана част " + partNumber + " — " + chunk.length() + " знака. Постави я в ChatGPT.",
                Toast.LENGTH_LONG
        ).show();
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(value * d);
    }

    @Override
    protected void onDestroy() {
        stopRequested = true;
        executor.shutdownNow();
        super.onDestroy();
    }
}
