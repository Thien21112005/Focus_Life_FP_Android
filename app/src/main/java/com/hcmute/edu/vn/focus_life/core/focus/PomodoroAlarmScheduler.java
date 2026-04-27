package com.hcmute.edu.vn.focus_life.core.focus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hcmute.edu.vn.focus_life.receiver.PomodoroAlarmReceiver;

public final class PomodoroAlarmScheduler {
    public static final String ACTION_COMPLETE = "com.hcmute.edu.vn.focus_life.action.POMODORO_COMPLETE";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_DURATION_MINUTES = "extra_duration_minutes";

    private static final int REQUEST_COMPLETE = 9201;

    private PomodoroAlarmScheduler() {}

    public static void scheduleCompletion(Context context, String taskTitle, int durationMinutes, long delayMillis) {
        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        long triggerAtMillis = System.currentTimeMillis() + Math.max(1000L, delayMillis);
        PendingIntent pi = pendingIntent(appContext, taskTitle, durationMinutes);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                }
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        } catch (SecurityException ignored) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else {
                manager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        }
    }

    public static void cancel(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pendingIntent(appContext, "", 0));
    }

    private static PendingIntent pendingIntent(Context context, String taskTitle, int durationMinutes) {
        Intent intent = new Intent(context, PomodoroAlarmReceiver.class);
        intent.setAction(ACTION_COMPLETE);
        intent.putExtra(EXTRA_TASK_TITLE, taskTitle == null ? "Phiên Focus" : taskTitle);
        intent.putExtra(EXTRA_DURATION_MINUTES, durationMinutes);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQUEST_COMPLETE, intent, flags);
    }
}
