package com.encarvoicesearch;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class EncarAccessibilityService extends AccessibilityService {

    private long lastMessageTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root == null) {
            return;
        }

        AccessibilityNodeInfo field = findSearchField(root);

        if (field != null) {

            long now = System.currentTimeMillis();

            if (now - lastMessageTime > 3000) {

                lastMessageTime = now;

                Rect rect = new Rect();
                field.getBoundsInScreen(rect);

                Toast.makeText(
                        this,
                        "Намерих поле в Encar! X=" +
                                rect.left +
                                " Y=" +
                                rect.top,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private AccessibilityNodeInfo findSearchField(
            AccessibilityNodeInfo node) {

        if (node == null) {
            return null;
        }

        String className = safe(node.getClassName());
        String text = safe(node.getText());
        String description = safe(node.getContentDescription());

        if (className.contains("EditText")) {
            return node;
        }

        String combined =
                (text + " " + description).toLowerCase();

        if (combined.contains("검색")
                || combined.contains("차량 검색")
                || combined.contains("search")) {

            if (node.isClickable()
                    || node.isFocusable()
                    || node.isEditable()) {

                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo result =
                    findSearchField(node.getChild(i));

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private String safe(CharSequence value) {

        if (value == null) {
            return "";
        }

        return value.toString();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        Toast.makeText(
                this,
                "ENCAR Accessibility е активиран",
                Toast.LENGTH_LONG
        ).show();
    }
}
