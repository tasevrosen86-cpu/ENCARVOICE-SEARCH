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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView status;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean scanEnabled = false;

    private final StringBuilder scanLog =
            new StringBuilder();

    private final Set<String> seenRequests =
            new LinkedHashSet<>();

    private static final int MAX_REQUESTS = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        // ==========================================
        // STATUS
        // ==========================================

        status =
                new TextView(this);

        status.setText(
                "Готово.\n" +
                "Натисни START NETWORK SCAN."
        );

        status.setTextSize(14);
        status.setPadding(
                20,
                15,
                20,
                15
        );

        status.setTextIsSelectable(true);

        // ==========================================
        // BUTTONS
        // ==========================================

        Button startButton =
                new Button(this);

        startButton.setText(
                "START NETWORK SCAN"
        );

        Button stopButton =
                new Button(this);

        stopButton.setText(
                "STOP SCAN"
        );

        Button clearButton =
                new Button(this);

        clearButton.setText(
                "CLEAR SCAN"
        );

        Button copyButton =
                new Button(this);

        copyButton.setText(
                "COPY SCAN"
        );

        // ==========================================
        // WEBVIEW
        // ==========================================

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

        // JavaScript -> Android bridge
        webView.addJavascriptInterface(
                new NetworkBridge(),
                "AndroidScanner"
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

                        if (scanEnabled) {

                            injectNetworkScanner();
                        }
                    }

                    @Override
                    public WebResourceResponse
                    shouldInterceptRequest(
                            WebView view,
                            WebResourceRequest request
                    ) {

                        if (
                                scanEnabled &&
                                request != null &&
                                request.getUrl() != null
                        ) {

                            String url =
                                    request
                                            .getUrl()
                                            .toString();

                            String method =
                                    request
                                            .getMethod();

                            logRequest(
                                    "WEBVIEW",
                                    method,
                                    url,
                                    ""
                            );
                        }

                        return super
                                .shouldInterceptRequest(
                                        view,
                                        request
                                );
                    }
                }
        );

        // ==========================================
        // LAYOUT
        // ==========================================

        root.addView(
                status,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                startButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                stopButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                clearButton,
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

        // ==========================================
        // BUTTON ACTIONS
        // ==========================================

        startButton.setOnClickListener(
                v -> startScan()
        );

        stopButton.setOnClickListener(
                v -> stopScan()
        );

        clearButton.setOnClickListener(
                v -> clearScan()
        );

        copyButton.setOnClickListener(
                v -> copyScan()
        );

        // ==========================================
        // LOAD ENCAR
        // ==========================================

        webView.loadUrl(
                "https://car.encar.com/list/car"
        );
    }

    // ==========================================================
    // START
    // ==========================================================

    private void startScan() {

        scanEnabled = true;

        scanLog.setLength(0);
        seenRequests.clear();

        scanLog.append(
                "===== ENCAR NETWORK SCAN =====\n\n"
        );

        status.setText(
                "SCAN ACTIVE ✅\n\n" +
                "Сега ръчно направи:\n\n" +
                "Change search conditions\n" +
                "→ Manufacturer\n" +
                "→ Kia\n" +
                "→ Sorento\n" +
                "→ поколение\n\n" +
                "После натисни STOP SCAN."
        );

        injectNetworkScanner();
    }

    // ==========================================================
    // STOP
    // ==========================================================

    private void stopScan() {

        scanEnabled = false;

        scanLog.append(
                "\n===== SCAN STOPPED =====\n"
        );

        status.setText(
                "SCAN STOPPED ✅\n\n" +
                "Хванати заявки: " +
                seenRequests.size() +
                "\n\n" +
                "Натисни COPY SCAN и ми прати отчета."
        );
    }

    // ==========================================================
    // CLEAR
    // ==========================================================

    private void clearScan() {

        scanEnabled = false;

        scanLog.setLength(0);
        seenRequests.clear();

        status.setText(
                "Скенерът е изчистен."
        );
    }

    // ==========================================================
    // JAVASCRIPT NETWORK SCANNER
    // ==========================================================

    private void injectNetworkScanner() {

        String script =
                "(function() {" +

                "if (window.__ENCAR_NETWORK_SCANNER__) {" +
                "   return;" +
                "}" +

                "window.__ENCAR_NETWORK_SCANNER__ = true;" +

                // =============================================
                // FETCH
                // =============================================

                "const originalFetch = window.fetch;" +

                "window.fetch = function(input, init) {" +

                "   try {" +

                "       let url = '';" +

                "       if (typeof input === 'string') {" +
                "           url = input;" +
                "       } else if (input && input.url) {" +
                "           url = input.url;" +
                "       }" +

                "       let method = " +
                "           (init && init.method) " +
                "           ? init.method " +
                "           : 'GET';" +

                "       let body = " +
                "           (init && init.body) " +
                "           ? String(init.body) " +
                "           : '';" +

                "       if (body.length > 3000) {" +
                "           body = body.substring(0,3000);" +
                "       }" +

                "       AndroidScanner.onNetworkRequest(" +
                "           'FETCH'," +
                "           method," +
                "           String(url)," +
                "           body" +
                "       );" +

                "   } catch(e) {}" +

                "   return originalFetch.apply(" +
                "       this," +
                "       arguments" +
                "   );" +

                "};" +

                // =============================================
                // XHR
                // =============================================

                "const originalOpen = " +
                "XMLHttpRequest.prototype.open;" +

                "const originalSend = " +
                "XMLHttpRequest.prototype.send;" +

                "XMLHttpRequest.prototype.open = " +
                "function(method, url) {" +

                "   try {" +
                "       this.__scanMethod = method;" +
                "       this.__scanUrl = url;" +
                "   } catch(e) {}" +

                "   return originalOpen.apply(" +
                "       this," +
                "       arguments" +
                "   );" +
                "};" +

                "XMLHttpRequest.prototype.send = " +
                "function(body) {" +

                "   try {" +

                "       let b = body " +
                "           ? String(body) " +
                "           : '';" +

                "       if (b.length > 3000) {" +
                "           b = b.substring(0,3000);" +
                "       }" +

                "       AndroidScanner.onNetworkRequest(" +
                "           'XHR'," +
                "           String(this.__scanMethod || 'GET')," +
                "           String(this.__scanUrl || '')," +
                "           b" +
                "       );" +

                "   } catch(e) {}" +

                "   return originalSend.apply(" +
                "       this," +
                "       arguments" +
                "   );" +
                "};" +

                // =============================================
                // HISTORY / URL CHANGES
                // =============================================

                "const originalPushState = " +
                "history.pushState;" +

                "history.pushState = function() {" +

                "   let result = " +
                "       originalPushState.apply(" +
                "           this," +
                "           arguments" +
                "       );" +

                "   try {" +

                "       AndroidScanner.onNetworkRequest(" +
                "           'HISTORY'," +
                "           'PUSH'," +
                "           location.href," +
                "           ''" +
                "       );" +

                "   } catch(e) {}" +

                "   return result;" +
                "};" +

                "const originalReplaceState = " +
                "history.replaceState;" +

                "history.replaceState = function() {" +

                "   let result = " +
                "       originalReplaceState.apply(" +
                "           this," +
                "           arguments" +
                "       );" +

                "   try {" +

                "       AndroidScanner.onNetworkRequest(" +
                "           'HISTORY'," +
                "           'REPLACE'," +
                "           location.href," +
                "           ''" +
                "       );" +

                "   } catch(e) {}" +

                "   return result;" +
                "};" +

                "AndroidScanner.onScannerInstalled();" +

                "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    // ==========================================================
    // JS BRIDGE
    // ==========================================================

    private class NetworkBridge {

        @JavascriptInterface
        public void onScannerInstalled() {

            if (!scanEnabled) {
                return;
            }

            runOnUiThread(
                    () -> status.setText(
                            "NETWORK SCANNER ACTIVE ✅\n\n" +
                            "Сега отвори филтрите на Encar " +
                            "и избери Kia → Sorento."
                    )
            );
        }

        @JavascriptInterface
        public void onNetworkRequest(
                String source,
                String method,
                String url,
                String body
        ) {

            if (!scanEnabled) {
                return;
            }

            logRequest(
                    source,
                    method,
                    url,
                    body
            );
        }
    }

    // ==========================================================
    // LOG
    // ==========================================================

    private synchronized void logRequest(
            String source,
            String method,
            String url,
            String body
    ) {

        if (!scanEnabled) {
            return;
        }

        if (
                url == null ||
                url.trim().isEmpty()
        ) {
            return;
        }

        /*
         * Филтрираме най-полезните заявки.
         * Иначе браузърът прави стотици заявки
         * за картинки, CSS, реклами и т.н.
         */

        String lower =
                url.toLowerCase();

        boolean interesting =
                lower.contains("api") ||
                lower.contains("search") ||
                lower.contains("model") ||
                lower.contains("manufacturer") ||
                lower.contains("vehicle") ||
                lower.contains("car/list") ||
                lower.contains("grade") ||
                lower.contains("category") ||
                lower.contains("encar.com");

        if (!interesting) {
            return;
        }

        String key =
                source +
                "|" +
                method +
                "|" +
                url +
                "|" +
                body;

        if (seenRequests.contains(key)) {
            return;
        }

        if (
                seenRequests.size() >=
                        MAX_REQUESTS
        ) {
            return;
        }

        seenRequests.add(key);

        scanLog.append(
                "------------------------------\n"
        );

        scanLog.append(
                "SOURCE: "
        );

        scanLog.append(
                source
        );

        scanLog.append(
                "\n"
        );

        scanLog.append(
                "METHOD: "
        );

        scanLog.append(
                method
        );

        scanLog.append(
                "\n"
        );

        scanLog.append(
                "URL:\n"
        );

        scanLog.append(
                url
        );

        scanLog.append(
                "\n"
        );

        if (
                body != null &&
                !body.trim().isEmpty()
        ) {

            scanLog.append(
                    "BODY:\n"
            );

            scanLog.append(
                    body
            );

            scanLog.append(
                    "\n"
            );
        }

        runOnUiThread(() -> {

            status.setText(
                    "NETWORK SCANNER ACTIVE ✅\n\n" +
                    "Хванати заявки: " +
                    seenRequests.size() +
                    "\n\n" +
                    "Продължи с филтрите."
            );

        });
    }

    // ==========================================================
    // COPY
    // ==========================================================

    private void copyScan() {

        String text =
                scanLog.toString();

        if (text.isEmpty()) {

            status.setText(
                    "Няма записани заявки."
            );

            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );

        ClipData clip =
                ClipData.newPlainText(
                        "Encar Network Scan",
                        text
                );

        clipboard.setPrimaryClip(
                clip
        );

        status.setText(
                "NETWORK REPORT COPIED ✅\n\n" +
                "Заявки: " +
                seenRequests.size() +
                "\n\n" +
                "Постави целия отчет в чата."
        );
    }

    // ==========================================================
    // BACK
    // ==========================================================

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
