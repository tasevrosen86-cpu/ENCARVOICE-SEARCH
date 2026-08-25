package com.encarvoicesearch;

import android.app.Activity;
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

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView status;
    private EditText searchInput;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private int readAttempts = 0;

    private static final int MAX_READ_ATTEMPTS = 12;
    private static final int SPEECH_REQUEST_CODE = 1001;

    private String lastResult = "";

    private boolean autoReadScheduled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 12, 12, 0);

        // ==============================
        // SEARCH BAR
        // ==============================

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);

        searchInput = new EditText(this);

        searchInput.setHint(
                "Kia Sorento 2025 дизел"
        );

        searchInput.setSingleLine(true);
        searchInput.setTextSize(17);

        Button micButton = new Button(this);
        micButton.setText("🎤");
        micButton.setTextSize(22);

        searchRow.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        searchRow.addView(
                micButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Button showResultsButton =
                new Button(this);

        showResultsButton.setText(
                "ПОКАЖИ РЕЗУЛТАТИТЕ"
        );

        // ==============================
        // STATUS
        // ==============================

        status = new TextView(this);

        status.setText(
                "Напиши или кажи автомобил"
        );

        status.setTextSize(14);
        status.setPadding(8, 15, 8, 15);
        status.setTextIsSelectable(true);

        // ==============================
        // DIAGNOSTIC BUTTONS
        // ==============================

        Button readButton =
                new Button(this);

        readButton.setText(
                "READ FIRST CAR"
        );

        Button copyButton =
                new Button(this);

        copyButton.setText(
                "COPY RESULT"
        );

        // ==============================
        // WEBVIEW
        // ==============================

        webView = new WebView(this);

        WebSettings settings =
                webView.getSettings();

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
                                url != null &&
                                url.contains(
                                        "car.encar.com/list/car"
                                )
                        ) {

                            if (!autoReadScheduled) {

                                autoReadScheduled = true;

                                status.setText(
                                        "Резултатите са заредени.\n" +
                                        "Търся първата обява..."
                                );

                                handler.postDelayed(
                                        () -> startReading(),
                                        1800
                                );
                            }
                        }
                    }
                }
        );

        // ==============================
        // ADD VIEWS
        // ==============================

        root.addView(
                searchRow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                showResultsButton,
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
                readButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                copyButton,
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

        // ==============================
        // BUTTON ACTIONS
        // ==============================

        micButton.setOnClickListener(
                v -> startVoiceRecognition()
        );

        showResultsButton.setOnClickListener(
                v -> searchFromInput()
        );

        readButton.setOnClickListener(
                v -> startReading()
        );

        copyButton.setOnClickListener(
                v -> copyResult()
        );
    }

    // ============================================================
    // VOICE
    // ============================================================

    private void startVoiceRecognition() {

        try {

            Intent intent =
                    new Intent(
                            RecognizerIntent
                                    .ACTION_RECOGNIZE_SPEECH
                    );

            intent.putExtra(
                    RecognizerIntent
                            .EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                            .LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "bg-BG"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Кажи марка, модел, година и гориво"
            );

            startActivityForResult(
                    intent,
                    SPEECH_REQUEST_CODE
            );

        } catch (Exception e) {

            status.setText(
                    "Гласовото разпознаване " +
                    "не е достъпно."
            );
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
                requestCode ==
                        SPEECH_REQUEST_CODE &&
                resultCode ==
                        RESULT_OK &&
                data != null
        ) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent
                                    .EXTRA_RESULTS
                    );

            if (
                    results != null &&
                    !results.isEmpty()
            ) {

                String spoken =
                        results.get(0);

                searchInput.setText(spoken);

                searchInput.setSelection(
                        searchInput
                                .getText()
                                .length()
                );

                status.setText(
                        "Разпознато:\n" +
                        spoken +
                        "\n\n" +
                        "Провери текста и натисни " +
                        "ПОКАЖИ РЕЗУЛТАТИТЕ."
                );
            }
        }
    }

    // ============================================================
    // SEARCH INPUT
    // ============================================================

    private void searchFromInput() {

        String command =
                searchInput
                        .getText()
                        .toString()
                        .trim();

        if (command.isEmpty()) {

            status.setText(
                    "Напиши или кажи " +
                    "каква кола търсиш."
            );

            return;
        }

        SearchParameters params =
                parseSearchCommand(command);

        if (params.error != null) {

            status.setText(
                    params.error
            );

            return;
        }

        searchEncar(params);
    }

    // ============================================================
    // PARSE VOICE / TEXT
    // ============================================================

    private SearchParameters parseSearchCommand(
            String command
    ) {

        SearchParameters result =
                new SearchParameters();

        String text =
                command
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();

        // ==============================
        // MANUFACTURER
        // ==============================

        boolean isKia =
                text.contains("kia") ||
                text.contains("киа");

        if (!isKia) {

            result.error =
                    "Засега имаме проверена " +
                    "Encar структура за Kia Sorento.\n\n" +
                    "Командата не съдържа Kia.";

            return result;
        }

        // ==============================
        // MODEL
        // ==============================

        boolean isSorento =
                text.contains("sorento") ||
                text.contains("соренто");

        if (!isSorento) {

            result.error =
                    "Разпознах Kia, но засега " +
                    "провереният модел е Sorento.";

            return result;
        }

        result.manufacturer =
                "기아";

        result.modelGroup =
                "쏘렌토";

        result.model =
                "더 뉴 쏘렌토 4세대";

        result.brandDisplay =
                "Kia";

        result.modelDisplay =
                "Sorento";

        // ==============================
        // YEAR
        // ==============================

        Matcher yearMatcher =
                Pattern.compile(
                        "\\b(20\\d{2})\\b"
                ).matcher(text);

        if (yearMatcher.find()) {

            result.year =
                    yearMatcher.group(1);

        } else {

            Matcher shortYear =
                    Pattern.compile(
                            "\\b(23|24|25|26)" +
                            "\\s*" +
                            "(?:г|г\\.|год|година)?\\b"
                    ).matcher(text);

            if (shortYear.find()) {

                result.year =
                        "20" +
                        shortYear.group(1);
            }
        }

        if (!result.year.isEmpty()) {

            try {

                int y =
                        Integer.parseInt(
                                result.year
                        );

                if (
                        y < 2023 ||
                        y > 2026
                ) {

                    result.error =
                            "За Kia Sorento " +
                            result.year +
                            " трябва да добавим " +
                            "правилната Encar генерация.\n\n" +
                            "Сегашната проверена генерация е " +
                            "The New Sorento 4th (2023–current).";

                    return result;
                }

            } catch (Exception ignored) {
            }
        }

        // ==============================
        // FUEL
        // ==============================

        if (
                text.contains("хибрид") ||
                text.contains("hybrid")
        ) {

            result.fuel =
                    "가솔린 하이브리드";

            result.fuelDisplay =
                    "Hybrid";

        } else if (
                text.contains("дизел") ||
                text.contains("diesel")
        ) {

            result.fuel =
                    "디젤";

            result.fuelDisplay =
                    "Diesel";

        } else if (
                text.contains("бензин") ||
                text.contains("gasoline") ||
                text.contains("petrol")
        ) {

            result.fuel =
                    "가솔린";

            result.fuelDisplay =
                    "Gasoline";
        }

        return result;
    }

    // ============================================================
    // BUILD ENCAR SEARCH
    // ============================================================

    private void searchEncar(
            SearchParameters params
    ) {

        try {

            StringBuilder action =
                    new StringBuilder();

            action.append("(And.");

            // YEAR
            if (!params.year.isEmpty()) {

                action.append(
                        "Year.range("
                );

                action.append(
                        params.year
                );

                action.append(
                        "00.."
                );

                action.append(
                        params.year
                );

                action.append(
                        "99)._."
                );
            }

            action.append(
                    "Hidden.N."
            );

            action.append(
                    "_.(Or.Separation.F._.Separation.B.)"
            );

            action.append(
                    "_.SellType.일반."
            );

            action.append(
                    "_.(C.CarType.Y." +
                    "_." +
                    "(C.Manufacturer."
            );

            action.append(
                    params.manufacturer
            );

            action.append(
                    "." +
                    "_." +
                    "(C.ModelGroup."
            );

            action.append(
                    params.modelGroup
            );

            action.append(
                    "." +
                    "_." +
                    "Model."
            );

            action.append(
                    params.model
            );

            action.append(
                    "." +
                    ")" +
                    ")" +
                    ")"
            );

            // FUEL
            if (!params.fuel.isEmpty()) {

                action.append(
                        "_." +
                        "FuelType."
                );

                action.append(
                        params.fuel
                );

                action.append(".");
            }

            action.append(")");

            JSONObject json =
                    new JSONObject();

            json.put(
                    "type",
                    "car"
            );

            json.put(
                    "action",
                    action.toString()
            );

            json.put(
                    "title",
                    "Kia The New Sorento 4Th(23년~현재)"
            );

            json.put(
                    "toggle",
                    new JSONObject()
            );

            json.put(
                    "layer",
                    ""
            );

            // Най-ниска цена първо
            json.put(
                    "sort",
                    "MobilePriceAsc"
            );

            String encoded =
                    URLEncoder.encode(
                            json.toString(),
                            StandardCharsets
                                    .UTF_8
                                    .toString()
                    );

            String url =
                    "https://car.encar.com/" +
                    "list/car?page=1&search=" +
                    encoded;

            StringBuilder understood =
                    new StringBuilder();

            understood.append(
                    "Разбрах:\n"
            );

            understood.append(
                    params.brandDisplay
            );

            understood.append(
                    " | "
            );

            understood.append(
                    params.modelDisplay
            );

            if (!params.year.isEmpty()) {

                understood.append(
                        " | "
                );

                understood.append(
                        params.year
                );
            }

            if (!params.fuelDisplay.isEmpty()) {

                understood.append(
                        " | "
                );

                understood.append(
                        params.fuelDisplay
                );
            }

            understood.append(
                    "\n\nЗареждам резултатите..."
            );

            status.setText(
                    understood.toString()
            );

            autoReadScheduled = false;

            webView.loadUrl(url);

        } catch (Exception e) {

            status.setText(
                    "Грешка при търсене: " +
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // READ FIRST CAR
    // ============================================================

    private void startReading() {

        readAttempts = 0;

        status.setText(
                "Търся първата обява..."
        );

        readFirstCar();
    }

    private void readFirstCar() {

        readAttempts++;

        String script =
                "(function() {" +

                "var allLinks = Array.from(" +
                "document.querySelectorAll(" +
                "'a[href*=\"/cars/detail/\"]'" +
                ")" +
                ");" +

                "var car = allLinks.find(function(a) {" +

                "var txt = " +
                "(a.innerText || a.textContent || '')" +
                ".replace(/\\\\s+/g, ' ')" +
                ".trim();" +

                "if (txt.length < 15) return false;" +

                "if (" +
                "a.classList.contains('sponsored_type')" +
                ") return false;" +

                "var p = a.parentElement;" +

                "while (p) {" +

                "if (" +
                "p.classList && " +
                "p.classList.contains('sponsored_type')" +
                ") return false;" +

                "p = p.parentElement;" +

                "}" +

                "return true;" +

                "});" +

                "if (!car) {" +

                "AndroidCarReader.receiveCar(" +
                "JSON.stringify({" +
                "error:'NO_CAR_FOUND'" +
                "})" +
                ");" +

                "return;" +

                "}" +

                "var text = " +
                "(car.innerText || car.textContent || '')" +
                ".replace(/\\\\s+/g, ' ')" +
                ".trim();" +

                "var mileageMatch = " +
                "text.match(/([0-9][0-9,]*)\\\\s*km/i);" +

                "var koreanYear = " +
                "text.match(/([0-9]{2}\\\\/[0-9]{2}식" +
                "(?:\\\\([0-9]{2}년형\\\\))?)/);" +

                "var englishYear = " +
                "text.match(/((?:0[1-9]|1[0-2])\\\\/20[0-9]{2})/);" +

                "var englishRegYear = " +
                "text.match(/([0-9]{2}\\\\s*\\\\(Reg\\\\.\\\\s*" +
                "(?:0[1-9]|1[0-2])\\\\/'[0-9]{2}\\\\))/i);" +

                "var koreanPrice = " +
                "text.match(/([0-9][0-9,]*)\\\\s*만원/);" +

                "var usdPrice = " +
                "text.match(/([0-9][0-9,]*)\\\\s*USD/i);" +

                "var fuelMatch = " +
                "text.match(/" +
                "(가솔린 하이브리드|" +
                "디젤 하이브리드|" +
                "LPG 하이브리드|" +
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

                "var href = car.href || '';" +

                "var idMatch = " +
                "href.match(/\\/cars\\/detail\\/([0-9]+)/);" +

                "var result = {" +

                "text:text," +

                "url:href," +

                "carId:" +
                "(idMatch ? idMatch[1] : '')," +

                "mileage:" +
                "(mileageMatch ? mileageMatch[1] : '')," +

                "year:" +
                "(koreanYear ? koreanYear[1] :" +
                "(englishYear ? englishYear[1] :" +
                "(englishRegYear ? englishRegYear[1] : '')))," +

                "fuel:" +
                "(fuelMatch ? fuelMatch[1] : '')," +

                "priceKrw:" +
                "(koreanPrice ? koreanPrice[1] : '')," +

                "priceUsd:" +
                "(usdPrice ? usdPrice[1] : '')" +

                "};" +

                "AndroidCarReader.receiveCar(" +
                "JSON.stringify(result)" +
                ");" +

                "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    // ============================================================
    // RECEIVE CAR
    // ============================================================

    private class CarReader {

        @JavascriptInterface
        public void receiveCar(
                String json
        ) {

            runOnUiThread(() -> {

                try {

                    JSONObject obj =
                            new JSONObject(json);

                    if (obj.has("error")) {

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

                    String priceText;

                    if (!priceKrw.isEmpty()) {

                        priceText =
                                priceKrw +
                                " 만원";

                    } else if (
                            !priceUsd.isEmpty()
                    ) {

                        priceText =
                                priceUsd +
                                " USD";

                    } else {

                        priceText =
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
                            priceText +
                            "\n\n" +

                            "LINK:\n" +
                            url +
                            "\n\n" +

                            "RAW:\n" +
                            raw;

                    status.setText(
                            lastResult
                    );

                } catch (Exception e) {

                    status.setText(
                            "Грешка при четене: " +
                            e.getMessage()
                    );
                }
            });
        }
    }

    // ============================================================
    // COPY
    // ============================================================

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
                                Context
                                        .CLIPBOARD_SERVICE
                        );

        ClipData clip =
                ClipData.newPlainText(
                        "Encar result",
                        text
                );

        clipboard.setPrimaryClip(
                clip
        );

        if (
                lastResult != null &&
                !lastResult.isEmpty()
        ) {

            status.setText(
                    lastResult +
                    "\n\nКОПИРАНО ✅"
            );

        } else {

            status.setText(
                    text +
                    "\n\nКОПИРАНО ✅"
            );
        }
    }

    // ============================================================
    // SEARCH PARAMETERS
    // ============================================================

    private static class SearchParameters {

        String manufacturer = "";
        String modelGroup = "";
        String model = "";

        String brandDisplay = "";
        String modelDisplay = "";

        String year = "";

        String fuel = "";
        String fuelDisplay = "";

        String error = null;
    }

    // ============================================================
    // BACK
    // ============================================================

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
