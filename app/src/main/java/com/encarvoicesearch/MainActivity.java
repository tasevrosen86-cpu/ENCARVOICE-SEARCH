package com.encarvoicesearch;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;
    private static final int MAX_READ_ATTEMPTS = 12;

    private WebView webView;
    private TextView status;
    private EditText searchInput;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int readAttempts = 0;
    private String lastResult = "";
    private String lastCarUrl = "";

    private static class CarSearchSpec {
        String displayName;
        String manufacturer;
        String carType;
        String modelGroup;
        String exactModel;

        CarSearchSpec(String displayName, String manufacturer, String carType,
                      String modelGroup, String exactModel) {
            this.displayName = displayName;
            this.manufacturer = manufacturer;
            this.carType = carType;
            this.modelGroup = modelGroup;
            this.exactModel = exactModel;
        }
    }

    // row = display model, Encar ModelGroup, exact Model (optional), aliases separated by |
    private static final String[][] KIA_MODELS = {
            {"Bongo", "봉고III 미니버스", "", "bongo"},
            {"Enterprise", "엔터프라이즈", "", "enterprise"},
            {"Sportage", "스포티지", "", "sportage|спортидж|спортаж|спортейдж"},
            {"Telluride", "텔루라이드", "", "telluride|телурайд"},
            {"Carnival", "카니발", "", "carnival|karnival|карнивал"},
            {"Potentia", "포텐샤", "", "potentia"},
            {"Parktown", "파크타운", "", "parktown"},
            {"Fiat 132", "피아트132", "", "fiat 132"},
            {"Sorento", "쏘렌토", "더 뉴 쏘렌토 4세대", "sorento|соренто"},
            {"Picanto", "모닝", "", "picanto|morning|пиканто"},
            {"Stinger", "스팅어", "", "stinger|стингер"},
            {"Capital", "캐피탈", "", "capital"},
            {"Carstar", "카스타", "", "carstar"},
            {"Rocksta", "록스타", "", "rocksta"},
            {"Spectra", "스펙트라", "", "spectra"},
            {"Concord", "콩코드", "", "concord"},
            {"Mohave", "모하비", "", "mohave|mohavi|мохаве"},
            {"Seltos", "셀토스", "", "seltos|селтос"},
            {"Stonic", "스토닉", "", "stonic|стоник"},
            {"Tasman", "타스만", "", "tasman|тасман"},
            {"Carens", "카렌스", "", "carens|каренс"},
            {"Opirus", "오피러스", "", "opirus|опирус"},
            {"Sephia", "세피아", "", "sephia"},
            {"Avella", "아벨라", "", "avella"},
            {"Credos", "크레도스", "", "credos"},
            {"Towner", "타우너", "", "towner"},
            {"Optima", "옵티마", "", "optima"},
            {"Cerato", "쎄라토", "", "cerato"},
            {"Pregio", "프레지오", "", "pregio"},
            {"Retona", "레토나", "", "ретона|retona"},
            {"X Trek", "X-TREK", "", "x trek"},
            {"Pride", "프라이드", "", "pride"},
            {"Forte", "포르테", "", "forte|форте"},
            {"Lotze", "로체", "", "lotze"},
            {"Regal", "리갈", "", "regal"},
            {"Visto", "비스토", "", "visto"},
            {"Besta", "베스타", "", "besta"},
            {"Topic", "토픽", "", "topic"},
            {"Shuma", "슈마", "", "shuma"},
            {"Brisa", "브리샤", "", "brisa"},
            {"Delta", "델타", "", "delta"},
            {"CEED", "씨드", "", "ceed|cee d"},
            {"NIRO", "니로", "", "niro|ниро"},
            {"SOUL", "쏘울", "", "soul|соул"},
            {"ELAN", "엘란", "", "elan"},
            {"RAY", "레이", "", "ray|рей"},
            {"EV6", "EV6", "", ""},
            {"EV3", "EV3", "", ""},
            {"EV9", "EV9", "", ""},
            {"EV5", "EV5", "", ""},
            {"EV4", "EV4", "", ""},
            {"PV5", "PV5", "", ""},
            {"RIO", "리오", "", "rio|рио"},
            {"K5", "K5", "", ""},
            {"K7", "K7", "", ""},
            {"K3", "K3", "", ""},
            {"K8", "K8", "", ""},
            {"K9", "K9", "", ""}
    };

    private static final String[][] HYUNDAI_MODELS = {
            {"Elantra Old", "엘란트라", "", "elantra old|old elantra"},
            {"Maxcruz", "맥스크루즈", "", "maxcruz|maxcruise"},
            {"Trajet Xg", "트라제 XG", "", "trajet xg"},
            {"Grandeur", "그랜저", "", "grandeur|azera|грандер"},
            {"Santa Fe", "싼타페", "", "santa fe|santafe|санта фе|сантафе"},
            {"Palisade", "팰리세이드", "", "palisade|палисейд|палисад"},
            {"Veloster", "벨로스터", "", "veloster|велостер"},
            {"Veracruz", "베라크루즈", "", "veracruz|веракруз"},
            {"Galloper", "갤로퍼", "", "galloper|галопер"},
            {"Terracan", "테라칸", "", "terracan|теракан"},
            {"Avante", "아반떼", "", "avante|elantra|елантра"},
            {"Starex", "스타렉스", "", "starex|старекс"},
            {"Genesis", "제네시스", "", "genesis|генезис"},
            {"Ioniq 5", "아이오닉5", "", "ioniq 5|ioniq5"},
            {"Ioniq 6", "아이오닉6", "", "ioniq 6|ioniq6"},
            {"Ioniq 9", "아이오닉9", "", "ioniq 9|ioniq9"},
            {"Tuscani", "투스카니", "", "tuscani"},
            {"Dynasty", "다이너스티", "", "dynasty"},
            {"Tiburon", "티뷰론", "", "tiburon"},
            {"Santamo", "산타모", "", "santamo"},
            {"Stellar", "스텔라", "", "stellar"},
            {"Cortina", "코티나", "", "cortina"},
            {"Granada", "그라나다", "", "granada"},
            {"Sonata", "쏘나타", "", "sonata|соната"},
            {"Tucson", "투싼", "", "tucson|tuson|туксон|тусон"},
            {"Staria", "스타리아", "", "staria|стария"},
            {"Casper", "캐스퍼", "", "casper|каспер"},
            {"Accent", "엑센트", "", "accent|акцент"},
            {"Solati", "쏠라티", "", "solati"},
            {"Marcia", "마르샤", "", "marcia"},
            {"Scoupe", "스쿠프", "", "scoupe"},
            {"Presto", "프레스토", "", "presto"},
            {"Lavita", "라비타", "", "lavita"},
            {"Blueon", "블루온", "", "blueon"},
            {"Equus", "에쿠스", "", "equus|екуус"},
            {"Venue", "베뉴", "", "venue|веню"},
            {"NEXO", "넥쏘", "", "nexo|нексо"},
            {"Ioniq", "아이오닉", "", "ioniq"},
            {"Aslan", "아슬란", "", "aslan"},
            {"Verna", "베르나", "", "verna"},
            {"Excel", "엑셀", "", "excel"},
            {"Grace", "그레이스", "", "grace"},
            {"Click", "클릭", "", "click"},
            {"KONA", "코나", "", "kona|кона"},
            {"PONY", "포니", "", "pony"},
            {"ATOS", "아토스", "", "atos"},
            {"i30", "i30", "", ""},
            {"i40", "i40", "", ""},
            {"ST1", "ST1", "", ""}
    };

    private static final String[][] MERCEDES_MODELS = {
            {"190", "190-클래스", "", "190|190 class"},
            {"Sprinter", "스프린터", "", "sprinter|спринтер"},
            {"E Class", "E-클래스", "", "e class|e-class"},
            {"S Class", "S-클래스", "", "s class|s-class"},
            {"GLC", "GLC-클래스", "", "glc|глц"},
            {"GLE", "GLE-클래스", "", "gle|гле"},
            {"C Class", "C-클래스", "", "c class|c-class"},
            {"CLS", "CLS-클래스", "", "cls"},
            {"A Class", "A-클래스", "", "a class|a-class"},
            {"GLB", "GLB-클래스", "", "glb"},
            {"G Class", "G-클래스", "", "g class|g-class"},
            {"CLA", "CLA-클래스", "", "cla"},
            {"GLS", "GLS-클래스", "", "gls"},
            {"GLA", "GLA-클래스", "", "gla"},
            {"CLE", "CLE-클래스", "", "cle"},
            {"B Class", "B-클래스", "", "b class|b-class"},
            {"GLK", "GLK-클래스", "", "glk"},
            {"M Class", "M-클래스", "", "m class|m-class"},
            {"SLK", "SLK-클래스", "", "slk"},
            {"SLC", "SLC-클래스", "", "slc"},
            {"V Class", "V-클래스", "", "v class|v-class"},
            {"SEL", "SEL/SEC", "", "sel|sec"},
            {"CLK", "CLK-클래스", "", "clk"},
            {"Sls Amg", "SLS AMG", "", ""},
            {"R Class", "R-클래스", "", "r class|r-class"},
            {"Amg Gt", "AMG GT", "", ""},
            {"SL", "SL-클래스", "", "sl"},
            {"CL", "CL-클래스", "", "cl"},
            {"GL", "GL-클래스", "", "gl"},
            {"Other", "기타", "", "other"},
            {"EQS", "EQS", "", ""},
            {"EQE", "EQE", "", ""},
            {"EQB", "EQB", "", ""},
            {"EQA", "EQA", "", ""},
            {"EQC", "EQC", "", ""},
            {"SLR", "SLR", "", ""}
    };

    private static final String[][] BMW_MODELS = {
            {"GT", "그란투리스모 (GT_", "", "gt|gran turismo|grand turismo"},
            {"M Coupe", "M 쿠페/로드스터", "", "m coupe|m roadster"},
            {"5 Series", "5시리즈", "", "5 series|5series"},
            {"3 Series", "3시리즈", "", "3 series|3series"},
            {"7 Series", "7시리즈", "", "7 series|7series"},
            {"1 Series", "1시리즈", "", "1 series|1series"},
            {"4 Series", "4시리즈", "", "4 series|4series"},
            {"2 Series", "2시리즈", "", "2 series|2series"},
            {"8 Series", "8시리즈", "", "8 series|8series"},
            {"6 Series", "6시리즈", "", "6 series|6series"},
            {"Other", "기타", "", "other"},
            {"iX3", "iX3", "", ""},
            {"X6M", "X6M", "", ""},
            {"X5M", "X5M", "", ""},
            {"X4M", "X4M", "", ""},
            {"X3M", "X3M", "", ""},
            {"iX2", "iX2", "", ""},
            {"iX1", "iX1", "", ""},
            {"X5", "X5", "", ""},
            {"X6", "X6", "", ""},
            {"X3", "X3", "", ""},
            {"X7", "X7", "", ""},
            {"X4", "X4", "", ""},
            {"X1", "X1", "", ""},
            {"Z4", "Z4", "", ""},
            {"i4", "i4", "", ""},
            {"M2", "M2", "", ""},
            {"X2", "X2", "", ""},
            {"M4", "M4", "", ""},
            {"M3", "M3", "", ""},
            {"M5", "M5", "", ""},
            {"i5", "i5", "", ""},
            {"i7", "i7", "", ""},
            {"XM", "XM", "", ""},
            {"iX", "iX", "", ""},
            {"i3", "i3", "", ""},
            {"i8", "i8", "", ""},
            {"M8", "M8", "", ""},
            {"M6", "M6", "", ""},
            {"1M", "1M", "", ""},
            {"Z3", "Z3", "", ""},
            {"Z8", "Z8", "", ""}
    };

    private static final String[][] AUDI_MODELS = {
            {"Allroad Quattro", "올로드 콰트로", "", "allroad quattro"},
            {"RS e-tron GT", "RS e-트론 GT", "", "rs e-tron gt|rs etron gt"},
            {"S e-tron GT", "S e-트론 GT", "", "s e-tron gt|s etron gt"},
            {"SQ6 e-tron", "SQ6 e-트론", "", "sq6 e-tron|sq6 etron"},
            {"SQ8 e-tron", "SQ8 e-트론", "", "sq8 e-tron|sq8 etron"},
            {"Q4 e-tron", "Q4 e-트론", "", "q4 e-tron|q4 etron"},
            {"A6 e-tron", "A6 e-트론", "", "a6 e-tron|a6 etron"},
            {"e-tron GT", "e-트론 GT", "", "e-tron gt|etron gt"},
            {"Q6 e-tron", "Q6 e-트론", "", "q6 e-tron|q6 etron"},
            {"Q8 e-tron", "Q8 e-트론", "", "q8 e-tron|q8 etron"},
            {"S6 e-tron", "S6 e-트론", "", "s6 e-tron|s6 etron"},
            {"e-tron", "e-트론", "", "e-tron|etron"},
            {"Other", "기타", "", "other"},
            {"RSQ8", "RSQ8", "", ""},
            {"TTRS", "TTRS", "", ""},
            {"SQ5", "SQ5", "", ""},
            {"RS7", "RS7", "", ""},
            {"RS3", "RS3", "", ""},
            {"RS5", "RS5", "", ""},
            {"SQ7", "SQ7", "", ""},
            {"RS6", "RS6", "", ""},
            {"TTS", "TTS", "", ""},
            {"RS4", "RS4", "", ""},
            {"SQ8", "SQ8", "", ""},
            {"A6", "A6", "", ""},
            {"Q5", "Q5", "", ""},
            {"Q7", "Q7", "", ""},
            {"A7", "A7", "", ""},
            {"A4", "A4", "", ""},
            {"A5", "A5", "", ""},
            {"A8", "A8", "", ""},
            {"Q3", "Q3", "", ""},
            {"Q8", "Q8", "", ""},
            {"A3", "A3", "", ""},
            {"R8", "R8", "", ""},
            {"Q2", "Q2", "", ""},
            {"S8", "S8", "", ""},
            {"TT", "TT", "", ""},
            {"S4", "S4", "", ""},
            {"S7", "S7", "", ""},
            {"S5", "S5", "", ""},
            {"S6", "S6", "", ""},
            {"A1", "A1", "", ""},
            {"S3", "S3", "", ""},
            {"V8", "V8", "", ""}
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        searchInput = new EditText(this);
        searchInput.setHint("Kia Sorento 2025 дизел");
        searchInput.setTextSize(18);
        searchInput.setSingleLine(false);
        searchInput.setMinLines(2);
        searchInput.setPadding(20, 15, 20, 15);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button voiceButton = new Button(this);
        voiceButton.setText("🎤 ГЛАС");

        Button searchButton = new Button(this);
        searchButton.setText("🔎 ТЪРСИ");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        buttons.addView(voiceButton, buttonParams);
        buttons.addView(searchButton, buttonParams);

        status = new TextView(this);
        status.setText("Готово");
        status.setTextSize(14);
        status.setPadding(20, 15, 20, 15);
        status.setTextIsSelectable(true);

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);

        Button readButton = new Button(this);
        readButton.setText("ПЪРВА ОБЯВА");

        Button openButton = new Button(this);
        openButton.setText("ОТВОРИ");

        Button copyButton = new Button(this);
        copyButton.setText("КОПИРАЙ");

        LinearLayout.LayoutParams toolParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tools.addView(readButton, toolParams);
        tools.addView(openButton, toolParams);
        tools.addView(copyButton, toolParams);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new CarReader(), "AndroidCarReader");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url != null && url.contains("car.encar.com/list/car")) {
                    status.setText("Резултатите се зареждат...");
                    handler.postDelayed(() -> startReading(), 1800);
                }
            }
        });

        root.addView(searchInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(buttons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(status,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(tools,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1));

        setContentView(root);

        voiceButton.setOnClickListener(v -> startVoice());
        searchButton.setOnClickListener(v -> searchFromInput());
        readButton.setOnClickListener(v -> startReading());
        openButton.setOnClickListener(v -> openFirstCar());
        copyButton.setOnClickListener(v -> copyResult());
    }

    private void startVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "bg-BG");

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Кажи марка, модел, година и гориво");

        try {
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    this,
                    "Няма гласово разпознаване",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (results != null && !results.isEmpty()) {
                String text = results.get(0);
                text = text.replaceAll("\\b20\\s+(\\d{2})\\b", "20$1");

                searchInput.setText(text);
                searchInput.setSelection(searchInput.getText().length());
            }
        }
    }

    private void searchFromInput() {
        String original = searchInput.getText().toString().trim();

        if (original.isEmpty()) {
            status.setText("Кажи или напиши автомобил.");
            return;
        }

        String text = original
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        CarSearchSpec car = detectCar(text);

        if (car == null) {
            status.setText(
                    "Не разпознах модела.\n" +
                    "Сканирани марки:\n" +
                    "• Kia\n" +
                    "• Hyundai\n" +
                    "• Mercedes\n" +
                    "• BMW\n" +
                    "• Audi"
            );
            return;
        }

        Integer year = findYear(text);

        if (year == null) {
            status.setText("Не разпознах годината.");
            return;
        }

        String fuelKorean;
        String fuelName;

        if (text.contains("дизел") || text.contains("diesel")) {
            fuelKorean = "디젤";
            fuelName = "DIESEL";

        } else if (
                text.contains("бензин") ||
                text.contains("gasoline") ||
                text.contains("petrol")
        ) {
            fuelKorean = "가솔린";
            fuelName = "GASOLINE";

        } else if (
                text.contains("хибрид") ||
                text.contains("hybrid")
        ) {
            fuelKorean = "가솔린+전기";
            fuelName = "HYBRID";

        } else {
            status.setText(
                    "Не разпознах горивото.\n" +
                    "Кажи дизел, бензин или хибрид."
            );
            return;
        }

        searchCar(car, year, fuelKorean, fuelName);
    }

    private CarSearchSpec detectCar(String text) {
        if (containsAnyTerm(text, "kia", "киа", "кия")) {
            return findModelInTable(text, "Kia", "기아", "Y", KIA_MODELS);
        }

        if (containsAnyTerm(
                text,
                "hyundai",
                "хюндай",
                "хундай",
                "хендай",
                "хюнде"
        )) {
            return findModelInTable(text, "Hyundai", "현대", "Y", HYUNDAI_MODELS);
        }

        if (containsAnyTerm(
                text,
                "mercedes-benz",
                "mercedes",
                "benz",
                "мерцедес",
                "мерседес"
        )) {
            return findModelInTable(text, "Mercedes", "벤츠", "N", MERCEDES_MODELS);
        }

        if (containsAnyTerm(text, "bmw", "бмв")) {
            return findModelInTable(text, "BMW", "BMW", "N", BMW_MODELS);
        }

        if (containsAnyTerm(text, "audi", "ауди")) {
            return findModelInTable(text, "Audi", "아우디", "N", AUDI_MODELS);
        }

        return null;
    }

    private CarSearchSpec findModelInTable(
            String text,
            String brandDisplay,
            String manufacturer,
            String carType,
            String[][] table
    ) {
        for (String[] row : table) {
            String displayModel = row[0];
            String modelGroup = row[1];
            String exactModel = row[2];
            String aliases = row[3];

            boolean matched = containsTerm(text, modelGroup);

            if (!matched && aliases != null && !aliases.isEmpty()) {
                matched = containsAnyTerm(text, aliases.split("\\|"));
            }

            if (matched) {
                return new CarSearchSpec(
                        brandDisplay + " " + displayModel,
                        manufacturer,
                        carType,
                        modelGroup,
                        exactModel.isEmpty() ? null : exactModel
                );
            }
        }

        return null;
    }

    private boolean containsAnyTerm(String text, String... terms) {
        for (String term : terms) {
            if (containsTerm(text, term)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTerm(String text, String term) {
        if (term == null || term.trim().isEmpty()) {
            return false;
        }

        String normalizedText = normalizeForMatch(text);
        String normalizedTerm = normalizeForMatch(term);

        return (" " + normalizedText + " ")
                .contains(" " + normalizedTerm + " ");
    }

    private String normalizeForMatch(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Integer findYear(String text) {
        text = text.replaceAll("\\b20\\s+(\\d{2})\\b", "20$1");

        Matcher full =
                Pattern.compile("\\b(20\\d{2})\\b").matcher(text);

        if (full.find()) {
            try {
                return Integer.parseInt(full.group(1));
            } catch (Exception ignored) {
            }
        }

        Matcher shortYear =
                Pattern.compile(
                        "\\b(1[5-9]|2[0-6])\\s*(?:г|година|год)?\\b"
                ).matcher(text);

        if (shortYear.find()) {
            try {
                return 2000 + Integer.parseInt(shortYear.group(1));
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private void searchCar(
            CarSearchSpec car,
            int year,
            String fuel,
            String fuelName
    ) {
        int yearFrom = year * 100;
        int yearTo = yearFrom + 99;

        StringBuilder action = new StringBuilder();

        action.append("(And.Year.range(");
        action.append(yearFrom);
        action.append("..");
        action.append(yearTo);
        action.append(").");

        action.append("_.Hidden.N.");
        action.append("_.(Or.Separation.F._.Separation.B.)");
        action.append("_.SellType.일반.");

        action.append("_.(C.CarType.");
        action.append(car.carType);
        action.append(".");

        action.append("_.(C.Manufacturer.");
        action.append(car.manufacturer);
        action.append(".");

        action.append("_.(C.ModelGroup.");
        action.append(car.modelGroup);
        action.append(".");

        // Запазваме точно работещия exact Model за Sorento.
        // За останалите модели не гадаем поколението.
        if (car.exactModel != null && !car.exactModel.isEmpty()) {
            action.append("_.Model.");
            action.append(car.exactModel);
            action.append(".");
        }

        action.append(")");
        action.append(")");
        action.append(")");

        action.append("_.FuelType.");
        action.append(fuel);
        action.append(".");
        action.append(")");

        String json =
                "{" +
                "\"type\":\"car\"," +
                "\"action\":\"" + action + "\"," +
                "\"toggle\":{}," +
                "\"layer\":\"\"," +
                "\"sort\":\"MobilePriceAsc\"" +
                "}";

        try {
            String encoded =
                    URLEncoder.encode(
                            json,
                            StandardCharsets.UTF_8.toString()
                    );

            String url =
                    "https://car.encar.com/list/car?page=1&search=" +
                    encoded;

            lastResult = "";
            lastCarUrl = "";

            status.setText(
                    "Търся " +
                    car.displayName +
                    " " +
                    year +
                    " " +
                    fuelName +
                    "\nНАЙ-НИСКА ЦЕНА ПЪРВО"
            );

            webView.loadUrl(url);

        } catch (Exception e) {
            status.setText(
                    "Грешка при търсене: " +
                    e.getMessage()
            );
        }
    }

    private void startReading() {
        readAttempts = 0;
        status.setText("Търся първата реална обява...");
        readFirstCar();
    }

    private void readFirstCar() {
        readAttempts++;

        String script =
                "(function(){" +

                "var links=Array.from(" +
                "document.querySelectorAll(" +
                "'a[href*=\"/cars/detail/\"]'" +
                ")" +
                ");" +

                "var car=links.find(function(a){" +

                "var txt=(a.innerText||a.textContent||'')" +
                ".replace(/\\\\s+/g,' ')" +
                ".trim();" +

                "if(txt.length<15)return false;" +

                "if(a.classList.contains('sponsored_type'))" +
                "return false;" +

                "var p=a.parentElement;" +

                "while(p){" +

                "if(p.classList&&" +
                "p.classList.contains('sponsored_type'))" +
                "return false;" +

                "p=p.parentElement;" +
                "}" +

                "return true;" +

                "});" +

                "if(!car){" +

                "AndroidCarReader.receiveCar(" +
                "JSON.stringify({" +
                "error:'NO_CAR_FOUND'" +
                "})" +
                ");" +

                "return;" +
                "}" +

                "var text=" +
                "(car.innerText||car.textContent||'')" +
                ".replace(/\\\\s+/g,' ')" +
                ".trim();" +

                "var mileage=" +
                "text.match(/([0-9][0-9,]*)\\\\s*km/i);" +

                "var krw=" +
                "text.match(/([0-9][0-9,]*)\\\\s*만원/);" +

                "var usd=" +
                "text.match(/([0-9][0-9,]*)\\\\s*USD/i);" +

                "var year1=" +
                "text.match(/([0-9]{2}\\\\/[0-9]{2}식" +
                "(?:\\\\([0-9]{2}년형\\\\))?)/);" +

                "var year2=" +
                "text.match(/((?:0[1-9]|1[0-2])\\\\/20[0-9]{2})/);" +

                "var fuel=" +
                "text.match(/" +
                "(가솔린 하이브리드|" +
                "디젤 하이브리드|" +
                "가솔린\\+전기|" +
                "디젤|" +
                "가솔린|" +
                "전기|" +
                "수소|" +
                "Diesel Hybrid|" +
                "Gasoline Hybrid|" +
                "Diesel|" +
                "Gasoline|" +
                "Electric|" +
                "EV|" +
                "Hydrogen|" +
                "LPG)" +
                "/i);" +

                "var href=car.href||'';" +

                "var id=" +
                "href.match(/\\/cars\\/detail\\/([0-9]+)/);" +

                "AndroidCarReader.receiveCar(" +

                "JSON.stringify({" +

                "text:text," +
                "url:href," +
                "carId:(id?id[1]:'')," +
                "mileage:(mileage?mileage[1]:'')," +
                "year:(year1?year1[1]:" +
                "(year2?year2[1]:''))," +
                "fuel:(fuel?fuel[1]:'')," +
                "priceKrw:(krw?krw[1]:'')," +
                "priceUsd:(usd?usd[1]:'')" +

                "})" +

                ");" +

                "})();";

        webView.evaluateJavascript(script, null);
    }

    private class CarReader {

        @JavascriptInterface
        public void receiveCar(String json) {
            runOnUiThread(() -> {
                try {
                    JSONObject obj = new JSONObject(json);

                    if (obj.has("error")) {
                        if (readAttempts < MAX_READ_ATTEMPTS) {
                            status.setText(
                                    "Обявите още се зареждат... " +
                                    readAttempts +
                                    "/" +
                                    MAX_READ_ATTEMPTS
                            );

                            handler.postDelayed(
                                    () -> readFirstCar(),
                                    1000
                            );

                        } else {
                            status.setText(
                                    "Не намерих обява след " +
                                    MAX_READ_ATTEMPTS +
                                    " опита."
                            );
                        }

                        return;
                    }

                    String carId = obj.optString("carId");
                    String year = obj.optString("year");
                    String mileage = obj.optString("mileage");
                    String fuel = obj.optString("fuel");
                    String priceKrw = obj.optString("priceKrw");
                    String priceUsd = obj.optString("priceUsd");
                    String url = obj.optString("url");
                    String raw = obj.optString("text");

                    lastCarUrl = url;

                    String price;

                    if (!priceKrw.isEmpty()) {
                        price = priceKrw + " 만원";
                    } else if (!priceUsd.isEmpty()) {
                        price = priceUsd + " USD";
                    } else {
                        price = "не е разпозната";
                    }

                    lastResult =
                            "ПЪРВА ОБЯВА\n\n" +
                            "ID: " + carId + "\n" +
                            "Година: " + year + "\n" +
                            "Пробег: " + mileage + " km\n" +
                            "Гориво: " + fuel + "\n" +
                            "Цена: " + price + "\n\n" +
                            "LINK:\n" + url + "\n\n" +
                            "RAW:\n" + raw;

                    status.setText(lastResult);

                } catch (Exception e) {
                    status.setText(
                            "Грешка при четене: " +
                            e.getMessage()
                    );
                }
            });
        }
    }

    private void openFirstCar() {
        if (lastCarUrl == null || lastCarUrl.isEmpty()) {
            status.setText(
                    "Първо изчакай да намеря първата обява."
            );
            return;
        }

        webView.loadUrl(lastCarUrl);
    }

    private void copyResult() {
        String text = lastResult;

        if (text == null || text.isEmpty()) {
            text = status.getText().toString();
        }

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip =
                ClipData.newPlainText(
                        "Encar result",
                        text
                );

        clipboard.setPrimaryClip(clip);

        status.setText(
                text +
                "\n\nКОПИРАНО ✅"
        );
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
