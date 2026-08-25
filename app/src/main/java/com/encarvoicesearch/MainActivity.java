package com.encarvoicesearch;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView status;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private int readAttempts = 0;
    private static final int MAX_READ_ATTEMPTS = 12;

    private String lastResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        status = new TextView(this);
        status.setText("Готово за тест");
        status.setTextSize(14);
        status.setPadding(20, 15, 20, 15);
        status.setTextIsSelectable(true);

        Button searchButton = new Button(this);
        searchButton.setText("KIA SORENTO 2025 DIESEL");

        Button readButton = new Button(this);
        readButton.setText("READ FIRST CAR");

        Button copyButton = new Button(this);
        copyButton.setText("COPY RESULT");

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance()
                .setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(
                new CarReader(),
                "AndroidCarReader"
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                super.onPageFinished(view, url);

                if (url.contains("car.encar.com/list/car")) {

                    status.setText(
                            "Резултатите се зареждат..."
                    );

                    handler.postDelayed(
                            () -> startReading(),
                            1800
                    );
                }
            }
        });

        root.addView(
                status,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                searchButton,
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

        searchButton.setOnClickListener(
                v -> searchKiaSorento2025Diesel()
        );

        readButton.setOnClickListener(
                v -> startReading()
        );

        copyButton.setOnClickListener(
                v -> copyResult()
        );
    }

    private void searchKiaSorento2025Diesel() {

        String action =
                "(And.Year.range(202500..202599)." +
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
                "FuelType.디젤." +
                ")";

        String json =
                "{" +
                "\"type\":\"car\"," +
                "\"action\":\"" + action + "\"," +
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
                    "https://car.encar.com/list/car?page=1&search="
                            + encoded;

            status.setText(
                    "Търся Kia Sorento 2025 Diesel..."
            );

            webView.loadUrl(url);

        } catch (Exception e) {

            status.setText(
                    "Грешка: " + e.getMessage()
            );
        }
    }

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

    private class CarReader {

        @JavascriptInterface
        public void receiveCar(String json) {

            runOnUiThread(() -> {

                try {

                    JSONObject obj =
                            new JSONObject(json);

                    if (obj.has("error")) {

                        if (readAttempts < MAX_READ_ATTEMPTS) {

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
                            obj.optString("carId");

                    String year =
                            obj.optString("year");

                    String mileage =
                            obj.optString("mileage");

                    String fuel =
                            obj.optString("fuel");

                    String priceKrw =
                            obj.optString("priceKrw");

                    String priceUsd =
                            obj.optString("priceUsd");

                    String url =
                            obj.optString("url");

                    String raw =
                            obj.optString("text");

                    String priceText;

                    if (!priceKrw.isEmpty()) {

                        priceText =
                                priceKrw + " 만원";

                    } else if (!priceUsd.isEmpty()) {

                        priceText =
                                priceUsd + " USD";

                    } else {

                        priceText =
                                "не е разпозната";
                    }

                    lastResult =
                            "ПЪРВА ОБЯВА\n\n" +

                            "ID: " +
                            carId + "\n" +

                            "Година: " +
                            year + "\n" +

                            "Пробег: " +
                            mileage + " km\n" +

                            "Гориво: " +
                            fuel + "\n" +

                            "Цена: " +
                            priceText + "\n\n" +

                            "LINK:\n" +
                            url + "\n\n" +

                            "RAW:\n" +
                            raw;

                    status.setText(lastResult);

                } catch (Exception e) {

                    status.setText(
                            "Грешка при четене: " +
                            e.getMessage()
                    );
                }
            });
        }
    }

    private void copyResult() {

        String text = lastResult;

        if (text == null || text.isEmpty()) {

            text = status.getText().toString();
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

        clipboard.setPrimaryClip(clip);

        status.setText(
                lastResult +
                "\n\nКОПИРАНО ✅"
        );
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
