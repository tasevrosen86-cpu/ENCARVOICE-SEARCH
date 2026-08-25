package com.encarvoicesearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;

    private static final String HOME =
            "https://m.encar.com/ca/search.do";

    private static final String API =
            "https://api.encar.com/search/car/list/mobile";

    private EditText input;
    private TextView status;
    private LinearLayout results;
    private WebView apiWebView;

    private boolean apiReady = false;
    private String pendingQuery = null;

    private final Map<String, Brand> brandAliases =
            new LinkedHashMap<>();

    private final Map<String, Map<String, String>> modelAliases =
            new LinkedHashMap<>();

    private static class Brand {

        final String key;
        final String encar;
        final String carType;

        Brand(
                String key,
                String encar,
                String carType
        ) {
            this.key = key;
            this.encar = encar;
            this.carType = carType;
        }
    }

    private static class Spec {

        Brand brand;

        String modelGroup;
        String fuel;

        Integer yearFrom;
        Integer yearTo;

        Integer maxMileage;
    }

    private static class Car {

        String id = "";
        String manufacturer = "";
        String model = "";
        String badge = "";
        String year = "";
        String mileage = "";
        String price = "";
        String fuel = "";

        long priceNumber =
                Long.MAX_VALUE;
    }

    @SuppressLint({
            "SetJavaScriptEnabled",
            "JavascriptInterface"
    })
    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        initDictionary();

        buildUi();

        WebSettings settings =
                apiWebView.getSettings();

        settings.setJavaScriptEnabled(
                true
        );

        settings.setDomStorageEnabled(
                true
        );

        settings.setDatabaseEnabled(
                true
        );

        CookieManager cm =
                CookieManager.getInstance();

        cm.setAcceptCookie(
                true
        );

        cm.setAcceptThirdPartyCookies(
                apiWebView,
                true
        );

        apiWebView.addJavascriptInterface(
                new ApiBridge(),
                "EncarApp"
        );

        apiWebView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(
                                view,
                                url
                        );

                        if (
                                url != null
                                        &&
                                url.contains(
                                        "encar.com"
                                )
                        ) {

                            apiReady =
                                    true;

                            status.setText(
                                    "Готово за търсене"
                            );

                            if (
                                    pendingQuery != null
                            ) {

                                String q =
                                        pendingQuery;

                                pendingQuery =
                                        null;

                                fetchApi(
                                        q
                                );
                            }
                        }
                    }
                }
        );

        apiWebView.loadUrl(
                HOME
        );
    }

    private void buildUi() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView title =
                new TextView(this);

        title.setText(
                "ENCAR VOICE SEARCH"
        );

        title.setTextSize(
                20f
        );

        title.setTextColor(
                Color.BLACK
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                12,
                14,
                12,
                8
        );

        input =
                new EditText(this);

        input.setHint(
                "Kia Sorento 2025 бензин до 100000 км"
        );

        input.setTextSize(
                17f
        );

        input.setMinLines(
                2
        );

        input.setMaxLines(
                4
        );

        input.setPadding(
                16,
                10,
                16,
                10
        );

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button voice =
                new Button(this);

        voice.setText(
                "🎤 ГЛАС"
        );

        voice.setOnClickListener(
                v -> startVoice()
        );

        Button search =
                new Button(this);

        search.setText(
                "🔎 ТЪРСИ"
        );

        search.setOnClickListener(
                v -> runSearch()
        );

        Button clear =
                new Button(this);

        clear.setText(
                "ИЗЧИСТИ"
        );

        clear.setOnClickListener(
                v -> {

                    input.setText("");

                    results.removeAllViews();

                    status.setText(
                            "Готово за ново търсене"
                    );
                }
        );

        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        buttons.addView(
                voice,
                bp
        );

        buttons.addView(
                search,
                bp
        );

        buttons.addView(
                clear,
                bp
        );

        status =
                new TextView(this);

        status.setText(
                "Зареждам Encar..."
        );

        status.setTextSize(
                13f
        );

        status.setTextColor(
                Color.DKGRAY
        );

        status.setPadding(
                14,
                8,
                14,
                8
        );

        status.setTextIsSelectable(
                true
        );

        results =
                new LinearLayout(this);

        results.setOrientation(
                LinearLayout.VERTICAL
        );

        results.setPadding(
                10,
                4,
                10,
                20
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(
                results
        );

        apiWebView =
                new WebView(this);

        apiWebView.setVisibility(
                View.INVISIBLE
        );

        root.addView(
                title
        );

        root.addView(
                input
        );

        root.addView(
                buttons
        );

        root.addView(
                status
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        root.addView(
                apiWebView,
                new LinearLayout.LayoutParams(
                        1,
                        1
                )
        );

        setContentView(
                root
        );
    }

    private void startVoice() {

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "bg-BG"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Например: Kia Sorento 2025 бензин до 100000 километра"
        );

        try {

            startActivityForResult(
                    intent,
                    VOICE_REQUEST
            );

        } catch (
                ActivityNotFoundException e
        ) {

            Toast.makeText(
                    this,
                    "Няма активна услуга за гласово разпознаване",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                requestCode == VOICE_REQUEST
                        &&
                resultCode == RESULT_OK
                        &&
                data != null
        ) {

            ArrayList<String> recognized =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (
                    recognized != null
                            &&
                    !recognized.isEmpty()
            ) {

                input.setText(
                        recognized.get(0)
                );

                input.setSelection(
                        input
                                .getText()
                                .length()
                );
            }
        }
    }

    private void runSearch() {

        String raw =
                input
                        .getText()
                        .toString()
                        .trim();

        if (
                raw.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Въведи автомобил",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Spec spec =
                parse(
                        raw
                );

        if (
                spec.brand == null
        ) {

            status.setText(
                    "Не разпознах марката. Поправи текста и натисни ТЪРСИ."
            );

            return;
        }

        String q =
                buildQ(
                        spec
                );

        results.removeAllViews();

        status.setText(
                buildStatus(spec)
                        +
                "\nТърся директно в Encar API..."
        );

        if (
                apiReady
        ) {

            fetchApi(
                    q
            );

        } else {

            pendingQuery =
                    q;

            status.append(
                    "\nИзчаквам Encar да се инициализира..."
            );

            apiWebView.loadUrl(
                    HOME
            );
        }
    }

    private String buildApiUrl(
            String q,
            String sort
    ) {

        return API
                +
                "?count=true"
                +
                "&q="
                +
                Uri.encode(q)
                +
                "&sr="
                +
                Uri.encode(
                        "|"
                                +
                        sort
                                +
                        "|0|100"
                )
                +
                "&inav="
                +
                Uri.encode(
                        "|Metadata|Sort"
                );
    }

    private void fetchApi(
            String q
    ) {

        String priceUrl =
                JSONObject.quote(
                        buildApiUrl(
                                q,
                                "Price"
                        )
                );

        String fallbackUrl =
                JSONObject.quote(
                        buildApiUrl(
                                q,
                                "ModifiedDate"
                        )
                );

        String javascript =

                "(async function(){"
                        +

                "let urls=["
                        +
                priceUrl
                        +
                ","
                        +
                fallbackUrl
                        +
                "];"
                        +

                "let last='';"
                        +

                "for(let i=0;i<urls.length;i++){"
                        +

                "try{"
                        +

                "let r=await fetch("
                        +
                "urls[i],"
                        +
                "{"
                        +
                "method:'GET',"
                        +
                "credentials:'include',"
                        +
                "headers:{'Accept':'application/json'}"
                        +
                "}"
                        +
                ");"
                        +

                "let t=await r.text();"
                        +

                "if(r.ok&&t){"
                        +
                "EncarApp.onApiResponse(t);"
                        +
                "return;"
                        +
                "}"
                        +

                "last='HTTP '+r.status+' '+t.substring(0,300);"
                        +

                "}catch(e){"
                        +
                "last=String(e);"
                        +
                "}"
                        +

                "}"
                        +

                "EncarApp.onApiError("
                        +
                "last||'Unknown API error'"
                        +
                ");"
                        +

                "})();";

        apiWebView.evaluateJavascript(
                javascript,
                null
        );
    }

    private String buildQ(
            Spec spec
    ) {

        List<String> filters =
                new ArrayList<>();

        filters.add(
                "Hidden.N."
        );

        filters.add(
                "SellType.일반."
        );

        if (
                spec.yearFrom != null
                        &&
                spec.yearTo != null
        ) {

            int from =
                    spec.yearFrom
                            *
                    100;

            int to =
                    spec.yearTo
                            *
                    100
                            +
                    99;

            filters.add(
                    "Year.range("
                            +
                    from
                            +
                    ".."
                            +
                    to
                            +
                    ")."
            );
        }

        if (
                spec.maxMileage != null
        ) {

            filters.add(
                    "Mileage.range(.."
                            +
                    spec.maxMileage
                            +
                    ")."
            );
        }

        if (
                spec.fuel != null
        ) {

            filters.add(
                    "FuelType."
                            +
                    spec.fuel
                            +
                    "."
            );
        }

        String branch;

        if (
                spec.modelGroup != null
                        &&
                !spec.modelGroup.isEmpty()
        ) {

            branch =
                    "(C.CarType."
                            +
                    spec.brand.carType
                            +
                    "._.(C.Manufacturer."
                            +
                    spec.brand.encar
                            +
                    "._.(C.ModelGroup."
                            +
                    spec.modelGroup
                            +
                    ".)))";

        } else {

            branch =
                    "(C.CarType."
                            +
                    spec.brand.carType
                            +
                    "._.(C.Manufacturer."
                            +
                    spec.brand.encar
                            +
                    ".))";
        }

        filters.add(
                branch
        );

        StringBuilder q =
                new StringBuilder(
                        "(And."
                );

        for (
                int i = 0;
                i < filters.size();
                i++
        ) {

            if (
                    i > 0
            ) {

                q.append(
                        "_."
                );
            }

            q.append(
                    filters.get(i)
            );
        }

        q.append(
                ")"
        );

        return q.toString();
    }

    private Spec parse(
            String raw
    ) {

        Spec spec =
                new Spec();

        String text =
                normalize(
                        raw
                );

        spec.brand =
                findBrand(
                        text
                );

        if (
                spec.brand != null
        ) {

            spec.modelGroup =
                    findModel(
                            text,
                            spec.brand
                    );
        }

        spec.fuel =
                findFuel(
                        text
                );

        spec.maxMileage =
                findMileage(
                        text
                );

        List<Integer> years =
                findYears(
                        raw
                );

        if (
                years.size() == 1
        ) {

            spec.yearFrom =
                    years.get(0);

            spec.yearTo =
                    years.get(0);

        } else if (
                years.size() >= 2
        ) {

            spec.yearFrom =
                    Collections.min(
                            years
                    );

            spec.yearTo =
                    Collections.max(
                            years
                    );
        }

        return spec;
    }

    private Brand findBrand(
            String text
    ) {

        Brand best =
                null;

        int bestLength =
                -1;

        for (
                Map.Entry<String, Brand> entry
                        :
                brandAliases.entrySet()
        ) {

            if (
                    containsPhrase(
                            text,
                            entry.getKey()
                    )
                            &&
                    entry
                            .getKey()
                            .length()
                            >
                    bestLength
            ) {

                best =
                        entry.getValue();

                bestLength =
                        entry
                                .getKey()
                                .length();
            }
        }

        return best;
    }

    private String findModel(
            String text,
            Brand brand
    ) {

        Map<String, String> map =
                modelAliases.get(
                        brand.key
                );

        if (
                map != null
        ) {

            String best =
                    null;

            int bestLength =
                    -1;

            for (
                    Map.Entry<String, String> entry
                            :
                    map.entrySet()
            ) {

                if (
                        containsPhrase(
                                text,
                                entry.getKey()
                        )
                                &&
                        entry
                                .getKey()
                                .length()
                                >
                        bestLength
                ) {

                    best =
                            entry.getValue();

                    bestLength =
                            entry
                                    .getKey()
                                    .length();
                }
            }

            if (
                    best != null
            ) {

                return best;
            }
        }

        if (
                "mercedes".equals(
                        brand.key
                )
        ) {

            Matcher suv =
                    Pattern.compile(
                            "\\b(gle|glc|gls|gla|glb|cla|cls|cle)\\s*[- ]?\\d*[a-z]*\\b"
                    )
                            .matcher(
                                    text
                            );

            if (
                    suv.find()
            ) {

                return suv
                        .group(1)
                        .toUpperCase(
                                Locale.ROOT
                        )
                        +
                        "-클래스";
            }

            Matcher normalClass =
                    Pattern.compile(
                            "\\b([acesg])\\s*[- ]?\\d{3}[a-z]*\\b"
                    )
                            .matcher(
                                    text
                            );

            if (
                    normalClass.find()
            ) {

                return normalClass
                        .group(1)
                        .toUpperCase(
                                Locale.ROOT
                        )
                        +
                        "-클래스";
            }
        }

        if (
                "bmw".equals(
                        brand.key
                )
        ) {

            Matcher series =
                    Pattern.compile(
                            "\\b([1-8])\\d{2}[a-z]*\\b"
                    )
                            .matcher(
                                    text
                            );

            if (
                    series.find()
            ) {

                return series
                        .group(1)
                        +
                        "시리즈";
            }

            Matcher x =
                    Pattern.compile(
                            "\\b(x[1-7]|z4|i[3-8]|ix)\\b"
                    )
                            .matcher(
                                    text
                            );

            if (
                    x.find()
            ) {

                return x
                        .group(1)
                        .toUpperCase(
                                Locale.ROOT
                        );
            }
        }

        return genericImportedModel(
                text,
                brand
        );
    }

    private String genericImportedModel(
            String text,
            Brand brand
    ) {

        if (
                "Y".equals(
                        brand.carType
                )
        ) {

            return null;
        }

        String cleaned =
                " "
                        +
                text
                        +
                " ";

        for (
                Map.Entry<String, Brand> entry
                        :
                brandAliases.entrySet()
        ) {

            if (
                    entry
                            .getValue()
                            .key
                            .equals(
                                    brand.key
                            )
            ) {

                cleaned =
                        cleaned.replace(
                                " "
                                        +
                                entry.getKey()
                                        +
                                " ",
                                " "
                        );
            }
        }

        cleaned =
                cleaned.replaceAll(
                        "\\b(19\\d{2}|20\\d{2})\\b",
                        " "
                );

        cleaned =
                cleaned.replaceAll(
                        "\\b\\d[\\d .]{2,}\\s*(km|км|километра|километри)\\b",
                        " "
                );

        String[] noise = {

                "diesel",
                "дизел",
                "дизелов",

                "petrol",
                "gasoline",
                "бензин",
                "бензинов",

                "hybrid",
                "хибрид",
                "хибриден",

                "electric",
                "ev",
                "електрически",
                "електрическа",

                "до",
                "под",

                "търси",
                "кола",
                "автомобил"
        };

        for (
                String word : noise
        ) {

            cleaned =
                    cleaned.replace(
                            " "
                                    +
                            normalize(word)
                                    +
                            " ",
                            " "
                    );
        }

        cleaned =
                cleaned.replaceAll(
                        "\\s+",
                        " "
                )
                        .trim();

        Matcher matcher =
                Pattern.compile(
                        "\\b[a-zA-Z][a-zA-Z0-9.-]{0,15}\\b"
                )
                        .matcher(
                                cleaned
                        );

        if (
                matcher.find()
        ) {

            return matcher
                    .group()
                    .toUpperCase(
                            Locale.ROOT
                    );
        }

        return null;
    }

    private String findFuel(
            String text
    ) {

        if (
                containsAny(
                        text,
                        "diesel",
                        "дизел",
                        "дизелов"
                )
        ) {

            return "디젤";
        }

        if (
                containsAny(
                        text,
                        "hybrid",
                        "хибрид",
                        "хибриден",
                        "plug in",
                        "plug-in",
                        "phev",
                        "плъгин"
                )
        ) {

            return "가솔린+전기";
        }

        if (
                containsAny(
                        text,
                        "electric",
                        "ev",
                        "електрически",
                        "електрическа",
                        "електромобил"
                )
        ) {

            return "전기";
        }

        if (
                containsAny(
                        text,
                        "petrol",
                        "gasoline",
                        "бензин",
                        "бензинов"
                )
        ) {

            return "가솔린";
        }

        if (
                containsAny(
                        text,
                        "lpg",
                        "газ"
                )
        ) {

            return "LPG";
        }

        return null;
    }

    private List<Integer> findYears(
            String raw
    ) {

        List<Integer> result =
                new ArrayList<>();

        Matcher matcher =
                Pattern.compile(
                        "\\b(19\\d{2}|20\\d{2})\\b"
                )
                        .matcher(
                                raw
                        );

        while (
                matcher.find()
        ) {

            try {

                int year =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                if (
                        year >= 1980
                                &&
                        year <= 2099
                ) {

                    result.add(
                            year
                    );
                }

            } catch (
                    Exception ignored
            ) {
            }
        }

        return result;
    }

    private Integer findMileage(
            String text
    ) {

        Matcher thousands =
                Pattern.compile(
                        "(\\d{1,3})\\s*(хиляди|хил|thousand)\\s*(km|км|километра|километри)?"
                )
                        .matcher(
                                text
                        );

        if (
                thousands.find()
        ) {

            try {

                return Integer.parseInt(
                        thousands.group(1)
                )
                        *
                        1000;

            } catch (
                    Exception ignored
            ) {
            }
        }

        Matcher km =
                Pattern.compile(
                        "([0-9][0-9 .]{1,10})\\s*(km|км|километра|километри)"
                )
                        .matcher(
                                text
                        );

        if (
                km.find()
        ) {

            try {

                String digits =
                        km.group(1)
                                .replaceAll(
                                        "[^0-9]",
                                        ""
                                );

                int value =
                        Integer.parseInt(
                                digits
                        );

                if (
                        value > 0
                                &&
                        value <= 2000000
                ) {

                    return value;
                }

            } catch (
                    Exception ignored
            ) {
            }
        }

        return null;
    }

    private String buildStatus(
            Spec spec
    ) {

        StringBuilder b =
                new StringBuilder(
                        "НАЙ-ЕВТИНИ ПЪРВО"
                );

        b.append(
                " | "
        );

        b.append(
                spec.brand.encar
        );

        b.append(
                " | CarType."
        );

        b.append(
                spec.brand.carType
        );

        if (
                spec.modelGroup != null
        ) {

            b.append(
                    " | "
            );

            b.append(
                    spec.modelGroup
            );

        } else {

            b.append(
                    " | всички модели"
            );
        }

        if (
                spec.yearFrom != null
        ) {

            if (
                    spec.yearFrom.equals(
                            spec.yearTo
                    )
            ) {

                b.append(
                        " | "
                );

                b.append(
                        spec.yearFrom
                );

            } else {

                b.append(
                        " | "
                );

                b.append(
                        spec.yearFrom
                );

                b.append(
                        "-"
                );

                b.append(
                        spec.yearTo
                );
            }
        }

        if (
                spec.fuel != null
        ) {

            b.append(
                    " | "
            );

            b.append(
                    spec.fuel
            );
        }

        if (
                spec.maxMileage != null
        ) {

            b.append(
                    " | до "
            );

            b.append(
                    spec.maxMileage
            );

            b.append(
                    " km"
            );
        }

        return b.toString();
    }

    public class ApiBridge {

        @JavascriptInterface
        public void onApiResponse(
                String json
        ) {

            runOnUiThread(
                    () ->
                            handleApiResponse(
                                    json
                            )
            );
        }

        @JavascriptInterface
        public void onApiError(
                String error
        ) {

            runOnUiThread(
                    () -> {

                        status.setText(
                                "Encar API грешка: "
                                        +
                                error
                        );

                        results.removeAllViews();

                        TextView message =
                                new TextView(
                                        MainActivity.this
                                );

                        message.setText(
                                "API заявката не беше приета.\n"
                                        +
                                "Ако виждаш HTTP 407, затвори и отвори приложението, "
                                        +
                                "за да се обновят Encar cookies."
                        );

                        message.setTextSize(
                                16f
                        );

                        message.setPadding(
                                14,
                                14,
                                14,
                                14
                        );

                        results.addView(
                                message
                        );
                    }
            );
        }
    }

    private void handleApiResponse(
            String json
    ) {

        try {

            Object root;

            String text =
                    json == null
                            ?
                    ""
                            :
                    json.trim();

            if (
                    text.startsWith(
                            "["
                    )
            ) {

                root =
                        new JSONArray(
                                text
                        );

            } else {

                root =
                        new JSONObject(
                                text
                        );
            }

            Map<String, Car> unique =
                    new LinkedHashMap<>();

            collectCars(
                    root,
                    unique
            );

            List<Car> cars =
                    new ArrayList<>(
                            unique.values()
                    );

            cars.sort(
                    Comparator.comparingLong(
                            car ->
                                    car.priceNumber
                    )
            );

            results.removeAllViews();

            if (
                    cars.isEmpty()
            ) {

                status.setText(
                        status.getText()
                                +
                        "\nAPI отговорът е получен, но няма намерени обяви."
                );

                TextView message =
                        new TextView(this);

                message.setText(
                        "Няма автомобили по тези критерии."
                );

                message.setTextSize(
                        16f
                );

                message.setPadding(
                        14,
                        14,
                        14,
                        14
                );

                results.addView(
                        message
                );

                return;
            }

            status.setText(
                    "Намерени "
                            +
                    cars.size()
                            +
                    " обяви | най-ниската цена първа"
            );

            int limit =
                    Math.min(
                            cars.size(),
                            100
                    );

            for (
                    int i = 0;
                    i < limit;
                    i++
            ) {

                addCarCard(
                        cars.get(i),
                        i + 1
                );
            }

        } catch (
                Exception e
        ) {

            status.setText(
                    "Не успях да прочета Encar JSON: "
                            +
                    e.getMessage()
            );
        }
    }

    private void collectCars(
            Object node,
            Map<String, Car> output
    ) {

        try {

            if (
                    node instanceof JSONObject
            ) {

                JSONObject object =
                        (JSONObject) node;

                String id =
                        pick(
                                object,
                                "Id",
                                "id",
                                "CarId",
                                "carId"
                        );

                String price =
                        pick(
                                object,
                                "Price",
                                "price"
                        );

                if (
                        !id.isEmpty()
                                &&
                        !price.isEmpty()
                ) {

                    Car car =
                            new Car();

                    car.id =
                            id;

                    car.price =
                            price;

                    car.priceNumber =
                            parseNumber(
                                    price
                            );

                    car.manufacturer =
                            pick(
                                    object,
                                    "Manufacturer",
                                    "manufacturer",
                                    "Maker",
                                    "maker"
                            );

                    car.model =
                            pick(
                                    object,
                                    "ModelGroup",
                                    "modelGroup",
                                    "Model",
                                    "model",
                                    "Name",
                                    "name"
                            );

                    car.badge =
                            pick(
                                    object,
                                    "Badge",
                                    "badge",
                                    "BadgeDetail",
                                    "badgeDetail",
                                    "Grade",
                                    "grade"
                            );

                    car.year =
                            pick(
                                    object,
                                    "Year",
                                    "year",
                                    "FormYear",
                                    "formYear"
                            );

                    car.mileage =
                            pick(
                                    object,
                                    "Mileage",
                                    "mileage"
                            );

                    car.fuel =
                            pick(
                                    object,
                                    "FuelType",
                                    "fuelType",
                                    "Fuel",
                                    "fuel"
                            );

                    output.put(
                            car.id,
                            car
                    );
                }

                JSONArray names =
                        object.names();

                if (
                        names != null
                ) {

                    for (
                            int i = 0;
                            i < names.length();
                            i++
                    ) {

                        String key =
                                names.optString(
                                        i
                                );

                        Object child =
                                object.opt(
                                        key
                                );

                        if (
                                child instanceof JSONObject
                                        ||
                                child instanceof JSONArray
                        ) {

                            collectCars(
                                    child,
                                    output
                            );
                        }
                    }
                }

            } else if (
                    node instanceof JSONArray
            ) {

                JSONArray array =
                        (JSONArray) node;

                for (
                        int i = 0;
                        i < array.length();
                        i++
                ) {

                    Object child =
                            array.opt(
                                    i
                            );

                    if (
                            child instanceof JSONObject
                                    ||
                            child instanceof JSONArray
                    ) {

                        collectCars(
                                child,
                                output
                        );
                    }
                }
            }

        } catch (
                Exception ignored
        ) {
        }
    }

    private void addCarCard(
            Car car,
            int position
    ) {

        TextView card =
                new TextView(this);

        StringBuilder text =
                new StringBuilder();

        text.append(
                position
        );

        text.append(
                ". "
        );

        if (
                !car.manufacturer.isEmpty()
        ) {

            text.append(
                    car.manufacturer
            );

            text.append(
                    " "
            );
        }

        if (
                !car.model.isEmpty()
        ) {

            text.append(
                    car.model
            );

            text.append(
                    " "
            );
        }

        if (
                !car.badge.isEmpty()
        ) {

            text.append(
                    car.badge
            );
        }

        if (
                !car.year.isEmpty()
        ) {

            text.append(
                    "\nГодина: "
            );

            text.append(
                    car.year
            );
        }

        if (
                !car.mileage.isEmpty()
        ) {

            text.append(
                    " | Пробег: "
            );

            text.append(
                    formatNumber(
                            car.mileage
                    )
            );

            text.append(
                    " km"
            );
        }

        if (
                !car.fuel.isEmpty()
        ) {

            text.append(
                    " | "
            );

            text.append(
                    car.fuel
            );
        }

        text.append(
                "\nЦена: "
        );

        text.append(
                formatPrice(
                        car.price
                )
        );

        text.append(
                "\nEncar ID: "
        );

        text.append(
                car.id
        );

        text.append(
                "\nНАТИСНИ ЗА ОБЯВАТА"
        );

        card.setText(
                text.toString()
        );

        card.setTextSize(
                16f
        );

        card.setTextColor(
                Color.BLACK
        );

        card.setPadding(
                18,
                16,
                18,
                16
        );

        card.setBackgroundColor(
                Color.rgb(
                        245,
                        245,
                        245
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                10
        );

        card.setOnClickListener(
                v ->
                        openListing(
                                car.id
                        )
        );

        results.addView(
                card,
                params
        );
    }

    private void openListing(
            String id
    ) {

        try {

            String url =
                    "https://www.encar.com/dc/dc_cardetailview.do?carid="
                            +
                    Uri.encode(
                            id
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(
                    intent
            );

        } catch (
                Exception e
        ) {

            Toast.makeText(
                    this,
                    "Не мога да отворя обявата",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String pick(
            JSONObject object,
            String... keys
    ) {

        for (
                String wanted : keys
        ) {

            JSONArray names =
                    object.names();

            if (
                    names == null
            ) {

                return "";
            }

            for (
                    int i = 0;
                    i < names.length();
                    i++
            ) {

                String actual =
                        names.optString(
                                i
                        );

                if (
                        actual.equalsIgnoreCase(
                                wanted
                        )
                ) {

                    Object value =
                            object.opt(
                                    actual
                            );

                    if (
                            value != null
                                    &&
                            value != JSONObject.NULL
                    ) {

                        String result =
                                String.valueOf(
                                        value
                                )
                                        .trim();

                        if (
                                !result.isEmpty()
                                        &&
                                !"null".equalsIgnoreCase(
                                        result
                                )
                        ) {

                            return result;
                        }
                    }
                }
            }
        }

        return "";
    }

    private long parseNumber(
            String value
    ) {

        try {

            String digits =
                    value.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (
                    digits.isEmpty()
            ) {

                return Long.MAX_VALUE;
            }

            return Long.parseLong(
                    digits
            );

        } catch (
                Exception e
        ) {

            return Long.MAX_VALUE;
        }
    }

    private String formatPrice(
            String price
    ) {

        long number =
                parseNumber(
                        price
                );

        if (
                number == Long.MAX_VALUE
        ) {

            return price;
        }

        if (
                number < 1000000L
        ) {

            return String.format(
                    Locale.US,
                    "%,d 만원",
                    number
            );
        }

        return String.format(
                Locale.US,
                "%,d",
                number
        );
    }

    private String formatNumber(
            String value
    ) {

        long number =
                parseNumber(
                        value
                );

        if (
                number == Long.MAX_VALUE
        ) {

            return value;
        }

        return String.format(
                Locale.US,
                "%,d",
                number
        );
    }

    private boolean containsAny(
            String text,
            String... values
    ) {

        for (
                String value : values
        ) {

            if (
                    containsPhrase(
                            text,
                            value
                    )
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean containsPhrase(
            String text,
            String value
    ) {

        String needle =
                normalize(
                        value
                );

        if (
                needle.isEmpty()
        ) {

            return false;
        }

        String source =
                " "
                        +
                text
                        +
                " ";

        String target =
                " "
                        +
                needle
                        +
                " ";

        if (
                source.contains(
                        target
                )
        ) {

            return true;
        }

        if (
                needle.contains("-")
                        ||
                needle.contains(".")
        ) {

            return text.contains(
                    needle
            );
        }

        return false;
    }

    private String normalize(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }

        return value
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        '–',
                        '-'
                )
                .replace(
                        '—',
                        '-'
                )
                .replace(
                        ',',
                        ' '
                )
                .replace(
                        ';',
                        ' '
                )
                .replace(
                        ':',
                        ' '
                )
                .replace(
                        '!',
                        ' '
                )
                .replace(
                        '?',
                        ' '
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private void addBrand(
            String key,
            String encar,
            String carType,
            String... aliases
    ) {

        Brand brand =
                new Brand(
                        key,
                        encar,
                        carType
                );

        for (
                String alias : aliases
        ) {

            brandAliases.put(
                    normalize(alias),
                    brand
            );
        }
    }

    private void addModels(
            String brandKey,
            String... rows
    ) {

        Map<String, String> map =
                modelAliases.get(
                        brandKey
                );

        if (
                map == null
        ) {

            map =
                    new LinkedHashMap<>();

            modelAliases.put(
                    brandKey,
                    map
            );
        }

        for (
                String row : rows
        ) {

            String[] parts =
                    row.split(
                            "=",
                            2
                    );

            if (
                    parts.length == 2
            ) {

                map.put(
                        normalize(
                                parts[0]
                        ),
                        parts[1]
                );
            }
        }
    }

    private void initDictionary() {

        /*
         * КОРЕЙСКИ МАРКИ
         * CarType.Y
         */

        addBrand(
                "kia",
                "기아",
                "Y",
                "kia",
                "киа",
                "кия"
        );

        addBrand(
                "hyundai",
                "현대",
                "Y",
                "hyundai",
                "хюндай",
                "хундай",
                "хендай",
                "хюнде"
        );

        addBrand(
                "genesis",
                "제네시스",
                "Y",
                "genesis",
                "генезис",
                "дженезис"
        );

        addBrand(
                "chevrolet",
                "쉐보레(GM대우)",
                "Y",
                "chevrolet",
                "chevy",
                "шевролет"
        );

        addBrand(
                "renault",
                "르노코리아(삼성)",
                "Y",
                "renault",
                "рено"
        );

        addBrand(
                "kgm",
                "KG모빌리티(쌍용)",
                "Y",
                "kgm",
                "kg mobility",
                "ssangyong",
                "сангйонг"
        );

        /*
         * ВНОСНИ МАРКИ
         * CarType.N
         */

        addBrand(
                "mercedes",
                "벤츠",
                "N",
                "mercedes",
                "mercedes benz",
                "mercedes-benz",
                "benz",
                "мерцедес",
                "мерседес"
        );

        addBrand(
                "bmw",
                "BMW",
                "N",
                "bmw",
                "бмв"
        );

        addBrand(
                "audi",
                "아우디",
                "N",
                "audi",
                "ауди"
        );

        addBrand(
                "volkswagen",
                "폭스바겐",
                "N",
                "volkswagen",
                "vw",
                "фолксваген",
                "волксваген"
        );

        addBrand(
                "porsche",
                "포르쉐",
                "N",
                "porsche",
                "порше"
        );

        addBrand(
                "ford",
                "포드",
                "N",
                "ford",
                "форд"
        );

        addBrand(
                "honda",
                "혼다",
                "N",
                "honda",
                "хонда"
        );

        addBrand(
                "peugeot",
                "푸조",
                "N",
                "peugeot",
                "пежо"
        );

        addBrand(
                "volvo",
                "볼보",
                "N",
                "volvo",
                "волво"
        );

        addBrand(
                "toyota",
                "도요타",
                "N",
                "toyota",
                "тойота"
        );

        addBrand(
                "lexus",
                "렉서스",
                "N",
                "lexus",
                "лексус"
        );

        addBrand(
                "nissan",
                "닛산",
                "N",
                "nissan",
                "нисан",
                "ниссан"
        );

        addBrand(
                "infiniti",
                "인피니티",
                "N",
                "infiniti",
                "инфинити"
        );

        addBrand(
                "tesla",
                "테슬라",
                "N",
                "tesla",
                "тесла"
        );

        addBrand(
                "landrover",
                "랜드로버",
                "N",
                "land rover",
                "landrover",
                "ленд ровер",
                "ленд ровър"
        );

        addBrand(
                "jaguar",
                "재규어",
                "N",
                "jaguar",
                "ягуар"
        );

        addBrand(
                "jeep",
                "지프",
                "N",
                "jeep",
                "джип"
        );

        addBrand(
                "mini",
                "미니",
                "N",
                "mini",
                "мини"
        );

        addBrand(
                "cadillac",
                "캐딜락",
                "N",
                "cadillac",
                "кадилак"
        );

        addBrand(
                "lincoln",
                "링컨",
                "N",
                "lincoln",
                "линкълн"
        );

        addBrand(
                "maserati",
                "마세라티",
                "N",
                "maserati",
                "мазерати"
        );

        addBrand(
                "bentley",
                "벤틀리",
                "N",
                "bentley",
                "бентли"
        );

        addBrand(
                "ferrari",
                "페라리",
                "N",
                "ferrari",
                "ферари"
        );

        addBrand(
                "lamborghini",
                "람보르기니",
                "N",
                "lamborghini",
                "ламборгини"
        );

        addBrand(
                "mclaren",
                "맥라렌",
                "N",
                "mclaren",
                "макларен",
                "макларън"
        );

        addBrand(
                "subaru",
                "스바루",
                "N",
                "subaru",
                "субару"
        );

        addBrand(
                "suzuki",
                "스즈키",
                "N",
                "suzuki",
                "сузуки"
        );

        addBrand(
                "mitsubishi",
                "미쓰비시",
                "N",
                "mitsubishi",
                "мицубиши"
        );

        addBrand(
                "mazda",
                "마쯔다",
                "N",
                "mazda",
                "мазда"
        );

        addBrand(
                "citroen",
                "시트로엥",
                "N",
                "citroen",
                "citroën",
                "ситроен"
        );

        addBrand(
                "fiat",
                "피아트",
                "N",
                "fiat",
                "фиат"
        );

        addBrand(
                "polestar",
                "폴스타",
                "N",
                "polestar",
                "полстар"
        );

        addBrand(
                "byd",
                "BYD",
                "N",
                "byd"
        );

        addBrand(
                "gmc",
                "GMC",
                "N",
                "gmc"
        );

        /*
         * KIA
         */

        addModels(
                "kia",

                "sorento=쏘렌토",
                "соренто=쏘렌토",

                "sportage=스포티지",
                "спортидж=스포티지",
                "спортиж=스포티지",

                "carnival=카니발",
                "карнивал=카니발",

                "seltos=셀토스",

                "niro=니로",

                "mohave=모하비",

                "morning=모닝",

                "ray=레이",

                "ev3=EV3",
                "ev4=EV4",
                "ev5=EV5",
                "ev6=EV6",
                "ev9=EV9",

                "k3=K3",
                "k5=K5",
                "k7=K7",
                "k8=K8",
                "k9=K9"
        );

        /*
         * HYUNDAI
         */

        addModels(
                "hyundai",

                "tucson=투싼",
                "тусон=투싼",

                "santa fe=싼타페",
                "santafe=싼타페",
                "санта фе=싼타페",

                "palisade=팰리세이드",
                "палисейд=팰리세이드",

                "kona=코나",

                "staria=스타리아",

                "ioniq 5=아이오닉5",
                "ioniq5=아이오닉5",

                "ioniq 6=아이오닉6",
                "ioniq6=아이오닉6",

                "elantra=아반떼",
                "avante=아반떼",

                "sonata=쏘나타",

                "grandeur=그랜저",

                "casper=캐스퍼",

                "nexo=넥쏘"
        );

        /*
         * GENESIS
         */

        addModels(
                "genesis",

                "g70=G70",
                "g80=G80",
                "g90=G90",

                "gv60=GV60",
                "gv70=GV70",
                "gv80=GV80"
        );

        /*
         * MERCEDES
         */

        addModels(
                "mercedes",

                "gle=GLE-클래스",
                "glc=GLC-클래스",
                "gls=GLS-클래스",

                "gla=GLA-클래스",
                "glb=GLB-클래스",

                "cla=CLA-클래스",
                "cls=CLS-클래스",
                "cle=CLE-클래스",

                "eqa=EQA",
                "eqb=EQB",
                "eqe=EQE",
                "eqs=EQS"
        );

        /*
         * VOLKSWAGEN
         */

        addModels(
                "volkswagen",

                "tiguan=티구안",
                "тигуан=티구안",

                "touareg=투아렉",
                "туарег=투아렉",

                "golf=골프",
                "голф=골프",

                "passat=파사트",
                "пасат=파사트",

                "arteon=아테온",
                "артеон=아테온",

                "t-roc=티록",
                "t roc=티록",

                "jetta=제타",

                "id.4=ID.4",
                "id4=ID.4",

                "id.5=ID.5",
                "id5=ID.5"
        );

        /*
         * PORSCHE
         */

        addModels(
                "porsche",

                "cayenne=카이엔",
                "кайен=카이엔",

                "macan=마칸",
                "макан=마칸",

                "panamera=파나메라",
                "панамера=파나메라",

                "taycan=타이칸",
                "тайкан=타이칸",

                "911=911",
                "718=718"
        );

        /*
         * FORD
         */

        addModels(
                "ford",

                "explorer=익스플로러",

                "mustang=머스탱",

                "ranger=레인저",

                "bronco=브롱코",

                "f150=F150",
                "f-150=F150",

                "expedition=익스페디션"
        );

        /*
         * HONDA
         */

        addModels(
                "honda",

                "accord=어코드",
                "акорд=어코드",

                "civic=시빅",
                "сивик=시빅",

                "cr-v=CR-V",
                "crv=CR-V",

                "hr-v=HR-V",
                "hrv=HR-V",

                "odyssey=오딧세이",

                "pilot=파일럿"
        );

        /*
         * PEUGEOT
         */

        addModels(
                "peugeot",

                "208=208",
                "308=308",
                "408=408",
                "508=508",

                "2008=2008",
                "3008=3008",
                "5008=5008"
        );

        /*
         * TOYOTA
         */

        addModels(
                "toyota",

                "rav4=RAV4",
                "rav 4=RAV4",

                "camry=캠리",

                "prius=프리우스",

                "highlander=하이랜더",

                "sienna=시에나",

                "land cruiser=랜드크루저"
        );

        /*
         * LEXUS
         */

        addModels(
                "lexus",

                "ux=UX",
                "nx=NX",
                "rx=RX",
                "gx=GX",
                "lx=LX",

                "is=IS",
                "es=ES",
                "ls=LS",

                "rc=RC",
                "lc=LC"
        );

        /*
         * VOLVO
         */

        addModels(
                "volvo",

                "xc40=XC40",
                "xc60=XC60",
                "xc90=XC90",

                "s60=S60",
                "s90=S90",

                "v60=V60",
                "v90=V90",

                "ex30=EX30",
                "ex90=EX90"
        );

        /*
         * TESLA
         */

        addModels(
                "tesla",

                "model 3=모델 3",
                "model y=모델 Y",

                "model s=모델 S",
                "model x=모델 X"
        );

        /*
         * LAND ROVER
         */

        addModels(
                "landrover",

                "range rover sport=레인지로버 스포츠",

                "range rover=레인지로버",

                "evoque=레인지로버 이보크",

                "velar=레인지로버 벨라",

                "discovery=디스커버리",

                "defender=디펜더"
        );

        /*
         * CHEVROLET KOREA
         */

        addModels(
                "chevrolet",

                "trax=트랙스",

                "trailblazer=트레일블레이저",

                "malibu=말리부",

                "spark=스파크",

                "camaro=카마로",

                "colorado=콜로라도",

                "traverse=트래버스",

                "tahoe=타호"
        );

        /*
         * KGM / SSANGYONG
         */

        addModels(
                "kgm",

                "torres=토레스",

                "rexton=렉스턴",

                "korando=코란도",

                "tivoli=티볼리"
        );
    }

    @Override
    protected void onDestroy() {

        if (
                apiWebView != null
        ) {

            apiWebView.removeJavascriptInterface(
                    "EncarApp"
            );

            apiWebView.stopLoading();

            apiWebView.destroy();
        }

        super.onDestroy();
    }
}
