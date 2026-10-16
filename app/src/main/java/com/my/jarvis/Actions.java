package com.my.jarvis;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;

import java.util.List;
import java.util.Locale;

public class Actions {

    public static void flashlight(Context c, boolean on) {
        try {
            CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            for (String id : cm.getCameraIdList()) {
                Boolean has = cm.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(has)) { cm.setTorchMode(id, on); return; }
            }
        } catch (Exception ignored) {}
    }

    public static void home(Context c) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    public static String findPackage(Context c, String name) {
        PackageManager pm = c.getPackageManager();
        String q = name.toLowerCase(Locale.ROOT).trim();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        String best = null;
        for (ApplicationInfo a : apps) {
            String label = pm.getApplicationLabel(a).toString().toLowerCase(Locale.ROOT);
            if (label.equals(q) && pm.getLaunchIntentForPackage(a.packageName) != null) return a.packageName;
            if (best == null && label.contains(q) && pm.getLaunchIntentForPackage(a.packageName) != null) best = a.packageName;
        }
        return best;
    }

    public static void launch(Context c, String pkg) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); }
    }
}
