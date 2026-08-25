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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView statusText;

    private final StringBuilder scanLog = new StringBuilder();

    private volatile boolean scanning = true;

    private static final String START_URL =
            "https://m.encar.com/ca/search.do";

    private static final String TARGET_SEARCH_API =
            "api.encar.com/search/car/list/mobile";

    private final Object responseLock = new Object();

    private final Map<String, ResponseCapture> responses =
            new HashMap<>();


    private static class ResponseCapture {

        String source;
        String method;
        String url;
        int status;
        String contentType;

        StringBuilder body =
                new StringBuilder();
    }


    @SuppressLint({
            "SetJavaScriptEnabled",
            "JavascriptInterface"
    })
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        createUi();

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        settings.setLoadsImagesAutomatically(true);

        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        cookieManager.setAcceptThirdPartyCookies(
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
                                url != null
                        ) {

                            appendLog(
                                    "\n------------------------------\n" +
                                    "PAGE STARTED\n" +
                                    url + "\n"
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


                        webView.postDelayed(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        injectNetworkInterceptor();
                                    }
                                },
                                1000
                        );


                        statusText.setText(
                                "SCANNER ACTIVE - промени филтър в Encar"
                        );
                    }


                    @Override
                    public WebResourceResponse shouldInterceptRequest(
                            WebView view,
                            WebResourceRequest request
                    ) {

                        if (
                                scanning &&
                                request != null &&
                                request.getUrl() != null
                        ) {

                            String url =
                                    request
                                            .getUrl()
                                            .toString();

                            if (
                                    url.contains(
                                            TARGET_SEARCH_API
                                    )
                            ) {

                                appendLog(
                                        "\n==============================\n" +
                                        "WEBVIEW REQUEST\n" +
                                        "METHOD: " +
                                        request.getMethod() +
                                        "\nURL:\n" +
                                        url +
                                        "\n==============================\n"
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

        startButton.setText("START");


        Button stopButton =
                new Button(this);

        stopButton.setText("STOP");


        Button copyButton =
                new Button(this);

        copyButton.setText("COPY");


        Button clearButton =
                new Button(this);

        clearButton.setText("CLEAR");


        startButton.setOnClickListener(
                v -> {

                    startNewScan();

                    Toast.makeText(
                            this,
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
                            "Log cleared"
                    );

                    Toast.makeText(
                            this,
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
                buttonParams
        );

        controls.addView(
                copyButton,
                buttonParams
        );

        controls.addView(
                clearButton,
                buttonParams
        );


        statusText =
                new TextView(this);

        statusText.setText(
                "Scanner loading..."
        );

        statusText.setTextSize(14f);

        statusText.setPadding(
                16,
                10,
                16,
                10
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


        setContentView(root);
    }


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


        synchronized (responseLock) {

            responses.clear();
        }


        statusText.setText(
                "SCANNING..."
        );
    }


    private void stopScan() {

        if (!scanning) {
            return;
        }


        appendLog(
                "\n==============================\n" +
                "SCAN STOPPED\n" +
                currentTime() +
                "\n==============================\n"
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


    private void copyScan() {

        String text;

        synchronized (scanLog) {

            text =
                    scanLog.toString();
        }


        if (text.trim().isEmpty()) {

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


        clipboard.setPrimaryClip(
                ClipData.newPlainText(
                        "ENCAR NETWORK SCAN",
                        text
                )
        );


        Toast.makeText(
                this,
                "Scan copied",
                Toast.LENGTH_SHORT
        ).show();
    }


    private void appendLog(
            String text
    ) {

        if (!scanning) {
            return;
        }


        synchronized (scanLog) {

            scanLog.append(text);

            if (!text.endsWith("\n")) {

                scanLog.append("\n");
            }
        }
    }


    private String currentTime() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }


    private void injectNetworkInterceptor() {

        String js =

                "(function() {" +

                "if(window.__ENCAR_RESPONSE_SCANNER__===true){" +
                "return;" +
                "}" +

                "window.__ENCAR_RESPONSE_SCANNER__=true;" +


                "var TARGET='api.encar.com/search/car/list/mobile';" +

                "var responseCounter=0;" +


                "function absUrl(u){" +

                "try{" +

                "return new URL(String(u||''),window.location.href).href;" +

                "}catch(e){" +

                "return String(u||'');" +

                "}" +

                "}" +


                "function isTarget(u){" +

                "var x=absUrl(u);" +

                "return x.indexOf(TARGET)!==-1;" +

                "}" +


                "function safeString(v){" +

                "if(v===undefined||v===null){" +
                "return '';" +
                "}" +

                "try{" +

                "if(typeof v==='string'){" +
                "return v;" +
                "}" +

                "return JSON.stringify(v);" +

                "}catch(e){" +

                "try{" +
                "return String(v);" +
                "}catch(e2){" +
                "return '';" +
                "}" +

                "}" +

                "}" +


                "function reportRequest(source,method,url,body){" +

                "url=absUrl(url);" +

                "if(!isTarget(url)){" +
                "return;" +
                "}" +

                "try{" +

                "window.AndroidNetworkScanner.onRequest(" +

                "String(source||'')," +

                "String(method||'GET')," +

                "String(url||'')," +

                "safeString(body)" +

                ");" +

                "}catch(e){}" +

                "}" +


                "function reportResponse(" +
                "source," +
                "method," +
                "url," +
                "status," +
                "contentType," +
                "body" +
                "){" +

                "url=absUrl(url);" +

                "if(!isTarget(url)){" +
                "return;" +
                "}" +

                "body=String(body||'');" +

                "responseCounter++;" +

                "var id='R'+Date.now()+'_'+responseCounter;" +

                "try{" +

                "window.AndroidNetworkScanner.onResponseStart(" +

                "id," +

                "String(source||'')," +

                "String(method||'GET')," +

                "String(url||'')," +

                "Number(status||0)," +

                "String(contentType||'')," +

                "body.length" +

                ");" +


                "var chunkSize=30000;" +

                "for(" +
                "var i=0;" +
                "i<body.length;" +
                "i+=chunkSize" +
                "){" +

                "window.AndroidNetworkScanner.onResponseChunk(" +

                "id," +

                "body.substring(" +
                "i," +
                "Math.min(i+chunkSize,body.length)" +
                ")" +

                ");" +

                "}" +


                "window.AndroidNetworkScanner.onResponseEnd(id);" +

                "}catch(e){}" +

                "}" +


                "if(window.fetch){" +

                "var originalFetch=window.fetch;" +


                "window.fetch=async function(input,init){" +

                "var url='';" +
                "var method='GET';" +
                "var requestBody='';" +


                "try{" +

                "if(typeof input==='string'){" +

                "url=input;" +

                "}else if(input&&input.url){" +

                "url=input.url;" +

                "}" +


                "if(init&&init.method){" +

                "method=init.method;" +

                "}else if(input&&input.method){" +

                "method=input.method;" +

                "}" +


                "if(init&&init.body!==undefined){" +

                "requestBody=init.body;" +

                "}" +

                "}catch(e){}" +


                "reportRequest(" +
                "'FETCH'," +
                "method," +
                "url," +
                "requestBody" +
                ");" +


                "var response;" +


                "try{" +

                "response=await originalFetch.apply(this,arguments);" +

                "}catch(error){" +

                "if(isTarget(url)){" +

                "reportResponse(" +
                "'FETCH'," +
                "method," +
                "url," +
                "0," +
                "''," +
                "'FETCH ERROR: '+String(error)" +
                ");" +

                "}" +

                "throw error;" +

                "}" +


                "try{" +

                "if(isTarget(url)){" +

                "var clone=response.clone();" +

                "var contentType='';" +


                "try{" +

                "contentType=" +
                "response.headers.get('content-type')||'';" +

                "}catch(e){}" +


                "clone.text()" +

                ".then(function(text){" +

                "reportResponse(" +
                "'FETCH'," +
                "method," +
                "url," +
                "response.status," +
                "contentType," +
                "text" +
                ");" +

                "})" +

                ".catch(function(error){" +

                "reportResponse(" +
                "'FETCH'," +
                "method," +
                "url," +
                "response.status," +
                "contentType," +
                "'READ ERROR: '+String(error)" +
                ");" +

                "});" +

                "}" +

                "}catch(e){" +

                "if(isTarget(url)){" +

                "reportResponse(" +
                "'FETCH'," +
                "method," +
                "url," +
                "response?response.status:0," +
                "''," +
                "'CLONE ERROR: '+String(e)" +
                ");" +

                "}" +

                "}" +


                "return response;" +

                "};" +

                "}" +


                "if(window.XMLHttpRequest){" +

                "var originalOpen=" +
                "XMLHttpRequest.prototype.open;" +

                "var originalSend=" +
                "XMLHttpRequest.prototype.send;" +


                "XMLHttpRequest.prototype.open=" +

                "function(method,url){" +

                "this.__encarMethod=method||'GET';" +

                "this.__encarUrl=url||'';" +

                "return originalOpen.apply(this,arguments);" +

                "};" +


                "XMLHttpRequest.prototype.send=" +

                "function(body){" +

                "var xhr=this;" +

                "var method=xhr.__encarMethod||'GET';" +

                "var url=xhr.__encarUrl||'';" +


                "reportRequest(" +
                "'XHR'," +
                "method," +
                "url," +
                "body" +
                ");" +


                "if(isTarget(url)){" +

                "xhr.addEventListener(" +
                "'loadend'," +

                "function(){" +

                "var responseBody='';" +

                "var contentType='';" +


                "try{" +

                "contentType=" +
                "xhr.getResponseHeader('content-type')||'';" +

                "}catch(e){}" +


                "try{" +

                "if(" +
                "xhr.responseType===''||" +
                "xhr.responseType==='text'" +
                "){" +

                "responseBody=xhr.responseText||'';" +

                "}else if(xhr.responseType==='json'){" +

                "responseBody=JSON.stringify(xhr.response);" +

                "}else{" +

                "responseBody=safeString(xhr.response);" +

                "}" +

                "}catch(e){" +

                "responseBody=" +
                "'XHR READ ERROR: '+String(e);" +

                "}" +


                "reportResponse(" +
                "'XHR'," +
                "method," +
                "url," +
                "xhr.status," +
                "contentType," +
                "responseBody" +
                ");" +

                "}" +

                ");" +

                "}" +


                "return originalSend.apply(this,arguments);" +

                "};" +

                "}" +


                "console.log('ENCAR RESPONSE SCANNER INSTALLED');" +

                "})();";


        webView.evaluateJavascript(
                js,
                null
        );
    }


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
                    !url.contains(
                            TARGET_SEARCH_API
                    )
            ) {

                return;
            }


            StringBuilder block =
                    new StringBuilder();


            block.append(
                    "\n========================================\n"
            );

            block.append(
                    "===== ENCAR SEARCH API REQUEST =====\n"
            );

            block.append(
                    "SOURCE: "
            );

            block.append(source);

            block.append(
                    "\nMETHOD: "
            );

            block.append(method);

            block.append(
                    "\nURL:\n"
            );

            block.append(url);

            block.append("\n");


            if (
                    body != null &&
                    !body.trim().isEmpty()
            ) {

                block.append(
                        "\nREQUEST BODY:\n"
                );

                block.append(body);

                block.append("\n");
            }


            block.append(
                    "===== END REQUEST =====\n"
            );

            block.append(
                    "========================================\n"
            );


            appendLog(
                    block.toString()
            );
        }


        @JavascriptInterface
        public void onResponseStart(
                String id,
                String source,
                String method,
                String url,
                int status,
                String contentType,
                int bodyLength
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


            ResponseCapture capture =
                    new ResponseCapture();


            capture.source =
                    source;

            capture.method =
                    method;

            capture.url =
                    url;

            capture.status =
                    status;

            capture.contentType =
                    contentType;


            synchronized (responseLock) {

                responses.put(
                        id,
                        capture
                );
            }
        }


        @JavascriptInterface
        public void onResponseChunk(
                String id,
                String chunk
        ) {

            if (!scanning) {
                return;
            }


            synchronized (responseLock) {

                ResponseCapture capture =
                        responses.get(id);

                if (capture == null) {
                    return;
                }


                if (chunk != null) {

                    capture.body.append(
                            chunk
                    );
                }
            }
        }


        @JavascriptInterface
        public void onResponseEnd(
                String id
        ) {

            if (!scanning) {
                return;
            }


            ResponseCapture capture;


            synchronized (responseLock) {

                capture =
                        responses.remove(id);
            }


            if (capture == null) {
                return;
            }


            StringBuilder block =
                    new StringBuilder();


            block.append(
                    "\n\n========================================\n"
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
                    capture.source
            );


            block.append(
                    "\nMETHOD: "
            );

            block.append(
                    capture.method
            );


            block.append(
                    "\nSTATUS: "
            );

            block.append(
                    capture.status
            );


            block.append(
                    "\nCONTENT-TYPE: "
            );

            block.append(
                    capture.contentType
            );


            block.append(
                    "\nURL:\n"
            );

            block.append(
                    capture.url
            );


            block.append(
                    "\n\nRESPONSE BODY:\n"
            );

            block.append(
                    capture.body
            );


            block.append(
                    "\n\n===== END ENCAR SEARCH API RESPONSE =====\n"
            );

            block.append(
                    "========================================\n"
            );


            appendLog(
                    block.toString()
            );


            runOnUiThread(
                    new Runnable() {

                        @Override
                        public void run() {

                            statusText.setText(
                                    "JSON CAPTURED - STOP + COPY"
                            );


                            Toast.makeText(
                                    MainActivity.this,
                                    "ENCAR JSON captured",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        }
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


    @Override
    protected void onDestroy() {

        if (webView != null) {

            try {

                webView.removeJavascriptInterface(
                        "AndroidNetworkScanner"
                );

                webView.stopLoading();

                webView.destroy();

            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}
