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
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST_CODE = 1001;

    private WebView webView;
    private TextView voiceResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Основен контейнер
        FrameLayout root = new FrameLayout(this);

        // ENCAR WebView
        webView = new WebView(this);

        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        root.addView(webView, webParams);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        // Показва какво е чуло приложението
        voiceResult = new TextView(this);
        voiceResult.setText("Кажи: Марка Модел Година");
        voiceResult.setTextSize(16);
        voiceResult.setTextColor(Color.BLACK);
        voiceResult.setBackgroundColor(Color.WHITE);
        voiceResult.setPadding(20, 14, 20, 14);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        textParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        textParams.topMargin = 30;

        root.addView(voiceResult, textParams);

        // Бутон за глас
        Button voiceButton = new Button(this);
        voiceButton.setText("🎤 ГЛ
