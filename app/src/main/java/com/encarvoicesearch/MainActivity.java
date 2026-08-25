package com.encarvoicesearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 7001;

    private static final String ENCAR_BASE =
            "https://m.encar.com/ca/search.do#!";

    private EditText searchInput;
    private TextView statusText;
    private WebView webView;

    private final List<BrandRule> brands = new ArrayList<>();
    private final List<ModelRule> models = new ArrayList<>();


    private static class BrandRule {

        final String key;
        final String encar;
        final String[] aliases;

        BrandRule(
                String key,
                String encar,
                String... aliases
        ) {
            this.key = key;
            this.encar = encar;
            this.aliases = aliases;
        }
    }


    private static class ModelRule {

        final String brandKey;
        final String encar;
        final String[] aliases;

        ModelRule(
                String brandKey,
                String encar,
                String... aliases
        ) {
            this.brandKey = brandKey;
            this.encar = encar;
            this.aliases = aliases;
        }
    }


    private static class SearchSpec {

        String raw;

        BrandRule brand;

        String modelGroup;

        String fuel;

        Integer yearFrom;
        Integer yearTo;

        Integer maxMileage;

        Integer maxPriceManWon;

        String sort = "MobileModifiedDate";
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        initRules();

        createUi();

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager
                .getInstance()
                .setAcceptCookie(true);

        CookieManager
                .getInstance()
                .setAcceptThirdPartyCookies(
                        webView,
                        true
                );

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.loadUrl(
                "https://m.encar.com/ca/search.do"
        );
    }


    private void createUi() {

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

        title.setTextSize(20f);

        title.setTextColor(
                Color.BLACK
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                16,
                14,
                16,
                8
        );


        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Напр.: Mercedes GLE 2024 diesel до 100 000 km най-евтини"
        );

        searchInput.setTextSize(16f);

        searchInput.setTextColor(
                Color.BLACK
        );

        searchInput.setHintTextColor(
                Color.GRAY
        );

        searchInput.setMinLines(2);

        searchInput.setMaxLines(4);

        searchInput.setPadding(
                20,
                12,
                20,
                12
        );


        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );


        Button voiceButton =
                new Button(this);

        voiceButton.setText(
                "🎤 ГЛАС"
        );

        voiceButton.setOnClickListener(
                v -> startVoiceInput()
        );


        Button searchButton =
                new Button(this);

        searchButton.setText(
                "🔎 ТЪРСИ"
        );

        searchButton.setOnClickListener(
                v -> runSearch()
        );


        Button clearButton =
                new Button(this);

        clearButton.setText(
                "ИЗЧИСТИ"
        );

        clearButton.setOnClickListener(
                v -> {

                    searchInput.setText("");

                    statusText.setText(
                            "Готово за ново търсене"
                    );

                    searchInput.requestFocus();
                }
        );


        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );


        buttons.addView(
                voiceButton,
                bp
        );

        buttons.addView(
                searchButton,
                bp
        );

        buttons.addView(
                clearButton,
                bp
        );


        statusText =
                new TextView(this);

        statusText.setText(
                "Кажи или напиши марка, модел и филтри"
        );

        statusText.setTextSize(13f);

        statusText.setTextColor(
                Color.DKGRAY
        );

        statusText.setPadding(
                16,
                8,
                16,
                8
        );

        statusText.setTextIsSelectable(
                true
        );


        webView =
                new WebView(this);


        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );


        root.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );


        root.addView(
                buttons,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );


        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );


        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );


        setContentView(root);
    }


    private void startVoiceInput() {

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
                "Кажи марка, модел, година, гориво и пробег"
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
                    "На телефона няма активна услуга за гласово разпознаване",
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

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );


            if (
                    results != null
                            &&
                    !results.isEmpty()
            ) {

                searchInput.setText(
                        results.get(0)
                );

                searchInput.setSelection(
                        searchInput
                                .getText()
                                .length()
                );
            }
        }
    }


    private void runSearch() {

        String raw =
                searchInput
                        .getText()
                        .toString()
                        .trim();


        if (raw.isEmpty()) {

            Toast.makeText(
                    this,
                    "Напиши или кажи каква кола търсиш",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        hideKeyboard();


        SearchSpec spec =
                parseQuery(raw);


        String action =
                buildAction(spec);


        String url =
                buildEncarUrl(
                        spec,
                        action
                );


        statusText.setText(
                buildStatus(spec)
        );


        webView.loadUrl(url);
    }


    private SearchSpec parseQuery(
            String raw
    ) {

        SearchSpec spec =
                new SearchSpec();

        spec.raw = raw;


        String n =
                normalize(raw);


        spec.brand =
                findBrand(n);


        if (
                spec.brand != null
        ) {

            spec.modelGroup =
                    resolveModelGroup(
                            spec.brand.key,
                            n,
                            raw
                    );
        }


        spec.fuel =
                detectFuel(n);


        List<Integer> years =
                extractYears(raw);


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


        spec.maxMileage =
                extractMaxMileage(n);


        spec.maxPriceManWon =
                extractMaxPriceManWon(n);


        if (
                containsAny(
                        n,
                        "най евтини",
                        "най-евтини",
                        "евтини първо",
                        "lowest price",
                        "price asc",
                        "cheapest"
                )
        ) {

            spec.sort =
                    "MobilePriceAsc";

        } else {

            spec.sort =
                    "MobileModifiedDate";
        }


        return spec;
    }


    private String buildAction(
            SearchSpec spec
    ) {

        List<String> parts =
                new ArrayList<>();


        parts.add(
                "Hidden.N."
        );

        parts.add(
                "MultiViewHidden.N."
        );

        parts.add(
                "SellType.일반."
        );


        if (
                spec.maxMileage != null
        ) {

            parts.add(
                    "Mileage.range(.."
                            +
                    spec.maxMileage
                            +
                    ")."
            );
        }


        if (
                spec.maxPriceManWon != null
        ) {

            parts.add(
                    "Price.range(.."
                            +
                    spec.maxPriceManWon
                            +
                    ")."
            );
        }


        if (
                spec.yearFrom != null
                        &&
                spec.yearTo != null
        ) {

            int from =
                    spec.yearFrom * 100;

            int to =
                    spec.yearTo * 100 + 99;


            parts.add(
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
                spec.fuel != null
        ) {

            parts.add(
                    "FuelType."
                            +
                    spec.fuel
                            +
                    "."
            );
        }


        parts.add(
                buildCarBranch(spec)
        );


        return "(And."
                +
                String.join(
                        "_.",
                        parts
                )
                +
                ")";
    }


    private String buildCarBranch(
            SearchSpec spec
    ) {

        if (
                spec.brand == null
        ) {

            return "(C.CarType.A.)";
        }


        String manufacturer =
                spec.brand.encar;


        if (
                spec.modelGroup == null
                        ||
                spec.modelGroup
                        .trim()
                        .isEmpty()
        ) {

            return "(C.CarType.A._.(C.Manufacturer."
                    +
                    manufacturer
                    +
                    ".))";
        }


        return "(C.CarType.A._.(C.Manufacturer."
                +
                manufacturer
                +
                "._.(C.ModelGroup."
                +
                spec.modelGroup
                +
                ".)))";
    }


    private String buildEncarUrl(
            SearchSpec spec,
            String action
    ) {

        String json =
                "{"
                        +
                "\"type\":\"car\","
                        +
                "\"action\":\""
                        +
                jsonEscape(action)
                        +
                "\","
                        +
                "\"title\":\""
                        +
                jsonEscape(spec.raw)
                        +
                "\","
                        +
                "\"toggle\":{},"
                        +
                "\"layer\":\"\","
                        +
                "\"sort\":\""
                        +
                jsonEscape(
                        spec.sort
                )
                        +
                "\""
                        +
                "}";


        return ENCAR_BASE
                +
                Uri.encode(json);
    }


    private String buildStatus(
            SearchSpec spec
    ) {

        StringBuilder s =
                new StringBuilder();


        if (
                spec.brand != null
        ) {

            s.append("Марка: ")
                    .append(
                            spec.brand.encar
                    );

        } else {

            s.append(
                    "Марка: всички"
            );
        }


        if (
                spec.modelGroup != null
        ) {

            s.append(
                    " | Модел: "
            )
                    .append(
                            spec.modelGroup
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

                s.append(
                        " | Година: "
                )
                        .append(
                                spec.yearFrom
                        );

            } else {

                s.append(
                        " | Години: "
                )
                        .append(
                                spec.yearFrom
                        )
                        .append("-")
                        .append(
                                spec.yearTo
                        );
            }
        }


        if (
                spec.fuel != null
        ) {

            s.append(
                    " | Гориво: "
            )
                    .append(
                            spec.fuel
                    );
        }


        if (
                spec.maxMileage != null
        ) {

            s.append(
                    " | До "
            )
                    .append(
                            spec.maxMileage
                    )
                    .append(
                            " km"
                    );
        }


        if (
                spec.maxPriceManWon != null
        ) {

            s.append(
                    " | Цена до "
            )
                    .append(
                            spec.maxPriceManWon
                                    * 10000L
                    )
                    .append(
                            " KRW"
                    );
        }


        s.append(
                " | 일반 | "
        )
                .append(
                        "MobilePriceAsc"
                                .equals(
                                        spec.sort
                                )
                                ?
                                "цена ↑"
                                :
                                "последно обновени"
                );


        if (
                spec.brand != null
                        &&
                spec.modelGroup == null
        ) {

            s.append(
                    "\nМоделът не беше разпознат точно — търся по марка и останалите филтри."
            );
        }


        return s.toString();
    }


    private BrandRule findBrand(
            String n
    ) {

        for (
                BrandRule b : brands
        ) {

            for (
                    String alias : b.aliases
            ) {

                if (
                        containsAlias(
                                n,
                                alias
                        )
                ) {

                    return b;
                }
            }
        }


        return null;
    }


    private String resolveModelGroup(
            String brandKey,
            String normalized,
            String raw
    ) {

        for (
                ModelRule m : models
        ) {

            if (
                    !m.brandKey
                            .equals(
                                    brandKey
                            )
            ) {

                continue;
            }


            for (
                    String alias : m.aliases
            ) {

                if (
                        containsAlias(
                                normalized,
                                alias
                        )
                ) {

                    return m.encar;
                }
            }
        }


        String special =
                resolveSpecialModel(
                        brandKey,
                        normalized
                );


        if (
                special != null
        ) {

            return special;
        }


        return guessModelGroup(
                brandKey,
                normalized,
                raw
        );
    }


    private String resolveSpecialModel(
            String brandKey,
            String n
    ) {

        if (
                "mercedes"
                        .equals(
                                brandKey
                        )
        ) {

            Matcher suv =
                    Pattern.compile(
                            "\\b(gl[abcse]|gle|glc|gls|gla|glb)\\s*\\d*[a-z]*\\b"
                    )
                            .matcher(n);


            if (
                    suv.find()
            ) {

                String x =
                        suv.group(1)
                                .toUpperCase(
                                        Locale.ROOT
                                );


                return x
                        +
                        "-클래스";
            }


            Matcher classModel =
                    Pattern.compile(
                            "\\b([aces])\\s*\\d{3}[a-z]*\\b"
                    )
                            .matcher(n);


            if (
                    classModel.find()
            ) {

                return classModel
                        .group(1)
                        .toUpperCase(
                                Locale.ROOT
                        )
                        +
                        "-클래스";
            }
        }


        if (
                "bmw"
                        .equals(
                                brandKey
                        )
        ) {

            Matcher series =
                    Pattern.compile(
                            "\\b([1-8])\\d{2}[a-z]{0,3}\\b"
                    )
                            .matcher(n);


            if (
                    series.find()
            ) {

                return series.group(1)
                        +
                        "시리즈";
            }
        }


        return null;
    }


    private String guessModelGroup(
            String brandKey,
            String n,
            String raw
    ) {

        String cleaned =
                n;


        BrandRule brand =
                null;


        for (
                BrandRule b : brands
        ) {

            if (
                    b.key.equals(
                            brandKey
                    )
            ) {

                brand = b;

                break;
            }
        }


        if (
                brand != null
        ) {

            for (
                    String alias : brand.aliases
            ) {

                cleaned =
                        cleaned.replace(
                                normalize(alias),
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
                        "\\b\\d[\\d\\s.,]*\\s*(km|км|километра|километри|kilometers?)\\b",
                        " "
                );


        String[] noise = {

                "diesel",
                "дизел",
                "디젤",

                "petrol",
                "gasoline",
                "бензин",
                "가솔린",

                "hybrid",
                "хибрид",
                "하이브리드",

                "electric",
                "електрическа",
                "електрически",
                "전기",

                "до",
                "под",
                "max",
                "maximum",

                "най евтини",
                "най-евтини",
                "cheapest",

                "търси",
                "търсене",
                "кола",
                "автомобил",

                "година",
                "години",
                "пробег"
        };


        for (
                String x : noise
        ) {

            cleaned =
                    cleaned.replace(
                            normalize(x),
                            " "
                    );
        }


        cleaned =
                cleaned.replaceAll(
                        "\\s+",
                        " "
                )
                        .trim();


        Matcher latin =
                Pattern.compile(
                        "\\b[A-Za-z][A-Za-z0-9+.-]{1,14}\\b"
                )
                        .matcher(raw);


        while (
                latin.find()
        ) {

            String token =
                    latin.group();


            if (
                    token.equalsIgnoreCase(
                            "km"
                    )
                            ||
                    token.equalsIgnoreCase(
                            "diesel"
                    )
                            ||
                    token.equalsIgnoreCase(
                            "petrol"
                    )
                            ||
                    token.equalsIgnoreCase(
                            "gasoline"
                    )
                            ||
                    token.equalsIgnoreCase(
                            "hybrid"
                    )
                            ||
                    token.equalsIgnoreCase(
                            "electric"
                    )
            ) {

                continue;
            }


            boolean isBrandAlias =
                    false;


            if (
                    brand != null
            ) {

                for (
                        String a : brand.aliases
                ) {

                    if (
                            token.equalsIgnoreCase(
                                    a
                            )
                    ) {

                        isBrandAlias =
                                true;

                        break;
                    }
                }
            }


            if (
                    !isBrandAlias
            ) {

                return normalizeModelToken(
                        brandKey,
                        token
                );
            }
        }


        Matcher hangul =
                Pattern.compile(
                        "[가-힣][가-힣0-9\\- ]{1,20}"
                )
                        .matcher(
                                cleaned
                        );


        if (
                hangul.find()
        ) {

            return hangul.group()
                    .trim();
        }


        return null;
    }


    private String normalizeModelToken(
            String brandKey,
            String token
    ) {

        String t =
                token.trim();


        if (
                "mercedes"
                        .equals(
                                brandKey
                        )
        ) {

            String u =
                    t.toUpperCase(
                            Locale.ROOT
                    );


            if (
                    u.equals("GLE")
                            ||
                    u.equals("GLC")
                            ||
                    u.equals("GLS")
                            ||
                    u.equals("GLA")
                            ||
                    u.equals("GLB")
                            ||
                    u.equals("CLA")
                            ||
                    u.equals("CLS")
                            ||
                    u.equals("CLE")
            ) {

                return u
                        +
                        "-클래스";
            }
        }


        return t.toUpperCase(
                Locale.ROOT
        );
    }


    private String detectFuel(
            String n
    ) {

        if (
                containsAny(
                        n,
                        "plug in",
                        "plug-in",
                        "плъгин",
                        "phev"
                )
        ) {

            return "가솔린+전기";
        }


        if (
                containsAny(
                        n,
                        "diesel",
                        "дизел",
                        "дизелов",
                        "디젤"
                )
        ) {

            return "디젤";
        }


        if (
                containsAny(
                        n,
                        "hybrid",
                        "хибрид",
                        "хибриден",
                        "하이브리드"
                )
        ) {

            return "하이브리드";
        }


        if (
                containsAny(
                        n,
                        "electric",
                        "електрическа",
                        "електрически",
                        "електромобил",
                        "ev",
                        "전기"
                )
        ) {

            return "전기";
        }


        if (
                containsAny(
                        n,
                        "petrol",
                        "gasoline",
                        "бензин",
                        "бензинов",
                        "가솔린"
                )
        ) {

            return "가솔린";
        }


        if (
                containsAny(
                        n,
                        "lpg",
                        "газ"
                )
        ) {

            return "LPG";
        }


        return null;
    }


    private List<Integer> extractYears(
            String raw
    ) {

        ArrayList<Integer> out =
                new ArrayList<>();


        Matcher m =
                Pattern.compile(
                        "\\b(19\\d{2}|20\\d{2})\\b"
                )
                        .matcher(
                                raw
                        );


        while (
                m.find()
        ) {

            try {

                int y =
                        Integer.parseInt(
                                m.group(1)
                        );


                if (
                        y >= 1980
                                &&
                        y <= 2099
                ) {

                    out.add(y);
                }

            } catch (
                    Exception ignored
            ) {
            }
        }


        return out;
    }


    private Integer extractMaxMileage(
            String n
    ) {

        Matcher thousand =
                Pattern.compile(
                        "(\\d{1,3})\\s*(хиляди|хил|thousand)\\s*(km|км|километра|километри)?"
                )
                        .matcher(n);


        if (
                thousand.find()
        ) {

            try {

                return Integer.parseInt(
                        thousand.group(1)
                )
                        *
                        1000;

            } catch (
                    Exception ignored
            ) {
            }
        }


        Matcher m =
                Pattern.compile(
                        "([0-9][0-9\\s.,]{0,10})\\s*(km|км|километра|километри|kilometers?)"
                )
                        .matcher(n);


        if (
                m.find()
        ) {

            Long x =
                    parseHumanNumber(
                            m.group(1)
                    );


            if (
                    x != null
                            &&
                    x >= 0
                            &&
                    x <= 2000000
            ) {

                return x.intValue();
            }
        }


        return null;
    }


    private Integer extractMaxPriceManWon(
            String n
    ) {

        Matcher million =
                Pattern.compile(
                        "(?:до|под|max|under)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(милиона|милион|million|m)\\s*(вона|вон|won|krw)"
                )
                        .matcher(n);


        if (
                million.find()
        ) {

            try {

                double millions =
                        Double.parseDouble(
                                million.group(1)
                                        .replace(
                                                ",",
                                                "."
                                        )
                        );


                long krw =
                        Math.round(
                                millions
                                        *
                                1_000_000d
                        );


                return (int)
                        Math.max(
                                1,
                                krw / 10000L
                        );

            } catch (
                    Exception ignored
            ) {
            }
        }


        Matcher man =
                Pattern.compile(
                        "(\\d{2,6})\\s*만원"
                )
                        .matcher(n);


        if (
                man.find()
        ) {

            try {

                return Integer.parseInt(
                        man.group(1)
                );

            } catch (
                    Exception ignored
            ) {
            }
        }


        return null;
    }


    private Long parseHumanNumber(
            String value
    ) {

        if (
                value == null
        ) {

            return null;
        }


        String s =
                value.replaceAll(
                        "[^0-9]",
                        ""
                );


        if (
                s.isEmpty()
        ) {

            return null;
        }


        try {

            return Long.parseLong(s);

        } catch (
                Exception e
        ) {

            return null;
        }
    }


    private boolean containsAny(
            String text,
            String... values
    ) {

        for (
                String x : values
        ) {

            if (
                    containsAlias(
                            text,
                            x
                    )
            ) {

                return true;
            }
        }


        return false;
    }


    private boolean containsAlias(
            String normalizedText,
            String alias
    ) {

        String a =
                normalize(alias);


        if (
                a.isEmpty()
        ) {

            return false;
        }


        String padded =
                " "
                        +
                normalizedText
                        +
                " ";


        String target =
                " "
                        +
                a
                        +
                " ";


        if (
                padded.contains(
                        target
                )
        ) {

            return true;
        }


        if (
                a.contains("-")
                        ||
                a.contains(".")
                        ||
                a.contains("+")
        ) {

            return normalizedText
                    .contains(a);
        }


        return false;
    }


    private String normalize(
            String s
    ) {

        if (
                s == null
        ) {

            return "";
        }


        return s
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
                .replaceAll(
                        "[,:;!?()\\{}\"']",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


    private String jsonEscape(
            String s
    ) {

        if (
                s == null
        ) {

            return "";
        }


        return s
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                );
    }


    private void hideKeyboard() {

        try {

            InputMethodManager imm =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );


            if (
                    getCurrentFocus()
                            != null
            ) {

                imm.hideSoftInputFromWindow(
                        getCurrentFocus()
                                .getWindowToken(),
                        0
                );
            }

        } catch (
                Exception ignored
        ) {
        }
    }


    private void initRules() {

        brand(
                "mercedes",
                "벤츠",
                "mercedes",
                "mercedes-benz",
                "benz",
                "мерцедес",
                "мерседес",
                "мерцедес бенц",
                "벤츠"
        );


        brand(
                "bmw",
                "BMW",
                "bmw",
                "бмв",
                "бмв-то",
                "би ем дабълю"
        );


        brand(
                "audi",
                "아우디",
                "audi",
                "ауди",
                "아우디"
        );


        brand(
                "volkswagen",
                "폭스바겐",
                "volkswagen",
                "vw",
                "фолксваген",
                "волксваген",
                "폭스바겐"
        );


        brand(
                "porsche",
                "포르쉐",
                "porsche",
                "порше",
                "포르쉐"
        );


        brand(
                "hyundai",
                "현대",
                "hyundai",
                "хюндай",
                "хундай",
                "хендай",
                "хюнде",
                "현대"
        );


        brand(
                "kia",
                "기아",
                "kia",
                "киа",
                "кия",
                "기아"
        );


        brand(
                "genesis",
                "제네시스",
                "genesis",
                "генезис",
                "дженезис",
                "제네시스"
        );


        brand(
                "peugeot",
                "푸조",
                "peugeot",
                "пежо",
                "푸조"
        );


        brand(
                "ford",
                "포드",
                "ford",
                "форд",
                "포드"
        );


        brand(
                "honda",
                "혼다",
                "honda",
                "хонда",
                "혼다"
        );


        brand(
                "toyota",
                "토요타",
                "toyota",
                "тойота",
                "тоёта",
                "토요타"
        );


        brand(
                "lexus",
                "렉서스",
                "lexus",
                "лексус",
                "렉서스"
        );


        brand(
                "volvo",
                "볼보",
                "volvo",
                "волво",
                "볼보"
        );


        brand(
                "nissan",
                "닛산",
                "nissan",
                "нисан",
                "ниссан",
                "닛산"
        );


        brand(
                "infiniti",
                "인피니티",
                "infiniti",
                "инфинити",
                "인피니티"
        );


        brand(
                "tesla",
                "테슬라",
                "tesla",
                "тесла",
                "테슬라"
        );


        brand(
                "mini",
                "미니",
                "mini",
                "мини",
                "미니"
        );


        brand(
                "landrover",
                "랜드로버",
                "land rover",
                "landrover",
                "ленд ровър",
                "ленд ровер",
                "랜드로버"
        );


        brand(
                "jaguar",
                "재규어",
                "jaguar",
                "ягуар",
                "재규어"
        );


        brand(
                "jeep",
                "지프",
                "jeep",
                "джип",
                "지프"
        );


        brand(
                "renault",
                "르노코리아(삼성)",
                "renault",
                "рено",
                "르노"
        );


        brand(
                "chevrolet",
                "쉐보레(GM대우)",
                "chevrolet",
                "chevy",
                "шевролет",
                "쉐보레"
        );


        brand(
                "kgm",
                "KG모빌리티(쌍용)",
                "kg mobility",
                "kgm",
                "ssangyong",
                "санг йонг",
                "сангйонг",
                "쌍용",
                "kg모빌리티"
        );


        brand(
                "citroen",
                "시트로엥",
                "citroen",
                "citroën",
                "ситроен",
                "시트로엥"
        );


        brand(
                "mazda",
                "마쯔다",
                "mazda",
                "мазда",
                "마쯔다"
        );


        brand(
                "subaru",
                "스바루",
                "subaru",
                "субару",
                "스바루"
        );


        brand(
                "mitsubishi",
                "미쓰비시",
                "mitsubishi",
                "мицубиши",
                "미쓰비시"
        );


        brand(
                "suzuki",
                "스즈키",
                "suzuki",
                "сузуки",
                "스즈키"
        );


        brand(
                "fiat",
                "피아트",
                "fiat",
                "фиат",
                "피아트"
        );


        brand(
                "alfaromeo",
                "알파로메오",
                "alfa romeo",
                "алфа ромео",
                "알파로메오"
        );


        brand(
                "maserati",
                "마세라티",
                "maserati",
                "мазерати",
                "마세라티"
        );


        brand(
                "bentley",
                "벤틀리",
                "bentley",
                "бентли",
                "벤틀리"
        );


        brand(
                "rollsroyce",
                "롤스로이스",
                "rolls royce",
                "rolls-royce",
                "ролс ройс",
                "롤스로이스"
        );


        brand(
                "ferrari",
                "페라리",
                "ferrari",
                "ферари",
                "페라리"
        );


        brand(
                "lamborghini",
                "람보르기니",
                "lamborghini",
                "ламборгини",
                "람보르기니"
        );


        brand(
                "mclaren",
                "맥라렌",
                "mclaren",
                "макларън",
                "макларен",
                "맥라렌"
        );


        brand(
                "astonmartin",
                "애스턴마틴",
                "aston martin",
                "астън мартин",
                "애스턴마틴"
        );


        brand(
                "cadillac",
                "캐딜락",
                "cadillac",
                "кадилак",
                "캐딜락"
        );


        brand(
                "
