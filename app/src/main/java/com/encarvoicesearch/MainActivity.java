package com.encarvoicesearch;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.ViewGroup;
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
    private TextView heardValue;

    private String selectedBrand = "";
    private String selectedModel = "";
    private String selectedYear = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главен контейнер
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // =========================
        // ГОРЕН ПАНЕЛ
        // =========================

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24, 16, 24, 16);
        panel.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("ENCAR VOICE SEARCH");
        title.setTextSize(18);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 12);

        panel.addView(title);

        // Марка
        brandValue = createValueView("Марка: -");
        panel.addView(brandValue);

        // Модел
        modelValue = createValueView("Модел: -");
        panel.addView(modelValue);

        // Година
        yearValue = createValueView("Година: -");
        panel.addView(yearValue);

        // Какво е чул телефонът
        heardValue = new TextView(this);
        heardValue.setText("Кажи например: Kia Sorento 2025");
        heardValue.setTextSize(13);
        heardValue.setTextColor(Color.DKGRAY);
        heardValue.setPadding(0, 10, 0, 10);

        panel.addView(heardValue);

        // Бутони
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

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

        buttons.addView(voiceButton, buttonParams);
        buttons.addView(searchButton, buttonParams);

        panel.addView(buttons);

        root.addView(
                panel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // =========================
        // ENCAR WEBVIEW
        // =========================

        webView = new WebView(this);

        LinearLayout.LayoutParams webParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        root.addView(webView, webParams);

        setContentView(root);

        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        String encarUrl =
                "https://m.encar.com/ca/search.do#!%7B%22type%22%3A%22car%22%2C%22action%22%3A%22(And.Hidden.N._.MultiViewHidden.N._.(Or.Separation.F._.Separation.B.)_.SellType.%EC%9D%BC%EB%B0%98._.CarType.A._.Mileage.range(..400000)._.Price.range(100..10000).)%22%2C%22toggle%22%3A%7B%7D%2C%22layer%22%3A%22%22%2C%22title%22%3A%22Mercedes-Benz%20GLE-Class%20W167(19%EB%85%84~%ED%98%84%EC%9E%AC)%22%2C%22sort%22%3A%22MobilePriceAsc%22%2C%22cursor%22%3A%22%22%7D";

        webView.loadUrl(encarUrl);

        // =========================
        // БУТОНИ
        // =========================

        voiceButton.setOnClickListener(v ->
                startVoiceRecognition()
        );

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

            Toast.makeText(
                    this,
                    selectedBrand
                            + " "
                            + selectedModel
                            + " "
                            + selectedYear,
                    Toast.LENGTH_LONG
            ).show();

            /*
             * СЛЕДВАЩА СТЪПКА:
             *
             * selectedBrand -> Manufacturer
             * selectedModel -> Model
             *
             * selectedYear:
             * начало = 01 / година
             * край    = 12 / година
             *
             * След това автоматично Search.
             */
        });
    }

    private TextView createValueView(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(17);
        view.setTextColor(Color.BLACK);
        view.setPadding(0, 4, 0, 4);

        return view;
    }

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

                heardValue.setText(
                        "Разпознато: " + spokenText
                );

                parseVoiceCommand(spokenText);
            }
        }
    }

    private void parseVoiceCommand(String spokenText) {

        String cleanText = spokenText.trim();

        // Търсим година от вида 2020, 2021, 2025 и т.н.
        Pattern yearPattern =
                Pattern.compile("\\b(19|20)\\d{2}\\b");

        Matcher matcher =
                yearPattern.matcher(cleanText);

        if (!matcher.find()) {

            Toast.makeText(
                    this,
                    "Не разпознах година.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        selectedYear = matcher.group();

        // Премахваме годината
        String carPart =
                cleanText
                        .replace(selectedYear, "")
                        .replace("година", "")
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

        // По подразбиране:
        // първата дума = марка
        // останалите = модел

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
                modelBuilder.toString();

        // Няколко чести марки с две думи
        String lower =
                carPart.toLowerCase();

        if (lower.startsWith("mercedes benz")) {

            selectedBrand = "Mercedes-Benz";

            selectedModel =
                    carPart.substring(
                            "mercedes benz".length()
                    ).trim();

        } else if (lower.startsWith("land rover")) {

            selectedBrand = "Land Rover";

            selectedModel =
                    carPart.substring(
                            "land rover".length()
                    ).trim();

        } else if (lower.startsWith("alfa romeo")) {

            selectedBrand = "Alfa Romeo";

            selectedModel =
                    carPart.substring(
                            "alfa romeo".length()
                    ).trim();
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
                        + "   (01/"
                        + selectedYear
                        + " - 12/"
                        + selectedYear
                        + ")"
        );
    }

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
