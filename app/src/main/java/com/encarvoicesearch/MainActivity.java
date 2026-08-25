package com.encarvoicesearch;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView statusText;

    private final StringBuilder fullReport =
            new StringBuilder();

    private String lastFingerprint = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button scanButton = new Button(this);
        scanButton.setText("SCAN");

        Button copyButton = new Button(this);
        copyButton.setText("COPY");

        Button clearButton = new Button(this);
        clearButton.setText("CLEAR");

        controls.addView(
                scanButton,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        controls.addView(
                copyButton,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        controls.addView(
                clearButton,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(controls);

        webView = new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.addJavascriptInterface(
                new DiagnosticBridge(),
                "AndroidDiagnostic"
        );

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {

                        super.onPageFinished(view, url);

                        installDiagnosticScript();

                        view.postDelayed(
                                () -> scanPage("PAGE_FINISHED"),
                                1500
                        );
                    }
                }
        );

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        7
                )
        );

        ScrollView scrollView =
                new ScrollView(this);

        statusText =
                new TextView(this);

        statusText.setTextSize(12);
        statusText.setPadding(
                20,
                20,
                20,
                20
        );

        statusText.setText(
                "ENCAR DIAGNOSTIC READY\n"
        );

        scrollView.addView(statusText);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        3
                )
        );

        setContentView(root);

        scanButton.setOnClickListener(
                v -> scanPage("MANUAL_SCAN")
        );

        copyButton.setOnClickListener(
                v -> copyReport()
        );

        clearButton.setOnClickListener(
                v -> {

                    fullReport.setLength(0);

                    lastFingerprint = "";

                    statusText.setText(
                            "REPORT CLEARED\n"
                    );
                }
        );

        webView.loadUrl(
                "https://m.encar.com/ca/search.do"
        );
    }

    private void installDiagnosticScript() {

        String script =
                "(function() {" +

                "if (window.__ENCAR_DIAGNOSTIC_INSTALLED__) return;" +
                "window.__ENCAR_DIAGNOSTIC_INSTALLED__ = true;" +

                "window.__encarVisible = function(el) {" +
                " try {" +
                "  var r = el.getBoundingClientRect();" +
                "  var s = window.getComputedStyle(el);" +
                "  return r.width > 0 && r.height > 0 &&" +
                "         s.display !== 'none' &&" +
                "         s.visibility !== 'hidden';" +
                " } catch(e) { return false; }" +
                "};" +

                "window.__encarText = function(el) {" +
                " try {" +
                "  return (" +
                "   el.innerText ||" +
                "   el.textContent ||" +
                "   el.value ||" +
                "   el.getAttribute('aria-label') ||" +
                "   el.getAttribute('title') ||" +
                "   ''" +
                "  ).replace(/\\s+/g,' ').trim();" +
                " } catch(e) { return ''; }" +
                "};" +

                "window.__encarScan = function(reason) {" +

                " try {" +

                " var out = [];" +

                " out.push('==============================');" +
                " out.push('REASON: ' + reason);" +
                " out.push('URL: ' + location.href);" +
                " out.push('TITLE: ' + document.title);" +
                " out.push('HASH: ' + location.hash);" +
                " out.push('IFRAMES: ' + document.querySelectorAll('iframe').length);" +
                " out.push('SELECTS: ' + document.querySelectorAll('select').length);" +

                " var dialogs = document.querySelectorAll(" +
                "  '[role=dialog], .layer, [id^=opt], [id*=Layer], [class*=layer]'" +
                " );" +

                " out.push('--- VISIBLE LAYERS ---');" +

                " var layerCount = 0;" +

                " dialogs.forEach(function(el) {" +

                "  if (!window.__encarVisible(el)) return;" +

                "  var text = window.__encarText(el);" +

                "  if (text.length > 250)" +
                "    text = text.substring(0,250);" +

                "  out.push(" +
                "   'LAYER #' + (el.id || '') +" +
                "   ' class=' + (el.className || '') +" +
                "   ' text=' + text" +
                "  );" +

                "  layerCount++;" +

                " });" +

                " out.push('VISIBLE_LAYER_COUNT: ' + layerCount);" +

                " out.push('--- INTERACTIVE ELEMENTS ---');" +

                " var nodes = document.querySelectorAll(" +
                "  'a,button,input,select,textarea,' +" +
                "  '[role=button],[role=option],[onclick]'" +
                " );" +

                " var count = 0;" +

                " for (var i=0; i<nodes.length && count<220; i++) {" +

                "  var el = nodes[i];" +

                "  if (!window.__encarVisible(el)) continue;" +

                "  var text = window.__encarText(el);" +

                "  if (text.length > 140)" +
                "    text = text.substring(0,140);" +

                "  var rect = el.getBoundingClientRect();" +

                "  out.push(" +
                "   '[' + count + '] '" +
                "   + el.tagName" +
                "   + ' id=' + (el.id || '')" +
                "   + ' class=' + (el.className || '')" +
                "   + ' role=' + (el.getAttribute('role') || '')" +
                "   + ' href=' + (el.getAttribute('href') || '')" +
                "   + ' text=' + text" +
                "   + ' x=' + Math.round(rect.left)" +
                "   + ' y=' + Math.round(rect.top)" +
                "  );" +

                "  count++;" +

                " }" +

                " out.push('INTERACTIVE_COUNT: ' + count);" +

                " out.push('--- VEHICLE / FILTER TEXT CANDIDATES ---');" +

                " var candidates = document.querySelectorAll(" +
                "  '.txt_item, .link_item, li, label, option'" +
                " );" +

                " var candidateCount = 0;" +

                " var seen = {};" +

                " for (" +
                "  var j=0;" +
                "  j<candidates.length && candidateCount<260;" +
                "  j++" +
                " ) {" +

                "  var c = candidates[j];" +

                "  if (!window.__encarVisible(c)) continue;" +

                "  var ct = window.__encarText(c);" +

                "  if (!ct || ct.length > 100) continue;" +

                "  var key =" +
                "   c.tagName + '|' +" +
                "   (c.id || '') + '|' +" +
                "   ct;" +

                "  if (seen[key]) continue;" +

                "  seen[key] = true;" +

                "  var parentId = '';" +
                "  var p = c;" +

                "  for (var z=0; z<5 && p; z++) {" +
                "   if (p.id) {" +
                "    parentId = p.id;" +
                "    break;" +
                "   }" +
                "   p = p.parentElement;" +
                "  }" +

                "  out.push(" +
                "   'CANDIDATE tag=' + c.tagName +" +
                "   ' id=' + (c.id || '') +" +
                "   ' class=' + (c.className || '') +" +
                "   ' parentId=' + parentId +" +
                "   ' text=' + ct" +
                "  );" +

                "  candidateCount++;" +

                " }" +

                " out.push('CANDIDATE_COUNT: ' + candidateCount);" +

                " var report = out.join('\\n');" +

                " var fingerprint =" +
                "  location.href +" +
                "  '|' + layerCount +" +
                "  '|' + count +" +
                "  '|' + candidateCount +" +
                "  '|' + document.body.innerText.length;" +

                " AndroidDiagnostic.onSnapshot(" +
                "  reason," +
                "  fingerprint," +
                "  report" +
                " );" +

                " } catch(e) {" +

                "  AndroidDiagnostic.onError(" +
                "   String(e)" +
                "  );" +

                " }" +

                "};" +

                "var timer = null;" +

                "var observer = new MutationObserver(function() {" +

                " clearTimeout(timer);" +

                " timer = setTimeout(function() {" +
                "  window.__encarScan('DOM_CHANGED');" +
                " }, 900);" +

                "});" +

                "observer.observe(" +
                " document.documentElement," +
                " {" +
                "  childList:true," +
                "  subtree:true," +
                "  attributes:true" +
                " }" +
                ");" +

                "setTimeout(function() {" +
                " window.__encarScan('INITIAL');" +
                "}, 1200);" +

                "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    private void scanPage(String reason) {

        webView.evaluateJavascript(
                "if(window.__encarScan){" +
                        "window.__encarScan('" +
                        reason +
                        "');" +
                        "}else{" +
                        "'NO_DIAGNOSTIC_SCRIPT';" +
                        "}",
                null
        );
    }

    private void copyReport() {

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );

        ClipData clip =
                ClipData.newPlainText(
                        "ENCAR Diagnostic Report",
                        fullReport.toString()
                );

        clipboard.setPrimaryClip(clip);

        Toast.makeText(
                this,
                "Отчетът е копиран",
                Toast.LENGTH_LONG
        ).show();
    }

    private class DiagnosticBridge {

        @JavascriptInterface
        public void onSnapshot(
                String reason,
                String fingerprint,
                String report) {

            runOnUiThread(() -> {

                if (fingerprint.equals(
                        lastFingerprint
                )) {
                    return;
                }

                lastFingerprint =
                        fingerprint;

                fullReport.append(
                        "\n\n"
                );

                fullReport.append(
                        report
                );

                String preview =
                        report;

                if (preview.length() > 6000) {

                    preview =
                            preview.substring(
                                    0,
                                    6000
                            );

                    preview +=
                            "\n\n... REPORT CONTINUES ...";
                }

                statusText.setText(
                        preview
                );
            });
        }

        @JavascriptInterface
        public void onError(
                String error) {

            runOnUiThread(() ->

                    statusText.setText(
                            "DIAGNOSTIC ERROR:\n" +
                                    error
                    )
            );
        }
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
