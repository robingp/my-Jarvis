package com.my.jarvis;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class ControlService extends AccessibilityService {

    private static ControlService instance;

    @Override public void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { super.onDestroy(); if (instance == this) instance = null; }

    public static String perform(String action) {
        if (instance == null)
            return "Please turn on the JARVIS accessibility service in Settings to control apps.";
        switch (action) {
            case "BACK": instance.performGlobalAction(GLOBAL_ACTION_BACK); return null;
            case "HOME": instance.performGlobalAction(GLOBAL_ACTION_HOME); return null;
            case "SCROLL_DOWN": instance.scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD); return null;
            case "SCROLL_UP": instance.scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD); return null;
            case "READ_SCREEN": return instance.readScreen();
        }
        return null;
    }

    private void scroll(int action) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo node = findScrollable(root);
        if (node != null) node.performAction(action);
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo n) {
        if (n == null) return null;
        if (n.isScrollable()) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo r = findScrollable(n.getChild(i));
            if (r != null) return r;
        }
        return null;
    }

    private String readScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "I cannot read the screen right now.";
        StringBuilder sb = new StringBuilder();
        collect(root, sb);
        String text = sb.toString().trim();
        if (text.isEmpty()) return "There is no readable text on screen.";
        if (text.length() > 300) text = text.substring(0, 300) + "...";
        return text;
    }

    private void collect(AccessibilityNodeInfo n, StringBuilder sb) {
        if (n == null) return;
        CharSequence t = n.getText();
        if (t != null && t.length() > 0 && sb.length() < 320) sb.append(t).append(". ");
        for (int i = 0; i < n.getChildCount(); i++) collect(n.getChild(i), sb);
    }
}
