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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;
    private static final String ENCAR_SEARCH =
            "https://m.encar.com/ca/search.do#!";

    private EditText input;
    private TextView status;
    private WebView webView;

    private final Map<String, Brand> brands = new HashMap<>();
    private final Map<String, Map<String, String>> models = new HashMap<>();

    private static class Brand {
        final String key;
        final String encar;
        final String carType;

        Brand(String key, String encar, String carType) {
            this.key = key;
            this.encar = encar;
            this.carType = carType;
        }
    }

    private static class SearchSpec {
        Brand brand;
        String modelGroup;
        String fuel;

        Integer yearFrom;
        Integer yearTo;
        Integer maxMileage;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        initDictionary();
        buildUi();

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager.getInstance()
                .setAcceptCookie(true);

        CookieManager.getInstance()
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
                "Mercedes GLE 2024 дизел до 100000 км"
        );

        input.setTextSize(17f);
        input.setMinLines(2);
        input.setMaxLines(4);

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

        } catch (ActivityNotFoundException e) {

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
                        input.getText().length()
                );
            }
        }
    }

    private void runSearch() {

        String raw =
                input.getText()
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
                escapeJson(action)
                        +
                "\","
                        +
                "\"title\":\""
                        +
                escapeJson(raw)
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
                ENCAR_SEARCH
                        +
                Uri.encode(json)
        );
    }

    private SearchSpec parse(
            String raw
    ) {

        String text =
                normalize(raw);

        SearchSpec spec =
                new SearchSpec();

        spec.brand =
                findBrand(text);

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

                min =
                        Math.min(
                                min,
                                year
                        );

                max =
                        Math.max(
                                max,
                                year
                        );
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
                        "(And.Hidden.N._.MultiViewHidden.N._.SellType.일반."
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
                    spec.yearFrom * 100;

            int to =
                    spec.yearTo * 100 + 99;

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
                    "_.(C.CarType."
            );

            query.append(
                    spec.brand.carType
            );

            query.append(
                    "._.(C.Manufacturer."
            );

            query.append(
                    spec.brand.encar
            );

            query.append(
                    "."
            );

            if (
                    spec.modelGroup != null
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
        }

        query.append(
                ")"
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
                    hasPhrase(
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

    private String findModel(
            String text,
            Brand brand
    ) {

        Map<String, String> map =
                models.get(
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
                        hasPhrase(
                                text,
                                entry.getKey()
                        )
                                &&
                        entry.getKey()
                                .length()
                                >
                        bestLength
                ) {

                    best =
                            entry.getValue();

                    bestLength =
                            entry.getKey()
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
                "mercedes"
                        .equals(
                                brand.key
                        )
        ) {

            Matcher matcher =
                    Pattern.compile(
                            "\\b(gle|glc|gls|gla|glb|cla|cls|cle)\\s*\\d*[a-z]*\\b"
                    )
                            .matcher(
                                    text
                            );

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

            Matcher classMatcher =
                    Pattern.compile(
                            "\\b([aces])\\s*\\d{3}[a-z]*\\b"
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
                "bmw"
                        .equals(
                                brand.key
                        )
        ) {

            Matcher matcher =
                    Pattern.compile(
                            "\\b([1-8])\\d{2}[a-z]*\\b"
                    )
                            .matcher(
                                    text
                            );

            if (
                    matcher.find()
            ) {

                return matcher
                        .group(1)
                        +
                        "시리즈";
            }
        }

        return null;
    }

    private String findFuel(
            String text
    ) {

        if (
                containsAny(
                        text,
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

            String digits =
                    km.group(1)
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
                        "Цена ↑ от най-евтините"
                );

        if (
                spec.brand != null
        ) {

            result.append(
                    " | "
            );

            result.append(
                    spec.brand.encar
            );
        }

        if (
                spec.modelGroup != null
        ) {

            result.append(
                    " "
            );

            result.append(
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

        if (
                spec.brand != null
                        &&
                spec.modelGroup == null
        ) {

            result.append(
                    " | моделът не е разпознат — търся всички модели на марката"
            );
        }

        return result.toString();
    }

    private void initDictionary() {

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
                "vw",
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
                "lexus",
                "렉서스",
                "N",
                "lexus",
                "лексус"
        );

        addBrand(
                "volvo",
                "볼보",
                "N",
                "volvo",
                "волво"
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
                "mini",
                "미니",
                "N",
                "mini",
                "мини"
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
                "dodge",
                "닷지",
                "N",
                "dodge",
                "додж"
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
                "mazda",
                "마쯔다",
                "N",
                "mazda",
                "мазда"
        );

        addBrand(
                "mitsubishi",
                "미쓰비시",
                "N",
                "mitsubishi",
                "мицубиши"
        );

        addBrand(
                "fiat",
                "피아트",
                "N",
                "fiat",
                "фиат"
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
                "polestar",
                "폴스타",
                "N",
                "polestar",
                "полстар"
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
                "генезис"
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

        addModels(
                "mercedes",
                "GLE-클래스=gle",
                "GLC-클래스=glc",
                "GLS-클래스=gls",
                "GLA-클래스=gla",
                "GLB-클래스=glb",
                "CLA-클래스=cla",
                "CLS-클래스=cls",
                "CLE-클래스=cle",
                "EQA=eqa",
                "EQB=eqb",
                "EQE=eqe",
                "EQS=eqs"
        );

        addModels(
                "bmw",
                "X1=x1",
                "X2=x2",
                "X3=x3",
                "X4=x4",
                "X5=x5",
                "X6=x6",
                "X7=x7",
                "Z4=z4",
                "i4=i4",
                "i5=i5",
                "i7=i7",
                "iX=ix"
        );

        addModels(
                "audi",
                "A3=a3",
                "A4=a4",
                "A5=a5",
                "A6=a6",
                "A7=a7",
                "A8=a8",
                "Q3=q3",
                "Q4=q4",
                "Q5=q5",
                "Q7=q7",
                "Q8=q8",
                "TT=tt",
                "R8=r8"
        );

        addModels(
                "vw",
                "티구안=tiguan|тигуан",
                "투아렉=touareg|туарег",
                "골프=golf|голф",
                "파사트=passat|пасат",
                "아테온=arteon|артеон",
                "티록=t-roc|t roc",
                "ID.4=id.4|id4",
                "ID.5=id.5|id5"
        );

        addModels(
                "porsche",
                "카이엔=cayenne|кайен",
                "마칸=macan|макан",
                "파나메라=panamera|панамера",
                "타이칸=taycan|тайкан",
                "911=911",
                "718=718"
        );

        addModels(
                "kia",
                "쏘렌토=sorento|соренто",
                "스포티지=sportage|спортидж|спортиж",
                "카니발=carnival",
                "셀토스=seltos",
                "니로=niro",
                "모하비=mohave",
                "EV6=ev6",
                "EV9=ev9",
                "K5=k5",
                "K8=k8",
                "K9=k9"
        );

        addModels(
                "hyundai",
                "투싼=tucson|тусон",
                "싼타페=santa fe|santafe|санта фе",
                "팰리세이드=palisade|палисейд",
                "코나=kona",
                "아이오닉5=ioniq 5|ioniq5",
                "아이오닉6=ioniq 6|ioniq6",
                "스타리아=staria",
                "아반떼=avante|elantra",
                "쏘나타=sonata",
                "그랜저=grandeur"
        );

        addModels(
                "genesis",
                "G70=g70",
                "G80=g80",
                "G90=g90",
                "GV60=gv60",
                "GV70=gv70",
                "GV80=gv80"
        );

        addModels(
                "ford",
                "익스플로러=explorer",
                "머스탱=mustang",
                "레인저=ranger",
                "브롱코=bronco",
                "F150=f150|f-150"
        );

        addModels(
                "honda",
                "어코드=accord",
                "시빅=civic",
                "CR-V=cr-v|crv",
                "HR-V=hr-v|hrv",
                "오딧세이=odyssey",
                "파일럿=pilot"
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
                "RAV4=rav4|rav 4",
                "캠리=camry",
                "프리우스=prius",
                "하이랜더=highlander",
                "시에나=sienna"
        );

        addModels(
                "lexus",
                "UX=ux",
                "NX=nx",
                "RX=rx",
                "GX=gx",
                "LX=lx",
                "IS=is",
                "ES=es",
                "LS=ls",
                "RC=rc",
                "LC=lc"
        );

        addModels(
                "volvo",
                "XC40=xc40",
                "XC60=xc60",
                "XC90=xc90",
                "S60=s60",
                "S90=s90",
                "V60=v60",
                "V90=v90",
                "EX30=ex30",
                "EX90=ex90"
        );

        addModels(
                "tesla",
                "모델 3=model 3",
                "모델 Y=model y",
                "모델 S=model s",
                "모델 X=model x"
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
                models.get(
                        brandKey
                );

        if (
                map == null
        ) {

            map =
                    new HashMap<>();

            models.put(
                    brandKey,
                    map
            );
        }

        for (
                String row : rows
        ) {

            String[] pair =
                    row.split(
                            "=",
                            2
                    );

            if (
                    pair.length != 2
            ) {

                continue;
            }

            String encarModel =
                    pair[0];

            String[] aliases =
                    pair[1].split(
                            "\\|"
                    );

            for (
                    String alias : aliases
            ) {

                map.put(
                        normalize(alias),
                        encarModel
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
                    hasPhrase(
                            text,
                            value
                    )
            ) {

                return true;
            }
        }

        return false;
    }

    private boolean hasPhrase(
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

    private String escapeJson(
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
                    getCurrentFocus() != null
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
