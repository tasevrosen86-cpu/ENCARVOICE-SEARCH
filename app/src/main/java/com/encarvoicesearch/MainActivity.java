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

    private static final String HOME =
            "https://m.encar.com/ca/search.do";

    private static final String SEARCH =
            "https://m.encar.com/ca/search.do#!";

    private EditText input;
    private TextView status;
    private WebView webView;

    private final Map<String, Brand> brands =
            new LinkedHashMap<>();

    private final Map<String, Map<String, String>> modelGroups =
            new LinkedHashMap<>();


    private static class Brand {

        final String key;
        final String encarName;
        final String carType;

        Brand(
                String key,
                String encarName,
                String carType
        ) {
            this.key = key;
            this.encarName = encarName;
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


    @SuppressLint("SetJavaScriptEnabled")
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
                webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setDatabaseEnabled(true);

        settings.setLoadsImagesAutomatically(true);

        settings.setUseWideViewPort(true);

        settings.setLoadWithOverviewMode(true);


        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        cookieManager.setAcceptThirdPartyCookies(
                webView,
                true
        );


        webView.setWebViewClient(
                new WebViewClient()
        );


        webView.loadUrl(
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
                "Mercedes GLE 2024 дизел до 100000 км"
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


        webView =
                new WebView(this);


        root.addView(
                title
        );

        root.addView(
                input
        );

        root.addView(
                row
        );

        root.addView(
                status
        );


        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
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


        hideKeyboard();


        Spec spec =
                parse(raw);


        if (
                spec.brand == null
        ) {

            status.setText(
                    "Не разпознах марката. Поправи текста и натисни ТЪРСИ."
            );

            Toast.makeText(
                    this,
                    "Марка не е разпозната",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        String action =
                buildAction(spec);


        String state =
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


        webView.loadUrl(
                SEARCH
                        +
                Uri.encode(state)
        );
    }


    private Spec parse(
            String raw
    ) {

        Spec spec =
                new Spec();


        String text =
                normalize(raw);


        spec.brand =
                findBrand(text);


        if (
                spec.brand != null
        ) {

            spec.modelGroup =
                    findModelGroup(
                            text,
                            spec.brand
                    );
        }


        spec.fuel =
                findFuel(text);


        spec.maxMileage =
                findMileage(text);


        List<Integer> years =
                findYears(raw);


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
            Spec spec
    ) {

        List<String> parts =
                new ArrayList<>();


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


        parts.add(
                "Hidden.N."
        );


        parts.add(
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
                join(
                        parts,
                        "_."
                )
                +
                ")";
    }


    private String buildCarBranch(
            Spec spec
    ) {

        StringBuilder query =
                new StringBuilder();


        query.append(
                "(C.CarType."
        );


        query.append(
                spec.brand.carType
        );


        query.append(
                "._.(C.Manufacturer."
        );


        query.append(
                spec.brand.encarName
        );


        query.append(
                "."
        );


        if (
                spec.modelGroup != null
                        &&
                !spec.modelGroup.isEmpty()
        ) {

            query.append(
                    "_.(C.ModelGroup."
            );


            query.append(
                    spec.modelGroup
            );


            query.append(
                    ".)"
            );
        }


        query.append(
                "))"
        );


        return query.toString();
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
                brands.entrySet()
        ) {

            String alias =
                    entry.getKey();


            if (
                    containsPhrase(
                            text,
                            alias
                    )
                            &&
                    alias.length()
                            >
                    bestLength
            ) {

                best =
                        entry.getValue();


                bestLength =
                        alias.length();
            }
        }


        return best;
    }


    private String findModelGroup(
            String text,
            Brand brand
    ) {

        Map<String, String> map =
                modelGroups.get(
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


            Matcher classMatcher =
                    Pattern.compile(
                            "\\b([acesg])\\s*[- ]?\\d{3}[a-z]*\\b"
                    )
                            .matcher(
                                    text
                            );


            if (
                    classMatcher.find()
            ) {

                return classMatcher
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

                return series.group(1)
                        +
                        "시리즈";
            }
        }


        String generic =
                genericModelToken(
                        text,
                        brand
                );


        if (
                generic != null
                        &&
                isGenericLatinBrand(
                        brand.key
                )
        ) {

            return generic;
        }


        return null;
    }


    private String genericModelToken(
            String text,
            Brand brand
    ) {

        String cleaned =
                " "
                        +
                text
                        +
                " ";


        for (
                Map.Entry<String, Brand> entry
                        :
                brands.entrySet()
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
                "електрически",
                "електрическа",

                "ev",

                "до",
                "под",

                "най",
                "евтини",
                "най-евтини",

                "кола",
                "автомобил",

                "търси"
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
                        "\\b[a-zA-Z0-9][a-zA-Z0-9.-]{0,12}\\b"
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


    private boolean isGenericLatinBrand(
            String key
    ) {

        return "audi".equals(key)
                ||
                "bmw".equals(key)
                ||
                "lexus".equals(key)
                ||
                "volvo".equals(key)
                ||
                "peugeot".equals(key)
                ||
                "nissan".equals(key)
                ||
                "infiniti".equals(key)
                ||
                "jaguar".equals(key)
                ||
                "cadillac".equals(key)
                ||
                "lincoln".equals(key)
                ||
                "tesla".equals(key)
                ||
                "mini".equals(key)
                ||
                "polestar".equals(key)
                ||
                "byd".equals(key)
                ||
                "gmc".equals(key);
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
                        "electric",
                        "електрически",
                        "електрическа",
                        "електромобил",
                        "ev"
                )
        ) {

            return "전기";
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
                    NumberFormatException ignored
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
                    NumberFormatException ignored
            ) {
            }
        }


        Matcher kilometers =
                Pattern.compile(
                        "([0-9][0-9 .]{1,10})\\s*(km|км|километра|километри)"
                )
                        .matcher(
                                text
                        );


        if (
                kilometers.find()
        ) {

            String digits =
                    kilometers
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
            Spec spec
    ) {

        StringBuilder result =
                new StringBuilder(
                        "Цена: най-евтините първо"
                );


        result.append(
                " | "
        );

        result.append(
                spec.brand.encarName
        );


        if (
                spec.modelGroup != null
        ) {

            result.append(
                    " | "
            );

            result.append(
                    spec.modelGroup
            );

        } else {

            result.append(
                    " | моделът не е разпознат точно"
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


    private void initDictionary() {

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
                "kia",
                "기아",
                "Y",
                "kia",
                "киа",
                "кия"
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


        addBrand(
                "mercedes",
                "벤츠",
                "N",
                "mercedes",
                "mercedes-benz",
                "mercedes benz",
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
                "volvo",
                "볼보",
                "N",
                "volvo",
                "волво"
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
                "peugeot",
                "푸조",
                "N",
                "peugeot",
                "пежо"
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
                "toyota",
                "도요타",
                "N",
                "toyota",
                "тойота"
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
                "lexus",
                "렉서스",
                "N",
                "lexus",
                "лексус"
        );


        addBrand(
                "ferrari",
                "페라리",
                "N",
                "ferrari",
                "ферари"
        );


        addBrand(
                "lincoln",
                "링컨",
                "N",
                "lincoln",
                "линкълн"
        );


        addBrand(
                "mini",
                "미니",
                "N",
                "mini",
                "мини"
        );


        addBrand(
                "infiniti",
                "인피니티",
                "N",
                "infiniti",
                "инфинити"
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
                "tesla",
                "테슬라",
                "N",
                "tesla",
                "тесла"
        );


        addBrand(
                "polestar",
                "폴스타",
                "N",
                "polestar",
                "полстар"
        );


        addBrand(
                "cadillac",
                "캐딜락",
                "N",
                "cadillac",
                "кадилак"
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

                "id.4=ID.4",

                "id4=ID.4",

                "id.5=ID.5",

                "id5=ID.5"
        );


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

                "grandeur=그랜저"
        );


        addModels(
                "genesis",

                "g70=G70",

                "g80=G80",

                "g90=G90",

                "gv60=GV60",

                "gv70=GV70",

                "gv80=GV80"
        );


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


        addModels(
                "toyota",

                "rav4=RAV4",

                "rav 4=RAV4",

                "camry=캠리",

                "камри=캠리",

                "prius=프리우스",

                "highlander=하이랜더",

                "sienna=시에나",

                "land cruiser=랜드크루저"
        );


        addModels(
                "landrover",

                "range rover sport=레인지로버 스포츠",

                "range rover=레인지로버",

                "evoque=레인지로버 이보크",

                "velar=레인지로버 벨라",

                "discovery=디스커버리",

                "defender=디펜더"
        );


        addModels(
                "tesla",

                "model 3=모델 3",

                "model y=모델 Y",

                "model s=모델 S",

                "model x=모델 X"
        );


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


        addModels(
                "kgm",

                "torres=토레스",

                "rexton=렉스턴",

                "korando=코란도",

                "tivoli=티볼리"
        );
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

            brands.put(
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
                modelGroups.get(
                        brandKey
                );


        if (
                map == null
        ) {

            map =
                    new LinkedHashMap<>();


            modelGroups.put(
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
                normalize(value);


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


    private String join(
            List<String> list,
            String separator
    ) {

        StringBuilder builder =
                new StringBuilder();


        for (
                int i = 0;
                i < list.size();
                i++
        ) {

            if (
                    i > 0
            ) {

                builder.append(
                        separator
                );
            }


            builder.append(
                    list.get(i)
            );
        }


        return builder.toString();
    }


    private String jsonEscape(
            String value
    ) {

        if (
                value == null
        ) {

            return "";
        }


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
