package com.encarvoicesearch;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("ENCAR VOICE SEARCH");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView info = new TextView(this);
        info.setText(
                "1. Активирай Accessibility\n" +
                "2. После отвори Encar"
        );
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 40, 0, 30);

        root.addView(info);

        Button accessibilityButton = new Button(this);
        accessibilityButton.setText("АКТИВИРАЙ ACCESSIBILITY");

        root.addView(
                accessibilityButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Button openEncarButton = new Button(this);
        openEncarButton.setText("ОТВОРИ ENCAR");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        buttonParams.setMargins(0, 20, 0, 0);

        root.addView(openEncarButton, buttonParams);

        accessibilityButton.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                    );

            startActivity(intent);
        });

        openEncarButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://car.encar.com/")
                    );

            startActivity(intent);
        });

        setContentView(root);
    }
}
