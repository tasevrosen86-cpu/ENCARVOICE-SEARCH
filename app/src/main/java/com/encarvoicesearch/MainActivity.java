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

    private static final String ENCAR_PACKAGE =
            "com.encar.encarMobileApp";

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
        title.setText("ENCAR DEEP LINK TEST");
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
                "Принудително подаваме Search линка директно към Encar."
        );
        statusText.setTextSize(15);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 35, 0, 30);

        root.addView(statusText);

        Button searchButton = new Button(this);
        searchButton.setText("1. FORCE ENCAR SEARCH");

        root.addView(
                searchButton,
                buttonParams()
        );

        Button filterButton = new Button(this);
        filterButton.setText("2. FORCE FILTER PAGE");

        root.addView(
                filterButton,
                buttonParams()
        );

        Button homeButton = new Button(this);
        homeButton.setText("3. ENCAR HOME");

        root.addView(
                homeButton,
                buttonParams()
        );

        // Простият мобилен Search адрес
        searchButton.setOnClickListener(v ->
                openInsideEncar(
                        "https://m.encar.com/ca/search.do",
                        "FORCE SEARCH"
                )
        );

        // Същата Search страница с начално състояние
        filterButton.setOnClickListener(v ->
                openInsideEncar(
                        "https://m.encar.com/ca/search.do#!%7B%22type%22%3A%22car%22%2C%22action%22%3A%22%22%2C%22toggle%22%3A%7B%7D%2C%22layer%22%3A%22%22%7D",
                        "FORCE FILTER"
                )
        );

        // Контролен тест
        homeButton.setOnClickListener(v ->
                openNormal(
                        "https://car.encar.com/",
                        "HOME"
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

        params.setMargins(0, 10, 0, 10);

        return params;
    }

    private void openInsideEncar(
            String url,
            String name
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            // КЛЮЧОВАТА РАЗЛИКА:
            // не позволяваме на Chrome да поеме линка
            intent.setPackage(ENCAR_PACKAGE);

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);

            statusText.setText(
                    name + " → подаден директно към Encar"
            );

        } catch (ActivityNotFoundException e) {

            statusText.setText(
                    name + " → Encar не прие този адрес"
            );

            Toast.makeText(
                    this,
                    "Encar не прие този deep link.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openNormal(
            String url,
            String name
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(intent);

            statusText.setText(
                    name + " → отворен"
            );

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "Неуспешно отваряне.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
