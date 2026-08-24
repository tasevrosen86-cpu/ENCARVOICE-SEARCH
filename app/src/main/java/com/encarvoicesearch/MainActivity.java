package com.encarvoicesearch;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        String encarUrl =
                "https://m.encar.com/ca/search.do#!%7B%22type%22%3A%22car%22%2C%22action%22%3A%22(And.Hidden.N._.MultiViewHidden.N._.(Or.Separation.F._.Separation.B.)_.SellType.%EC%9D%BC%EB%B0%98._.CarType.A._.Mileage.range(..400000)._.Price.range(100..10000).)%22%2C%22toggle%22%3A%7B%7D%2C%22layer%22%3A%22%22%2C%22title%22%3A%22Mercedes-Benz%20GLE-Class%20W167(19%EB%85%84~%ED%98%84%EC%9E%AC)%22%2C%22sort%22%3A%22MobilePriceAsc%22%2C%22cursor%22%3A%22%22%7D";

        webView.loadUrl(encarUrl);
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
