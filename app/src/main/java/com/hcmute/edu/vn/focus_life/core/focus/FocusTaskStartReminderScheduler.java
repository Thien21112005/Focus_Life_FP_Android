package com.hcmute.edu.vn.focus_life.core.focus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmute.edu.vn.focus_life.domain.model.FocusTask;
import com.hcmute.edu.vn.focus_life.receiver.FocusTaskStartReminderReceiver;

public final class FocusTaskStartReminderScheduler {
    public static final String ACTION_TASK_START_REMINDER = "com.hcmute.edu.vn.focus_life.action.FOCUS_TASK_START_REMINDER";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_START_AT = "extra_start_at";

    private static final long MIN_DELAY_MS = 1000L;

    private FocusTaskStartReminderScheduler() {
    }

    public static void scheduleTaskStartReminder(@NonNull Context context, @Nullable FocusTask task) {
        if (task == null || task.id == null || task.id.trim().isEmpty()) return;

        if (task.completed || task.deleted || task.startAt <= System.currentTimeMillis() + MIN_DELAY_MS) {
            cancelTaskStartReminder(context, task.id);
            return;
        }

        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;

        PendingIntent pendingIntent = buildPendingIntent(appContext, task.id, task.title, task.startAt);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.startAt, pendingIntent);
                } else {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.startAt, pendingIntent);
                }
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, task.startAt, pendingIntent);
            }
        } catch (SecurityException ignored) {
            manager.set(AlarmManager.RTC_WAKEUP, task.startAt, pendingIntent);
        }
    }

    public static void cancelTaskStartReminder(@NonNull Context context, @Nullable String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) return;

        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;

        manager.cancel(buildPendingIntent(appContext, taskId, "", 0L));
    }

    private static PendingIntent buildPendingIntent(@NonNull Context context,
                                                    @NonNull String taskId,
                                                    @Nullable String taskTitle,
                                                    long startAt) {
        Intent intent = new Intent(context, FocusTaskStartReminderReceiver.class);
        intent.setAction(ACTION_TASK_START_REMINDER);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_TASK_TITLE, taskTitle == null ? "Task Focus" : taskTitle);
        intent.putExtra(EXTRA_START_AT, startAt);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, requestCode(taskId), intent, flags);
    }

    public static int requestCode(@NonNull String taskId) {
        return taskId.hashCode() & 0x7fffffff;
    }
}
