package com.encarvoicesearch;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // ==============================
        // ГОРЕН ПАНЕЛ
        // ==============================

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
        statusValue.setText("Кажи например: Kia Sorento 2025");
        statusValue.setTextSize(13);
        statusValue.setTextColor(Color.DKGRAY);
        statusValue.setPadding(0, 8, 0, 8);

        panel.addView(statusValue);

        // ==============================
        // БУТОНИ
        // ==============================

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        Button voiceButton = new Button(this);
        voiceButton.setText("ГЛАС");

        Button searchButton = new Button(this);
        searchButton.setText("ТЪРСИ");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        buttonParams.setMargins(5, 0, 5, 0);

        buttonRow.addView(voiceButton, buttonParams);
        buttonRow.addView(searchButton, buttonParams);

        panel.addView(buttonRow);

        root.addView(
                panel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ==============================
        // WEBVIEW
        // ==============================

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

        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        // Позволява JavaScript да показва статус в нашия панел
        webView.addJavascriptInterface(
                new EncarBridge(),
                "AndroidBridge"
        );

        // Директно ENCAR - никакъв Google
        String encarUrl =
                "https://m.encar.com/ca/search.do#!%7B%22type%22%3A%22car%22%2C%22action%22%3A%22(And.Hidden.N._.MultiViewHidden.N._.(Or.Separation.F._.Separation.B.)_.SellType.%EC%9D%BC%EB%B0%98._.CarType.A._.Mileage.range(..400000)._.Price.range(100..10000).)%22%2C%22toggle%22%3A%7B%7D%2C%22layer%22%3A%22%22%2C%22title%22%3A%22Mercedes-Benz%20GLE-Class%20W167(19%EB%85%84~%ED%98%84%EC%9E%AC)%22%2C%22sort%22%3A%22MobilePriceAsc%22%2C%22cursor%22%3A%22%22%7D";

        webView.loadUrl(encarUrl);

        // ==============================
        // ГЛАС
        // ==============================

        voiceButton.setOnClickListener(v ->
                startVoiceRecognition()
        );

        // ==============================
        // ТЪРСЕНЕ
        // ==============================

        searchButton.setOnClickListener(v -> {

            if (selectedBrand.isEmpty()
                    || selectedModel.isEmpty()
                    || selectedYear.isEmpty()) {

                Toast.makeText(
                        this,
                        "Първо кажи марка, модел и година.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            statusValue.setText(
                    "Стартирам търсене..."
            );

            automateEncarSearch();
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

    // =========================================================
    // ГЛАСОВО РАЗПОЗНАВАНЕ
    // =========================================================

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

                String spokenText =
                        results.get(0);

                statusValue.setText(
                        "Разпознато: " + spokenText
                );

                parseVoiceCommand(
                        spokenText
                );
            }
        }
    }

    // =========================================================
    // РАЗДЕЛЯНЕ НА МАРКА / МОДЕЛ / ГОДИНА
    // =========================================================

    private void parseVoiceCommand(String spokenText) {

        String text =
                spokenText.trim();

        String yearToken = "";

        // 2025
        Matcher fullYear =
                Pattern.compile(
                        "\\b(19\\d{2}|20\\d{2})\\b"
                ).matcher(text);

        if (fullYear.find()) {

            selectedYear =
                    fullYear.group(1);

            yearToken =
                    fullYear.group();

        } else {

            // Поддържаме и "25" или "25-та"
            Matcher shortYear =
                    Pattern.compile(
                            "\\b(\\d{2})(?:-?та)?\\b"
                    ).matcher(text);

            while (shortYear.find()) {

                int value =
                        Integer.parseInt(
                                shortYear.group(1)
                        );

                if (value >= 15
                        && value <= 40) {

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

        selectedBrand =
                words[0];

        StringBuilder model =
                new StringBuilder();

        for (int i = 1;
             i < words.length;
             i++) {

            if (i > 1) {
                model.append(" ");
            }

            model.append(
                    words[i]
            );
        }

        selectedModel =
                model.toString()
                        .trim();

        /*
         * Ако гласът каже:
         * Mercedes Benz GLE 2025
         */
        String lower =
                carPart.toLowerCase();

        if (lower.startsWith(
                "mercedes benz "
        )) {

            selectedBrand =
                    "Mercedes-Benz";

            selectedModel =
                    carPart.substring(
                            "mercedes benz".length()
                    ).trim();
        }

        if (lower.startsWith(
                "land rover "
        )) {

            selectedBrand =
                    "Land Rover";

            selectedModel =
                    carPart.substring(
                            "land rover".length()
                    ).trim();
        }

        brandValue.setText(
                "Марка: "
                        + selectedBrand
        );

        modelValue.setText(
                "Модел: "
                        + selectedModel
        );

        yearValue.setText(
                "Година: "
                        + selectedYear
                        + "  (01/"
                        + selectedYear
                        + " - 12/"
                        + selectedYear
                        + ")"
        );
    }

    // =========================================================
    // ENCAR АВТОМАТИЗАЦИЯ
    // =========================================================

    private void automateEncarSearch() {

        String brand =
                escapeJs(
                        selectedBrand
                );

        String model =
                escapeJs(
                        selectedModel
                );

        String year =
                escapeJs(
                        selectedYear
                );

        String script =

                "(async function() {" +

                "const BRAND='" + brand + "';" +
                "const MODEL='" + model + "';" +
                "const YEAR='" + year + "';" +

                "function report(msg) {" +
                " try {" +
                "  AndroidBridge.report(msg);" +
                " } catch(e) {}" +
                "}" +

                "function sleep(ms) {" +
                " return new Promise(r => setTimeout(r, ms));" +
                "}" +

                "function norm(s) {" +
                " return (s || '')" +
                "  .toLowerCase()" +
                "  .replace(/[^a-z0-9가-힣]/g,'');" +
                "}" +

                "function visible(el) {" +
                " if (!el) return false;" +
                " const r=el.getBoundingClientRect();" +
                " const s=getComputedStyle(el);" +
                " return r.width>0 && r.height>0" +
                "  && s.display!=='none'" +
                "  && s.visibility!=='hidden';" +
                "}" +

                /*
                 * Намира видим текст на страницата
                 * и натиска най-малкия подходящ елемент.
                 */
                "function clickText(values) {" +

                " const wanted=values.map(norm);" +

                " let nodes=[...document.querySelectorAll(" +
                " 'button,a,[role=\"button\"],li,label,div,span,p'" +
                " )].filter(visible);" +

                " nodes=nodes.filter(el => {" +
                "  const t=norm(el.innerText || el.textContent);" +

                "  if (!t) return false;" +

                "  return wanted.some(w =>" +
                "   t===w || t.includes(w)" +
                "  );" +
                " });" +

                " nodes.sort((a,b) =>" +
                "  (a.innerText || '').length -" +
                "  (b.innerText || '').length" +
                " );" +

                " if (!nodes.length)" +
                "  return false;" +

                " let target=nodes[0];" +

                " let clickable=target.closest(" +
                " 'button,a,[role=\"button\"],li,label'" +
                " );" +

                " if (clickable)" +
                "  target=clickable;" +

                " target.scrollIntoView({" +
                "  block:'center'" +
                " });" +

                " target.click();" +

                " return true;" +
                "}" +

                /*
                 * Настройване на истински HTML SELECT,
                 * ако Encar използва такъв.
                 */
                "function setSelectValue(select, wanted) {" +

                " const options=[...select.options];" +

                " let option=options.find(o =>" +
                "  norm(o.textContent)===norm(wanted)" +
                " );" +

                " if (!option) {" +
                "  option=options.find(o =>" +
                "   norm(o.textContent).includes(norm(wanted))" +
                "  );" +
                " }" +

                " if (!option)" +
                "  return false;" +

                " select.value=option.value;" +

                " select.dispatchEvent(" +
                "  new Event('input',{bubbles:true})" +
                " );" +

                " select.dispatchEvent(" +
                "  new Event('change',{bubbles:true})" +
                " );" +

                " return true;" +
                "}" +

                // =========================================
                // 1. MANUFACTURER
                // =========================================

                "report('Отварям Manufacturer...');" +

                "if (!clickText([" +
                " 'Manufacturer'," +
                " 'Maker'," +
                " '제조사'" +
                " ])) {" +

                " report('Не намерих Manufacturer');" +
                " return;" +
                "}" +

                "await sleep(900);" +

                "report('Избирам марка: '+BRAND);" +

                "if (!clickText([" +
                " BRAND," +
                " BRAND.replace('-', ' ')" +
                " ])) {" +

                " report('Не намерих марката: '+BRAND);" +
                " return;" +
                "}" +

                "await sleep(900);" +

                // =========================================
                // 2. MODEL
                // =========================================

                "report('Отварям Model...');" +

                "if (!clickText([" +
                " 'Model'," +
                " '모델'" +
                " ])) {" +

                " report('Не намерих Model');" +
                " return;" +
                "}" +

                "await sleep(900);" +

                "report('Избирам модел: '+MODEL);" +

                "if (!clickText([MODEL])) {" +

                " report('Не намерих модела: '+MODEL);" +
                " return;" +
                "}" +

                "await sleep(900);" +

                // =========================================
                // 3. YEAR
                // =========================================

                "report('Отварям Year...');" +

                "if (!clickText([" +
                " 'Year'," +
                " '연식'" +
                " ])) {" +

                " report('Не намерих Year');" +
                " return;" +
                "}" +

                "await sleep(900);" +

                /*
                 * Първо опитваме директно със SELECT полета.
                 */
                "let selects=[...document.querySelectorAll('select')]" +
                " .filter(visible);" +

                "let yearSelects=selects.filter(s =>" +
                " [...s.options].some(o =>" +
                "  norm(o.textContent).includes(norm(YEAR))" +
                " )" +
                ");" +

                "if (yearSelects.length >= 2) {" +

                " setSelectValue(yearSelects[0], YEAR);" +
                " setSelectValue(yearSelects[1], YEAR);" +

                " report('Годината е зададена: '+YEAR);" +

                "} else {" +

                /*
                 * Ако са custom менюта:
                 * Minimum year -> YEAR
                 */
                " report('Задавам начална година...');" +

                " clickText([" +
                "  'Minimum model year'," +
                "  'Minimum year'," +
                "  'Min year'," +
                "  'From'" +
                " ]);" +

                " await sleep(500);" +

                " clickText([YEAR]);" +

                " await sleep(500);" +

                /*
                 * Maximum year -> YEAR
                 */
                " report('Задавам крайна година...');" +

                " clickText([" +
                "  'Maximum model year'," +
                "  'Maximum year'," +
                "  'Max year'," +
                "  'To'" +
                " ]);" +

                " await sleep(500);" +

                " clickText([YEAR]);" +

                " await sleep(500);" +

                "}" +

                // =========================================
                // 4. MONTHS
                // =========================================

                "selects=[...document.querySelectorAll('select')]" +
                " .filter(visible);" +

                "let monthSelects=selects.filter(s => {" +

                " const opts=[...s.options]" +
                "  .map(o => norm(o.textContent));" +

                " return opts.some(x => x==='01' || x==='1')" +
                " && opts.some(x => x==='12');" +

                "});" +

                "if (monthSelects.length >= 2) {" +

                " setSelectValue(monthSelects[0], '01');" +
                " setSelectValue(monthSelects[1], '12');" +

                "} else {" +

                /*
                 * Fallback за custom month dropdowns
                 */
                " clickText([" +
                "  'January'," +
                "  '01'," +
                "  '1월'" +
                " ]);" +

                " await sleep(400);" +

                " clickText([" +
                "  'December'," +
                "  '12'," +
                "  '12월'" +
                " ]);" +
                "}" +

                "await sleep(700);" +

                // =========================================
                // 5. SEARCH
                // =========================================

                "report(" +
                " 'Търся '+BRAND+' '+MODEL+' '+YEAR+'...'" +
                ");" +

                "let searchButtons=[..." +
                " document.querySelectorAll(" +
                " 'button,[role=\"button\"],a'" +
                " )" +
                "].filter(visible)" +
                ".filter(el =>" +
                " norm(el.innerText || el.textContent)" +
                " .includes('search')" +
                ");" +

                "searchButtons.sort((a,b) =>" +
                " (a.innerText || '').length -" +
                " (b.innerText || '').length" +
                ");" +

                "if (searchButtons.length) {" +

                " let button=searchButtons[0];" +

                " button.scrollIntoView({" +
                "  block:'center'" +
                " });" +

                " button.click();" +

                " report('Търсенето е стартирано.');" +

                "} else {" +

                " report('Не намерих бутона Search.');" +
                "}" +

                "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    // =========================================================
    // JAVASCRIPT -> ANDROID STATUS
    // =========================================================

    public class EncarBridge {

        @JavascriptInterface
        public void report(
                final String message
        ) {

            runOnUiThread(() ->
                    statusValue.setText(
                            message
                    )
            );
        }
    }

    // =========================================================
    // ESCAPE ЗА JAVASCRIPT
    // =========================================================

    private String escapeJs(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    // =========================================================
    // BACK
    // =========================================================

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
