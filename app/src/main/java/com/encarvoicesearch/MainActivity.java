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
            "https://m.encar.com/ca/search.do";

    private EditText searchField;
    private TextView statusText;
    private WebView webView;

    private final Map<String, MakeInfo> makes =
            new LinkedHashMap<>();

    private final Map<String, ModelInfo> models =
            new LinkedHashMap<>();


    private static class MakeInfo {

        String key;
        String carType;
        String encarName;

        MakeInfo(
                String key,
                String carType,
                String encarName
        ) {

            this.key = key;
            this.carType = carType;
            this.encarName = encarName;
        }
    }


    private static class ModelInfo {

        String makeKey;
        String modelGroup;

        ModelInfo(
                String makeKey,
                String modelGroup
        ) {

            this.makeKey = makeKey;
            this.modelGroup = modelGroup;
        }
    }


    private static class SearchData {

        MakeInfo make;

        String modelGroup;

        String generation;

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

        super.onCreate(savedInstanceState);

        createCatalog();

        createUi();


        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setDatabaseEnabled(true);

        settings.setLoadsImagesAutomatically(true);

        settings.setUseWideViewPort(true);

        settings.setLoadWithOverviewMode(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(
                true
        );

        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        );


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
                ENCAR_URL
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

        root.setPadding(
                12,
                12,
                12,
                0
        );


        TextView title =
                new TextView(this);

        title.setText(
                "ENCAR VOICE SEARCH"
        );

        title.setTextSize(
                20f
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                8,
                0,
                8
        );


        searchField =
                new EditText(this);

        searchField.setHint(
                "Mercedes GLE 2024 дизел 200000 км"
        );

        searchField.setTextSize(
                17f
        );

        searchField.setMinLines(
                2
        );

        searchField.setMaxLines(
                3
        );

        searchField.setPadding(
                16,
                12,
                16,
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
                "ГЛАС"
        );


        Button searchButton =
                new Button(this);

        searchButton.setText(
                "ТЪРСИ"
        );


        Button clearButton =
                new Button(this);

        clearButton.setText(
                "ИЗЧИСТИ"
        );


        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );


        buttons.addView(
                voiceButton,
                buttonParams
        );

        buttons.addView(
                searchButton,
                buttonParams
        );

        buttons.addView(
                clearButton,
                buttonParams
        );


        statusText =
                new TextView(this);

        statusText.setText(
                "Кажи или напиши колата."
        );

        statusText.setTextSize(
                14f
        );

        statusText.setPadding(
                8,
                8,
                8,
                10
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
                searchField,
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


        setContentView(
                root
        );


        voiceButton.setOnClickListener(
                v -> startVoice()
        );


        searchButton.setOnClickListener(
                v -> search()
        );


        clearButton.setOnClickListener(
                v -> {

                    searchField.setText("");

                    statusText.setText(
                            "Полето е изчистено."
                    );
                }
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
                "Кажи марка, модел, година, гориво и километри"
        );


        intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
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
                    "Няма активно гласово разпознаване.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    @Override
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
                requestCode != VOICE_REQUEST ||
                resultCode != RESULT_OK ||
                data == null
        ) {

            return;
        }


        ArrayList<String> results =
                data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );


        if (
                results != null &&
                !results.isEmpty()
        ) {

            String result =
                    results.get(0);


            searchField.setText(
                    result
            );


            searchField.setSelection(
                    searchField
                            .getText()
                            .length()
            );


            statusText.setText(
                    "Можеш да поправиш текста и да натиснеш ТЪРСИ."
            );
        }
    }


    private void search() {

        String text =
                searchField
                        .getText()
                        .toString()
                        .trim();


        if (
                text.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Напиши или кажи кола.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        hideKeyboard();


        SearchData data =
                parseSearch(
                        text
                );


        if (
                data.make == null
        ) {

            statusText.setText(
                    "Не разпознах марката."
            );

            return;
        }


        String action =
                buildAction(
                        data
                );


        String url =
                buildUrl(
                        action
                );


        StringBuilder result =
                new StringBuilder();


        result.append(
                data.make.key
                        .toUpperCase(
                                Locale.ROOT
                        )
        );


        if (
                data.modelGroup != null
        ) {

            result.append(
                    " "
            );

            result.append(
                    data.modelGroup
            );
        }


        if (
                data.yearFrom != null
        ) {

            result.append(
                    " "
            );

            result.append(
                    data.yearFrom
            );


            if (
                    data.yearTo != null &&
                    !data.yearFrom.equals(
                            data.yearTo
                    )
            ) {

                result.append(
                        "-"
                );

                result.append(
                        data.yearTo
                );
            }
        }


        if (
                data.fuel != null
        ) {

            result.append(
                    " "
            );

            result.append(
                    fuelName(
                            data.fuel
                    )
            );
        }


        if (
                data.maxMileage != null
        ) {

            result.append(
                    " до "
            );

            result.append(
                    data.maxMileage
            );

            result.append(
                    " км"
            );
        }


        result.append(
                " | най-ниска цена първо"
        );


        statusText.setText(
                result.toString()
        );


        webView.loadUrl(
                url
        );
    }


    private SearchData parseSearch(
            String original
    ) {

        SearchData result =
                new SearchData();


        String text =
                normalize(
                        original
                );


        result.make =
                findMake(
                        text
                );


        ModelInfo model =
                findModel(
                        text,
                        result.make
                );


        if (
                model != null
        ) {

            if (
                    result.make == null
            ) {

                result.make =
                        findMakeByKey(
                                model.makeKey
                        );
            }


            result.modelGroup =
                    model.modelGroup;
        }


        if (
                result.make != null &&
                result.modelGroup == null
        ) {

            result.modelGroup =
                    deriveModel(
                            text,
                            result.make
                    );
        }


        parseYear(
                text,
                result
        );


        result.fuel =
                parseFuel(
                        text
                );


        result.maxMileage =
                parseMileage(
                        text
                );


        if (
                result.make != null &&
                "mercedes".equals(
                        result.make.key
                ) &&
                result.modelGroup != null
        ) {

            Matcher generation =
                    Pattern
                            .compile(
                                    "\\b(w\\d{3})\\b",
                                    Pattern.CASE_INSENSITIVE
                            )
                            .matcher(
                                    text
                            );


            if (
                    generation.find()
            ) {

                result.generation =
                        generation
                                .group(1)
                                .toUpperCase(
                                        Locale.ROOT
                                );
            }
        }


        return result;
    }


    private MakeInfo findMake(
            String text
    ) {

        String bestAlias =
                null;

        MakeInfo result =
                null;


        for (
                Map.Entry<String, MakeInfo> entry :
                makes.entrySet()
        ) {

            String alias =
                    entry.getKey();


            if (
                    contains(
                            text,
                            alias
                    )
            ) {

                if (
                        bestAlias == null ||
                        alias.length() >
                                bestAlias.length()
                ) {

                    bestAlias =
                            alias;

                    result =
                            entry.getValue();
                }
            }
        }


        return result;
    }


    private MakeInfo findMakeByKey(
            String key
    ) {

        for (
                MakeInfo info :
                makes.values()
        ) {

            if (
                    info.key.equals(
                            key
                    )
            ) {

                return info;
            }
        }


        return null;
    }


    private ModelInfo findModel(
            String text,
            MakeInfo make
    ) {

        String bestAlias =
                null;

        ModelInfo result =
                null;


        for (
                Map.Entry<String, ModelInfo> entry :
                models.entrySet()
        ) {

            ModelInfo model =
                    entry.getValue();


            if (
                    make != null &&
                    !model.makeKey.equals(
                            make.key
                    )
            ) {

                continue;
            }


            String alias =
                    entry.getKey();


            if (
                    contains(
                            text,
                            alias
                    )
            ) {

                if (
                        bestAlias == null ||
                        alias.length() >
                                bestAlias.length()
                ) {

                    bestAlias =
                            alias;

                    result =
                            model;
                }
            }
        }


        if (
                result != null
        ) {

            return result;
        }


        if (
                make == null
        ) {

            for (
                    Map.Entry<String, ModelInfo> entry :
                    models.entrySet()
            ) {

                String alias =
                        entry.getKey();


                if (
                        contains(
                                text,
                                alias
                        )
                ) {

                    if (
                            bestAlias == null ||
                            alias.length() >
                                    bestAlias.length()
                    ) {

                        bestAlias =
                                alias;

                        result =
                                entry.getValue();
                    }
                }
            }
        }


        return result;
    }


    private void parseYear(
            String text,
            SearchData result
    ) {

        Matcher range =
                Pattern.compile(
                        "\\b(19\\d{2}|20\\d{2})\\s*(?:-|до|to)\\s*(19\\d{2}|20\\d{2})\\b",
                        Pattern.CASE_INSENSITIVE
                ).matcher(
                        text
                );


        if (
                range.find()
        ) {

            int first =
                    Integer.parseInt(
                            range.group(1)
                    );


            int second =
                    Integer.parseInt(
                            range.group(2)
                    );


            result.yearFrom =
                    Math.min(
                            first,
                            second
                    );


            result.yearTo =
                    Math.max(
                            first,
                            second
                    );


            return;
        }


        Matcher exact =
                Pattern.compile(
                        "\\b(19\\d{2}|20\\d{2})\\b"
                ).matcher(
                        text
                );


        if (
                exact.find()
        ) {

            int year =
                    Integer.parseInt(
                            exact.group(1)
                    );


            result.yearFrom =
                    year;

            result.yearTo =
                    year;
        }
    }


    private String parseFuel(
            String text
    ) {

        if (
                text.contains(
                        "хибрид"
                ) ||
                text.contains(
                        "hybrid"
                )
        ) {

            return "하이브리드";
        }


        if (
                text.contains(
                        "електр"
                ) ||
                contains(
                        text,
                        "electric"
                ) ||
                contains(
                        text,
                        "ev"
                )
        ) {

            return "전기";
        }


        if (
                text.contains(
                        "дизел"
                ) ||
                text.contains(
                        "diesel"
                )
        ) {

            return "디젤";
        }


        if (
                text.contains(
                        "lpg"
                ) ||
                text.contains(
                        "автогаз"
                )
        ) {

            return "LPG";
        }


        if (
                text.contains(
                        "бензин"
                ) ||
                text.contains(
                        "gasoline"
                ) ||
                text.contains(
                        "petrol"
                )
        ) {

            return "가솔린";
        }


        return null;
    }


    private Integer parseMileage(
            String text
    ) {

        Matcher km =
                Pattern.compile(
                        "(\\d[\\d\\s.,]*|\\d+(?:[.,]\\d+)?\\s*[kк])\\s*(?:km|км|километра|километри)\\b",
                        Pattern.CASE_INSENSITIVE
                ).matcher(
                        text
                );


        if (
                km.find()
        ) {

            return parseDistance(
                    km.group(1)
            );
        }


        Matcher thousand =
                Pattern.compile(
                        "\\b(\\d+(?:[.,]\\d+)?)\\s*[kк]\\b",
                        Pattern.CASE_INSENSITIVE
                ).matcher(
                        text
                );


        if (
                thousand.find()
        ) {

            String value =
                    thousand
                            .group(1)
                            .replace(
                                    ",",
                                    "."
                            );


            double number =
                    Double.parseDouble(
                            value
                    );


            return (int)
                    Math.round(
                            number * 1000.0
                    );
        }


        return null;
    }


    private int parseDistance(
            String value
    ) {

        String text =
                value
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();


        if (
                text.endsWith(
                        "k"
                ) ||
                text.endsWith(
                        "к"
                )
        ) {

            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );


            text =
                    text
                            .replace(
                                    ",",
                                    "."
                            )
                            .trim();


            double number =
                    Double.parseDouble(
                            text
                    );


            return (int)
                    Math.round(
                            number * 1000.0
                    );
        }


        text =
                text
                        .replace(
                                " ",
                                ""
                        )
                        .replace(
                                ".",
                                ""
                        )
                        .replace(
                                ",",
                                ""
                        );


        return Integer.parseInt(
                text
        );
    }


    private String deriveModel(
            String text,
            MakeInfo make
    ) {

        String work =
                text;


        for (
                Map.Entry<String, MakeInfo> entry :
                makes.entrySet()
        ) {

            if (
                    entry.getValue()
                            .key
                            .equals(
                                    make.key
                            )
            ) {

                work =
                        remove(
                                work,
                                entry.getKey()
                        );
            }
        }


        work =
                work.replaceAll(
                        "\\b(19\\d{2}|20\\d{2})\\b",
                        " "
                );


        work =
                work.replaceAll(
                        "\\b\\d[\\d\\s.,]*\\s*(km|км|километра|километри)\\b",
                        " "
                );


        String[] removeWords = {

                "дизел",
                "diesel",

                "бензин",
                "gasoline",
                "petrol",

                "хибрид",
                "hybrid",

                "електрически",
                "електрическа",
                "electric",
                "ev",

                "lpg",
                "автогаз",

                "до",
                "под",
                "under",
                "maximum",
                "max",

                "година",
                "год",
                "year",

                "модел",
                "model",

                "автоматик",
                "automatic",

                "awd",
                "4x4",
                "fwd",
                "rwd",

                "най евтини",
                "най евтин",
                "цена",
                "price"
        };


        for (
                String removeWord :
                removeWords
        ) {

            work =
                    remove(
                            work,
                            removeWord
                    );
        }


        work =
                work.replaceAll(
                        "\\s+",
                        " "
                ).trim();


        if (
                work.isEmpty()
        ) {

            return null;
        }


        if (
                "mercedes".equals(
                        make.key
                )
        ) {

            String upper =
                    work.toUpperCase(
                            Locale.ROOT
                    );


            String first =
                    upper.split(
                            " "
                    )[0];


            if (
                    first.matches(
                            "A|B|C|E|S|G|GLA|GLB|GLC|GLE|GLS|CLA|CLS|SL|SLC|SLK|V"
                    )
            ) {

                return first +
                        "-클래스";
            }
        }


        if (
                "volkswagen".equals(
                        make.key
                )
        ) {

            String model =
                    normalize(
                            work
                    );


            if (
                    model.equals(
                            "tiguan"
                    )
            ) {

                return "티구안";
            }


            if (
                    model.equals(
                            "touareg"
                    )
            ) {

                return "투아렉";
            }


            if (
                    model.equals(
                            "golf"
                    )
            ) {

                return "골프";
            }


            if (
                    model.equals(
                            "passat"
                    )
            ) {

                return "파사트";
            }


            if (
                    model.equals(
                            "arteon"
                    )
            ) {

                return "아테온";
            }
        }


        if (
                "porsche".equals(
                        make.key
                )
        ) {

            String model =
                    normalize(
                            work
                    );


            if (
                    model.equals(
                            "cayenne"
                    )
            ) {

                return "카이엔";
            }


            if (
                    model.equals(
                            "macan"
                    )
            ) {

                return "마칸";
            }


            if (
                    model.equals(
                            "panamera"
                    )
            ) {

                return "파나메라";
            }


            if (
                    model.equals(
                            "taycan"
                    )
            ) {

                return "타이칸";
            }
        }


        if (
                work.matches(
                        "(?i)[a-z0-9.\\- ]+"
                )
        ) {

            return work
                    .toUpperCase(
                            Locale.ROOT
                    );
        }


        return work;
    }


    private String buildAction(
            SearchData data
    ) {

        List<String> parts =
                new ArrayList<>();


        parts.add(
                "Hidden.N."
        );


        parts.add(
                "MultiViewHidden.N."
        );


        // Само нормални продажби.
        // Без лизинг и rental.
        parts.add(
                "SellType.일반."
        );


        if (
                data.maxMileage != null
        ) {

            parts.add(
                    "Mileage.range(.." +
                            data.maxMileage +
                            ")."
            );
        }


        if (
                data.yearFrom != null
        ) {

            int to =
                    data.yearTo != null
                            ? data.yearTo
                            : data.yearFrom;


            String fromValue =
                    String.format(
                            Locale.US,
                            "%04d00",
                            data.yearFrom
                    );


            String toValue =
                    String.format(
                            Locale.US,
                            "%04d99",
                            to
                    );


            parts.add(
                    "Year.range(" +
                            fromValue +
                            ".." +
                            toValue +
                            ")."
            );
        }


        if (
                data.fuel != null
        ) {

            parts.add(
                    "FuelType." +
                            data.fuel +
                            "."
            );
        }


        parts.add(
                buildCarTree(
                        data
                )
        );


        StringBuilder action =
                new StringBuilder(
                        "(And."
                );


        for (
                int i = 0;
                i < parts.size();
                i++
        ) {

            if (
                    i > 0
            ) {

                action.append(
                        "_."
                );
            }


            action.append(
                    parts.get(i)
            );
        }


        action.append(
                ")"
        );


        return action.toString();
    }


    private String buildCarTree(
            SearchData data
    ) {

        MakeInfo make =
                data.make;


        if (
                data.modelGroup == null ||
                data.modelGroup
                        .trim()
                        .isEmpty()
        ) {

            return "(C.CarType." +
                    make.carType +
                    "._.Manufacturer." +
                    make.encarName +
                    ".)";
        }


        if (
                data.generation != null &&
                "mercedes".equals(
                        make.key
                )
        ) {

            String exactModel =
                    data.modelGroup +
                            " " +
                            data.generation;


            return "(C.CarType." +
                    make.carType +
                    "._.(C.Manufacturer." +
                    make.encarName +
                    "._.(C.ModelGroup." +
                    data.modelGroup +
                    "._.Model." +
                    exactModel +
                    ".)))";
        }


        return "(C.CarType." +
                make.carType +
                "._.(C.Manufacturer." +
                make.encarName +
                "._.ModelGroup." +
                data.modelGroup +
                ".))";
    }


    private String buildUrl(
            String action
    ) {

        String json =
                "{\"type\":\"car\"," +

                "\"action\":\"" +
                escapeJson(
                        action
                ) +
                "\"," +

                "\"toggle\":{}," +

                "\"layer\":\"\"," +

                "\"sort\":\"MobilePriceAsc\"}";


        return ENCAR_URL +
                "#!" +
                Uri.encode(
                        json
                );
    }


    private String escapeJson(
            String text
    ) {

        return text
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                );
    }


    private String fuelName(
            String fuel
    ) {

        if (
                "디젤".equals(
                        fuel
                )
        ) {

            return "дизел";
        }


        if (
                "가솔린".equals(
                        fuel
                )
        ) {

            return "бензин";
        }


        if (
                "하이브리드".equals(
                        fuel
                )
        ) {

            return "хибрид";
        }


        if (
                "전기".equals(
                        fuel
                )
        ) {

            return "електрически";
        }


        return fuel;
    }


    private void hideKeyboard() {

        try {

            InputMethodManager manager =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );


            if (
                    manager != null &&
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


    private boolean contains(
            String text,
            String phrase
    ) {

        String first =
                " " +
                normalize(
                        text
                ) +
                " ";


        String second =
                " " +
                normalize(
                        phrase
                ) +
                " ";


        return first.contains(
                second
        );
    }


    private String remove(
            String text,
            String phrase
    ) {

        String result =
                " " +
                normalize(
                        text
                ) +
                " ";


        result =
                result.replace(
                        " " +
                                normalize(
                                        phrase
                                ) +
                                " ",
                        " "
                );


        return result
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


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
                        '–',
                        '-'
                )
                .replace(
                        '—',
                        '-'
                )
                .replace(
                        '/',
                        ' '
                )
                .replace(
                        '_',
                        ' '
                )
                .replaceAll(
                        "[()\\{}:;!?\"']",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


    private void createCatalog() {

        /*
         * KOREAN
         */

        addMake(
                "hyundai",
                "Y",
                "현대",
                "hyundai",
                "хюндай",
                "хендай",
                "хундай"
        );


        addMake(
                "kia",
                "Y",
                "기아",
                "kia",
                "киа"
        );


        addMake(
                "genesis",
                "Y",
                "제네시스",
                "genesis",
                "дженезис",
                "генезис"
        );


        addMake(
                "chevrolet",
                "Y",
                "쉐보레",
                "chevrolet",
                "chevy",
                "шевролет"
        );


        addMake(
                "renaultkorea",
                "Y",
                "르노코리아",
                "renault korea",
                "рено корея",
                "renault samsung"
        );


        addMake(
                "kgm",
                "Y",
                "KG모빌리티",
                "kgm",
                "ssangyong",
                "ssang yong",
                "санг йонг",
                "сангйонг"
        );


        /*
         * IMPORT
         */

        addMake(
                "mercedes",
                "N",
                "벤츠",
                "mercedes benz",
                "mercedes",
                "benz",
                "мерцедес бенц",
                "мерцедес",
                "бенц"
        );


        addMake(
                "bmw",
                "N",
                "BMW",
                "bmw",
                "бмв"
        );


        addMake(
                "audi",
                "N",
                "아우디",
                "audi",
                "ауди"
        );


        addMake(
                "volkswagen",
                "N",
                "폭스바겐",
                "volkswagen",
                "vw",
                "фолксваген"
        );


        addMake(
                "porsche",
                "N",
                "포르쉐",
                "porsche",
                "порше"
        );


        addMake(
                "volvo",
                "N",
                "볼보",
                "volvo",
                "волво"
        );


        addMake(
                "landrover",
                "N",
                "랜드로버",
                "land rover",
                "range rover",
                "ленд ровер",
                "рейндж ровер"
        );


        addMake(
                "lexus",
                "N",
                "렉서스",
                "lexus",
                "лексус"
        );


        addMake(
                "toyota",
                "N",
                "도요타",
                "toyota",
                "тойота"
        );


        addMake(
                "honda",
                "N",
                "혼다",
                "honda",
                "хонда"
        );


        addMake(
                "nissan",
                "N",
                "닛산",
                "nissan",
                "нисан"
        );


        addMake(
                "infiniti",
                "N",
                "인피니티",
                "infiniti",
                "инфинити"
        );


        addMake(
                "mazda",
                "N",
                "마쯔다",
                "mazda",
                "мазда"
        );


        addMake(
                "mitsubishi",
                "N",
                "미쯔비시",
                "mitsubishi",
                "мицубиши"
        );


        addMake(
                "subaru",
                "N",
                "스바루",
                "subaru",
                "субару"
        );


        addMake(
                "suzuki",
                "N",
                "스즈키",
                "suzuki",
                "сузуки"
        );


        addMake(
                "ford",
                "N",
                "포드",
                "ford",
                "форд"
        );


        addMake(
                "lincoln",
                "N",
                "링컨",
                "lincoln",
                "линкълн"
        );


        addMake(
                "jeep",
                "N",
                "지프",
                "jeep",
                "джип",
                "джийп"
        );


        addMake(
                "cadillac",
                "N",
                "캐딜락",
                "cadillac",
                "кадилак"
        );


        addMake(
                "tesla",
                "N",
                "테슬라",
                "tesla",
                "тесла"
        );


        addMake(
                "mini",
                "N",
                "미니",
                "mini",
                "мини"
        );


        addMake(
                "peugeot",
                "N",
                "푸조",
                "peugeot",
                "пежо"
        );


        addMake(
                "citroen",
                "N",
                "시트로엥",
                "citroen",
                "citroën",
                "ситроен"
        );


        addMake(
                "renault",
                "N",
                "르노",
                "renault",
                "reno",
                "рено"
        );


        addMake(
                "jaguar",
                "N",
                "재규어",
                "jaguar",
                "ягуар"
        );


        addMake(
                "maserati",
                "N",
                "마세라티",
                "maserati",
                "мазерати"
        );


        addMake(
                "ferrari",
                "N",
                "페라리",
                "ferrari",
                "ферари"
        );


        addMake(
                "lamborghini",
                "N",
                "람보르기니",
                "lamborghini",
                "ламборгини"
        );


        addMake(
                "bentley",
                "N",
                "벤틀리",
                "bentley",
                "бентли"
        );


        addMake(
                "rollsroyce",
                "N",
                "롤스로이스",
                "rolls royce",
                "rolls-royce",
                "ролс ройс"
        );


        addMake(
                "astonmartin",
                "N",
                "애스턴마틴",
                "aston martin",
                "астън мартин"
        );


        addMake(
                "mclaren",
                "N",
                "맥라렌",
                "mclaren",
                "макларън"
        );


        addMake(
                "fiat",
                "N",
                "피아트",
                "fiat",
                "фиат"
        );


        addMake(
                "alfaromeo",
                "N",
                "알파로메오",
                "alfa romeo",
                "алфа ромео"
        );


        addMake(
                "smart",
                "N",
                "스마트",
                "smart",
                "смарт"
        );


        addMake(
                "polestar",
                "N",
                "폴스타",
                "polestar",
                "полстар"
        );


        /*
         * HYUNDAI
         */

        addModel(
                "hyundai",
                "아반떼",
                "avante",
                "elantra",
                "елантра"
        );

        addModel(
                "hyundai",
                "쏘나타",
                "sonata",
                "соната"
        );

        addModel(
                "hyundai",
                "그랜저",
                "grandeur"
        );

        addModel(
                "hyundai",
                "코나",
                "kona",
                "кона"
        );

        addModel(
                "hyundai",
                "투싼",
                "tucson",
                "туксон"
        );

        addModel(
                "hyundai",
                "싼타페",
                "santa fe",
                "santafe",
                "санта фе"
        );

        addModel(
                "hyundai",
                "팰리세이드",
                "palisade",
                "палисейд"
        );

        addModel(
                "hyundai",
                "아이오닉5",
                "ioniq 5",
                "ioniq5"
        );

        addModel(
                "hyundai",
                "아이오닉6",
                "ioniq 6",
                "ioniq6"
        );

        addModel(
                "hyundai",
                "스타리아",
                "staria"
        );


        /*
         * KIA
         */

        addModel(
                "kia",
                "K3",
                "k3"
        );

        addModel(
                "kia",
                "K5",
                "k5",
                "optima",
                "оптима"
        );

        addModel(
                "kia",
                "K7",
                "k7"
        );

        addModel(
                "kia",
                "K8",
                "k8"
        );

        addModel(
                "kia",
                "K9",
                "k9"
        );

        addModel(
                "kia",
                "스팅어",
                "stinger",
                "стингер"
        );

        addModel(
                "kia",
                "니로",
                "niro",
                "ниро"
        );

        addModel(
                "kia",
                "셀토스",
                "seltos",
                "селтос"
        );

        addModel(
                "kia",
                "스포티지",
                "sportage",
                "спортидж"
        );

        addModel(
                "kia",
                "쏘렌토",
                "sorento",
                "соренто"
        );

        addModel(
                "kia",
                "모하비",
                "mohave"
        );

        addModel(
                "kia",
                "카니발",
                "carnival",
                "карнивал"
        );

        addModel(
                "kia",
                "EV6",
                "ev6"
        );

        addModel(
                "kia",
                "EV9",
                "ev9"
        );


        /*
         * GENESIS
         */

        addModel(
                "genesis",
                "G70",
                "g70"
        );

        addModel(
                "genesis",
                "G80",
                "g80"
        );

        addModel(
                "genesis",
                "G90",
                "g90"
        );

        addModel(
                "genesis",
                "GV60",
                "gv60"
        );

        addModel(
                "genesis",
                "GV70",
                "gv70"
        );

        addModel(
                "genesis",
                "GV80",
                "gv80"
        );


        /*
         * MERCEDES
         */

        addModel(
                "mercedes",
                "A-클래스",
                "a class",
                "a-class"
        );

        addModel(
                "mercedes",
                "C-클래스",
                "c class",
                "c-class"
        );

        addModel(
                "mercedes",
                "E-클래스",
                "e class",
                "e-class"
        );

        addModel(
                "mercedes",
                "S-클래스",
                "s class",
                "s-class"
        );

        addModel(
                "mercedes",
                "CLA-클래스",
                "cla"
        );

        addModel(
                "mercedes",
                "CLS-클래스",
                "cls"
        );

        addModel(
                "mercedes",
                "GLA-클래스",
                "gla"
        );

        addModel(
                "mercedes",
                "GLB-클래스",
                "glb"
        );

        addModel(
                "mercedes",
                "GLC-클래스",
                "glc"
        );

        addModel(
                "mercedes",
                "GLE-클래스",
                "gle"
        );

        addModel(
                "mercedes",
                "GLS-클래스",
                "gls"
        );

        addModel(
                "mercedes",
                "G-클래스",
                "g class",
                "g-class"
        );


        /*
         * BMW
         */

        addModel(
                "bmw",
                "1시리즈",
                "1 series"
        );

        addModel(
                "bmw",
                "3시리즈",
                "3 series"
        );

        addModel(
                "bmw",
                "5시리즈",
                "5 series"
        );

        addModel(
                "bmw",
                "7시리즈",
                "7 series"
        );

        addModel(
                "bmw",
                "X1",
                "x1"
        );

        addModel(
                "bmw",
                "X3",
                "x3"
        );

        addModel(
                "bmw",
                "X4",
                "x4"
        );

        addModel(
                "bmw",
                "X5",
                "x5"
        );

        addModel(
                "bmw",
                "X6",
                "x6"
        );

        addModel(
                "bmw",
                "X7",
                "x7"
        );

        addModel(
                "bmw",
                "iX",
                "ix"
        );


        /*
         * VOLKSWAGEN
         */

        addModel(
                "volkswagen",
                "골프",
                "golf",
                "голф"
        );

        addModel(
                "volkswagen",
                "파사트",
                "passat",
                "пасат"
        );

        addModel(
                "volkswagen",
                "티구안",
                "tiguan",
                "тигуан"
        );

        addModel(
                "volkswagen",
                "투아렉",
                "touareg",
                "туарег"
        );

        addModel(
                "volkswagen",
                "아테온",
                "arteon",
                "артеон"
        );

        addModel(
                "volkswagen",
                "ID.4",
                "id4",
                "id.4"
        );

        addModel(
                "volkswagen",
                "ID.5",
                "id5",
                "id.5"
        );


        /*
         * PORSCHE
         */

        addModel(
                "porsche",
                "911",
                "911"
        );

        addModel(
                "porsche",
                "카이엔",
                "cayenne",
                "кайен"
        );

        addModel(
                "porsche",
                "마칸",
                "macan",
                "макан"
        );

        addModel(
                "porsche",
                "파나메라",
                "panamera",
                "панамера"
        );

        addModel(
                "porsche",
                "타이칸",
                "taycan",
                "тайкан"
        );

        addModel(
                "porsche",
                "718",
                "718"
        );


        /*
         * HONDA
         */

        addModel(
                "honda",
                "어코드",
                "accord",
                "акорд"
        );

        addModel(
                "honda",
                "CR-V",
                "cr-v",
                "crv",
                "cr v"
        );

        addModel(
                "honda",
                "HR-V",
                "hr-v",
                "hrv",
                "hr v"
        );

        addModel(
                "honda",
                "시빅",
                "civic",
                "сивик"
        );

        addModel(
                "honda",
                "오딧세이",
                "odyssey",
                "одисей"
        );

        addModel(
                "honda",
                "파일럿",
                "pilot",
                "пилот"
        );


        /*
         * FORD
         */

        addModel(
                "ford",
                "익스플로러",
                "explorer",
                "експлорър"
        );

        addModel(
                "ford",
                "머스탱",
                "mustang",
                "мустанг"
        );

        addModel(
                "ford",
                "레인저",
                "ranger",
                "рейнджър"
        );

        addModel(
                "ford",
                "브롱코",
                "bronco",
                "бронко"
        );

        addModel(
                "ford",
                "F150",
                "f150",
                "f-150"
        );


        /*
         * TESLA
         */

        addModel(
                "tesla",
                "모델 3",
                "model 3",
                "модел 3"
        );

        addModel(
                "tesla",
                "모델 Y",
                "model y",
                "мод
