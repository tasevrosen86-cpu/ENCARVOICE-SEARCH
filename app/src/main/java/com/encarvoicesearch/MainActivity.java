package com.encarvoicesearch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText searchInput;
    private TextView status;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private static final int SPEECH_REQUEST_CODE = 1001;

    private SearchCommand pendingCommand = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(10, 10, 10, 0);

        // =====================================================
        // SEARCH ROW
        // =====================================================

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);

        searchInput = new EditText(this);
        searchInput.setHint(
                "Например: BMW X5 2024 дизел"
        );
        searchInput.setSingleLine(true);
        searchInput.setTextSize(17);

        Button micButton = new Button(this);
        micButton.setText("🎤");
        micButton.setTextSize(22);

        searchRow.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        searchRow.addView(
                micButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Button searchButton = new Button(this);

        searchButton.setText(
                "ПОКАЖИ РЕЗУЛТАТИТЕ"
        );

        status = new TextView(this);
        status.setPadding(10, 12, 10, 12);
        status.setTextSize(14);
        status.setText(
                "Напиши или кажи автомобил."
        );

        webView = new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager.getInstance()
                .setAcceptCookie(true);

        CookieManager.getInstance()
                .setAcceptThirdPartyCookies(
                        webView,
                        true
                );

        webView.addJavascriptInterface(
                new SearchBridge(),
                "AndroidSearch"
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

                        if (
                                pendingCommand != null &&
                                url != null &&
                                url.contains(
                                        "car.encar.com/list/car"
                                )
                        ) {

                            status.setText(
                                    "Encar е зареден.\n" +
                                    "Търся производителя..."
                            );

                            handler.postDelayed(
                                    () ->
                                            startUniversalSearch(
                                                    pendingCommand
                                            ),
                                    1500
                            );
                        }
                    }
                }
        );

        root.addView(searchRow);

        root.addView(
                searchButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                status,
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

        micButton.setOnClickListener(
                v -> startVoiceRecognition()
        );

        searchButton.setOnClickListener(
                v -> beginSearch()
        );

        // Зареждаме Encar още при стартиране.
        webView.loadUrl(
                "https://car.encar.com/list/car"
        );
    }

    // =========================================================
    // VOICE
    // =========================================================

    private void startVoiceRecognition() {

        try {

            Intent intent =
                    new Intent(
                            RecognizerIntent
                                    .ACTION_RECOGNIZE_SPEECH
                    );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                            .LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "bg-BG"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Кажи марка, модел, година и гориво"
            );

            startActivityForResult(
                    intent,
                    SPEECH_REQUEST_CODE
            );

        } catch (Exception e) {

            status.setText(
                    "Гласовото разпознаване " +
                    "не е достъпно."
            );
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

        if (
                requestCode ==
                        SPEECH_REQUEST_CODE &&
                resultCode ==
                        RESULT_OK &&
                data != null
        ) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (
                    results != null &&
                    !results.isEmpty()
            ) {

                String spoken =
                        results.get(0);

                searchInput.setText(spoken);

                searchInput.setSelection(
                        spoken.length()
                );

                status.setText(
                        "Разпознато:\n" +
                        spoken +
                        "\n\n" +
                        "Провери текста и натисни " +
                        "ПОКАЖИ РЕЗУЛТАТИТЕ."
                );
            }
        }
    }

    // =========================================================
    // START SEARCH
    // =========================================================

    private void beginSearch() {

        String text =
                searchInput
                        .getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {

            status.setText(
                    "Напиши или кажи автомобил."
            );

            return;
        }

        SearchCommand command =
                parseCommand(text);

        if (command.brand.isEmpty()) {

            status.setText(
                    "Не успях да определя марката."
            );

            return;
        }

        if (command.model.isEmpty()) {

            status.setText(
                    "Не успях да определя модела."
            );

            return;
        }

        pendingCommand =
                command;

        status.setText(
                "Разбрах:\n" +
                "Марка: " +
                command.brand +
                "\n" +
                "Модел: " +
                command.model +
                "\n" +
                "Година: " +
                command.year +
                "\n" +
                "Гориво: " +
                command.fuel +
                "\n\n" +
                "Подготвям Encar..."
        );

        String current =
                webView.getUrl();

        if (
                current == null ||
                !current.contains(
                        "car.encar.com/list/car"
                )
        ) {

            webView.loadUrl(
                    "https://car.encar.com/list/car"
            );

        } else {

            startUniversalSearch(
                    command
            );
        }
    }

    // =========================================================
    // PARSER
    // =========================================================

    private SearchCommand parseCommand(
            String original
    ) {

        SearchCommand result =
                new SearchCommand();

        String text =
                original
                        .trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        String lower =
                text.toLowerCase(
                        Locale.ROOT
                );

        // YEAR

        Matcher matcher =
                Pattern.compile(
                        "\\b(20\\d{2})\\b"
                ).matcher(lower);

        if (matcher.find()) {

            result.year =
                    matcher.group(1);

            lower =
                    lower.replace(
                            result.year,
                            " "
                    );
        }

        // FUEL

        if (
                lower.contains("дизел") ||
                lower.contains("diesel")
        ) {

            result.fuel =
                    "Diesel";

            lower =
                    lower
                            .replace(
                                    "дизел",
                                    " "
                            )
                            .replace(
                                    "diesel",
                                    " "
                            );

        } else if (
                lower.contains("хибрид") ||
                lower.contains("hybrid")
        ) {

            result.fuel =
                    "Hybrid";

            lower =
                    lower
                            .replace(
                                    "хибрид",
                                    " "
                            )
                            .replace(
                                    "hybrid",
                                    " "
                            );

        } else if (
                lower.contains("бензин") ||
                lower.contains("gasoline") ||
                lower.contains("petrol")
        ) {

            result.fuel =
                    "Gasoline";

            lower =
                    lower
                            .replace(
                                    "бензин",
                                    " "
                            )
                            .replace(
                                    "gasoline",
                                    " "
                            )
                            .replace(
                                    "petrol",
                                    " "
                            );

        } else if (
                lower.contains("електр") ||
                lower.contains("electric") ||
                lower.contains(" ev")
        ) {

            result.fuel =
                    "Electric";

            lower =
                    lower
                            .replace(
                                    "електрически",
                                    " "
                            )
                            .replace(
                                    "електрическа",
                                    " "
                            )
                            .replace(
                                    "electric",
                                    " "
                            );
        }

        lower =
                lower.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        String[] parts =
                lower.split(" ");

        if (parts.length >= 1) {

            result.brand =
                    normalizeBrand(
                            parts[0]
                    );
        }

        if (parts.length >= 2) {

            StringBuilder model =
                    new StringBuilder();

            for (
                    int i = 1;
                    i < parts.length;
                    i++
            ) {

                if (
                        parts[i].equals("г") ||
                        parts[i].equals("г.") ||
                        parts[i].equals("година")
                ) {
                    continue;
                }

                if (model.length() > 0) {
                    model.append(" ");
                }

                model.append(
                        parts[i]
                );
            }

            result.model =
                    model
                            .toString()
                            .trim();
        }

        return result;
    }

    // =========================================================
    // BRAND NORMALIZATION
    //
    // Това НЕ съдържа модели.
    // Само различни начини, по които гласът може
    // да изпише името на производителя.
    // =========================================================

    private String normalizeBrand(
            String brand
    ) {

        String b =
                brand
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();

        switch (b) {

            case "киа":
                return "Kia";

            case "хюндай":
            case "хюндаи":
            case "хундай":
            case "hyundai":
                return "Hyundai";

            case "мерцедес":
            case "мерседес":
            case "mercedes":
                return "Mercedes";

            case "бмв":
            case "bmw":
                return "BMW";

            case "ауди":
            case "audi":
                return "Audi";

            case "фолксваген":
            case "волксваген":
            case "volkswagen":
            case "vw":
                return "Volkswagen";

            case "порше":
            case "porsche":
                return "Porsche";

            case "форд":
            case "ford":
                return "Ford";

            case "тойота":
            case "toyota":
                return "Toyota";

            case "лексус":
            case "lexus":
                return "Lexus";

            case "волво":
            case "volvo":
                return "Volvo";

            case "пежо":
            case "peugeot":
                return "Peugeot";

            case "хонда":
            case "honda":
                return "Honda";

            case "ниссан":
            case "nissan":
                return "Nissan";

            case "шевролет":
            case "chevrolet":
                return "Chevrolet";

            case "джип":
            case "jeep":
                return "Jeep";

            case "рендж":
            case "landrover":
                return "Land Rover";

            default:

                if (brand.length() > 0) {

                    return brand
                            .substring(0, 1)
                            .toUpperCase(
                                    Locale.ROOT
                            ) +
                            brand.substring(1);
                }

                return "";
        }
    }

    // =========================================================
    // UNIVERSAL ENCAR SEARCH
    // =========================================================

    private void startUniversalSearch(
            SearchCommand command
    ) {

        String brand =
                jsEscape(
                        command.brand
                );

        String model =
                jsEscape(
                        command.model
                );

        String year =
                jsEscape(
                        command.year
                );

        String fuel =
                jsEscape(
                        command.fuel
                );

        /*
         * Основната разлика спрямо Sorento версията:
         *
         * НЯМА:
         *
         * Manufacturer.기아
         * ModelGroup.쏘렌토
         * Model.더 뉴 쏘렌토...
         *
         * Тук четем живите опции на Encar.
         */

        String script =
                "(function() {" +

                "const BRAND='" +
                brand +
                "';" +

                "const MODEL='" +
                model +
                "';" +

                "const YEAR='" +
                year +
                "';" +

                "const FUEL='" +
                fuel +
                "';" +

                "function norm(s) {" +
                " return (s || '')" +
                ".toLowerCase()" +
                ".replace(/[^a-z0-9가-힣]+/g,'');" +
                "}" +

                "function text(el) {" +
                " return (" +
                "el.innerText || " +
                "el.textContent || ''" +
                ").trim();" +
                "}" +

                "function visible(el) {" +
                " if (!el) return false;" +
                " const r=el.getBoundingClientRect();" +
                " const st=getComputedStyle(el);" +
                " return r.width>0 && " +
                "r.height>0 && " +
                "st.display!=='none' && " +
                "st.visibility!=='hidden';" +
                "}" +

                "function clickable() {" +
                " return Array.from(" +
                "document.querySelectorAll(" +
                "'a,button,[role=\"button\"],label'" +
                ")" +
                ").filter(visible);" +
                "}" +

                "function bestMatch(word) {" +

                " const target=norm(word);" +

                " if (!target) return null;" +

                " const els=clickable();" +

                " let exact=null;" +
                " let contains=null;" +

                " for (const el of els) {" +

                "   const t=norm(text(el));" +

                "   if (!t) continue;" +

                "   if (t===target) {" +
                "      exact=el;" +
                "      break;" +
                "   }" +

                "   if (" +
                "      !contains && " +
                "      (t.includes(target) || " +
                "       target.includes(t))" +
                "   ) {" +
                "      contains=el;" +
                "   }" +

                " }" +

                " return exact || contains;" +
                "}" +

                "function clickText(word) {" +

                " const el=bestMatch(word);" +

                " if (!el) return false;" +

                " el.scrollIntoView({" +
                "   block:'center'" +
                " });" +

                " el.click();" +

                " return true;" +
                "}" +

                "function report(step,msg) {" +

                " AndroidSearch.onStatus(" +
                " step + '|' + msg" +
                " );" +
                "}" +

                // ===========================================
                // MANUFACTURER
                // ===========================================

                "let manufacturer=" +
                "document.querySelector('#optManufact');" +

                "if (!manufacturer) {" +

                " const candidates=clickable();" +

                " manufacturer=" +
                "candidates.find(function(el) {" +

                "   const t=norm(text(el));" +

                "   return " +
                "t==='manufacturer' || " +
                "t==='make' || " +
                "t.includes('manufacturer');" +

                " });" +
                "}" +

                "if (!manufacturer) {" +

                " report(" +
                "'ERROR'," +
                "'Не намерих Manufacturer филтъра'" +
                ");" +

                " return;" +
                "}" +

                "manufacturer.click();" +

                "report(" +
                "'MANUFACTURER_OPEN'," +
                "'Manufacturer е отворен'" +
                ");" +

                // ===========================================
                // BRAND
                // ===========================================

                "setTimeout(function() {" +

                " if (!clickText(BRAND)) {" +

                "   report(" +
                "'ERROR'," +
                "'Не намерих марка: '+BRAND" +
                ");" +

                "   return;" +
                " }" +

                " report(" +
                "'BRAND_OK'," +
                "'Избрана марка: '+BRAND" +
                ");" +

                // ===========================================
                // MODEL CONTROL
                // ===========================================

                " setTimeout(function() {" +

                "   let modelControl=null;" +

                "   const controls=clickable();" +

                "   modelControl=" +
                "controls.find(function(el) {" +

                "      const t=norm(text(el));" +

                "      return " +
                "      t==='model' || " +
                "      t.includes('model');" +

                "   });" +

                "   if (!modelControl) {" +

                "      const ids=Array.from(" +
                "      document.querySelectorAll('[id]')" +
                "      );" +

                "      modelControl=ids.find(function(el) {" +

                "         return " +
                "         /model/i.test(el.id) && " +
                "         visible(el);" +

                "      });" +
                "   }" +

                "   if (!modelControl) {" +

                "      report(" +
                "'ERROR'," +
                "'Марката е избрана, но не намерих Model филтъра'" +
                ");" +

                "      return;" +
                "   }" +

                "   modelControl.click();" +

                "   report(" +
                "'MODEL_OPEN'," +
                "'Model е отворен'" +
                ");" +

                // ===========================================
                // MODEL
                // ===========================================

                "   setTimeout(function() {" +

                "      if (!clickText(MODEL)) {" +

                "         report(" +
                "'ERROR'," +
                "'Не намерих модел: '+MODEL" +
                ");" +

                "         return;" +
                "      }" +

                "      report(" +
                "'MODEL_OK'," +
                "'Избран модел: '+MODEL" +
                ");" +

                // ===========================================
                // YEAR
                // ===========================================

                "      setTimeout(function() {" +

                "         if (YEAR) {" +

                "            let yearControl=" +
                "            clickable().find(function(el) {" +

                "               const t=norm(text(el));" +

                "               return " +
                "               t==='year' || " +
                "               t.includes('modelyear') || " +
                "               t.includes('year');" +

                "            });" +

                "            if (yearControl) {" +

                "               yearControl.click();" +

                "               setTimeout(function() {" +

                "                  if (clickText(YEAR)) {" +

                "                     report(" +
                "'YEAR_OK'," +
                "'Избрана година: '+YEAR" +
                ");" +

                "                  } else {" +

                "                     report(" +
                "'YEAR_SKIP'," +
                "'Не намерих директна година '+YEAR" +
                ");" +
                "                  }" +

                "               },700);" +
                "            }" +
                "         }" +

                // ===========================================
                // FUEL
                // ===========================================

                "         setTimeout(function() {" +

                "            if (FUEL) {" +

                "               let fuelControl=" +
                "               clickable().find(function(el) {" +

                "                  const t=norm(text(el));" +

                "                  return " +
                "                  t==='fuel' || " +
                "                  t.includes('fueltype') || " +
                "                  t.includes('fuel');" +

                "               });" +

                "               if (fuelControl) {" +

                "                  fuelControl.click();" +

                "                  setTimeout(function() {" +

                "                     if (" +
                "                     clickText(FUEL)" +
                "                     ) {" +

                "                        report(" +
                "'FUEL_OK'," +
                "'Избрано гориво: '+FUEL" +
                ");" +

                "                     } else {" +

                "                        report(" +
                "'FUEL_SKIP'," +
                "'Не намерих гориво: '+FUEL" +
                ");" +

                "                     }" +

                "                  },700);" +
                "               }" +
                "            }" +

                // ===========================================
                // FINISH
                // ===========================================

                "            setTimeout(function() {" +

                "               report(" +
                "'DONE'," +
                "'Филтрите са приложени'" +
                ");" +

                "            },1200);" +

                "         },900);" +

                "      },900);" +

                "   },900);" +

                " },900);" +

                "},700);" +

                "})();";

        webView.evaluateJavascript(
                script,
                null
        );
    }

    private String jsEscape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "'",
                        "\\'"
                )
                .replace(
                        "\n",
                        " "
                );
    }

    // =========================================================
    // ANDROID <-> JAVASCRIPT
    // =========================================================

    private class SearchBridge {

        @JavascriptInterface
        public void onStatus(
                String message
        ) {

            runOnUiThread(() -> {

                String[] parts =
                        message.split(
                                "\\|",
                                2
                        );

                String step =
                        parts.length > 0
                                ? parts[0]
                                : "";

                String msg =
                        parts.length > 1
                                ? parts[1]
                                : message;

                status.setText(
                        msg
                );

                if (
                        step.equals(
                                "DONE"
                        )
                ) {

                    pendingCommand = null;
                }
            });
        }
    }

    // =========================================================
    // COMMAND CLASS
    // =========================================================

    private static class SearchCommand {

        String brand = "";
        String model = "";
        String year = "";
        String fuel = "";
    }

    // =========================================================
    // BACK
    // =========================================================

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
