package com.encarvoicesearch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String ENCAR_PACKAGE =
            "com.encar.encarMobileApp";

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("ENCAR VOICE SEARCH");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        titleParams.setMargins(0, 0, 0, 40);

        root.addView(title, titleParams);

        statusText = new TextView(this);
        statusText.setText(
                "Тест: нашето приложение → официалното Encar приложение"
        );
        statusText.setTextSize(16);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(0, 0, 0, 30);

        root.addView(statusText, statusParams);

        Button openButton = new Button(this);
        openButton.setText("ОТВОРИ ENCAR");
        openButton.setTextSize(18);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        root.addView(openButton, buttonParams);

        openButton.setOnClickListener(v ->
                openEncarApp()
        );

        setContentView(root);
    }

    private void openEncarApp() {

        Intent launchIntent =
                getPackageManager()
                        .getLaunchIntentForPackage(
                                ENCAR_PACKAGE
                        );

        if (launchIntent != null) {

            statusText.setText(
                    "Отварям Encar..."
            );

            startActivity(launchIntent);

        } else {

            statusText.setText(
                    "Encar не беше намерено."
            );

            Toast.makeText(
                    this,
                    "Официалното Encar приложение не беше намерено.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
