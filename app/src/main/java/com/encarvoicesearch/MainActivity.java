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

        // =========================
        // ГОРЕН ПАНЕЛ
        // =========================

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(20, 12, 20, 12);
        panel.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("ENCAR VOICE SEARCH");
        title.setTextSize(18);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        panel.addView(title);

        brandValue = makeText("Марка: -");
        modelValue = makeText("Модел: -");
        yearValue = makeText("Година: -");

        panel.addView(brandValue);
        panel.addView(modelValue);
        panel.addView(yearValue);

        statusValue = new TextView(this);
        statusValue.setText("Кажи например: Kia Sorento 2025");
        statusValue.setTextSize(13);
        statusValue.setTextColor(Color.DKGRAY);
        statusValue.setPadding(0, 8, 0, 8);

        panel.addView(statusValue);

        // =========================
        // БУТОНИ
        // =========================

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

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

        buttons.addView(voiceButton, buttonParams);
        buttons.addView(diagnosticButton, buttonParams);

        panel.addView(buttons);

        root.addView(
                panel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // =========================
        // WEBVIEW
        // =========================

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

        webView.addJavascriptInterface(
                new EncarBridge(),
                "AndroidBridge"
        );

        webView.setWebViewClient(new WebViewClient());

        String encarUrl =
                "https://m.encar.com/ca/search.do#!%7B%22type%22%3A%22car%22%2C%22action%22%3A%22(And.Hidden.N._.MultiViewHidden.N._.(Or.Separation.F._.Separation.B.)_.SellType.%EC%9D%BC%EB%B0%98._.CarType.A._.Mileage.range(..400000)._.Price.range(100..10000).)%22%2C%22toggle%22%3A%7B%7D%2C%22layer%22%3A%22%22%2C%22title%22%3A%22Mercedes-Benz%20GLE-Class%20W167(19%EB%85%84~%ED%98%84%EC%9E%AC)%22%2C%22sort%22%3A%22MobilePriceAsc%22%2C%22cursor%22%3A%22%22%7D";

        webView.loadUrl(encarUrl);

        // =========================
        // ГЛАС
        // =========================

        voiceButton.setOnClickListener(v ->
                startVoiceRecognition()
        );

        // =========================
        // ДИАГНОСТИКА
        // =========================

        diagnosticButton.setOnClickListener(v -> {

            statusValue.setText(
                    "Чета структурата на Encar..."
            );

            runDiagnostics();
        });
    }

    private TextView makeText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(Color.BLACK);
        view.setPadding(0, 3, 0, 3);

        return view;
    }

    // =====================================================
    // ГЛАС
    // =====================================================

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

            if (results != null
                    && !results.isEmpty()) {

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

        Matcher matcher =
                Pattern.compile(
                        "\\b(19\\d{2}|20\\d{2})\\b"
                ).matcher(text);

        String yearToken = "";

        if (matcher.find()) {

            selectedYear = matcher.group(1);
            yearToken = matcher.group();

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

            StringBuilder model =
                    new StringBuilder();

            for (int i = 1; i < words.length; i++) {

                if (i > 1) {
                    model.append(" ");
                }

                model.append(words[i]);
            }

            selectedModel =
                    model.toString().trim();
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

    // =====================================================
    // DOM ДИАГНОСТИКА
    // =====================================================

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
                " return r.width>0 && r.height>0" +
                " && s.display!=='none'" +
                " && s.visibility!=='hidden';" +
                "}" +

                "function describe(el) {" +

                " if (!el) return 'NULL';" +

                " const r=el.getBoundingClientRect();" +

                " const text=" +
                "  (el.innerText || el.textContent || '')" +
                "  .trim()" +
                "  .replace(/\\s+/g,' ')" +
                "  .slice(0,140);" +

                " const cls=" +
                "  (typeof el.className==='string'" +
                "   ? el.className : '')" +
                "  .slice(0,180);" +

                " return '<'+el.tagName.toLowerCase()" +
                "  +' id=\"'+(el.id||'')+'\"'" +
                "  +' class=\"'+cls+'\"'" +
                "  +' role=\"'+(el.getAttribute('role')||'')+'\"'" +
                "  +' text=\"'+text+'\"'" +
                "  +' rect='" +
                "  +Math.round(r.left)+','" +
                "  +Math.round(r.top)+','" +
                "  +Math.round(r.width)+','" +
                "  +Math.round(r.height)+'>';" +
                "}" +

                "AndroidBridge.clear();" +

                "send('ENCAR DOM DIAGNOSTICS');" +
                "send('URL: '+location.href);" +
                "send('TITLE: '+document.title);" +
                "send('READY: '+document.readyState);" +

                // IFRAМЕ проверка
                "const frames=[...document.querySelectorAll('iframe')];" +
                "send('IFRAMES: '+frames.length);" +

                "frames.slice(0,10).forEach((f,i) => {" +
                " send('IFRAME '+i+': '+(f.src||''));" +
                "});" +

                // SELECT полета
                "const selects=[...document.querySelectorAll('select')];" +
                "send('SELECTS: '+selects.length);" +

                "selects.slice(0,20).forEach((s,i) => {" +

                " const opts=[...s.options]" +
                "  .slice(0,8)" +
                "  .map(o =>" +
                "   (o.textContent||'').trim()" +
                "  ).join(' | ');" +

                " send(" +
                "  'SELECT '+i+': '" +
                "  +describe(s)" +
                "  +' OPTIONS=['+opts+']'" +
                " );" +
                "});" +

                // Диагностика за ключовите полета
                "const labels=[" +
                " 'Manufacturer'," +
                " 'Model'," +
                " 'Year'," +
                " 'Mileage'," +
                " 'Price'" +
                "];" +

                "labels.forEach(label => {" +

                " send('');" +
                " send('===== '+label+' =====');" +

                " let matches=[..." +
                " document.querySelectorAll('body *')" +
                "].filter(el => {" +

                " if (!visible(el)) return false;" +

                " const t=" +
                "  (el.innerText || el.textContent || '')" +
                "  .trim()" +
                "  .replace(/\\s+/g,' ');" +

                " return t===label" +
                "  || t.startsWith(label+' ')" +
                "  || t.includes(label);" +
                "});" +

                // По-малките елементи първи
                "matches.sort((a,b) => {" +

                " const ta=" +
                "  (a.innerText||a.textContent||'').length;" +

                " const tb=" +
                "  (b.innerText||b.textContent||'').length;" +

                " return ta-tb;" +
                "});" +

                "send('MATCHES: '+matches.length);" +

                "matches.slice(0,6).forEach((el,i) => {" +

                " send('MATCH '+i+': '+describe(el));" +

                " let p=el.parentElement;" +

                " for (let level=1;" +
                "      level<=4 && p;" +
                "      level++,p=p.parentElement) {" +

                "  send(" +
                "   '  PARENT '+level+': '" +
                "   +describe(p)" +
                "  );" +
                " }" +
                "});" +
                "});" +

                // Кликваеми елементи
                "send('');" +
                "send('===== CLICKABLE ELEMENTS =====');" +

                "let clickable=[..." +
                " document.querySelectorAll(" +
                "  'button,a,[role=\"button\"],input,label'" +
                " )" +
                "].filter(visible);" +

                "send('CLICKABLE COUNT: '+clickable.length);" +

                "clickable.slice(0,80).forEach((el,i) => {" +

                " const text=" +
                "  (el.innerText || el.value || el.textContent || '')" +
                "  .trim()" +
                "  .replace(/\\s+/g,' ');" +

                " if (" +
                "  text.includes('Manufacturer')" +
                "  || text.includes('Model')" +
                "  || text.includes('Year')" +
                "  || text.includes('Search')" +
                " ) {" +

                "  send(" +
                "   'CLICK '+i+': '+describe(el)" +
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

    // =====================================================
    // JAVASCRIPT BRIDGE
    // =====================================================

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

                if (diagnosticText.length() < 30000) {

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
                        "Диагностиката е готова."
                );

                showDiagnosticDialog();
            });
        }
    }

    // =====================================================
    // ПОКАЗВАНЕ НА РЕЗУЛТАТА
    // =====================================================

    private void showDiagnosticDialog() {

        String result;

        synchronized (diagnosticText) {
            result = diagnosticText.toString();
        }

        TextView textView = new TextView(this);

        textView.setText(result);
        textView.setTextSize(12);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(24, 20, 24, 20);
        textView.setTextIsSelectable(true);
        textView.setMovementMethod(
                new ScrollingMovementMethod()
        );

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.addView(textView);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("ENCAR диагностика")
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
                                                    "Encar diagnostics",
                                                    result
                                            );

                                    clipboard.setPrimaryClip(clip);

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

    // =====================================================
    // BACK
    // =====================================================

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
