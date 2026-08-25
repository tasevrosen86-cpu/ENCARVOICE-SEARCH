package com.encarvoicesearch;

import android.app.Activity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        status = new TextView(this);
        status.setText("Готово за тест");
        status.setTextSize(15);
        status.setPadding(20, 20, 20, 20);

        Button searchButton = new Button(this);
        searchButton.setText("KIA SORENTO 2025 DIESEL");

        Button readButton = new Button(this);
        readButton.setText("READ FIRST CAR");

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(
                webView,
                true
        );

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

                    new Handler(Looper.getMainLooper())
                            .postDelayed(
                                    () -> readFirstCar(),
                                    2500
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
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);

        searchButton.setOnClickListener(v ->
                searchKiaSorento2025Diesel()
        );

        readButton.setOnClickListener(v ->
                readFirstCar()
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
                    "Търсене: Kia Sorento 2025 Diesel..."
            );

            webView.loadUrl(url);

        } catch (Exception e) {

            status.setText(
                    "Грешка: " + e.getMessage()
            );
        }
    }

    private void readFirstCar() {

        String script =
                "(function() {" +

                "var links = Array.from(" +
                "document.querySelectorAll('a[href*=\"/cars/detail/\"]')" +
                ");" +

                "var car = links.find(function(a) {" +
                "return a.innerText && a.innerText.trim().length > 20;" +
                "});" +

                "if (!car) {" +
                "AndroidCarReader.receiveCar(" +
                "JSON.stringify({error:'NO_CAR_FOUND'})" +
                ");" +
                "return;" +
                "}" +

                "var text = car.innerText" +
                ".replace(/\\\\s+/g,' ')" +
                ".trim();" +

                "var mileageMatch = text.match(/([0-9,]+)km/i);" +

                "var priceMatch = text.match(/([0-9,]+)만원/);" +

                "var yearMatch = text.match(" +
                "/([0-9]{2}\\\\/[0-9]{2}식(?:\\\\([0-9]{2}년형\\\\))?)/" +
                ");" +

                "var fuelMatch = text.match(" +
                "/(디젤|가솔린 하이브리드|가솔린|LPG|전기|수소)/" +
                ");" +

                "var result = {" +
                "text:text," +
                "url:car.href," +
                "mileage:mileageMatch ? mileageMatch[1] : ''," +
                "price:priceMatch ? priceMatch[1] : ''," +
                "year:yearMatch ? yearMatch[1] : ''," +
                "fuel:fuelMatch ? fuelMatch[1] : ''" +
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

                        status.setText(
                                "Не намерих обява."
                        );

                        return;
                    }

                    String year =
                            obj.optString("year");

                    String mileage =
                            obj.optString("mileage");

                    String fuel =
                            obj.optString("fuel");

                    String price =
                            obj.optString("price");

                    String url =
                            obj.optString("url");

                    String text =
                            obj.optString("text");

                    String result =
                            "ПЪРВА ОБЯВА\n\n" +
                            "Година: " + year + "\n" +
                            "Пробег: " + mileage + " km\n" +
                            "Гориво: " + fuel + "\n" +
                            "Цена: " + price + " 만원\n\n" +
                            "LINK:\n" + url + "\n\n" +
                            "RAW:\n" + text;

                    status.setText(result);

                } catch (Exception e) {

                    status.setText(
                            "Грешка при четене: "
                                    + e.getMessage()
                    );
                }
            });
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
