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

    private WebView webView;
    private TextView status;
    private EditText searchInput;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private int readAttempts = 0;
    private static final int MAX_READ_ATTEMPTS = 12;

    private String lastResult = "";
    private String lastCarUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        /*
         * ТЕКСТОВО ПОЛЕ
         */
        searchInput =
                new EditText(this);

        searchInput.setHint(
                "Kia Sorento 2025 дизел"
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

        /*
         * БУТОНИ ГЛАС + ТЪРСЕНЕ
         */
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

        /*
         * STATUS
         */
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

        status.setTextIsSelectable(true);

        /*
         * ДОЛНИ БУТОНИ
         */
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

        /*
         * WEBVIEW
         */
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

        /*
         * JS -> ANDROID
         */
        webView.addJavascriptInterface(
                new CarReader(),
                "AndroidCarReader"
        );

        /*
         * КОГАТО ENCAR РЕЗУЛТАТИТЕ СЕ ЗАРЕДЯТ
         */
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
                                url != null &&
                                url.contains(
                                        "car.encar.com/list/car"
                                )
                        ) {

                            status.setText(
                                    "Резултатите се зареждат..."
                            );

                            handler.postDelayed(
                                    () -> startReading(),
                                    1800
                            );
                        }
                    }
                }
        );

        /*
         * UI
         */
        root.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                buttons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                status,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                tools,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);

        /*
         * BUTTON ACTIONS
         */
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

    /*
     * ==========================
     * VOICE
     * ==========================
     */
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
                "Кажи: Kia Sorento 2025 дизел"
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
                requestCode == VOICE_REQUEST &&
                resultCode == RESULT_OK &&
                data != null
        ) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (
                    results != null &&
                    !results.isEmpty()
            ) {

                String text =
                        results.get(0);

                /*
                 * Поправя примерно:
                 * "20 25" -> "2025"
                 */
                text =
                        text.replaceAll(
                                "\\b20\\s+(\\d{2})\\b",
                                "20$1"
                        );

                searchInput.setText(text);

                searchInput.setSelection(
                        searchInput
                                .getText()
                                .length()
                );
            }
        }
    }

    /*
     * ==========================
     * SEARCH PARSER
     * ==========================
     */
    private void searchFromInput() {

        String original =
                searchInput
                        .getText()
                        .toString()
                        .trim();

        if (original.isEmpty()) {

            status.setText(
                    "Кажи или напиши автомобил."
            );

            return;
        }

        String text =
                original
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        /*
         * Засега НЕ разширяваме архитектурата.
         *
         * Първо доказваме старото работещо
         * търсене + глас.
         */
        boolean kia =
                text.contains("kia") ||
                text.contains("киа") ||
                text.contains("кия");

        boolean sorento =
                text.contains("sorento") ||
                text.contains("соренто");

        if (!kia || !sorento) {

            status.setText(
                    "Тази тестова версия засега е за Kia Sorento.\n" +
                    "Първо потвърждаваме работещата основа."
            );

            return;
        }

        Integer year =
                findYear(text);

        if (year == null) {

            status.setText(
                    "Не разпознах годината."
            );

            return;
        }

        if (
                year < 2023 ||
                year > 2026
        ) {

            status.setText(
                    "Тази работеща Sorento основа е за поколението 2023–2026."
            );

            return;
        }

        String fuelKorean;
        String fuelName;

        if (
                text.contains("дизел") ||
                text.contains("diesel")
        ) {

            fuelKorean =
                    "디젤";

            fuelName =
                    "DIESEL";

        } else if (
                text.contains("бензин") ||
                text.contains("gasoline") ||
                text.contains("petrol")
        ) {

            fuelKorean =
                    "가솔린";

            fuelName =
                    "GASOLINE";

        } else {

            status.setText(
                    "Не разпознах горивото.\n" +
                    "Кажи дизел или бензин."
            );

            return;
        }

        searchSorento(
                year,
                fuelKorean,
                fuelName
        );
    }

    private Integer findYear(
            String text
    ) {

        /*
         * 20 25 -> 2025
         */
        text =
                text.replaceAll(
                        "\\b20\\s+(\\d{2})\\b",
                        "20$1"
                );

        Matcher full =
                Pattern.compile(
                        "\\b(20\\d{2})\\b"
                )
                        .matcher(text);

        if (full.find()) {

            try {

                return Integer.parseInt(
                        full.group(1)
                );

            } catch (
                    Exception ignored
            ) {
            }
        }

        /*
         * "25 година" -> 2025
         */
        Matcher shortYear =
                Pattern.compile(
                        "\\b(2[3-6])\\s*(?:г|година|год)?\\b"
                )
                        .matcher(text);

        if (shortYear.find()) {

            try {

                return 2000 +
                        Integer.parseInt(
                                shortYear.group(1)
                        );

            } catch (
                    Exception ignored
            ) {
            }
        }

        return null;
    }

    /*
     * ==========================
     * СТАРАТА РАБОТЕЩА ОСНОВА
     * ==========================
     */
    private void searchSorento(
            int year,
            String fuel,
            String fuelName
    ) {

        int yearFrom =
                year * 100;

        int yearTo =
                yearFrom + 99;

        /*
         * ВАЖНО:
         *
         * Не сменяме работещата Encar структура.
         */
        String action =
                "(And.Year.range(" +
                yearFrom +
                ".." +
                yearTo +
                ")." +

                "_.Hidden.N." +

                "_." +
                "(Or.Separation.F._.Separation.B.)" +

                "_." +
                "SellType.일반." +

                "_." +
                "(C.CarType.Y." +

                "_." +
                "(C.Manufacturer.기아." +

                "_." +
                "(C.ModelGroup.쏘렌토." +

                "_." +
                "Model.더 뉴 쏘렌토 4세대." +

                ")" +
                ")" +
                ")" +

                "_." +
                "FuelType." +
                fuel +
                "." +

                ")";

        String json =
                "{" +
                "\"type\":\"car\"," +
                "\"action\":\"" +
                action +
                "\"," +

                "\"title\":\"Kia The New Sorento 4Th(23년~현재)\"," +

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
                    "Търся Kia Sorento " +
                    year +
                    " " +
                    fuelName +
                    "\nНАЙ-НИСКА ЦЕНА ПЪРВО"
            );

            webView.loadUrl(url);

        } catch (
                Exception e
        ) {

            status.setText(
                    "Грешка при търсене: " +
                    e.getMessage()
            );
        }
    }

    /*
     * ==========================
     * READ FIRST REAL CAR
     * ==========================
     */
    private void startReading() {

        readAttempts = 0;

        status.setText(
                "Търся първата реална обява..."
        );

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

        webView.evaluateJavascript(
                script,
                null
        );
    }

    /*
     * ==========================
     * RECEIVE CAR
     * ==========================
     */
    private class CarReader {

        @JavascriptInterface
        public void receiveCar(
                String json
        ) {

            runOnUiThread(
                    () -> {

                        try {

                            JSONObject obj =
                                    new JSONObject(json);

                            if (
                                    obj.has("error")
                            ) {

                                if (
                                        readAttempts <
                                        MAX_READ_ATTEMPTS
                                ) {

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
                                        priceKrw +
                                        " 만원";

                            } else if (
                                    !priceUsd.isEmpty()
                            ) {

                                price =
                                        priceUsd +
                                        " USD";

                            } else {

                                price =
                                        "не е разпозната";
                            }

                            lastResult =
                                    "ПЪРВА ОБЯВА\n\n" +

                                    "ID: " +
                                    carId +
                                    "\n" +

                                    "Година: " +
                                    year +
                                    "\n" +

                                    "Пробег: " +
                                    mileage +
                                    " km\n" +

                                    "Гориво: " +
                                    fuel +
                                    "\n" +

                                    "Цена: " +
                                    price +
                                    "\n\n" +

                                    "LINK:\n" +
                                    url +
                                    "\n\n" +

                                    "RAW:\n" +
                                    raw;

                            status.setText(
                                    lastResult
                            );

                        } catch (
                                Exception e
                        ) {

                            status.setText(
                                    "Грешка при четене: " +
                                    e.getMessage()
                            );
                        }
                    }
            );
        }
    }

    /*
     * ==========================
     * OPEN FIRST CAR
     * ==========================
     */
    private void openFirstCar() {

        if (
                lastCarUrl == null ||
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

    /*
     * ==========================
     * COPY
     * ==========================
     */
    private void copyResult() {

        String text =
                lastResult;

        if (
                text == null ||
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
                text +
                "\n\nКОПИРАНО ✅"
        );
    }

    @Override
    public void onBackPressed() {

        if (
                webView != null &&
                webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
