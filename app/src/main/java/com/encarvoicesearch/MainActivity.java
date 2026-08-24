package com.encarvoicesearch;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        statusText = new TextView(this);
        statusText.setText("Тест за директно отваряне на Encar");
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 40, 0, 30);

        root.addView(statusText);

        Button openButton = new Button(this);
        openButton.setText("ОТВОРИ ENCAR");
        openButton.setTextSize(18);

        root.addView(
                openButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        openButton.setOnClickListener(v -> openEncar());

        setContentView(root);
    }

    private void openEncar() {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage("com.encar.encarMobileApp");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {

            startActivity(intent);

            statusText.setText("Отварям Encar...");

        } catch (ActivityNotFoundException e) {

            statusText.setText("Не успях да стартирам Encar.");

            Toast.makeText(
                    this,
                    "Encar не можа да бъде стартирано.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
