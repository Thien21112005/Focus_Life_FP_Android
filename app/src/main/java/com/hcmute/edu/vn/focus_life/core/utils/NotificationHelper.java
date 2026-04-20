package com.hcmute.edu.vn.focus_life.core.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {
    public static final String REMINDER_CHANNEL = "focuslife_reminders";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                REMINDER_CHANNEL,
                "FocusLife Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Pomodoro, water, habit reminders");
        manager.createNotificationChannel(channel);
    }
}
