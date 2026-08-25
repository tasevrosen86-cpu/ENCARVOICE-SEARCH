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
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST = 1001;
    private static final int MAX_READ_ATTEMPTS = 12;

    private WebView webView;
    private TextView status;
    private EditText searchInput;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private int readAttempts = 0;

    private String lastResult = "";
    private String lastCarUrl = "";

    private final List<CarModel> catalog =
            new ArrayList<>();


    // =========================================================
    // MODEL CATALOG
    // =========================================================

    private static class Generation {

        int fromYear;
        int toYear;

        String exactModel;
        String label;

        Generation(
                int fromYear,
                int toYear,
                String exactModel,
                String label
        ) {

            this.fromYear = fromYear;
            this.toYear = toYear;
            this.exactModel = exactModel;
            this.label = label;
        }

        boolean accepts(int year) {

            return year >= fromYear &&
                    year <= toYear;
        }
    }


    private static class CarModel {

        String brand;
        String displayName;

        String manufacturer;
        String modelGroup;

        String[] aliases;

        List<Generation> generations =
                new ArrayList<>();

        CarModel(
                String brand,
                String displayName,
                String manufacturer,
                String modelGroup,
                String... aliases
        ) {

            this.brand = brand;
            this.displayName = displayName;
            this.manufacturer = manufacturer;
            this.modelGroup = modelGroup;
            this.aliases = aliases;
        }

        CarModel generation(
                int from,
                int to,
                String exactModel,
                String label
        ) {

            generations.add(
                    new Generation(
                            from,
                            to,
                            exactModel,
                            label
                    )
            );

            return this;
        }

        Generation generationFor(
                int year
        ) {

            for (
                    Generation generation :
                            generations
            ) {

                if (
                        generation.accepts(year)
                ) {

                    return generation;
                }
            }

            return null;
        }
    }


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        initCatalog();


        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );


        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Например: Hyundai Santa Fe 2024 хибрид"
        );

        searchInput.setTextSize(18);

        searchInput.setSingleLine(false);

        searchInput.setMinLines(2);

        searchInput.setPadding(
                20,
                15,
                20,
                15
        );


        // =====================================================
        // TOP BUTTONS
        // =====================================================

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


        Button searchButton =
                new Button(this);

        searchButton.setText(
                "🔎 ТЪРСИ"
        );


        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );


        buttons.addView(
                voiceButton,
                buttonParams
        );

        buttons.addView(
                searchButton,
                buttonParams
        );


        // =====================================================
        // STATUS
        // =====================================================

        status =
                new TextView(this);

        status.setText(
                "Готово"
        );

        status.setTextSize(14);

        status.setPadding(
                20,
                15,
                20,
                15
        );

        status.setTextIsSelectable(
                true
        );


        // =====================================================
        // LOWER BUTTONS
        // =====================================================

        LinearLayout tools =
                new LinearLayout(this);

        tools.setOrientation(
                LinearLayout.HORIZONTAL
        );


        Button readButton =
                new Button(this);

        readButton.setText(
                "ПЪРВА ОБЯВА"
        );


        Button openButton =
                new Button(this);

        openButton.setText(
                "ОТВОРИ"
        );


        Button copyButton =
                new Button(this);

        copyButton.setText(
                "КОПИРАЙ"
        );


        LinearLayout.LayoutParams toolParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );


        tools.addView(
                readButton,
                toolParams
        );

        tools.addView(
                openButton,
                toolParams
        );

        tools.addView(
                copyButton,
                toolParams
        );


        // =====================================================
        // WEBVIEW
        // =====================================================

        webView =
                new WebView(this);


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


        webView.addJavascriptInterface(
                new CarReader(),
                "AndroidCarReader"
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
                                url != null
                                        &&
                                url.contains(
                                        "car.encar.com/list/car"
                                )
                        ) {

                            status.setText(
                                    "Резултатите са заредени.\n" +
                                    "Търся първата реална обява..."
                            );

                            handler.postDelayed(
                                    () -> startReading(),
                                    1800
                            );
                        }
                    }
                }
        );


        // =====================================================
        // ADD UI
        // =====================================================

        root.addView(
                searchInput
        );

        root.addView(
                buttons
        );

        root.addView(
                status
        );

        root.addView(
                tools
        );

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );


        setContentView(
                root
        );


        // =====================================================
        // BUTTON EVENTS
        // =====================================================

        voiceButton.setOnClickListener(
                v -> startVoice()
        );


        searchButton.setOnClickListener(
                v -> searchFromInput()
        );


        readButton.setOnClickListener(
                v -> startReading()
        );


        openButton.setOnClickListener(
                v -> openFirstCar()
        );


        copyButton.setOnClickListener(
                v -> copyResult()
        );
    }


    // =========================================================
    // CATALOG
    // =========================================================

    private void initCatalog() {

        catalog.clear();


        // =====================================================
        // KIA
        // =====================================================

        CarModel sorento =
                new CarModel(
                        "KIA",
                        "Kia Sorento",
                        "기아",
                        "쏘렌토",
                        "kia sorento",
                        "киа соренто",
                        "кия соренто",
                        "sorento",
                        "соренто"
                );


        /*
         * ПОТВЪРДЕН ПРОФИЛ
         */
        sorento.generation(
                2023,
                2099,
                "더 뉴 쏘렌토 4세대",
                "The New Sorento 4th"
        );


        /*
         * За по-старите години:
         * ModelGroup + Year.
         */
        sorento.generation(
                2000,
                2022,
                null,
                "Sorento"
        );


        catalog.add(
                sorento
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia Sportage",
                        "기아",
                        "스포티지",
                        "kia sportage",
                        "киа спортидж",
                        "киа спортиж",
                        "sportage",
                        "спортидж",
                        "спортиж"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia Carnival",
                        "기아",
                        "카니발",
                        "kia carnival",
                        "киа карнивал",
                        "carnival",
                        "карнивал"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia Seltos",
                        "기아",
                        "셀토스",
                        "kia seltos",
                        "seltos",
                        "селтос"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia Niro",
                        "기아",
                        "니로",
                        "kia niro",
                        "niro",
                        "ниро"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia Mohave",
                        "기아",
                        "모하비",
                        "kia mohave",
                        "mohave",
                        "мохаве"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia EV6",
                        "기아",
                        "EV6",
                        "kia ev6",
                        "ev6"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia EV9",
                        "기아",
                        "EV9",
                        "kia ev9",
                        "ev9"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia K5",
                        "기아",
                        "K5",
                        "kia k5",
                        "k5"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia K8",
                        "기아",
                        "K8",
                        "kia k8",
                        "k8"
                )
        );


        catalog.add(
                new CarModel(
                        "KIA",
                        "Kia K9",
                        "기아",
                        "K9",
                        "kia k9",
                        "k9"
                )
        );


        // =====================================================
        // HYUNDAI PALISADE
        // =====================================================

        CarModel palisade =
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Palisade",
                        "현대",
                        "팰리세이드",
                        "hyundai palisade",
                        "хюндай палисейд",
                        "хендай палисейд",
                        "хундай палисейд",
                        "palisade",
                        "палисейд"
                );


        /*
         * ПОТВЪРДЕН РАБОТЕЩ ПРОФИЛ
         */
        palisade.generation(
                2025,
                2099,
                "팰리세이드 (LX3_)",
                "Palisade LX3"
        );


        /*
         * ПОТВЪРДЕН РАБОТЕЩ ПРОФИЛ
         */
        palisade.generation(
                2022,
                2024,
                "더 뉴 팰리세이드",
                "The New Palisade"
        );


        palisade.generation(
                2018,
                2021,
                "팰리세이드",
                "Palisade"
        );


        catalog.add(
                palisade
        );


        // =====================================================
        // HYUNDAI SANTA FE
        // =====================================================

        CarModel santaFe =
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Santa Fe",
                        "현대",
                        "싼타페",
                        "hyundai santa fe",
                        "hyundai santafe",
                        "хюндай санта фе",
                        "хендай санта фе",
                        "santa fe",
                        "santafe",
                        "санта фе"
                );


        /*
         * Диапазоните са от показания Encar каталог.
         * exactModel нарочно остава null,
         * докато не бъде потвърден от scanner.
         */
        santaFe.generation(
                2023,
                2099,
                null,
                "Santa Fe MX5"
        );

        santaFe.generation(
                2020,
                2022,
                null,
                "The New Santa Fe"
        );

        santaFe.generation(
                2018,
                2019,
                null,
                "Santa Fe TM"
        );

        santaFe.generation(
                2015,
                2017,
                null,
                "Santa Fe Prime"
        );

        santaFe.generation(
                2012,
                2014,
                null,
                "Santa Fe DM"
        );


        catalog.add(
                santaFe
        );


        // =====================================================
        // HYUNDAI TUCSON
        // =====================================================

        CarModel tucson =
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Tucson",
                        "현대",
                        "투싼",
                        "hyundai tucson",
                        "хюндай тусон",
                        "хендай тусон",
                        "tucson",
                        "тусон"
                );


        tucson.generation(
                2023,
                2099,
                null,
                "The New Tucson NX4"
        );

        tucson.generation(
                2020,
                2022,
                null,
                "Tucson NX4"
        );

        tucson.generation(
                2015,
                2019,
                null,
                "All New Tucson"
        );

        tucson.generation(
                2013,
                2014,
                null,
                "New Tucson ix"
        );


        catalog.add(
                tucson
        );


        // =====================================================
        // HYUNDAI AVANTE
        // =====================================================

        CarModel avante =
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Avante",
                        "현대",
                        "아반떼",
                        "hyundai avante",
                        "hyundai elantra",
                        "avante",
                        "elantra",
                        "авантe",
                        "аванте",
                        "елантра"
                );


        avante.generation(
                2023,
                2099,
                null,
                "The New Avante CN7"
        );

        avante.generation(
                2020,
                2022,
                null,
                "Avante CN7"
        );

        avante.generation(
                2018,
                2019,
                null,
                "The New Avante"
        );

        avante.generation(
                2015,
                2017,
                null,
                "Avante AD"
        );


        catalog.add(
                avante
        );


        // =====================================================
        // HYUNDAI SONATA
        // =====================================================

        CarModel sonata =
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Sonata",
                        "현대",
                        "쏘나타",
                        "hyundai sonata",
                        "хюндай соната",
                        "sonata",
                        "соната"
                );


        sonata.generation(
                2023,
                2099,
                null,
                "Sonata The Edge DN8"
        );

        sonata.generation(
                2019,
                2022,
                null,
                "Sonata DN8"
        );

        sonata.generation(
                2017,
                2018,
                null,
                "Sonata New Rise"
        );

        sonata.generation(
                2014,
                2016,
                null,
                "LF Sonata"
        );


        catalog.add(
                sonata
        );


        // =====================================================
        // HYUNDAI GRANDEUR
        // =====================================================

        CarModel grandeur =
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Grandeur",
                        "현대",
                        "그랜저",
                        "hyundai grandeur",
                        "grandeur",
                        "грандер",
                        "грандьор",
                        "грандеур"
                );


        grandeur.generation(
                2022,
                2099,
                null,
                "Grandeur GN7"
        );

        grandeur.generation(
                2019,
                2021,
                null,
                "The New Grandeur IG"
        );

        grandeur.generation(
                2016,
                2018,
                null,
                "Grandeur IG"
        );


        catalog.add(
                grandeur
        );


        // =====================================================
        // OTHER HYUNDAI
        // =====================================================

        catalog.add(
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Kona",
                        "현대",
                        "코나",
                        "hyundai kona",
                        "kona",
                        "кона"
                )
        );


        catalog.add(
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Staria",
                        "현대",
                        "스타리아",
                        "hyundai staria",
                        "staria",
                        "стария",
                        "стария хюндай"
                )
        );


        catalog.add(
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Ioniq 5",
                        "현대",
                        "아이오닉5",
                        "hyundai ioniq 5",
                        "ioniq 5",
                        "ioniq5",
                        "йоник 5",
                        "ионик 5"
                )
        );


        catalog.add(
                new CarModel(
                        "HYUNDAI",
                        "Hyundai Ioniq 6",
                        "현대",
                        "아이오닉6",
                        "hyundai ioniq 6",
                        "ioniq 6",
                        "ioniq6",
                        "йоник 6",
                        "ионик 6"
                )
        );
    }


    // =========================================================
    // VOICE
    // =========================================================

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
                "Кажи марка, модел, година и гориво"
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
                    "Няма гласово разпознаване",
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

                String text =
                        results.get(0);


                /*
                 * 20 25 -> 2025
                 */
                text =
                        text.replaceAll(
                                "\\b20\\s+(\\d{2})\\b",
                                "20$1"
                        );


                searchInput.setText(
                        text
                );


                searchInput.setSelection(
                        searchInput
                                .getText()
                                .length()
                );
            }
        }
    }


    // =========================================================
    // SEARCH INPUT
    // =========================================================

    private void searchFromInput() {

        String original =
                searchInput
                        .getText()
                        .toString()
                        .trim();


        if (
                original.isEmpty()
        ) {

            status.setText(
                    "Кажи или напиши автомобил."
            );

            return;
        }


        String text =
                normalize(
                        original
                );


        Integer year =
                findYear(
                        text
                );


        if (
                year == null
        ) {

            status.setText(
                    "Не разпознах годината."
            );

            return;
        }


        CarModel car =
                findCar(
                        text
                );


        if (
                car == null
        ) {

            status.setText(
                    "Не разпознах модела.\n" +
                    "Каталогът засега е Kia + Hyundai."
            );

            return;
        }


        String fuel =
                findFuel(
                        text
                );


        Generation generation =
                car.generationFor(
                        year
                );


        searchCar(
                car,
                generation,
                year,
                fuel
        );
    }


    // =========================================================
    // FIND CAR
    // =========================================================

    private CarModel findCar(
            String text
    ) {

        CarModel best =
                null;

        int longest =
                0;


        for (
                CarModel car :
                        catalog
        ) {

            for (
                    String alias :
                            car.aliases
            ) {

                String normalizedAlias =
                        normalize(
                                alias
                        );


                if (
                        containsPhrase(
                                text,
                                normalizedAlias
                        )
                                &&
                        normalizedAlias.length()
                                >
                        longest
                ) {

                    best =
                            car;

                    longest =
                            normalizedAlias.length();
                }
            }
        }


        return best;
    }


    // =========================================================
    // YEAR
    // =========================================================

    private Integer findYear(
            String text
    ) {

        text =
                text.replaceAll(
                        "\\b20\\s+(\\d{2})\\b",
                        "20$1"
                );


        Matcher matcher =
                Pattern.compile(
                        "\\b(20\\d{2})\\b"
                )
                        .matcher(
                                text
                        );


        if (
                matcher.find()
        ) {

            try {

                return Integer.parseInt(
                        matcher.group(1)
                );

            } catch (
                    Exception ignored
            ) {
            }
        }


        /*
         * "24 година" -> 2024
         */
        matcher =
                Pattern.compile(
                        "\\b(1[0-9]|2[0-9])\\s*(?:г|година|год)?\\b"
                )
                        .matcher(
                                text
                        );


        if (
                matcher.find()
        ) {

            try {

                return 2000
                        +
                        Integer.parseInt(
                                matcher.group(1)
                        );

            } catch (
                    Exception ignored
            ) {
            }
        }


        return null;
    }


    // =========================================================
    // FUEL
    // =========================================================

    private String findFuel(
            String text
    ) {

        /*
         * HYBRID FIRST
         */
        if (
                text.contains("хибрид")
                        ||
                text.contains("hybrid")
                        ||
                text.contains("hev")
        ) {

            return "가솔린+전기";
        }


        if (
                text.contains("електр")
                        ||
                text.contains("electric")
                        ||
                text.contains(" ev ")
        ) {

            return "전기";
        }


        if (
                text.contains("дизел")
                        ||
                text.contains("diesel")
        ) {

            return "디젤";
        }


        if (
                text.contains("бензин")
                        ||
                text.contains("gasoline")
                        ||
                text.contains("petrol")
        ) {

            return "가솔린";
        }


        /*
         * Ако не е казано гориво:
         * НЕ слагаме FuelType филтър.
         */
        return null;
    }


    // =========================================================
    // WORKING ENCAR SEARCH
    // =========================================================

    private void searchCar(
            CarModel car,
            Generation generation,
            int year,
            String fuel
    ) {

        int yearFrom =
                year * 100;

        int yearTo =
                yearFrom + 99;


        StringBuilder action =
                new StringBuilder();


        /*
         * Това е работещата архитектура.
         * Не я сменяме.
         */
        action.append(
                "(And.Year.range("
        );

        action.append(
                yearFrom
        );

        action.append(
                ".."
        );

        action.append(
                yearTo
        );

        action.append(
                ")."
        );


        action.append(
                "_.Hidden.N."
        );


        action.append(
                "_.(Or.Separation.F._.Separation.B.)"
        );


        action.append(
                "_.SellType.일반."
        );


        action.append(
                "_.(C.CarType.Y."
        );


        action.append(
                "_.(C.Manufacturer."
        );


        action.append(
                car.manufacturer
        );


        action.append(
                "."
        );


        action.append(
                "_.(C.ModelGroup."
        );


        action.append(
                car.modelGroup
        );


        action.append(
                "."
        );


        /*
         * Exact Model се използва САМО
         * когато вече е потвърден.
         */
        if (
                generation != null
                        &&
                generation.exactModel != null
                        &&
                !generation.exactModel.isEmpty()
        ) {

            action.append(
                    "_.Model."
            );

            action.append(
                    generation.exactModel
            );

            action.append(
                    "."
            );
        }


        action.append(
                ")"
        );


        action.append(
                ")"
        );


        action.append(
                ")"
        );


        /*
         * Ако потребителят е казал гориво.
         */
        if (
                fuel != null
                        &&
                !fuel.isEmpty()
        ) {

            action.append(
                    "_.FuelType."
            );

            action.append(
                    fuel
            );

            action.append(
                    "."
            );
        }


        action.append(
                ")"
        );


        String json =
                "{"
                        +
                "\"type\":\"car\","
                        +
                "\"action\":\""
                        +
                action
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


        try {

            String encoded =
                    URLEncoder.encode(
                            json,
                            StandardCharsets.UTF_8.toString()
                    );


            String url =
                    "https://car.encar.com/list/car?page=1&search="
                            +
                    encoded;


            lastResult = "";
            lastCarUrl = "";


            String generationText =
                    "";


            if (
                    generation != null
                            &&
                    generation.label != null
            ) {

                generationText =
                        "\nПоколение: "
                                +
                        generation.label;
            }


            String fuelText =
                    fuel == null
                            ?
                    "всички горива"
                            :
                    fuel;


            status.setText(
                    "Търся "
                            +
                    car.displayName
                            +
                    " "
                            +
                    year
                            +
                    generationText
                            +
                    "\nГориво: "
                            +
                    fuelText
                            +
                    "\nНАЙ-НИСКА ЦЕНА ПЪРВО"
            );


            webView.loadUrl(
                    url
            );


        } catch (
                Exception e
        ) {

            status.setText(
                    "Грешка при търсене: "
                            +
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // READ FIRST CAR
    // =========================================================

    private void startReading() {

        readAttempts = 0;

        readFirstCar();
    }


    private void readFirstCar() {

        readAttempts++;


        String script =
                "(function(){"

                        +

                "var links=Array.from(" +
                "document.querySelectorAll(" +
                "'a[href*=\"/cars/detail/\"]'" +
                ")" +
                ");"

                        +

                "var car=links.find(function(a){"

                        +

                "var txt=(a.innerText||a.textContent||'')" +
                ".replace(/\\\\s+/g,' ')" +
                ".trim();"

                        +

                "if(txt.length<15)return false;"

                        +

                "if(a.classList.contains('sponsored_type'))" +
                "return false;"

                        +

                "var p=a.parentElement;"

                        +

                "while(p){"

                        +

                "if(p.classList&&" +
                "p.classList.contains('sponsored_type'))" +
                "return false;"

                        +

                "p=p.parentElement;"

                        +

                "}"

                        +

                "return true;"

                        +

                "});"

                        +

                "if(!car){"

                        +

                "AndroidCarReader.receiveCar(" +
                "JSON.stringify({" +
                "error:'NO_CAR_FOUND'" +
                "})" +
                ");"

                        +

                "return;"

                        +

                "}"

                        +

                "var text=" +
                "(car.innerText||car.textContent||'')" +
                ".replace(/\\\\s+/g,' ')" +
                ".trim();"

                        +

                "var mileage=" +
                "text.match(/([0-9][0-9,]*)\\\\s*km/i);"

                        +

                "var krw=" +
                "text.match(/([0-9][0-9,]*)\\\\s*만원/);"

                        +

                "var usd=" +
                "text.match(/([0-9][0-9,]*)\\\\s*USD/i);"

                        +

                "var year1=" +
                "text.match(/([0-9]{2}\\\\/[0-9]{2}식" +
                "(?:\\\\([0-9]{2}년형\\\\))?)/);"

                        +

                "var year2=" +
                "text.match(/((?:0[1-9]|1[0-2])\\\\/20[0-9]{2})/);"

                        +

                "var fuel=" +
                "text.match(/" +

                "(가솔린\\\\+전기|" +
                "가솔린 하이브리드|" +
                "디젤 하이브리드|" +
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

                "/i);"

                        +

                "var href=car.href||'';"

                        +

                "var id=" +
                "href.match(/\\/cars\\/detail\\/([0-9]+)/);"

                        +

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

                ");"

                        +

                "})();";


        webView.evaluateJavascript(
                script,
                null
        );
    }


    // =========================================================
    // RECEIVE FIRST CAR
    // =========================================================

    private class CarReader {

        @JavascriptInterface
        public void receiveCar(
                String json
        ) {

            runOnUiThread(
                    () -> {

                        try {

                            JSONObject obj =
                                    new JSONObject(
                                            json
                                    );


                            if (
                                    obj.has(
                                            "error"
                                    )
                            ) {

                                if (
                                        readAttempts
                                                <
                                        MAX_READ_ATTEMPTS
                                ) {

                                    status.setText(
                                            "Обявите още се зареждат... "
                                                    +
                                            readAttempts
                                                    +
                                            "/"
                                                    +
                                            MAX_READ_ATTEMPTS
                                    );


                                    handler.postDelayed(
                                            () -> readFirstCar(),
                                            1000
                                    );

                                } else {

                                    status.setText(
                                            "Няма намерена реална обява.\n" +
                                            "Ако Encar показва 0 коли, този модел/поколение ще го сканираме."
                                    );
                                }


                                return;
                            }


                            String carId =
                                    obj.optString(
                                            "carId"
                                    );


                            String year =
                                    obj.optString(
                                            "year"
                                    );


                            String mileage =
                                    obj.optString(
                                            "mileage"
                                    );


                            String fuel =
                                    obj.optString(
                                            "fuel"
                                    );


                            String priceKrw =
                                    obj.optString(
                                            "priceKrw"
                                    );


                            String priceUsd =
                                    obj.optString(
                                            "priceUsd"
                                    );


                            String url =
                                    obj.optString(
                                            "url"
                                    );


                            String raw =
                                    obj.optString(
                                            "text"
                                    );


                            lastCarUrl =
                                    url;


                            String price;


                            if (
                                    !priceKrw.isEmpty()
                            ) {

                                price =
                                        priceKrw
                                                +
                                        " 만원";

                            } else if (
                                    !priceUsd.isEmpty()
                            ) {

                                price =
                                        priceUsd
                                                +
                                        " USD";

                            } else {

                                price =
                                        "не е разпозната";
                            }


                            lastResult =
                                    "ПЪРВА ОБЯВА\n\n"

                                            +

                                    "ID: "
                                            +
                                    carId
                                            +
                                    "\n"

                                            +

                                    "Година: "
                                            +
                                    year
                                            +
                                    "\n"

                                            +

                                    "Пробег: "
                                            +
                                    mileage
                                            +
                                    " km\n"

                                            +

                                    "Гориво: "
                                            +
                                    fuel
                                            +
                                    "\n"

                                            +

                                    "Цена: "
                                            +
                                    price
                                            +
                                    "\n\n"

                                            +

                                    "LINK:\n"
                                            +
                                    url
                                            +
                                    "\n\n"

                                            +

                                    "RAW:\n"
                                            +
                                    raw;


                            status.setText(
                                    lastResult
                            );


                        } catch (
                                Exception e
                        ) {

                            status.setText(
                                    "Грешка при четене: "
                                            +
                                    e.getMessage()
                            );
                        }
                    }
            );
        }
    }


    // =========================================================
    // OPEN CAR
    // =========================================================

    private void openFirstCar() {

        if (
                lastCarUrl == null
                        ||
                lastCarUrl.isEmpty()
        ) {

            status.setText(
                    "Първо изчакай да намеря първата обява."
            );

            return;
        }


        webView.loadUrl(
                lastCarUrl
        );
    }


    // =========================================================
    // COPY
    // =========================================================

    private void copyResult() {

        String text =
                lastResult;


        if (
                text == null
                        ||
                text.isEmpty()
        ) {

            text =
                    status
                            .getText()
                            .toString();
        }


        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );


        ClipData clip =
                ClipData.newPlainText(
                        "Encar result",
                        text
                );


        clipboard.setPrimaryClip(
                clip
        );


        status.setText(
                text
                        +
                "\n\nКОПИРАНО ✅"
        );
    }


    // =========================================================
    // TEXT HELPERS
    // =========================================================

    private String normalize(
            String text
    ) {

        if (
                text == null
        ) {

            return "";
        }


        return text
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        '-',
                        ' '
                )
                .replace(
                        ',',
                        ' '
                )
                .replace(
                        '.',
                        ' '
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


    private boolean containsPhrase(
            String text,
            String phrase
    ) {

        String source =
                " "
                        +
                text
                        +
                " ";


        String target =
                " "
                        +
                phrase
                        +
                " ";


        return source.contains(
                target
        );
    }


    // =========================================================
    // BACK
    // =========================================================

    @Override
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
}
