package com.encarvoicesearch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST_CODE = 1001;

    private WebView webView;

    private TextView brandValue;
    private TextView modelValue;
    private TextView yearValue;
    private TextView statusValue;

    private String selectedBrand = "";
    private String selectedModel = "";
    private String selectedYear = "";

    private final StringBuilder diagnosticText = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // =========================================
        // ГОРЕН ПАНЕЛ
        // =========================================

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(20, 12, 20, 12);
        panel.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("ENCAR VOICE SEARCH");
        title.setTextSize(18);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);

        panel.addView(title);

        brandValue = createText("Марка: -");
        modelValue = createText("Модел: -");
        yearValue = createText("Година: -");

        panel.addView(brandValue);
        panel.addView(modelValue);
        panel.addView(yearValue);

        statusValue = new TextView(this);
        statusValue.setText("Отвори Manufacturer и натисни ДИАГНОСТИКА");
        statusValue.setTextSize(13);
        statusValue.setTextColor(Color.DKGRAY);
        statusValue.setPadding(0, 8, 0, 8);

        panel.addView(statusValue);

        // =========================================
        // БУТОНИ
        // =========================================

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        Button voiceButton = new Button(this);
        voiceButton.setText("ГЛАС");

        Button diagnosticButton = new Button(this);
        diagnosticButton.setText("ДИАГНОСТИКА");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        buttonParams.setMargins(5, 0, 5, 0);

        buttonRow.addView(voiceButton, buttonParams);
        buttonRow.addView(diagnosticButton, buttonParams);

        panel.addView(buttonRow);

        root.addView(
                panel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // =========================================
        // WEBVIEW
        // =========================================

        webView = new WebView(this);

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
                new EncarBridge(),
                "AndroidBridge"
        );

        // Директно ENCAR - без Google
        String encarUrl =
                "https://m.encar.com/ca/search.do#!%7B%22type%22%3A%22car%22%2C%22action%22%3A%22(And.Hidden.N._.MultiViewHidden.N._.(Or.Separation.F._.Separation.B.)_.SellType.%EC%9D%BC%EB%B0%98._.CarType.A._.Mileage.range(..400000)._.Price.range(100..10000).)%22%2C%22toggle%22%3A%7B%7D%2C%22layer%22%3A%22%22%2C%22title%22%3A%22Mercedes-Benz%20GLE-Class%20W167(19%EB%85%84~%ED%98%84%EC%9E%AC)%22%2C%22sort%22%3A%22ModifiedDate%22%2C%22cursor%22%3A%22%22%7D";

        webView.loadUrl(encarUrl);

        // =========================================
        // ГЛАС
        // =========================================

        voiceButton.setOnClickListener(v ->
                startVoiceRecognition()
        );

        // =========================================
        // ДИАГНОСТИКА
        // =========================================

        diagnosticButton.setOnClickListener(v -> {

            statusValue.setText(
                    "Чета Manufacturer структурата..."
            );

            runDiagnostics();
        });
    }

    private TextView createText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(Color.BLACK);
        view.setPadding(0, 3, 0, 3);

        return view;
    }

    // =========================================
    // ГЛАСОВО РАЗПОЗНАВАНЕ
    // =========================================

    private void startVoiceRecognition() {

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "bg-BG"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Кажи марка, модел и година"
        );

        try {

            startActivityForResult(
                    intent,
                    VOICE_REQUEST_CODE
            );

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "Няма налично гласово разпознаване.",
                    Toast.LENGTH_LONG
            ).show();
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

        if (requestCode == VOICE_REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null && !results.isEmpty()) {

                String spokenText = results.get(0);

                statusValue.setText(
                        "Разпознато: " + spokenText
                );

                parseVoiceCommand(spokenText);
            }
        }
    }

    private void parseVoiceCommand(String spokenText) {

        selectedBrand = "";
        selectedModel = "";
        selectedYear = "";

        String text = spokenText.trim();

        String yearToken = "";

        Matcher fullYear =
                Pattern.compile(
                        "\\b(19\\d{2}|20\\d{2})\\b"
                ).matcher(text);

        if (fullYear.find()) {

            selectedYear = fullYear.group(1);
            yearToken = fullYear.group();

        } else {

            Matcher shortYear =
                    Pattern.compile(
                            "\\b(\\d{2})(?:-?та)?\\b"
                    ).matcher(text);

            while (shortYear.find()) {

                int value =
                        Integer.parseInt(
                                shortYear.group(1)
                        );

                if (value >= 15 && value <= 40) {

                    selectedYear =
                            String.valueOf(
                                    2000 + value
                            );

                    yearToken =
                            shortYear.group();

                    break;
                }
            }
        }

        if (selectedYear.isEmpty()) {

            Toast.makeText(
                    this,
                    "Не разпознах година.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String carPart =
                text.replace(yearToken, "")
                        .replace("година", "")
                        .replace("Година", "")
                        .trim();

        String lower =
                carPart.toLowerCase();

        if (lower.startsWith("mercedes benz ")) {

            selectedBrand = "Mercedes-Benz";

            selectedModel =
                    carPart.substring(
                            "mercedes benz".length()
                    ).trim();

        } else if (lower.startsWith("land rover ")) {

            selectedBrand = "Land Rover";

            selectedModel =
                    carPart.substring(
                            "land rover".length()
                    ).trim();

        } else {

            String[] words =
                    carPart.split("\\s+");

            if (words.length < 2) {

                Toast.makeText(
                        this,
                        "Кажи марка, модел и година.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            selectedBrand = words[0];

            StringBuilder modelBuilder =
                    new StringBuilder();

            for (int i = 1; i < words.length; i++) {

                if (i > 1) {
                    modelBuilder.append(" ");
                }

                modelBuilder.append(words[i]);
            }

            selectedModel =
                    modelBuilder.toString().trim();
        }

        brandValue.setText(
                "Марка: " + selectedBrand
        );

        modelValue.setText(
                "Модел: " + selectedModel
        );

        yearValue.setText(
                "Година: "
                        + selectedYear
                        + " (01/"
                        + selectedYear
                        + " - 12/"
                        + selectedYear
                        + ")"
        );
    }

    // =========================================
    // MANUFACTURER ДИАГНОСТИКА
    // =========================================

    private void runDiagnostics() {

        String script =

                "(function() {" +

                "function send(s) {" +
                " try {" +
                "  AndroidBridge.report(String(s));" +
                " } catch(e) {}" +
                "}" +

                "function visible(el) {" +
                " if (!el) return false;" +
                " const r=el.getBoundingClientRect();" +
                " const s=getComputedStyle(el);" +
                " return r.width>0" +
                " && r.height>0" +
                " && s.display!=='none'" +
                " && s.visibility!=='hidden';" +
                "}" +

                "function describe(el) {" +

                " if (!el) return 'NULL';" +

                " const text=" +
                " (el.innerText || el.textContent || '')" +
                " .trim()" +
                " .replace(/\\s+/g,' ')" +
                " .slice(0,160);" +

                " const cls=" +
                " (typeof el.className==='string'" +
                " ? el.className : '')" +
                " .slice(0,160);" +

                " const r=el.getBoundingClientRect();" +

                " return '<'+el.tagName.toLowerCase()" +
                " +' id=\"'+(el.id||'')+'\"'" +
                " +' class=\"'+cls+'\"'" +
                " +' href=\"'+(el.getAttribute('href')||'')+'\"'" +
                " +' onclick=\"'+(el.getAttribute('onclick')||'')+'\"'" +
                " +' data-value=\"'+(el.getAttribute('data-value')||'')+'\"'" +
                " +' data-code=\"'+(el.getAttribute('data-code')||'')+'\"'" +
                " +' data-manufact=\"'+(el.getAttribute('data-manufact')||'')+'\"'" +
                " +' text=\"'+text+'\"'" +
                " +' rect='" +
                " +Math.round(r.left)+','" +
                " +Math.round(r.top)+','" +
                " +Math.round(r.width)+','" +
                " +Math.round(r.height)+'>';" +
                "}" +

                "AndroidBridge.clear();" +

                "send('ENCAR MANUFACTURER DIAGNOSTICS');" +
                "send('URL: '+location.href);" +
                "send('READY: '+document.readyState);" +
                "send('');" +

                "const layer=" +
                " document.querySelector('#optManufact');" +

                "if (!layer) {" +

                " send('ERROR: #optManufact NOT FOUND');" +
                " send('Отвори Manufacturer ръчно и пусни диагностиката отново.');" +

                " AndroidBridge.done();" +
                " return;" +
                "}" +

                "send('FOUND: #optManufact');" +
                "send('LAYER: '+describe(layer));" +
                "send('');" +

                // =====================================
                // ВСИЧКИ CLICKABLE ЕЛЕМЕНТИ
                // =====================================

                "send('===== MANUFACTURER CLICKABLE ELEMENTS =====');" +

                "const elements=[..." +
                " layer.querySelectorAll(" +
                " 'a,button,li,label,[role=\"button\"],input'" +
                ")" +
                "].filter(visible);" +

                "send('ELEMENT COUNT: '+elements.length);" +

                "elements.forEach((el,i) => {" +

                " const text=" +
                " (el.innerText || el.textContent || '')" +
                " .trim()" +
                " .replace(/\\s+/g,' ');" +

                " if (text) {" +
                "   send(i+': '+describe(el));" +
                " }" +
                "});" +

                // =====================================
                // KIA
                // =====================================

                "send('');" +
                "send('===== KIA EXACT =====');" +

                "const all=[...layer.querySelectorAll('*')];" +

                "const kia=all.filter(el => {" +

                " const t=" +
                " (el.innerText || el.textContent || '')" +
                " .trim()" +
                " .replace(/\\s+/g,' ');" +

                " return t==='Kia'" +
                " || t.startsWith('Kia ');" +
                "});" +

                "send('KIA MATCHES: '+kia.length);" +

                "kia.slice(0,15).forEach((el,i) => {" +

                " send('KIA '+i+': '+describe(el));" +

                " let p=el.parentElement;" +

                " for (" +
                " let level=1;" +
                " level<=5 && p;" +
                " level++,p=p.parentElement" +
                " ) {" +

                "  send(" +
                "   '  PARENT '+level+': '+describe(p)" +
                "  );" +
                " }" +
                "});" +

                // =====================================
                // GENESIS
                // =====================================

                "send('');" +
                "send('===== GENESIS EXACT =====');" +

                "const genesis=all.filter(el => {" +

                " const t=" +
                " (el.innerText || el.textContent || '')" +
                " .trim()" +
                " .replace(/\\s+/g,' ');" +

                " return t==='GENESIS'" +
                " || t.startsWith('GENESIS ');" +
                "});" +

                "send('GENESIS MATCHES: '+genesis.length);" +

                "genesis.slice(0,10).forEach((el,i) => {" +

                " send('GENESIS '+i+': '+describe(el));" +

                " let p=el.parentElement;" +

                " for (" +
                " let level=1;" +
                " level<=4 && p;" +
                " level++,p=p.parentElement" +
                " ) {" +

                "  send(" +
                "   '  PARENT '+level+': '+describe(p)" +
                "  );" +
                " }" +
                "});" +

                "send('');" +
                "send('=== END ===');" +

                "AndroidBridge.done();" +

                "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    // =========================================
    // JAVASCRIPT BRIDGE
    // =========================================

    public class EncarBridge {

        @JavascriptInterface
        public void clear() {

            synchronized (diagnosticText) {

                diagnosticText.setLength(0);
            }
        }

        @JavascriptInterface
        public void report(String message) {

            synchronized (diagnosticText) {

                if (diagnosticText.length() < 50000) {

                    diagnosticText
                            .append(message)
                            .append("\n");
                }
            }
        }

        @JavascriptInterface
        public void done() {

            runOnUiThread(() -> {

                statusValue.setText(
                        "Manufacturer диагностиката е готова."
                );

                showDiagnosticDialog();
            });
        }
    }

    // =========================================
    // ПОКАЗВАНЕ НА ДИАГНОСТИКАТА
    // =========================================

    private void showDiagnosticDialog() {

        final String result;

        synchronized (diagnosticText) {

            result =
                    diagnosticText.toString();
        }

        TextView output = new TextView(this);

        output.setText(result);
        output.setTextSize(12);
        output.setTextColor(Color.BLACK);
        output.setPadding(24, 20, 24, 20);
        output.setTextIsSelectable(true);

        output.setMovementMethod(
                new ScrollingMovementMethod()
        );

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.addView(output);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Manufacturer диагностика"
                        )
                        .setView(scrollView)

                        .setPositiveButton(
                                "КОПИРАЙ",
                                (d, which) -> {

                                    ClipboardManager clipboard =
                                            (ClipboardManager)
                                                    getSystemService(
                                                            Context.CLIPBOARD_SERVICE
                                                    );

                                    ClipData clip =
                                            ClipData.newPlainText(
                                                    "Encar manufacturer diagnostics",
                                                    result
                                            );

                                    clipboard.setPrimaryClip(
                                            clip
                                    );

                                    Toast.makeText(
                                            this,
                                            "Диагностиката е копирана.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        )

                        .setNegativeButton(
                                "ЗАТВОРИ",
                                null
                        )

                        .create();

        dialog.show();
    }

    // =========================================
    // BACK
    // =========================================

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
