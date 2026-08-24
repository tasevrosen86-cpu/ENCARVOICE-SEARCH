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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST_CODE = 1001;

    private WebView webView;
    private TextView voiceResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);

        // WebView за ENCAR
        webView = new WebView(this);

        FrameLayout.LayoutParams webParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        root.addView(webView, webParams);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        // Поле, в което показваме какво е разпознато
        voiceResult = new TextView(this);
        voiceResult.setText("Кажи: Марка Модел Година");
        voiceResult.setTextSize(16);
        voiceResult.setTextColor(Color.BLACK);
        voiceResult.setBackgroundColor(Color.WHITE);
        voiceResult.setPadding(20, 14, 20, 14);

        FrameLayout.LayoutParams textParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        textParams.gravity =
                Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        textParams.topMargin = 30;

        root.addView(voiceResult, textParams);

        // Бутон за гласово разпознаване
        Button voiceButton = new Button(this);
        voiceButton.setText("ГЛАС");
        voiceButton.setTextSize(16);

        FrameLayout.LayoutParams buttonParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        buttonParams.gravity =
                Gravity.BOTTOM | Gravity.END;

        buttonParams.setMargins(
                20,
                20,
                30,
                40
        );

        root.addView(voiceButton, buttonParams);

        voiceButton.setOnClickListener(v ->
                startVoiceRecognition()
        );

        setContentView(root);

        // Директно към ENCAR - без Google
        String encarUrl =
                "https://m.encar.com/ca/search.do#!%7B%22type%22%3A%22car%22%2C%22action%22%3A%22(And.Hidden.N._.MultiViewHidden.N._.(Or.Separation.F._.Separation.B.)_.SellType.%EC%9D%BC%EB%B0%98._.CarType.A._.Mileage.range(..400000)._.Price.range(100..10000).)%22%2C%22toggle%22%3A%7B%7D%2C%22layer%22%3A%22%22%2C%22title%22%3A%22Mercedes-Benz%20GLE-Class%20W167(19%EB%85%84~%ED%98%84%EC%9E%AC)%22%2C%22sort%22%3A%22MobilePriceAsc%22%2C%22cursor%22%3A%22%22%7D";

        webView.loadUrl(encarUrl);
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

                String spokenText =
                        results.get(0);

                voiceResult.setText(
                        "Разпознато: " + spokenText
                );

                Toast.makeText(
                        this,
                        spokenText,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
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
