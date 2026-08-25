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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            "https://api.encar.com/search/car/list/general";

    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES = 5;

    private EditText input;
    private TextView status;
    private LinearLayout results;
    private WebView cookieWebView;

    private String userAgent =
            "Mozilla/5.0";

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

        String maker = "";
        String model = "";
        String badge = "";

        String year = "";
        String mileage = "";
        String fuel = "";

        String price = "";
        String sellType = "";

        long priceNumber =
                Long.MAX_VALUE;
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


        WebSettings ws =
                cookieWebView.getSettings();

        ws.setJavaScriptEnabled(
                true
        );

        ws.setDomStorageEnabled(
                true
        );

        ws.setDatabaseEnabled(
                true
        );


        userAgent =
                ws.getUserAgentString();


        CookieManager cm =
                CookieManager.getInstance();

        cm.setAcceptCookie(
                true
        );

        cm.setAcceptThirdPartyCookies(
                cookieWebView,
                true
        );


        cookieWebView.setWebViewClient(
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

                        CookieManager
                                .getInstance()
                                .flush();


                        if (
                                status != null
                                        &&
                                status
                                        .getText()
                                        .toString()
                                        .startsWith(
                                                "Зареждам"
                                        )
                        ) {

                            status.setText(
                                    "Готово за търсене"
                            );
                        }
                    }
                }
        );


        cookieWebView.loadUrl(
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


        row.addView(
                voice,
                bp
        );

        row.addView(
                search,
                bp
        );

        row.addView(
                clear,
                bp
        );


        status =
                new TextView(this);

        status.setText(
                "Зареждам Encar cookies..."
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


        cookieWebView =
                new WebView(this);

        cookieWebView.setVisibility(
                View.INVISIBLE
        );


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
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );


        root.addView(
                cookieWebView,
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
                    "Няма активно гласово разпознаване",
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

            ArrayList<String> r =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );


            if (
                    r != null
                            &&
                    !r.isEmpty()
            ) {

                input.setText(
                        r.get(0)
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


        results.removeAllViews();


        status.setText(
                buildStatus(spec)
                        +
                "\nТърся в Encar..."
        );


        requestNative(
                spec,
                true,
                false
        );
    }


    /*
     * Първо пробваме с ModelGroup.
     *
     * Ако Encar не приеме ModelGroup и върне 400,
     * автоматично изпращаме заявката без ModelGroup
     * и филтрираме модела от получения JSON.
     */
    private void requestNative(
            Spec spec,
            boolean includeModel,
            boolean cookiesRefreshed
    ) {

        CookieManager
                .getInstance()
                .flush();


        final String cookies =
                collectCookies();


        final String ua =
                userAgent;


        new Thread(
                () -> {

                    try {

                        String q =
                                buildQ(
                                        spec,
                                        includeModel
                                );


                        LinkedHashMap<String, Car> all =
                                new LinkedHashMap<>();


                        for (
                                int page = 0;
                                page < MAX_PAGES;
                                page++
                        ) {

                            int offset =
                                    page * PAGE_SIZE;


                            HttpURLConnection connection =
                                    null;


                            try {

                                String apiUrl =
                                        buildApiUrl(
                                                q,
                                                offset
                                        );


                                connection =
                                        (HttpURLConnection)
                                                new URL(
                                                        apiUrl
                                                )
                                                        .openConnection();


                                connection.setRequestMethod(
                                        "GET"
                                );


                                connection.setConnectTimeout(
                                        15000
                                );


                                connection.setReadTimeout(
                                        25000
                                );


                                connection.setInstanceFollowRedirects(
                                        true
                                );


                                connection.setRequestProperty(
                                        "Accept",
                                        "application/json, text/plain, */*"
                                );


                                connection.setRequestProperty(
                                        "Accept-Language",
                                        "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"
                                );


                                connection.setRequestProperty(
                                        "User-Agent",
                                        ua == null
                                                ?
                                        "Mozilla/5.0"
                                                :
                                        ua
                                );


                                connection.setRequestProperty(
                                        "Referer",
                                        HOME
                                );


                                if (
                                        cookies != null
                                                &&
                                        !cookies.isEmpty()
                                ) {

                                    connection.setRequestProperty(
                                            "Cookie",
                                            cookies
                                    );
                                }


                                int code =
                                        connection.getResponseCode();


                                InputStream stream =
                                        (
                                                code >= 200
                                                        &&
                                                code < 400
                                        )
                                                ?
                                        connection.getInputStream()
                                                :
                                        connection.getErrorStream();


                                String body =
                                        readAll(
                                                stream
                                        );


                                /*
                                 * Cookie проблем:
                                 * обновяваме ги и опитваме още веднъж.
                                 */
                                if (
                                        (
                                                code == 401
                                                        ||
                                                code == 403
                                                        ||
                                                code == 407
                                        )
                                                &&
                                        !cookiesRefreshed
                                ) {

                                    runOnUiThread(
                                            () ->
                                                    refreshCookiesThenRetry(
                                                            spec,
                                                            includeModel
                                                    )
                                    );

                                    return;
                                }


                                /*
                                 * Ако ModelGroup не се приема от general API,
                                 * повтаряме без него.
                                 */
                                if (
                                        code == 400
                                                &&
                                        includeModel
                                ) {

                                    runOnUiThread(
                                            () ->
                                                    status.setText(
                                                            buildStatus(spec)
                                                                    +
                                                            "\nПовтарям заявката без ModelGroup..."
                                                    )
                                    );


                                    requestNative(
                                            spec,
                                            false,
                                            cookiesRefreshed
                                    );

                                    return;
                                }


                                if (
                                        code < 200
                                                ||
                                        code >= 300
                                ) {

                                    String shortBody =
                                            body == null
                                                    ?
                                            ""
                                                    :
                                            body.substring(
                                                    0,
                                                    Math.min(
                                                            body.length(),
                                                            500
                                                    )
                                            );


                                    final String error =
                                            "Encar API HTTP "
                                                    +
                                            code
                                                    +
                                            "\n"
                                                    +
                                            shortBody;


                                    runOnUiThread(
                                            () ->
                                                    showError(
                                                            error
                                                    )
                                    );

                                    return;
                                }


                                List<Car> pageCars =
                                        parseCars(
                                                body
                                        );


                                for (
                                        Car car : pageCars
                                ) {

                                    all.put(
                                            car.id,
                                            car
                                    );
                                }


                                if (
                                        pageCars.size()
                                                <
                                        PAGE_SIZE
                                ) {

                                    break;
                                }


                            } finally {

                                if (
                                        connection != null
                                ) {

                                    connection.disconnect();
                                }
                            }
                        }


                        List<Car> cars =
                                new ArrayList<>(
                                        all.values()
                                );


                        List<Car> filtered =
                                filterCars(
                                        cars,
                                        spec
                                );


                        /*
                         * ВИНАГИ:
                         * най-ниската цена първа.
                         */
                        filtered.sort(
                                Comparator.comparingLong(
                                        car ->
                                                car.priceNumber
                                )
                        );


                        runOnUiThread(
                                () ->
                                        showCars(
                                                filtered,
                                                spec
                                        )
                        );


                    } catch (
                            Exception e
                    ) {

                        String msg =
                                e.getClass()
                                        .getSimpleName()
                                        +
                                ": "
                                        +
                                (
                                        e.getMessage() == null
                                                ?
                                        ""
                                                :
                                        e.getMessage()
                                );


                        runOnUiThread(
                                () ->
                                        showError(
                                                "Мрежова грешка: "
                                                        +
                                                msg
                                        )
                        );
                    }
                }
        ).start();
    }


    private void refreshCookiesThenRetry(
            Spec spec,
            boolean includeModel
    ) {

        status.setText(
                buildStatus(spec)
                        +
                "\nОбновявам Encar cookies..."
        );


        cookieWebView.stopLoading();


        cookieWebView.setWebViewClient(
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


                        CookieManager
                                .getInstance()
                                .flush();


                        requestNative(
                                spec,
                                includeModel,
                                true
                        );
                    }
                }
        );


        cookieWebView.loadUrl(
                HOME
                        +
                "?refresh="
                        +
                System.currentTimeMillis()
        );
    }


    /*
     * ВАЖНО:
     *
     * general API използва ModifiedDate в sr.
     *
     * След това ние сортираме получените коли
     * локално по Price.
     */
    private String buildApiUrl(
            String q,
            int offset
    ) throws Exception {

        return API
                +
                "?count=true"
                +
                "&q="
                +
                URLEncoder.encode(
                        q,
                        StandardCharsets.UTF_8.name()
                )
                +
                "&sr="
                +
                URLEncoder.encode(
                        "|ModifiedDate|"
                                +
                        offset
                                +
                        "|"
                                +
                        PAGE_SIZE,
                        StandardCharsets.UTF_8.name()
                );
    }


    /*
     * GENERAL API Q FORMAT:
     *
     * (And.Hidden.N.
     * _.CarType.Y.
     * _.Manufacturer.기아.
     * _.ModelGroup.쏘렌토.
     * _.FuelType.가솔린.
     * _.Mileage.0_100000.)
     *
     * НЯМА:
     *
     * (C.CarType...)
     * (C.Manufacturer...)
     *
     * както беше в стария mobile/action формат.
     */
    private String buildQ(
            Spec spec,
            boolean includeModel
    ) {

        StringBuilder q =
                new StringBuilder(
                        "(And.Hidden.N."
                );


        q.append(
                "_.CarType."
        );

        q.append(
                spec.brand.carType
        );

        q.append(
                "."
        );


        q.append(
                "_.Manufacturer."
        );

        q.append(
                spec.brand.encar
        );

        q.append(
                "."
        );


        if (
                includeModel
                        &&
                spec.modelGroup != null
                        &&
                !spec.modelGroup.isEmpty()
        ) {

            q.append(
                    "_.ModelGroup."
            );

            q.append(
                    spec.modelGroup
            );

            q.append(
                    "."
            );
        }


        if (
                spec.fuel != null
        ) {

            q.append(
                    "_.FuelType."
            );

            q.append(
                    spec.fuel
            );

            q.append(
                    "."
            );
        }


        /*
         * Потвърден general API формат:
         *
         * Mileage.0_90000
         */
        if (
                spec.maxMileage != null
        ) {

            q.append(
                    "_.Mileage.0_"
            );

            q.append(
                    spec.maxMileage
            );

            q.append(
                    "."
            );
        }


        /*
         * Годината нарочно НЕ я изпращаме към API,
         * докато не използваме потвърден general Year синтаксис.
         *
         * Филтрираме я локално от JSON.
         */
        q.append(
                ")"
        );


        return q.toString();
    }


    private List<Car> filterCars(
            List<Car> source,
            Spec spec
    ) {

        List<Car> output =
                new ArrayList<>();


        for (
                Car car : source
        ) {

            if (
                    !matchesYear(
                            car,
                            spec
                    )
            ) {

                continue;
            }


            if (
                    !matchesMileage(
                            car,
                            spec
                    )
            ) {

                continue;
            }


            if (
                    !matchesFuel(
                            car,
                            spec
                    )
            ) {

                continue;
            }


            if (
                    !matchesModel(
                            car,
                            spec
                    )
            ) {

                continue;
            }


            if (
                    isLeaseOrRent(
                            car
                    )
            ) {

                continue;
            }


            output.add(
                    car
            );
        }


        return output;
    }


    private boolean matchesYear(
            Car car,
            Spec spec
    ) {

        if (
                spec.yearFrom == null
                        ||
                spec.yearTo == null
        ) {

            return true;
        }


        String digits =
                car.year.replaceAll(
                        "[^0-9]",
                        ""
                );


        if (
                digits.length()
                        <
                4
        ) {

            return true;
        }


        try {

            int year =
                    Integer.parseInt(
                            digits.substring(
                                    0,
                                    4
                            )
                    );


            return year >= spec.yearFrom
                    &&
                    year <= spec.yearTo;


        } catch (
                Exception e
        ) {

            return true;
        }
    }


    private boolean matchesMileage(
            Car car,
            Spec spec
    ) {

        if (
                spec.maxMileage == null
                        ||
                car.mileage.isEmpty()
        ) {

            return true;
        }


        long n =
                number(
                        car.mileage
                );


        return n == Long.MAX_VALUE
                ||
                n <= spec.maxMileage;
    }


    private boolean matchesFuel(
            Car car,
            Spec spec
    ) {

        if (
                spec.fuel == null
                        ||
                car.fuel.isEmpty()
        ) {

            return true;
        }


        return normalize(
                car.fuel
        )
                .contains(
                        normalize(
                                spec.fuel
                        )
                );
    }


    private boolean matchesModel(
            Car car,
            Spec spec
    ) {

        if (
                spec.modelGroup == null
                        ||
                spec.modelGroup.isEmpty()
        ) {

            return true;
        }


        String hay =
                normalize(
                        car.model
                                +
                        " "
                                +
                        car.badge
                );


        /*
         * Ако API обектът няма model field,
         * не го изхвърляме автоматично.
         */
        return hay.isEmpty()
                ||
                hay.contains(
                        normalize(
                                spec.modelGroup
                        )
                );
    }


    private boolean isLeaseOrRent(
            Car car
    ) {

        String text =
                normalize(
                        car.sellType
                                +
                        " "
                                +
                        car.model
                                +
                        " "
                                +
                        car.badge
                );


        return text.contains(
                "렌트"
        )
                ||
                text.contains(
                        "리스"
                )
                ||
                text.contains(
                        "월"
                );
    }


    private String collectCookies() {

        CookieManager manager =
                CookieManager.getInstance();


        LinkedHashMap<String, String> merged =
                new LinkedHashMap<>();


        mergeCookies(
                merged,
                manager.getCookie(
                        "https://m.encar.com"
                )
        );


        mergeCookies(
                merged,
                manager.getCookie(
                        "https://api.encar.com"
                )
        );


        StringBuilder output =
                new StringBuilder();


        for (
                Map.Entry<String, String> entry
                        :
                merged.entrySet()
        ) {

            if (
                    output.length()
                            >
                    0
            ) {

                output.append(
                        "; "
                );
            }


            output.append(
                    entry.getKey()
            );


            output.append(
                    "="
            );


            output.append(
                    entry.getValue()
            );
        }


        return output.toString();
    }


    private void mergeCookies(
            Map<String, String> output,
            String cookies
    ) {

        if (
                cookies == null
                        ||
                cookies.trim().isEmpty()
        ) {

            return;
        }


        String[] parts =
                cookies.split(
                        ";"
                );


        for (
                String part : parts
        ) {

            String p =
                    part.trim();


            int index =
                    p.indexOf(
                            '='
                    );


            if (
                    index > 0
            ) {

                output.put(
                        p.substring(
                                0,
                                index
                        )
                                .trim(),

                        p.substring(
                                index + 1
                        )
                                .trim()
                );
            }
        }
    }


    private Spec parse(
            String raw
    ) {

        String text =
                normalize(
                        raw
                );


        Spec spec =
                new Spec();


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


        int longest =
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
                    longest
            ) {

                best =
                        entry.getValue();


                longest =
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


            int longest =
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
                        longest
                ) {

                    best =
                            entry.getValue();


                    longest =
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


        /*
         * Mercedes:
         *
         * GLE 300d -> GLE-클래스
         */
        if (
                "mercedes".equals(
                        brand.key
                )
        ) {

            Matcher matcher =
                    Pattern.compile(
                            "\\b(gle|glc|gls|gla|glb|cla|cls|cle)\\s*[- ]?\\d*[a-z]*\\b"
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


            matcher =
                    Pattern.compile(
                            "\\b([acesg])\\s*[- ]?\\d{3}[a-z]*\\b"
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
        }


        /*
         * BMW:
         *
         * 520d -> 5시리즈
         */
        if (
                "bmw".equals(
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

        List<Integer> output =
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

                    output.add(
                            year
                    );
                }

            } catch (
                    Exception ignored
            ) {
            }
        }


        return output;
    }


    private Integer findMileage(
            String text
    ) {

        Matcher matcher =
                Pattern.compile(
                        "(\\d{1,3})\\s*(хиляди|хил|thousand)\\s*(km|км|километра|километри)?"
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
                )
                        *
                        1000;

            } catch (
                    Exception ignored
            ) {
            }
        }


        matcher =
                Pattern.compile(
                        "([0-9][0-9 .]{1,10})\\s*(km|км|километра|километри)"
                )
                        .matcher(
                                text
                        );


        if (
                matcher.find()
        ) {

            try {

                int value =
                        Integer.parseInt(
                                matcher
                                        .group(1)
                                        .replaceAll(
                                                "[^0-9]",
                                                ""
                                        )
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

        StringBuilder output =
                new StringBuilder(
                        "НАЙ-ЕВТИНИ ПЪРВО | "
                );


        output.append(
                spec.brand.encar
        );


        if (
                spec.modelGroup != null
        ) {

            output.append(
                    " | "
            );

            output.append(
                    spec.modelGroup
            );
        }


        if (
                spec.yearFrom != null
        ) {

            output.append(
                    " | "
            );

            output.append(
                    spec.yearFrom
            );


            if (
                    !spec.yearFrom.equals(
                            spec.yearTo
                    )
            ) {

                output.append(
                        "-"
                );

                output.append(
                        spec.yearTo
                );
            }
        }


        if (
                spec.fuel != null
        ) {

            output.append(
                    " | "
            );

            output.append(
                    spec.fuel
            );
        }


        if (
                spec.maxMileage != null
        ) {

            output.append(
                    " | до "
            );

            output.append(
                    spec.maxMileage
            );

            output.append(
                    " km"
            );
        }


        return output.toString();
    }


    private String readAll(
            InputStream stream
    ) throws Exception {

        if (
                stream == null
        ) {

            return "";
        }


        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
                        )
                );


        StringBuilder output =
                new StringBuilder();


        String line;


        while (
                (
                        line =
                                reader.readLine()
                )
                        != null
        ) {

            output.append(
                    line
            );
        }


        reader.close();


        return output.toString();
    }


    private List<Car> parseCars(
            String body
    ) throws Exception {

        Object root;


        if (
                body
                        .trim()
                        .startsWith(
                                "["
                        )
        ) {

            root =
                    new JSONArray(
                            body
                    );

        } else {

            root =
                    new JSONObject(
                            body
                    );
        }


        LinkedHashMap<String, Car> unique =
                new LinkedHashMap<>();


        collectCars(
                root,
                unique
        );


        return new ArrayList<>(
                unique.values()
        );
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
                            number(
                                    price
                            );


                    car.maker =
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


                    car.sellType =
                            pick(
                                    object,
                                    "SellType",
                                    "sellType",
                                    "SaleType",
                                    "saleType"
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

                        Object child =
                                object.opt(
                                        names.optString(
                                                i
                                        )
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


    private String pick(
            JSONObject object,
            String... wanted
    ) {

        JSONArray names =
                object.names();


        if (
                names == null
        ) {

            return "";
        }


        for (
                String wantedName : wanted
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


                if (
                        key.equalsIgnoreCase(
                                wantedName
                        )
                ) {

                    Object value =
                            object.opt(
                                    key
                            );


                    if (
                            value != null
                                    &&
                            value != JSONObject.NULL
                    ) {

                        return String.valueOf(
                                value
                        )
                                .trim();
                    }
                }
            }
        }


        return "";
    }


    private long number(
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


    private void showCars(
            List<Car> cars,
            Spec spec
    ) {

        results.removeAllViews();


        if (
                cars.isEmpty()
        ) {

            status.setText(
                    buildStatus(spec)
                            +
                    "\nAPI работи, но след филтрите няма намерени обяви."
            );


            addMessage(
                    "Няма автомобили по тези критерии."
            );


            return;
        }


        status.setText(
                buildStatus(spec)
                        +
                "\nНамерени "
                        +
                cars.size()
                        +
                " обяви | цена ↑"
        );


        int limit =
                Math.min(
                        100,
                        cars.size()
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
                !car.maker.isEmpty()
        ) {

            text.append(
                    car.maker
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
                    car.mileage
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
                car.price
        );

        text.append(
                " 만원"
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

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    "https://www.encar.com/dc/dc_cardetailview.do?carid="
                                            +
                                    Uri.encode(
                                            id
                                    )
                            )
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


    private void showError(
            String message
    ) {

        status.setText(
                message
        );


        results.removeAllViews();


        addMessage(
                message
        );
    }


    private void addMessage(
            String message
    ) {

        TextView view =
                new TextView(this);


        view.setText(
                message
        );

        view.setTextSize(
                16f
        );

        view.setPadding(
                14,
                14,
                14,
                14
        );


        results.addView(
                view
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


        return source.contains(
                target
        )
                ||
                (
                        (
                                needle.contains("-")
                                        ||
                                needle.contains(".")
                        )
                                &&
                        text.contains(
                                needle
                        )
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
                    normalize(
                            alias
                    ),
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
         * KOREAN
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
         * IMPORTED
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
    }


    @Override
    protected void onDestroy() {

        if (
                cookieWebView != null
        ) {

            cookieWebView.stopLoading();

            cookieWebView.destroy();
        }


        super.onDestroy();
    }
            }
