package com.hcmute.edu.vn.focus_life.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.utils.NotificationHelper;

public class PomodoroService extends Service {

    public static final String ACTION_START = "pomodoro_action_start";
    public static final String ACTION_STOP = "pomodoro_action_stop";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_DURATION_MINUTES = "extra_duration_minutes";
    public static final String EXTRA_ENABLE_DND = "extra_enable_dnd";

    private static final int NOTIFICATION_ID = 2027;
    private int previousInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
    private boolean dndApplied = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            restoreDndIfNeeded();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        int durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25);
        boolean enableDnd = intent.getBooleanExtra(EXTRA_ENABLE_DND, true);

        startForeground(NOTIFICATION_ID, buildNotification(taskTitle, durationMinutes));
        if (enableDnd) {
            applyDndIfPossible();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        restoreDndIfNeeded();
        super.onDestroy();
    }

    private Notification buildNotification(@Nullable String taskTitle, int durationMinutes) {
        String contentTitle = taskTitle == null || taskTitle.trim().isEmpty() ? "Phiên Focus đang chạy" : taskTitle;
        String contentText = "Pomodoro " + durationMinutes + " phút đang hoạt động";
        return new NotificationCompat.Builder(this, NotificationHelper.FOCUS_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void applyDndIfPossible() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (!manager.isNotificationPolicyAccessGranted()) return;

        previousInterruptionFilter = manager.getCurrentInterruptionFilter();
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        dndApplied = true;
    }

    private void restoreDndIfNeeded() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (!dndApplied) return;
        if (!manager.isNotificationPolicyAccessGranted()) return;

        manager.setInterruptionFilter(previousInterruptionFilter);
        dndApplied = false;
    }
}