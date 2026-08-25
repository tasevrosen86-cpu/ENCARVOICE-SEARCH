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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;

    private static final String ENCAR_URL =
            "https://m.encar.com/ca/search.do#!";

    private EditText input;
    private TextView status;
    private WebView webView;

    private final Map<String, String> brandAliases =
            new LinkedHashMap<>();

    private final Map<String, String> modelAliases =
            new LinkedHashMap<>();


    private static class SearchSpec {

        String brand;

        String model;

        String fuel;

        Integer yearFrom;

        Integer yearTo;

        Integer maxMileage;
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        initAliases();

        buildUi();


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

        title.setTextSize(20f);

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
                "Mercedes GLE 2024 diesel до 100000 km"
        );

        input.setTextSize(17f);

        input.setMinLines(2);

        input.setMaxLines(4);

        input.setPadding(
                18,
                12,
                18,
                12
        );


        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
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

                    status.setText(
                            "Готово за ново търсене"
                    );

                    input.requestFocus();
                }
        );


        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );


        row.addView(
                voice,
                buttonParams
        );

        row.addView(
                search,
                buttonParams
        );

        row.addView(
                clear,
                buttonParams
        );


        status =
                new TextView(this);

        status.setText(
                "Кажи или напиши марка, модел, година, гориво и пробег"
        );

        status.setTextSize(13f);

        status.setTextColor(
                Color.DKGRAY
        );

        status.setPadding(
                14,
                8,
                14,
                8
        );


        webView =
                new WebView(this);


        root.addView(title);

        root.addView(input);

        root.addView(row);

        root.addView(status);


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
                "Например: Mercedes GLE 2024 дизел до 100000 километра"
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

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );


            if (
                    results != null
                            &&
                    !results.isEmpty()
            ) {

                input.setText(
                        results.get(0)
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


        if (raw.isEmpty()) {

            Toast.makeText(
                    this,
                    "Въведи автомобил",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        hideKeyboard();


        SearchSpec spec =
                parse(raw);


        String action =
                buildAction(spec);


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
                jsonEscape(raw)
                        +
                "\","
                        +
                "\"toggle\":{},"
                        +
                "\"layer\":\"\","
                        +
                "\"sort\":\"MobilePriceAsc\""
                        +
                "}";


        status.setText(
                buildStatus(spec)
        );


        String finalUrl =
                ENCAR_URL
                        +
                Uri.encode(json);


        webView.loadUrl(
                finalUrl
        );
    }


    private SearchSpec parse(
            String raw
    ) {

        SearchSpec spec =
                new SearchSpec();


        String text =
                normalize(raw);


        spec.brand =
                resolveBrand(text);


        spec.model =
                resolveModel(
                        text,
                        spec.brand
                );


        spec.fuel =
                resolveFuel(text);


        spec.maxMileage =
                extractMileage(text);


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

            int min =
                    years.get(0);

            int max =
                    years.get(0);


            for (
                    int year : years
            ) {

                if (
                        year < min
                ) {

                    min =
                            year;
                }


                if (
                        year > max
                ) {

                    max =
                            year;
                }
            }


            spec.yearFrom =
                    min;

            spec.yearTo =
                    max;
        }


        return spec;
    }


    private String buildAction(
            SearchSpec spec
    ) {

        StringBuilder query =
                new StringBuilder(
                        "(And."
                );


        query.append(
                "Hidden.N."
        );


        query.append(
                "_.MultiViewHidden.N."
        );


        query.append(
                "_.SellType.일반."
        );


        if (
                spec.maxMileage != null
        ) {

            query.append(
                    "_.Mileage.range(.."
            );

            query.append(
                    spec.maxMileage
            );

            query.append(
                    ")."
            );
        }


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


            query.append(
                    "_.Year.range("
            );

            query.append(
                    from
            );

            query.append(
                    ".."
            );

            query.append(
                    to
            );

            query.append(
                    ")."
            );
        }


        if (
                spec.fuel != null
        ) {

            query.append(
                    "_.FuelType."
            );

            query.append(
                    spec.fuel
            );

            query.append(
                    "."
            );
        }


        if (
                spec.brand == null
        ) {

            query.append(
                    "_.(C.CarType.A.)"
            );

        } else {

            query.append(
                    "_.(C.CarType.A._.(C.Manufacturer."
            );


            query.append(
                    spec.brand
            );


            query.append(
                    "."
            );


            if (
                    spec.model != null
                            &&
                    !spec.model.isEmpty()
            ) {

                query.append(
                        "_.(C.ModelGroup."
                );

                query.append(
                        spec.model
                );

                query.append(
                        ".)"
                );
            }


            query.append(
                    "))"
            );
        }


        query.append(
                ")"
        );


        return query.toString();
    }


    private String resolveBrand(
            String text
    ) {

        for (
                Map.Entry<String, String> entry
                        :
                brandAliases.entrySet()
        ) {

            if (
                    hasWord(
                            text,
                            entry.getKey()
                    )
            ) {

                return entry.getValue();
            }
        }


        return null;
    }


    private String resolveModel(
            String text,
            String brand
    ) {

        if (
                brand == null
        ) {

            return null;
        }


        for (
                Map.Entry<String, String> entry
                        :
                modelAliases.entrySet()
        ) {

            String prefix =
                    brand
                            +
                    "|";


            if (
                    entry
                            .getKey()
                            .startsWith(prefix)
            ) {

                String alias =
                        entry
                                .getKey()
                                .substring(
                                        prefix.length()
                                );


                if (
                        hasWord(
                                text,
                                alias
                        )
                ) {

                    return entry.getValue();
                }
            }
        }


        /*
         * MERCEDES:
         * GLE 300d -> GLE-класа
         * E220d -> E-класа
         * C220 -> C-класа
         */
        if (
                "벤츠".equals(
                        brand
                )
        ) {

            Matcher matcher =
                    Pattern.compile(
                            "\\b(gle|glc|gls|gla|glb|cla|cls|cle|[acesg])\\s*-?\\s*\\d{0,3}[a-z]*\\b"
                    )
                            .matcher(text);


            if (
                    matcher.find()
            ) {

                return matcher
                        .group(1)
                        .toUpperCase(
                                Locale.ROOT
                        )
                        +
                        "-클래스";
            }
        }


        /*
         * BMW:
         * 520d -> 5 Series
         * 320d -> 3 Series
         */
        if (
                "BMW".equals(
                        brand
                )
        ) {

            Matcher matcher =
                    Pattern.compile(
                            "\\b([1-8])\\d{2}[a-z]*\\b"
                    )
                            .matcher(text);


            if (
                    matcher.find()
            ) {

                return matcher
                        .group(1)
                        +
                        "시리즈";
            }
        }


        /*
         * Ако моделът не е в списъка,
         * опитваме да го използваме директно.
         */
        return genericModel(
                text,
                brand
        );
    }


    private String genericModel(
            String text,
            String brand
    ) {

        String cleaned =
                text;


        for (
                Map.Entry<String, String> entry
                        :
                brandAliases.entrySet()
        ) {

            if (
                    brand.equals(
                            entry.getValue()
                    )
            ) {

                cleaned =
                        cleaned.replace(
                                entry.getKey(),
                                " "
                        );
            }
        }


        cleaned =
                cleaned.replaceAll(
                        "\\b(19|20)\\d{2}\\b",
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
                "електромобил",

                "до",
                "под",
                "пробег",

                "километра",
                "километри",

                "km",
                "км",

                "най евтини",
                "най-евтини",

                "търси",
                "кола",
                "автомобил"
        };


        for (
                String word : noise
        ) {

            cleaned =
                    cleaned.replace(
                            normalize(word),
                            " "
                    );
        }


        cleaned =
                cleaned.replaceAll(
                        "\\d{4,}",
                        " "
                );


        cleaned =
                cleaned.replaceAll(
                        "\\s+",
                        " "
                )
                        .trim();


        if (
                cleaned.isEmpty()
        ) {

            return null;
        }


        String[] parts =
                cleaned.split(
                        " "
                );


        for (
                String part : parts
        ) {

            String candidate =
                    part.trim();


            if (
                    candidate.length()
                            >=
                    2
            ) {

                return candidate
                        .toUpperCase(
                                Locale.ROOT
                        );
            }
        }


        return null;
    }


    private String resolveFuel(
            String text
    ) {

        if (
                containsAny(
                        text,
                        "plug-in",
                        "plug in",
                        "phev",
                        "плъгин"
                )
        ) {

            return "가솔린+전기";
        }


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
                        "хибриден"
                )
        ) {

            return "하이브리드";
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


    private List<Integer> extractYears(
            String raw
    ) {

        List<Integer> result =
                new ArrayList<>();


        Matcher matcher =
                Pattern.compile(
                        "(19\\d{2}|20\\d{2})"
                )
                        .matcher(raw);


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
                    NumberFormatException ignored
            ) {
            }
        }


        return result;
    }


    private Integer extractMileage(
            String text
    ) {

        Matcher thousands =
                Pattern.compile(
                        "(\\d{1,3})\\s*(хиляди|хил|thousand)"
                )
                        .matcher(text);


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
                    NumberFormatException ignored
            ) {
            }
        }


        Matcher km =
                Pattern.compile(
                        "([0-9][0-9 .,]{1,10})\\s*(km|км|километра|километри)"
                )
                        .matcher(text);


        if (
                km.find()
        ) {

            String digits =
                    km
                            .group(1)
                            .replaceAll(
                                    "[^0-9]",
                                    ""
                            );


            try {

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
                    NumberFormatException ignored
            ) {
            }
        }


        return null;
    }


    private String buildStatus(
            SearchSpec spec
    ) {

        StringBuilder result =
                new StringBuilder(
                        "Цена ↑ от най-евтини"
                );


        if (
                spec.brand != null
        ) {

            result.append(
                    " | "
            );

            result.append(
                    spec.brand
            );
        }


        if (
                spec.model != null
        ) {

            result.append(
                    " "
            );

            result.append(
                    spec.model
            );
        }


        if (
                spec.yearFrom != null
        ) {

            if (
                    spec.yearFrom
                            .equals(
                                    spec.yearTo
                            )
            ) {

                result.append(
                        " | "
                );

                result.append(
                        spec.yearFrom
                );

            } else {

                result.append(
                        " | "
                );

                result.append(
                        spec.yearFrom
                );

                result.append(
                        "-"
                );

                result.append(
                        spec.yearTo
                );
            }
        }


        if (
                spec.fuel != null
        ) {

            result.append(
                    " | "
            );

            result.append(
                    spec.fuel
            );
        }


        if (
                spec.maxMileage != null
        ) {

            result.append(
                    " | до "
            );

            result.append(
                    spec.maxMileage
            );

            result.append(
                    " km"
            );
        }


        return result.toString();
    }


    private void initAliases() {

        /*
         * МАРКИ
         */

        brand(
                "벤츠",
                "mercedes",
                "mercedes-benz",
                "benz",
                "мерцедес",
                "мерседес"
        );


        brand(
                "BMW",
                "bmw",
                "бмв"
        );


        brand(
                "아우디",
                "audi",
                "ауди"
        );


        brand(
                "폭스바겐",
                "volkswagen",
                "vw",
                "фолксваген"
        );


        brand(
                "포르쉐",
                "porsche",
                "порше"
        );


        brand(
                "기아",
                "kia",
                "киа",
                "кия"
        );


        brand(
                "현대",
                "hyundai",
                "хюндай",
                "хундай",
                "хендай"
        );


        brand(
                "제네시스",
                "genesis",
                "генезис"
        );


        brand(
                "포드",
                "ford",
                "форд"
        );


        brand(
                "혼다",
                "honda",
                "хонда"
        );


        brand(
                "푸조",
                "peugeot",
                "пежо"
        );


        brand(
                "도요타",
                "toyota",
                "тойота"
        );


        brand(
                "렉서스",
                "lexus",
                "лексус"
        );


        brand(
                "볼보",
                "volvo",
                "волво"
        );


        brand(
                "닛산",
                "nissan",
                "нисан",
                "ниссан"
        );


        brand(
                "인피니티",
                "infiniti",
                "инфинити"
        );


        brand(
                "테슬라",
                "tesla",
                "тесла"
        );


        brand(
                "미니",
                "mini",
                "мини"
        );


        brand(
                "랜드로버",
                "land rover",
                "landrover",
                "ленд ровър",
                "ленд ровер"
        );


        brand(
                "재규어",
                "jaguar",
                "ягуар"
        );


        brand(
                "지프",
                "jeep",
                "джип"
        );


        brand(
                "쉐보레(GM대우)",
                "chevrolet",
                "chevy",
                "шевролет"
        );


        brand(
                "르노코리아(삼성)",
                "renault",
                "рено"
        );


        brand(
                "KG모빌리티(쌍용)",
                "kgm",
                "ssangyong",
                "сангйонг"
        );


        brand(
                "캐딜락",
                "cadillac",
                "кадилак"
        );


        brand(
                "링컨",
                "lincoln",
                "линкълн"
        );


        brand(
                "닷지",
                "dodge",
                "додж"
        );


        brand(
                "크라이슬러",
                "chrysler",
                "крайслер"
        );


        brand(
                "마세라티",
                "maserati",
                "мазерати"
        );


        brand(
                "벤틀리",
                "bentley",
                "бентли"
        );


        brand(
                "페라리",
                "ferrari",
                "ферари"
        );


        brand(
                "람보르기니",
                "lamborghini",
                "ламборгини"
        );


        brand(
                "맥라렌",
                "mclaren",
                "макларен",
                "макларън"
        );


        brand(
                "애스턴마틴",
                "aston martin",
                "астън мартин"
        );


        brand(
                "스바루",
                "subaru",
                "субару"
        );


        brand(
                "스즈키",
                "suzuki",
                "сузуки"
        );


        brand(
                "미쯔비시",
                "mitsubishi",
                "мицубиши"
        );


        brand(
                "마쯔다",
                "mazda",
                "мазда"
        );


        brand(
                "시트로엥/DS",
                "citroen",
                "citroën",
                "ситроен"
        );


        brand(
                "피아트",
                "fiat",
                "фиат"
        );


        brand(
                "폴스타",
                "polestar",
                "полстар"
        );


        brand(
                "BYD",
                "byd"
        );


        brand(
                "GMC",
                "gmc"
        );


        /*
         * MERCEDES
         */

        model(
                "벤츠",
                "GLE-클래스",
                "gle"
        );


        model(
                "벤츠",
                "GLC-클래스",
                "glc"
        );


        model(
                "벤츠",
                "GLS-클래스",
                "gls"
        );


        model(
                "벤츠",
                "GLA-클래스",
                "gla"
        );


        model(
                "벤츠",
                "GLB-클래스",
                "glb"
        );


        model(
                "벤츠",
                "CLA-클래스",
                "cla"
        );


        model(
                "벤츠",
                "CLS-클래스",
                "cls"
        );


        model(
                "벤츠",
                "CLE-클래스",
                "cle"
        );


        model(
                "벤츠",
                "EQA",
                "eqa"
        );


        model(
                "벤츠",
                "EQB",
                "eqb"
        );


        model(
                "벤츠",
                "EQE",
                "eqe"
        );


        model(
                "벤츠",
                "EQS",
                "eqs"
        );


        /*
         * BMW
         */

        models(
                "BMW",
                new String[][]{

                        {"X1", "x1"},

                        {"X2", "x2"},

                        {"X3", "x3"},

                        {"X4", "x4"},

                        {"X5", "x5"},

                        {"X6", "x6"},

                        {"X7", "x7"},

                        {"Z4", "z4"},

                        {"I4", "i4"},

                        {"I5", "i5"},

                        {"I7", "i7"},

                        {"IX", "ix"}
                }
        );


        /*
         * AUDI
         */

        models(
                "아우디",
                new String[][]{

                        {"A3", "a3"},

                        {"A4", "a4"},

                        {"A5", "a5"},

                        {"A6", "a6"},

                        {"A7", "a7"},

                        {"A8", "a8"},

                        {"Q3", "q3"},

                        {"Q4", "q4"},

                        {"Q5", "q5"},

                        {"Q7", "q7"},

                        {"Q8", "q8"},

                        {"TT", "tt"},

                        {"R8", "r8"}
                }
        );


        /*
         * VOLKSWAGEN
         */

        model(
                "폭스바겐",
                "티구안",
                "tiguan",
                "тигуан"
        );


        model(
                "폭스바겐",
                "투아렉",
                "touareg",
                "туарег"
        );


        model(
                "폭스바겐",
                "골프",
                "golf",
                "голф"
        );


        model(
                "폭스바겐",
                "파사트",
                "passat",
                "пасат"
        );


        model(
                "폭스바겐",
                "아테온",
                "arteon",
                "артеон"
        );


        model(
                "폭스바겐",
                "티록",
                "t-roc",
                "t roc"
        );


        model(
                "폭스바겐",
                "ID.4",
                "id.4",
                "id4"
        );


        model(
                "폭스바겐",
                "ID.5",
                "id.5",
                "id5"
        );


        /*
         * PORSCHE
         */

        model(
                "포르쉐",
                "카이엔",
                "cayenne",
                "кайен"
        );


        model(
                "포르쉐",
                "마칸",
                "macan",
                "макан"
        );


        model(
                "포르쉐",
                "파나메라",
                "panamera",
                "панамера"
        );


        model(
                "포르쉐",
                "타이칸",
                "taycan",
                "тайкан"
        );


        model(
                "포르쉐",
                "911",
                "911"
        );


        model(
                "포르쉐",
                "718",
                "718"
        );


        /*
         * KIA
         */

        model(
                "기아",
                "쏘렌토",
                "sorento",
                "соренто"
        );


        model(
                "기아",
                "스포티지",
                "sportage",
                "спортидж",
                "спортиж"
        );


        model(
                "기아",
                "카니발",
                "carnival"
        );


        model(
                "기아",
                "셀토스",
                "seltos"
        );


        model(
                "기아",
                "니로",
                "niro"
        );


        model(
                "기아",
                "모하비",
                "mohave"
        );


        model(
                "기아",
                "EV6",
                "ev6"
        );


        model(
                "기아",
                "EV9",
                "ev9"
        );


        model(
                "기아",
                "K5",
                "k5"
        );


        model(
                "기아",
                "K8",
                "k8"
        );


        model(
                "기아",
                "K9",
                "k9"
        );


        /*
         * HYUNDAI
         */

        model(
                "현대",
                "투싼",
                "tucson",
                "тусон"
        );


        model(
                "현대",
                "싼타페",
                "santa fe",
                "santafe",
                "санта фе"
        );


        model(
                "현대",
                "팰리세이드",
                "palisade",
                "палисейд"
        );


        model(
                "현대",
                "코나",
                "kona"
        );


        model(
                "현대",
                "아이오닉5",
                "ioniq 5",
                "ioniq5"
        );


        model(
                "현대",
                "아이오닉6",
                "ioniq 6",
                "ioniq6"
        );


        model(
                "현대",
                "스타리아",
                "staria"
        );


        model(
                "현대",
                "아반떼",
                "avante",
                "elantra"
        );


        model(
                "현대",
                "쏘나타",
                "sonata"
        );


        model(
                "현대",
                "그랜저",
                "grandeur"
        );


        /*
         * GENESIS
         */

        models(
                "제네시스",
                new String[][]{

                        {"G70", "g70"},

                        {"G80", "g80"},

                        {"G90", "g90"},

                        {"GV60", "gv60"},

                        {"GV70", "gv70"},

                        {"GV80", "gv80"}
                }
        );


        /*
         * FORD
         */

        model(
                "포드",
                "익스플로러",
                "explorer"
        );


        model(
                "포드",
                "머스탱",
                "mustang"
        );


        model(
                "포드",
                "레인저",
                "ranger"
        );


        model(
                "포드",
                "브롱코",
                "bronco"
        );


        model(
                "포드",
                "F150",
                "f150",
                "f-150"
        );


        /*
         * HONDA
         */

        model(
                "혼다",
                "어코드",
                "accord"
        );


        model(
                "혼다",
                "시빅",
                "civic"
        );


        model(
                "혼다",
                "CR-V",
                "cr-v",
                "crv"
        );


        model(
                "혼다",
                "HR-V",
                "hr-v",
                "hrv"
        );


        model(
                "혼다",
                "오딧세이",
                "odyssey"
        );


        model(
                "혼다",
                "파일럿",
                "pilot"
        );


        /*
         * PEUGEOT
         */

        models(
                "푸조",
                new String[][]{

                        {"208", "208"},

                        {"308", "308"},

                        {"408", "408"},

                        {"508", "508"},

                        {"2008", "2008"},

                        {"3008", "3008"},

                        {"5008", "5008"}
                }
        );


        /*
         * TOYOTA
         */

        model(
                "도요타",
                "RAV4",
                "rav4",
                "rav 4"
        );


        model(
                "도요타",
                "캠리",
                "camry"
        );


        model(
                "도요타",
                "프리우스",
                "prius"
        );


        model(
                "도요타",
                "하이랜더",
                "highlander"
        );


        model(
                "도요타",
                "시에나",
                "sienna"
        );


        /*
         * LEXUS
         */

        models(
                "렉서스",
                new String[][]{

                        {"UX", "ux"},

                        {"NX", "nx"},

                        {"RX", "rx"},

                        {"GX", "gx"},

                        {"LX", "lx"},

                        {"IS", "is"},

                        {"ES", "es"},

                        {"LS", "ls"},

                        {"RC", "rc"},

                        {"LC", "lc"}
                }
        );


        /*
         * VOLVO
         */

        models(
                "볼보",
                new String[][]{

                        {"XC40", "xc40"},

                        {"XC60", "xc60"},

                        {"XC90", "xc90"},

                        {"S60", "s60"},

                        {"S90", "s90"},

                        {"V60", "v60"},

                        {"V90", "v90"},

                        {"EX30", "ex30"},

                        {"EX90", "ex90"}
                }
        );


        /*
         * TESLA
         */

        model(
                "테슬라",
                "모델 3",
                "model 3"
        );


        model(
                "테슬라",
                "모델 Y",
                "model y"
        );


        model(
                "테슬라",
                "모델 S",
                "model s"
        );


        model(
                "테슬라",
                "모델 X",
                "model x"
        );
    }


    private void brand(
            String encarName,
            String... aliases
    ) {

        for (
                String alias : aliases
        ) {

            brandAliases.put(
                    normalize(alias),
                    encarName
            );
        }
    }


    private void model(
            String brand,
            String encarModel,
            String... aliases
    ) {

        for (
                String alias : aliases
        ) {

            modelAliases.put(
                    brand
                            +
                    "|"
                            +
                    normalize(alias),
                    encarModel
            );
        }
    }


    private void models(
            String brand,
            String[][] rows
    ) {

        for (
                String[] row : rows
        ) {

            model(
                    brand,
                    row[0],
                    row[1]
            );
        }
    }


    private boolean containsAny(
            String text,
            String... values
    ) {

        for (
                String value : values
        ) {

            if (
                    hasWord(
                            text,
                            value
                    )
            ) {

                return true;
            }
        }


        return false;
    }


    private boolean hasWord(
            String text,
            String value
    ) {

        String needle =
                normalize(value);


        if (
                needle.isEmpty()
        ) {

            return false;
        }


        return (
                " "
                        +
                text
                        +
                " "
        )
                .contains(
                        " "
                                +
                        needle
                                +
                        " "
                );
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


    private String jsonEscape(
            String value
    ) {

        return value
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

            InputMethodManager manager =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );


            if (
                    getCurrentFocus()
                            != null
            ) {

                manager.hideSoftInputFromWindow(
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


    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {

        if (
                webView != null
                        &&
                webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }


    @Override
    protected void onDestroy() {

        if (
                webView != null
        ) {

            webView.stopLoading();

            webView.destroy();
        }


        super.onDestroy();
    }
            }
