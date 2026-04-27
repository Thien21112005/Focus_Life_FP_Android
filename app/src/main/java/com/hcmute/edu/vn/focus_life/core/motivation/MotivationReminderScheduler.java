package com.hcmute.edu.vn.focus_life.core.motivation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.hcmute.edu.vn.focus_life.receiver.MotivationReminderReceiver;
import com.hcmute.edu.vn.focus_life.worker.MotivationReminderWorker;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class MotivationReminderScheduler {
    public static final String ACTION_MOTIVATION_REMINDER = "com.hcmute.edu.vn.focus_life.action.MOTIVATION_REMINDER";
    public static final String ACTION_BOOT_RESCHEDULE = "com.hcmute.edu.vn.focus_life.action.MOTIVATION_BOOT_RESCHEDULE";
    public static final String ACTION_UNLOCK_REMINDER = "com.hcmute.edu.vn.focus_life.action.MOTIVATION_UNLOCK_REMINDER";
    public static final String EXTRA_SLOT = "extra_slot";

    private static final String TAG = "MotivationAlarm";
    private static final int REQUEST_DAILY = 8300;
    private static final String UNIQUE_WORK_DAILY = "focuslife_motivation_daily";

    private MotivationReminderScheduler() {}

    public static void scheduleDefaultDailyReminders(Context context) {
        scheduleConfiguredDailyReminder(context);
    }

    public static void scheduleConfiguredDailyReminder(Context context) {
        Context appContext = context.getApplicationContext();
        MotivationPreferences preferences = new MotivationPreferences(appContext);
        if (!preferences.isEnabled()) {
            cancel(appContext);
            return;
        }
        scheduleSlot(appContext, 0, preferences.getHour(), preferences.getMinute());
    }

    public static void scheduleNextForSlot(Context context, int slot) {
        scheduleConfiguredDailyReminder(context);
    }

    public static void cancel(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createPendingIntent(appContext));
        }
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_DAILY);
        Log.d(TAG, "Cancelled motivation reminder");
    }

    private static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, MotivationReminderReceiver.class);
        intent.setAction(ACTION_MOTIVATION_REMINDER);
        intent.putExtra(EXTRA_SLOT, 0);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQUEST_DAILY, intent, flags);
    }

    private static void scheduleSlot(Context context, int slot, int hour, int minute) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(appContext);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        long triggerAtMillis = calendar.getTimeInMillis();

        setAlarmManagerReminder(appContext, alarmManager, triggerAtMillis, pendingIntent);
        scheduleWorkFallback(appContext, slot, triggerAtMillis);
        Log.d(TAG, "Scheduled motivation reminder at " + triggerAtMillis + " (" + String.format(java.util.Locale.US, "%02d:%02d", hour, minute) + ")");
    }

    private static void setAlarmManagerReminder(Context context, AlarmManager alarmManager, long triggerAtMillis, PendingIntent pendingIntent) {
        if (alarmManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }

    private static void scheduleWorkFallback(Context appContext, int slot, long triggerAtMillis) {
        Data data = new Data.Builder().putInt(EXTRA_SLOT, slot).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MotivationReminderWorker.class)
                .setInputData(data)
                .setInitialDelay(Math.max(0L, triggerAtMillis - System.currentTimeMillis()), TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(appContext).enqueueUniqueWork(UNIQUE_WORK_DAILY, ExistingWorkPolicy.REPLACE, request);
    }
}
