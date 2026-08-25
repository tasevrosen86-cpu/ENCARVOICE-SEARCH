package com.example.encarnetworkscanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var statusText: TextView

    private val scanLog = StringBuilder()

    @Volatile
    private var scanning = true

    private val startUrl = "https://m.encar.com/ca/search.do"

    // Само този API response е най-важен за нас.
    private val targetSearchApi =
        "api.encar.com/search/car/list/mobile"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUi()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.addJavascriptInterface(
            NetworkBridge(),
            "AndroidNetworkScanner"
        )

        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: android.graphics.Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)

                if (scanning && !url.isNullOrBlank()) {
                    appendLog(
                        """
------------------------------
SOURCE: PAGE
EVENT: STARTED
URL:
$url
""".trimIndent()
                    )
                }
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                injectNetworkInterceptor()

                if (scanning && !url.isNullOrBlank()) {
                    appendLog(
                        """
------------------------------
SOURCE: PAGE
EVENT: FINISHED
URL:
$url
""".trimIndent()
                    )
                }

                statusText.text = "Scanner active"
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): android.webkit.WebResourceResponse? {

                if (scanning && request != null) {

                    val url = request.url?.toString() ?: ""

                    appendLog(
                        """
------------------------------
SOURCE: WEBVIEW
METHOD: ${request.method}
URL:
$url
""".trimIndent()
                    )
                }

                return super.shouldInterceptRequest(view, request)
            }
        }

        startNewScan()

        webView.loadUrl(startUrl)
    }

    private fun createUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val startButton = Button(this).apply {
            text = "START"

            setOnClickListener {
                startNewScan()
                Toast.makeText(
                    this@MainActivity,
                    "Scan started",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val stopButton = Button(this).apply {
            text = "STOP"

            setOnClickListener {
                stopScan()
            }
        }

        val copyButton = Button(this).apply {
            text = "COPY"

            setOnClickListener {
                copyScan()
            }
        }

        val clearButton = Button(this).apply {
            text = "CLEAR"

            setOnClickListener {
                synchronized(scanLog) {
                    scanLog.clear()
                }

                statusText.text = "Log cleared"

                Toast.makeText(
                    this@MainActivity,
                    "Log cleared",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        controls.addView(
            startButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        controls.addView(
            stopButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        controls.addView(
            copyButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        controls.addView(
            clearButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        statusText = TextView(this).apply {
            text = "Scanner loading..."
            textSize = 14f
            setPadding(16, 8, 16, 8)
        }

        webView = WebView(this)

        root.addView(
            controls,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun startNewScan() {

        scanning = true

        synchronized(scanLog) {

            scanLog.clear()

            scanLog.append(
                """
===== ENCAR NETWORK SCAN =====
STARTED: ${currentTime()}

""".trimIndent()
            )

            scanLog.append("\n")
        }

        statusText.text = "SCANNING..."
    }

    private fun stopScan() {

        if (!scanning) {
            return
        }

        appendLog(
            """
------------------------------
===== SCAN STOPPED =====
TIME: ${currentTime()}
""".trimIndent()
        )

        scanning = false

        statusText.text = "SCAN STOPPED"

        Toast.makeText(
            this,
            "Scan stopped",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun copyScan() {

        val text = synchronized(scanLog) {
            scanLog.toString()
        }

        if (text.isBlank()) {

            Toast.makeText(
                this,
                "Nothing to copy",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "ENCAR NETWORK SCAN",
                text
            )
        )

        Toast.makeText(
            this,
            "Scan copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun appendLog(text: String) {

        if (!scanning) {
            return
        }

        synchronized(scanLog) {

            scanLog.append(text)

            if (!text.endsWith("\n")) {
                scanLog.append("\n")
            }
        }
    }

    private fun currentTime(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

    private fun injectNetworkInterceptor() {

        val javascript = """
(function() {

    if (window.__ENCAR_SCANNER_INSTALLED__) {
        return;
    }

    window.__ENCAR_SCANNER_INSTALLED__ = true;

    const TARGET_API =
        "api.encar.com/search/car/list/mobile";


    function safeString(value) {

        if (value === undefined || value === null) {
            return "";
        }

        try {

            if (typeof value === "string") {
                return value;
            }

            if (value instanceof URLSearchParams) {
                return value.toString();
            }

            if (value instanceof FormData) {

                const obj = {};

                value.forEach(function(v, k) {
                    obj[k] = String(v);
                });

                return JSON.stringify(obj);
            }

            return JSON.stringify(value);

        } catch (e) {

            try {
                return String(value);
            } catch (e2) {
                return "";
            }
        }
    }


    function reportRequest(
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


    function reportResponse(
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
     * ==========================
     * FETCH INTERCEPTOR
     * ==========================
     */

    if (window.fetch) {

        const originalFetch = window.fetch;

        window.fetch = async function(input, init) {

            let url = "";
            let method = "GET";
            let requestBody = "";

            try {

                if (typeof input === "string") {
                    url = input;

                } else if (input && input.url) {
                    url = input.url;
                }

                if (init && init.method) {
                    method = init.method;

                } else if (input && input.method) {
                    method = input.method;
                }

                if (init && init.body !== undefined) {
                    requestBody = init.body;
                }

            } catch (e) {
            }

            reportRequest(
                "FETCH",
                method,
                url,
                requestBody
            );

            let response;

            try {

                response =
                    await originalFetch.apply(
                        this,
                        arguments
                    );

            } catch (error) {

                if (
                    url &&
                    url.indexOf(TARGET_API) !== -1
                ) {

                    reportResponse(
                        "FETCH",
                        method,
                        url,
                        0,
                        "",
                        "FETCH ERROR: " + String(error)
                    );
                }

                throw error;
            }


            /*
             * Тук прихващаме RESPONSE BODY
             * само на Encar search API.
             */
            try {

                if (
                    url &&
                    url.indexOf(TARGET_API) !== -1
                ) {

                    const clone = response.clone();

                    const contentType =
                        response.headers
                            ? (
                                response.headers.get(
                                    "content-type"
                                ) || ""
                              )
                            : "";

                    clone.text()
                        .then(function(bodyText) {

                            reportResponse(
                                "FETCH",
                                method,
                                url,
                                response.status,
                                contentType,
                                bodyText
                            );

                        })
                        .catch(function(error) {

                            reportResponse(
                                "FETCH",
                                method,
                                url,
                                response.status,
                                contentType,
                                "RESPONSE READ ERROR: "
                                    + String(error)
                            );

                        });
                }

            } catch (e) {

                reportResponse(
                    "FETCH",
                    method,
                    url,
                    response ? response.status : 0,
                    "",
                    "RESPONSE CLONE ERROR: "
                        + String(e)
                );
            }

            return response;
        };
    }


    /*
     * ==========================
     * XMLHttpRequest INTERCEPTOR
     * ==========================
     */

    if (window.XMLHttpRequest) {

        const originalOpen =
            XMLHttpRequest.prototype.open;

        const originalSend =
            XMLHttpRequest.prototype.send;


        XMLHttpRequest.prototype.open =
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


        XMLHttpRequest.prototype.send =
            function(body) {

                const xhr = this;

                const method =
                    xhr.__scannerMethod || "GET";

                const url =
                    xhr.__scannerUrl || "";

                reportRequest(
                    "XHR",
                    method,
                    url,
                    body
                );


                if (
                    url &&
                    url.indexOf(TARGET_API) !== -1
                ) {

                    xhr.addEventListener(
                        "loadend",
                        function() {

                            let responseBody = "";

                            let contentType = "";

                            try {

                                contentType =
                                    xhr.getResponseHeader(
                                        "content-type"
                                    ) || "";

                            } catch (e) {
                            }


                            try {

                                if (
                                    xhr.responseType === ""
                                    ||
                                    xhr.responseType === "text"
                                ) {

                                    responseBody =
                                        xhr.responseText || "";

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
                                    "XHR RESPONSE READ ERROR: "
                                    + String(e);
                            }


                            reportResponse(
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
        "ENCAR Network Scanner interceptor installed"
    );

})();
""".trimIndent()

        webView.evaluateJavascript(
            javascript,
            null
        )
    }

    inner class NetworkBridge {

        @JavascriptInterface
        fun onRequest(
            source: String,
            method: String,
            url: String,
            body: String
        ) {

            if (!scanning) {
                return
            }

            val cleanUrl = url.trim()

            if (cleanUrl.isBlank()) {
                return
            }

            val block = StringBuilder()

            block.append("------------------------------\n")
            block.append("SOURCE: ")
            block.append(source)
            block.append("\n")

            block.append("METHOD: ")
            block.append(method)
            block.append("\n")

            block.append("URL:\n")
            block.append(cleanUrl)
            block.append("\n")

            if (body.isNotBlank()) {

                block.append("BODY:\n")
                block.append(body)
                block.append("\n")
            }

            appendLog(
                block.toString()
            )
        }


        /*
         * ТОВА Е НОВАТА ВАЖНА ЧАСТ.
         *
         * Тук идва JSON response-а от:
         *
         * api.encar.com/search/car/list/mobile
         */
        @JavascriptInterface
        fun onResponse(
            source: String,
            method: String,
            url: String,
            status: Int,
            contentType: String,
            body: String
        ) {

            if (!scanning) {
                return
            }

            if (
                !url.contains(
                    targetSearchApi,
                    ignoreCase = true
                )
            ) {
                return
            }

            val block = StringBuilder()

            block.append("\n")
            block.append("====================================\n")
            block.append("===== ENCAR SEARCH API RESPONSE =====\n")
            block.append("====================================\n")

            block.append("SOURCE: ")
            block.append(source)
            block.append("\n")

            block.append("METHOD: ")
            block.append(method)
            block.append("\n")

            block.append("STATUS: ")
            block.append(status)
            block.append("\n")

            block.append("CONTENT-TYPE: ")
            block.append(contentType)
            block.append("\n")

            block.append("URL:\n")
            block.append(url)
            block.append("\n")

            block.append("\nRESPONSE BODY:\n")
            block.append(body)
            block.append("\n")

            block.append(
                "===== END ENCAR SEARCH API RESPONSE =====\n"
            )

            block.append(
                "=========================================\n"
            )

            appendLog(
                block.toString()
            )

            runOnUiThread {

                statusText.text =
                    "✅ ENCAR JSON RESPONSE CAPTURED"

                Toast.makeText(
                    this@MainActivity,
                    "ENCAR response captured!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }

    override fun onDestroy() {

        try {
            webView.removeJavascriptInterface(
                "AndroidNetworkScanner"
            )

            webView.destroy()

        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
