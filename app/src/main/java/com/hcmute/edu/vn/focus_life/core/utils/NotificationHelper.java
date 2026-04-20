package com.hcmute.edu.vn.focus_life.core.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {
    public static final String REMINDER_CHANNEL = "focuslife_reminders";
    public static final String RUNNING_CHANNEL = "focuslife_running";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel reminderChannel = new NotificationChannel(
                REMINDER_CHANNEL,
                "FocusLife Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        reminderChannel.setDescription("Pomodoro, water, habit reminders");
        manager.createNotificationChannel(reminderChannel);

        NotificationChannel runningChannel = new NotificationChannel(
                RUNNING_CHANNEL,
                "FocusLife Running Tracker",
                NotificationManager.IMPORTANCE_LOW
        );
        runningChannel.setDescription("Theo dõi phiên chạy nền");
        manager.createNotificationChannel(runningChannel);
    }
}