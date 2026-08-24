package com.encarvoicesearch;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
        root.setPadding(35, 35, 35, 35);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("ENCAR LINK DIAGNOSTICS");
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
        statusText.setText(
                "Тестваме кой линк отваря директно Search / Filter в Encar."
        );
        statusText.setTextSize(15);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 35, 0, 25);

        root.addView(statusText);

        // 1
        Button homeButton = new Button(this);
        homeButton.setText("1. ENCAR HOME");

        root.addView(
                homeButton,
                buttonParams()
        );

        // 2
        Button mobileSearchButton = new Button(this);
        mobileSearchButton.setText("2. MOBILE SEARCH");

        root.addView(
                mobileSearchButton,
                buttonParams()
        );

        // 3
        Button carSearchButton = new Button(this);
        carSearchButton.setText("3. CAR SEARCH");

        root.addView(
                carSearchButton,
                buttonParams()
        );

        homeButton.setOnClickListener(v ->
                openLink(
                        "https://car.encar.com/",
                        "HOME"
                )
        );

        mobileSearchButton.setOnClickListener(v ->
                openLink(
                        "https://m.encar.com/ca/search.do",
                        "MOBILE SEARCH"
                )
        );

        carSearchButton.setOnClickListener(v ->
                openLink(
                        "https://car.encar.com/ca/search.do",
                        "CAR SEARCH"
                )
        );

        setContentView(root);
    }

    private LinearLayout.LayoutParams buttonParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                10,
                0,
                10
        );

        return params;
    }

    private void openLink(
            String url,
            String name
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);

            statusText.setText(
                    name + " → " + url
            );

        } catch (ActivityNotFoundException e) {

            statusText.setText(
                    name + " не можа да бъде отворен."
            );

            Toast.makeText(
                    this,
                    "Неуспешно отваряне: " + url,
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
