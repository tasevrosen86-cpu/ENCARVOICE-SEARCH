package com.encarvoicesearch;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        status.setTextSize(16);
        status.setPadding(20, 20, 20, 20);

        Button testButton = new Button(this);
        testButton.setText("KIA SORENTO 2025 DIESEL");

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

        webView.setWebViewClient(new WebViewClient());

        root.addView(
                status,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                testButton,
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

        testButton.setOnClickListener(v ->
                searchKiaSorento2025Diesel()
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
                    "Kia Sorento • 2025 • Diesel • Най-ниска цена"
            );

            webView.loadUrl(url);

        } catch (Exception e) {

            status.setText(
                    "Грешка: " + e.getMessage()
            );
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
