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

import org.json.JSONObject;

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

    private String pendingSearchUrl = null;
    private boolean loadingBlank = false;

    private final Map<String, Brand> brandAliases =
            new LinkedHashMap<>();

    private final Map<String, Map<String, String>> modelAliases =
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
                                loadingBlank
                                        &&
                                "about:blank".equals(url)
                                        &&
                                pendingSearchUrl != null
                        ) {

                            String target =
                                    pendingSearchUrl;

                            pendingSearchUrl =
                                    null;

                            loadingBlank =
                                    false;

                            view.loadUrl(
                                    target
                            );
                        }
                    }
                }
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
                "Kia Sorento 2025 бензин"
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

        LinearLayout.LayoutParams buttons =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        row.addView(
                voice,
                buttons
        );

        row.addView(
                search,
                buttons
        );

        row.addView(
                clear,
                buttons
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

        status.setTextIsSelectable(
                true
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
                "Например: Kia Sorento 2025 бензин"
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
                        input.getText().length()
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

        SearchSpec spec =
                parse(raw);

        if (
                spec.brand == null
        ) {

            status.setText(
                    "Не разпознах марката. Поправи текста и натисни ТЪРСИ."
            );

            return;
        }

        String action =
                buildAction(spec);

        try {

            JSONObject state =
                    new JSONObject();

            state.put(
                    "type",
                    "car"
            );

            state.put(
                    "action",
                    action
            );

            state.put(
                    "toggle",
                    new JSONObject()
            );

            state.put(
                    "layer",
                    ""
            );

            /*
             * ВИНАГИ:
             * от най-евтините към най-скъпите
             */
            state.put(
                    "sort",
                    "MobilePriceAsc"
            );

            String finalUrl =
                    SEARCH
                            +
                    Uri.encode(
                            state.toString()
                    );

            status.setText(
                    buildStatus(spec)
            );

            forceFreshLoad(
                    finalUrl
            );

        } catch (
                Exception e
        ) {

            status.setText(
                    "Грешка при създаване на заявката: "
                            +
                    e.getMessage()
            );
        }
    }

    /*
     * КЛЮЧОВАТА ПРОМЯНА:
     *
     * Не сменяме само hash-а на вече отворения Encar.
     *
     * 1. about:blank
     * 2. чакаме blank да се зареди
     * 3. зареждаме пълния search.do#!...
     *
     * Така Encar стартира отначало и прочита action.
     */
    private void forceFreshLoad(
            String finalUrl
    ) {

        pendingSearchUrl =
                finalUrl;

        loadingBlank =
                true;

        webView.stopLoading();

        webView.loadUrl(
                "about:blank"
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

    /*
     * Потвърдена структура:
     *
     * And
     *   Hidden
     *   Mileage
     *   SellType
     *   Year
     *   FuelType
     *   CarType
     *     Manufacturer
     *       ModelGroup
     */
    private String buildAction(
            SearchSpec spec
    ) {

        List<String> parts =
                new ArrayList<>();

        parts.add(
                "Hidden.N."
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
            SearchSpec spec
    ) {

        StringBuilder branch =
                new StringBuilder();

        branch.append(
                "(C.CarType."
        );

        branch.append(
                spec.brand.carType
        );

        branch.append(
                "._.(C.Manufacturer."
        );

        branch.append(
                spec.brand.encarName
        );

        if (
                spec.modelGroup != null
                        &&
                !spec.modelGroup.isEmpty()
        ) {

            branch.append(
                    "._.(C.ModelGroup."
            );

            branch.append(
                    spec.modelGroup
            );

            branch.append(
                    ".)))"
            );

        } else {

            /*
             * Ако моделът не е сигурен:
             * не изпращаме измислен ModelGroup.
             */
            branch.append(
                    ".))"
            );
        }

        return branch.toString();
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

            if (
                    best != null
            ) {

                return best;
            }
        }

        /*
         * Mercedes:
         * GLE 300d -> GLE-클래스
         * GLC 220d -> GLC-클래스
         */
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

            Matcher cls =
                    Pattern.compile(
                            "\\b([acesg])\\s*[- ]?\\d{3}[a-z]*\\b"
                    )
                            .matcher(
                                    text
                            );

            if (
                    cls.find()
            ) {

                return cls
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
         * 520d -> 5시리즈
         * 320d -> 3시리즈
         */
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
                new StringBuilder();

        result.append(
                "НАЙ-ЕВТИНИ ПЪРВО"
        );

        result.append(
                " | "
        );

        result.append(
                spec.brand.encarName
        );

        result.append(
                " | CarType."
        );

        result.append(
                spec.brand.carType
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

        /*
         * КОРЕЙСКИ:
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
         * ВНОСНИ:
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
                "seltos=셀토스",
                "niro=니로",
                "mohave=모하비",
                "ev6=EV6",
                "ev9=EV9",
                "k5=K5",
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
                "grandeur=그랜저"
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
         * VW
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
                "f-150=F150"
        );

        /*
         * HONDA
         */

        addModels(
                "honda",
                "accord=어코드",
                "акорд=어코드",
                "civic=시빅",
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
                "sienna=시에나"
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
    }

    private void addBrand(
            String key,
            String encarName,
            String carType,
            String... aliases
    ) {

        Brand brand =
                new Brand(
                        key,
                        encarName,
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
            List<String> values,
            String separator
    ) {

        StringBuilder output =
                new StringBuilder();

        for (
                int i = 0;
                i < values.size();
                i++
        ) {

            if (
                    i > 0
            ) {

                output.append(
                        separator
                );
            }

            output.append(
                    values.get(i)
            );
        }

        return output.toString();
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
