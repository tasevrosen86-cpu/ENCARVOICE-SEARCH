package com.encarvoicesearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView statusText;

    private final StringBuilder scanLog = new StringBuilder();

    private volatile boolean scanning = true;

    private static final String START_URL =
            "https://m.encar.com/ca/search.do";

    private static final String TARGET_SEARCH_API =
            "api.encar.com/search/car/list/mobile";


    @SuppressLint({
            "SetJavaScriptEnabled",
            "JavascriptInterface"
    })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createUi();

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        CookieManager.getInstance()
                .setAcceptThirdPartyCookies(
                        webView,
                        true
                );


        webView.addJavascriptInterface(
                new NetworkBridge(),
                "AndroidNetworkScanner"
        );


        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            Bitmap favicon
                    ) {

                        super.onPageStarted(
                                view,
                                url,
                                favicon
                        );

                        if (
                                scanning &&
                                url != null &&
                                !url.isEmpty()
                        ) {

                            appendLog(
                                    "------------------------------\n" +
                                    "SOURCE: PAGE\n" +
                                    "EVENT: STARTED\n" +
                                    "URL:\n" +
                                    url +
                                    "\n"
                            );
                        }
                    }


                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(
                                view,
                                url
                        );

                        injectNetworkInterceptor();

                        if (
                                scanning &&
                                url != null &&
                                !url.isEmpty()
                        ) {

                            appendLog(
                                    "------------------------------\n" +
                                    "SOURCE: PAGE\n" +
                                    "EVENT: FINISHED\n" +
                                    "URL:\n" +
                                    url +
                                    "\n"
                            );
                        }

                        statusText.setText(
                                "SCANNER ACTIVE"
                        );
                    }


                    @Override
                    public WebResourceResponse shouldInterceptRequest(
                            WebView view,
                            WebResourceRequest request
                    ) {

                        if (
                                scanning &&
                                request != null
                        ) {

                            String url = "";

                            if (request.getUrl() != null) {
                                url =
                                        request
                                                .getUrl()
                                                .toString();
                            }

                            /*
                             * Записваме основно Encar API,
                             * за да няма огромен шум от
                             * картинки, Google, реклами и т.н.
                             */
                            if (
                                    url.contains(
                                            "api.encar.com"
                                    )
                            ) {

                                appendLog(
                                        "------------------------------\n" +
                                        "SOURCE: WEBVIEW\n" +
                                        "METHOD: " +
                                        request.getMethod() +
                                        "\n" +
                                        "URL:\n" +
                                        url +
                                        "\n"
                                );
                            }
                        }

                        return super.shouldInterceptRequest(
                                view,
                                request
                        );
                    }
                }
        );


        startNewScan();

        webView.loadUrl(
                START_URL
        );
    }


    /*
     * =========================================
     * UI
     * =========================================
     */

    private void createUi() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.WHITE
        );


        LinearLayout controls =
                new LinearLayout(this);

        controls.setOrientation(
                LinearLayout.HORIZONTAL
        );


        Button startButton =
                new Button(this);

        startButton.setText(
                "START"
        );


        Button stopButton =
                new Button(this);

        stopButton.setText(
                "STOP"
        );


        Button copyButton =
                new Button(this);

        copyButton.setText(
                "COPY"
        );


        Button clearButton =
                new Button(this);

        clearButton.setText(
                "CLEAR"
        );


        startButton.setOnClickListener(
                v -> {

                    startNewScan();

                    Toast.makeText(
                            MainActivity.this,
                            "Scan started",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );


        stopButton.setOnClickListener(
                v -> stopScan()
        );


        copyButton.setOnClickListener(
                v -> copyScan()
        );


        clearButton.setOnClickListener(
                v -> {

                    synchronized (scanLog) {
                        scanLog.setLength(0);
                    }

                    statusText.setText(
                            "LOG CLEARED"
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "Log cleared",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );


        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );


        controls.addView(
                startButton,
                buttonParams
        );

        controls.addView(
                stopButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        controls.addView(
                copyButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        controls.addView(
                clearButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );


        statusText =
                new TextView(this);

        statusText.setText(
                "Scanner loading..."
        );

        statusText.setTextSize(
                14f
        );

        statusText.setPadding(
                16,
                8,
                16,
                8
        );


        webView =
                new WebView(this);


        root.addView(
                controls,
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
    }


    /*
     * =========================================
     * START SCAN
     * =========================================
     */

    private void startNewScan() {

        scanning = true;

        synchronized (scanLog) {

            scanLog.setLength(0);

            scanLog.append(
                    "===== ENCAR NETWORK SCAN =====\n"
            );

            scanLog.append(
                    "STARTED: "
            );

            scanLog.append(
                    currentTime()
            );

            scanLog.append(
                    "\n\n"
            );
        }

        statusText.setText(
                "SCANNING..."
        );


        /*
         * Ако страницата вече е заредена,
         * инжектираме скенера веднага.
         */
        injectNetworkInterceptor();
    }


    /*
     * =========================================
     * STOP
     * =========================================
     */

    private void stopScan() {

        if (!scanning) {
            return;
        }

        appendLog(
                "------------------------------\n" +
                "===== SCAN STOPPED =====\n" +
                "TIME: " +
                currentTime() +
                "\n"
        );

        scanning = false;

        statusText.setText(
                "SCAN STOPPED"
        );

        Toast.makeText(
                this,
                "Scan stopped",
                Toast.LENGTH_SHORT
        ).show();
    }


    /*
     * =========================================
     * COPY
     * =========================================
     */

    private void copyScan() {

        String text;

        synchronized (scanLog) {

            text =
                    scanLog.toString();
        }


        if (
                text == null ||
                text.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Nothing to copy",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );


        ClipData clipData =
                ClipData.newPlainText(
                        "ENCAR NETWORK SCAN",
                        text
                );


        clipboard.setPrimaryClip(
                clipData
        );


        Toast.makeText(
                this,
                "Scan copied",
                Toast.LENGTH_SHORT
        ).show();
    }


    /*
     * =========================================
     * LOG
     * =========================================
     */

    private void appendLog(
            String text
    ) {

        if (!scanning) {
            return;
        }

        synchronized (scanLog) {

            scanLog.append(
                    text
            );

            if (
                    !text.endsWith("\n")
            ) {

                scanLog.append(
                        "\n"
                );
            }
        }
    }


    private String currentTime() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                );

        return formatter.format(
                new Date()
        );
    }


    /*
     * =========================================
     *
     * JAVASCRIPT NETWORK INTERCEPTOR
     *
     * ТУК ХВАЩАМЕ FETCH + XHR RESPONSE BODY
     *
     * =========================================
     */

    private void injectNetworkInterceptor() {

        if (
                webView == null
        ) {
            return;
        }


        String javascript = """
                (function() {

                    if (window.__ENCAR_SCANNER_INSTALLED__) {
                        return;
                    }

                    window.__ENCAR_SCANNER_INSTALLED__ = true;

                    const TARGET_API =
                        "api.encar.com/search/car/list/mobile";


                    function safeString(value) {

                        if (
                            value === undefined ||
                            value === null
                        ) {
                            return "";
                        }

                        try {

                            if (
                                typeof value === "string"
                            ) {
                                return value;
                            }


                            if (
                                value instanceof URLSearchParams
                            ) {
                                return value.toString();
                            }


                            if (
                                value instanceof FormData
                            ) {

                                const obj = {};

                                value.forEach(
                                    function(v, k) {

                                        obj[k] =
                                            String(v);
                                    }
                                );

                                return JSON.stringify(
                                    obj
                                );
                            }


                            return JSON.stringify(
                                value
                            );

                        } catch (e) {

                            try {

                                return String(
                                    value
                                );

                            } catch (e2) {

                                return "";
                            }
                        }
                    }


                    function sendRequest(
                        source,
                        method,
                        url,
                        body
                    ) {

                        try {

                            window.AndroidNetworkScanner.onRequest(
                                String(source || ""),
                                String(method || "GET"),
                                String(url || ""),
                                safeString(body)
                            );

                        } catch (e) {
                        }
                    }


                    function sendResponse(
                        source,
                        method,
                        url,
                        status,
                        contentType,
                        body
                    ) {

                        try {

                            window.AndroidNetworkScanner.onResponse(
                                String(source || ""),
                                String(method || "GET"),
                                String(url || ""),
                                Number(status || 0),
                                String(contentType || ""),
                                String(body || "")
                            );

                        } catch (e) {
                        }
                    }


                    /*
                     * ============================
                     * FETCH
                     * ============================
                     */

                    if (window.fetch) {

                        const originalFetch =
                            window.fetch;


                        window.fetch =
                            async function(input, init) {

                                let url = "";
                                let method = "GET";
                                let requestBody = "";


                                try {

                                    if (
                                        typeof input === "string"
                                    ) {

                                        url = input;

                                    } else if (
                                        input &&
                                        input.url
                                    ) {

                                        url =
                                            input.url;
                                    }


                                    if (
                                        init &&
                                        init.method
                                    ) {

                                        method =
                                            init.method;

                                    } else if (
                                        input &&
                                        input.method
                                    ) {

                                        method =
                                            input.method;
                                    }


                                    if (
                                        init &&
                                        init.body !== undefined
                                    ) {

                                        requestBody =
                                            init.body;
                                    }

                                } catch (e) {
                                }


                                if (
                                    url.indexOf(
                                        "api.encar.com"
                                    ) !== -1
                                ) {

                                    sendRequest(
                                        "FETCH",
                                        method,
                                        url,
                                        requestBody
                                    );
                                }


                                let response;


                                try {

                                    response =
                                        await originalFetch.apply(
                                            this,
                                            arguments
                                        );

                                } catch (error) {


                                    if (
                                        url.indexOf(
                                            TARGET_API
                                        ) !== -1
                                    ) {

                                        sendResponse(
                                            "FETCH",
                                            method,
                                            url,
                                            0,
                                            "",
                                            "FETCH ERROR: " +
                                                String(error)
                                        );
                                    }


                                    throw error;
                                }


                                /*
                                 * Това е най-важната част.
                                 *
                                 * clone() позволява да прочетем
                                 * response body без да счупим
                                 * оригиналния Encar response.
                                 */

                                try {

                                    if (
                                        url.indexOf(
                                            TARGET_API
                                        ) !== -1
                                    ) {

                                        const clonedResponse =
                                            response.clone();


                                        let contentType = "";


                                        try {

                                            contentType =
                                                response.headers.get(
                                                    "content-type"
                                                ) || "";

                                        } catch (e) {
                                        }


                                        clonedResponse.text()
                                            .then(
                                                function(bodyText) {

                                                    sendResponse(
                                                        "FETCH",
                                                        method,
                                                        url,
                                                        response.status,
                                                        contentType,
                                                        bodyText
                                                    );
                                                }
                                            )
                                            .catch(
                                                function(error) {

                                                    sendResponse(
                                                        "FETCH",
                                                        method,
                                                        url,
                                                        response.status,
                                                        contentType,
                                                        "RESPONSE READ ERROR: " +
                                                            String(error)
                                                    );
                                                }
                                            );
                                    }

                                } catch (error) {

                                    if (
                                        url.indexOf(
                                            TARGET_API
                                        ) !== -1
                                    ) {

                                        sendResponse(
                                            "FETCH",
                                            method,
                                            url,
                                            response
                                                ? response.status
                                                : 0,
                                            "",
                                            "RESPONSE CLONE ERROR: " +
                                                String(error)
                                        );
                                    }
                                }


                                return response;
                            };
                    }


                    /*
                     * ============================
                     * XMLHttpRequest
                     * ============================
                     */

                    if (
                        window.XMLHttpRequest
                    ) {

                        const originalOpen =
                            XMLHttpRequest
                                .prototype
                                .open;


                        const originalSend =
                            XMLHttpRequest
                                .prototype
                                .send;


                        XMLHttpRequest
                            .prototype
                            .open =
                            function(
                                method,
                                url
                            ) {

                                try {

                                    this.__scannerMethod =
                                        method || "GET";

                                    this.__scannerUrl =
                                        url || "";

                                } catch (e) {
                                }


                                return originalOpen.apply(
                                    this,
                                    arguments
                                );
                            };


                        XMLHttpRequest
                            .prototype
                            .send =
                            function(body) {

                                const xhr =
                                    this;


                                const method =
                                    xhr.__scannerMethod ||
                                    "GET";


                                const url =
                                    xhr.__scannerUrl ||
                                    "";


                                if (
                                    url.indexOf(
                                        "api.encar.com"
                                    ) !== -1
                                ) {

                                    sendRequest(
                                        "XHR",
                                        method,
                                        url,
                                        body
                                    );
                                }


                                if (
                                    url.indexOf(
                                        TARGET_API
                                    ) !== -1
                                ) {

                                    xhr.addEventListener(
                                        "loadend",
                                        function() {

                                            let responseBody =
                                                "";

                                            let contentType =
                                                "";


                                            try {

                                                contentType =
                                                    xhr.getResponseHeader(
                                                        "content-type"
                                                    ) || "";

                                            } catch (e) {
                                            }


                                            try {

                                                if (
                                                    xhr.responseType === "" ||
                                                    xhr.responseType === "text"
                                                ) {

                                                    responseBody =
                                                        xhr.responseText ||
                                                        "";

                                                } else if (
                                                    xhr.responseType === "json"
                                                ) {

                                                    responseBody =
                                                        JSON.stringify(
                                                            xhr.response
                                                        );

                                                } else {

                                                    responseBody =
                                                        safeString(
                                                            xhr.response
                                                        );
                                                }

                                            } catch (e) {

                                                responseBody =
                                                    "XHR RESPONSE READ ERROR: " +
                                                    String(e);
                                            }


                                            sendResponse(
                                                "XHR",
                                                method,
                                                url,
                                                xhr.status,
                                                contentType,
                                                responseBody
                                            );
                                        }
                                    );
                                }


                                return originalSend.apply(
                                    this,
                                    arguments
                                );
                            };
                    }


                    console.log(
                        "ENCAR Network Scanner installed"
                    );

                })();
                """;


        webView.evaluateJavascript(
                javascript,
                null
        );
    }


    /*
     * =========================================
     *
     * JAVASCRIPT -> ANDROID BRIDGE
     *
     * =========================================
     */

    public class NetworkBridge {


        @JavascriptInterface
        public void onRequest(
                String source,
                String method,
                String url,
                String body
        ) {

            if (!scanning) {
                return;
            }


            if (
                    url == null ||
                    url.trim().isEmpty()
            ) {
                return;
            }


            /*
             * Пазим само Encar API заявки.
             */

            if (
                    !url.contains(
                            "api.encar.com"
                    )
            ) {
                return;
            }


            StringBuilder block =
                    new StringBuilder();


            block.append(
                    "------------------------------\n"
            );

            block.append(
                    "SOURCE: "
            );

            block.append(
                    source
            );

            block.append(
                    "\n"
            );


            block.append(
                    "METHOD: "
            );

            block.append(
                    method
            );

            block.append(
                    "\n"
            );


            block.append(
                    "URL:\n"
            );

            block.append(
                    url
            );

            block.append(
                    "\n"
            );


            if (
                    body != null &&
                    !body.trim().isEmpty()
            ) {

                block.append(
                        "BODY:\n"
                );

                block.append(
                        body
                );

                block.append(
                        "\n"
                );
            }


            appendLog(
                    block.toString()
            );
        }


        /*
         * ======================================
         *
         * ТОВА Е RESPONSE BODY
         *
         * ======================================
         */

        @JavascriptInterface
        public void onResponse(
                String source,
                String method,
                String url,
                int status,
                String contentType,
                String body
        ) {

            if (!scanning) {
                return;
            }


            if (
                    url == null ||
                    !url.contains(
                            TARGET_SEARCH_API
                    )
            ) {
                return;
            }


            StringBuilder block =
                    new StringBuilder();


            block.append(
                    "\n"
            );

            block.append(
                    "========================================\n"
            );

            block.append(
                    "===== ENCAR SEARCH API RESPONSE =====\n"
            );

            block.append(
                    "========================================\n"
            );


            block.append(
                    "SOURCE: "
            );

            block.append(
                    source
            );

            block.append(
                    "\n"
            );


            block.append(
                    "METHOD: "
            );

            block.append(
                    method
            );

            block.append(
                    "\n"
            );


            block.append(
                    "STATUS: "
            );

            block.append(
                    status
            );

            block.append(
                    "\n"
            );


            block.append(
                    "CONTENT-TYPE: "
            );

            block.append(
                    contentType
            );

            block.append(
                    "\n"
            );


            block.append(
                    "URL:\n"
            );

            block.append(
                    url
            );

            block.append(
                    "\n\n"
            );


            block.append(
                    "RESPONSE BODY:\n"
            );


            if (body != null) {

                block.append(
                        body
                );

            } else {

                block.append(
                        "(empty)"
                );
            }


            block.append(
                    "\n"
            );


            block.append(
                    "===== END ENCAR SEARCH API RESPONSE =====\n"
            );

            block.append(
                    "========================================\n"
            );


            appendLog(
                    block.toString()
            );


            runOnUiThread(
                    () -> {

                        statusText.setText(
                                "✅ ENCAR JSON CAPTURED"
                        );


                        Toast.makeText(
                                MainActivity.this,
                                "ENCAR JSON captured!",
                                Toast.LENGTH_LONG
                        ).show();
                    }
            );
        }
    }


    /*
     * =========================================
     * BACK
     * =========================================
     */

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


    /*
     * =========================================
     * DESTROY
     * =========================================
     */

    @Override
    protected void onDestroy() {

        try {

            if (
                    webView != null
            ) {

                webView.removeJavascriptInterface(
                        "AndroidNetworkScanner"
                );

                webView.destroy();
            }

        } catch (Exception ignored) {
        }


        super.onDestroy();
    }
}
