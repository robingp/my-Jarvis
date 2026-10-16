package com.my.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String text = i.getStringExtra("text");
        if (text == null) text = "Reminder";
        String id = "jarvis_rem";
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(new NotificationChannel(id, "Reminders", NotificationManager.IMPORTANCE_HIGH));
        Notification n = new Notification.Builder(c, id)
                .setContentTitle("JARVIS reminder")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true)
                .build();
        nm.notify((int) (System.currentTimeMillis() & 0xfffffff), n);
    }
}
