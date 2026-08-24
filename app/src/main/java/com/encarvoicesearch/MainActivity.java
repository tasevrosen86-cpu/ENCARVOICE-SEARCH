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
        root.setPadding(40, 40, 40, 40);
        root.setBackgroundColor(Color.WHITE);

        // Заглавие
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

        titleParams.setMargins(
                0,
                0,
                0,
                40
        );

        root.addView(
                title,
                titleParams
        );

        // Статус
        statusText = new TextView(this);

        statusText.setText(
                "Тест: отваряне на официалното Encar приложение чрез линк"
        );

        statusText.setTextSize(16);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(
                0,
                0,
                0,
                30
        );

        root.addView(
                statusText,
                statusParams
        );

        // Бутон
        Button openButton = new Button(this);

        openButton.setText("ОТВОРИ ENCAR");
        openButton.setTextSize(18);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        root.addView(
                openButton,
                buttonParams
        );

        openButton.setOnClickListener(
                v -> openEncar()
        );

        setContentView(root);
    }

    private void openEncar() {

        try {

            Uri encarUri =
                    Uri.parse(
                            "https://car.encar.com/"
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            encarUri
                    );

            startActivity(intent);

            statusText.setText(
                    "Подавам линка към Encar..."
            );

        } catch (ActivityNotFoundException e) {

            statusText.setText(
                    "Не успях да отворя Encar."
            );

            Toast.makeText(
                    this,
                    "Няма приложение, което може да отвори Encar.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
