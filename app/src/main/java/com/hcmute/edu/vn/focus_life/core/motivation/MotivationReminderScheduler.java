package com.hcmute.edu.vn.focus_life.core.motivation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hcmute.edu.vn.focus_life.receiver.MotivationReminderReceiver;

import java.util.Calendar;

public final class MotivationReminderScheduler {
    public static final String ACTION_MOTIVATION_REMINDER = "com.hcmute.edu.vn.focus_life.action.MOTIVATION_REMINDER";
    public static final String ACTION_BOOT_RESCHEDULE = "com.hcmute.edu.vn.focus_life.action.MOTIVATION_BOOT_RESCHEDULE";
    public static final String ACTION_UNLOCK_REMINDER = "com.hcmute.edu.vn.focus_life.action.MOTIVATION_UNLOCK_REMINDER";
    public static final String EXTRA_SLOT = "extra_slot";

    private static final int REQUEST_DAILY = 8300;

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
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        alarmManager.cancel(createPendingIntent(context.getApplicationContext()));
    }

    private static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, MotivationReminderReceiver.class);
        intent.setAction(ACTION_MOTIVATION_REMINDER);
        intent.putExtra(EXTRA_SLOT, 0);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void scheduleSlot(Context context, int slot, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = createPendingIntent(context);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
}
