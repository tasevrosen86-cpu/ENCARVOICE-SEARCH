package com.encarvoicesearch;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class EncarAccessibilityService
        extends AccessibilityService {

    private long lastMessageTime = 0;

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return;
        }

        AccessibilityNodeInfo searchField =
                findSearchField(root);

        if (searchField != null) {

            long now =
                    System.currentTimeMillis();

            if (now - lastMessageTime > 3000) {

                lastMessageTime = now;

                Rect bounds = new Rect();

                searchField.getBoundsInScreen(
                        bounds
                );

                Toast.makeText(
                        this,
                        "Намерих поле в Encar!\n" +
                                "X=" + bounds.left +
                                " Y=" + bounds.top,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
