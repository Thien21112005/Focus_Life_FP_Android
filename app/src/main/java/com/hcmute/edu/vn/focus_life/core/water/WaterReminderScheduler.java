package com.hcmute.edu.vn.focus_life.core.water;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.hcmute.edu.vn.focus_life.receiver.WaterReminderReceiver;
import com.hcmute.edu.vn.focus_life.worker.WaterReminderWorker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WaterReminderScheduler {
    private static final String PREFS = "focuslife_water_reminder";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_START_HOUR = "start_hour";
    public static final String KEY_END_HOUR = "end_hour";
    public static final String KEY_INTERVAL_HOURS = "interval_hours";
    public static final String KEY_TARGET_GLASSES = "target_glasses";
    public static final String KEY_LOCKED_DATE = "locked_date";
    public static final String EXTRA_CUP_INDEX = "extra_cup_index";
    public static final String EXTRA_CUP_TOTAL = "extra_cup_total";
    public static final String EXTRA_HOUR = "extra_hour";
    public static final String EXTRA_MINUTE = "extra_minute";
    public static final String ACTION_REMIND = "com.hcmute.edu.vn.focus_life.action.WATER_REMIND";
    public static final String ACTION_RESCHEDULE = "com.hcmute.edu.vn.focus_life.action.WATER_RESCHEDULE";
    public static final String ACTION_ACTIVATE_DEFAULT = "com.hcmute.edu.vn.focus_life.action.WATER_ACTIVATE_DEFAULT";

    private static final String TAG = "WaterAlarm";
    private static final int BASE_REQUEST_CODE = 7100;
    private static final int DEFAULT_START_HOUR = 8;
    private static final int DEFAULT_END_HOUR = 22;
    private static final int DEFAULT_TARGET_GLASSES = 8;
    private static final String WORK_PREFIX = "focuslife_water_reminder_cup_";

    /**
     * Backward compatible API. Older code passed intervalHours here, but the new UX uses a fixed
     * cup-by-cup schedule. Keep this method so old callers still compile, then schedule 8 cups.
     */
    public static void schedule(Context context, int startHour, int endHour, int intervalHours) {
        scheduleDailyCups(context, startHour, endHour, DEFAULT_TARGET_GLASSES);
    }

    public static void scheduleDailyCups(Context context, int startHour, int endHour, int targetGlasses) {
        Context appContext = context.getApplicationContext();
        startHour = clamp(startHour, 0, 23, DEFAULT_START_HOUR);
        endHour = clamp(endHour, 0, 23, DEFAULT_END_HOUR);
        targetGlasses = DEFAULT_TARGET_GLASSES;

        prefs(appContext).edit()
                .putBoolean(KEY_ENABLED, true)
                .putInt(KEY_START_HOUR, startHour)
                .putInt(KEY_END_HOUR, endHour)
                .putInt(KEY_TARGET_GLASSES, targetGlasses)
                .putInt(KEY_INTERVAL_HOURS, Math.max(1, Math.round((endHour - startHour) / Math.max(1f, targetGlasses - 1f))))
                .apply();

        cancelAlarmsOnly(appContext);
        scheduleDefaultActivationAlarm(appContext);
        List<Slot> slots = buildSchedule(startHour, endHour, targetGlasses);
        for (Slot slot : slots) {
            scheduleOne(appContext, slot, nextTriggerMillis(slot.hour, slot.minute));
        }
        Log.d(TAG, "Scheduled water reminders " + startHour + ":00-" + endHour + ":00");
    }

    public static void ensureDailyDefaultIfNeeded(Context context) {
        Context appContext = context.getApplicationContext();
        String today = todayKey();
        Calendar now = Calendar.getInstance();
        boolean afterDefaultStart = now.get(Calendar.HOUR_OF_DAY) >= DEFAULT_START_HOUR;
        if (afterDefaultStart && !today.equals(prefs(appContext).getString(KEY_LOCKED_DATE, ""))) {
            prefs(appContext).edit().putString(KEY_LOCKED_DATE, today).apply();
            if (!isEnabled(appContext)) {
                scheduleDailyCups(appContext, DEFAULT_START_HOUR, DEFAULT_END_HOUR, DEFAULT_TARGET_GLASSES);
            } else {
                rescheduleSavedPlan(appContext);
            }
        } else if (isEnabled(appContext)) {
            rescheduleSavedPlan(appContext);
        } else {
            scheduleDefaultActivationAlarm(appContext);
        }
    }

    public static boolean isLockedForToday(Context context) {
        return todayKey().equals(prefs(context).getString(KEY_LOCKED_DATE, ""));
    }

    public static void lockForToday(Context context) {
        prefs(context).edit().putString(KEY_LOCKED_DATE, todayKey()).apply();
    }

    public static void rescheduleSavedPlan(Context context) {
        if (!isEnabled(context)) return;
        scheduleDailyCups(context, startHour(context), endHour(context), targetGlasses(context));
    }

    public static void scheduleNextDayForCup(Context context, int cupIndex, int cupTotal, int hour, int minute) {
        if (!isEnabled(context)) return;
        Slot slot = new Slot(cupIndex, Math.max(1, cupTotal), clamp(hour, 0, 23, DEFAULT_START_HOUR), clamp(minute, 0, 59, 0));
        Calendar trigger = Calendar.getInstance();
        trigger.add(Calendar.DAY_OF_YEAR, 1);
        trigger.set(Calendar.HOUR_OF_DAY, slot.hour);
        trigger.set(Calendar.MINUTE, slot.minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        scheduleOne(context.getApplicationContext(), slot, trigger.getTimeInMillis());
    }

    public static void cancel(Context context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, false).apply();
        cancelAlarmsOnly(context.getApplicationContext());
    }

    private static void cancelAlarmsOnly(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            for (int i = 1; i <= 12; i++) {
                manager.cancel(pendingIntent(appContext, i, i, DEFAULT_TARGET_GLASSES, DEFAULT_START_HOUR, 0));
            }
            manager.cancel(defaultActivationPendingIntent(appContext));
        }
        WorkManager workManager = WorkManager.getInstance(appContext);
        for (int i = 1; i <= 12; i++) {
            workManager.cancelUniqueWork(workName(i));
        }
        workManager.cancelUniqueWork("focuslife_water_default_activation");
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static int startHour(Context context) {
        return prefs(context).getInt(KEY_START_HOUR, DEFAULT_START_HOUR);
    }

    public static int endHour(Context context) {
        return prefs(context).getInt(KEY_END_HOUR, DEFAULT_END_HOUR);
    }

    public static int intervalHours(Context context) {
        return prefs(context).getInt(KEY_INTERVAL_HOURS, 2);
    }

    public static int targetGlasses(Context context) {
        return prefs(context).getInt(KEY_TARGET_GLASSES, DEFAULT_TARGET_GLASSES);
    }

    public static boolean shouldNotifyNow(Context context) {
        if (!isEnabled(context)) return false;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int start = startHour(context);
        int end = endHour(context);
        if (start <= end) return hour >= start && hour <= end;
        return hour >= start || hour <= end;
    }

    public static String getScheduleSummary(Context context) {
        return getScheduleSummary(startHour(context), endHour(context), targetGlasses(context));
    }

    public static String getScheduleSummary(int startHour, int endHour, int targetGlasses) {
        List<Slot> slots = buildSchedule(
                clamp(startHour, 0, 23, DEFAULT_START_HOUR),
                clamp(endHour, 0, 23, DEFAULT_END_HOUR),
                DEFAULT_TARGET_GLASSES
        );
        StringBuilder builder = new StringBuilder();
        for (Slot slot : slots) {
            if (builder.length() > 0) builder.append("  •  ");
            builder.append("Ly ").append(slot.cupIndex).append(": ").append(formatTime(slot.hour, slot.minute));
        }
        return builder.toString();
    }

    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static void markCupLogged(Context context, int cupIndex) {
        prefs(context).edit().putBoolean("logged_" + todayKey() + "_" + cupIndex, true).apply();
    }

    public static boolean isCupLogged(Context context, int cupIndex) {
        return prefs(context).getBoolean("logged_" + todayKey() + "_" + cupIndex, false);
    }

    public static void markCupNotified(Context context, int cupIndex) {
        prefs(context).edit().putBoolean("notified_" + todayKey() + "_" + cupIndex, true).apply();
    }

    public static boolean isCupNotified(Context context, int cupIndex) {
        return prefs(context).getBoolean("notified_" + todayKey() + "_" + cupIndex, false);
    }

    private static List<Slot> buildSchedule(int startHour, int endHour, int targetGlasses) {
        List<Slot> slots = new ArrayList<>();
        targetGlasses = DEFAULT_TARGET_GLASSES;
        int startMinutes = startHour * 60;
        int endMinutes = endHour * 60;
        if (endMinutes <= startMinutes) endMinutes = startMinutes + 14 * 60;
        int totalRange = Math.max(0, endMinutes - startMinutes);
        for (int i = 0; i < targetGlasses; i++) {
            int minuteOfDay = targetGlasses == 1
                    ? startMinutes
                    : startMinutes + Math.round(totalRange * (i / (float) (targetGlasses - 1)));
            minuteOfDay = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60);
            slots.add(new Slot(i + 1, targetGlasses, minuteOfDay / 60, minuteOfDay % 60));
        }
        return slots;
    }

    private static void scheduleOne(Context context, Slot slot, long triggerAtMillis) {
        Context appContext = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pendingIntent(appContext, slot.cupIndex, slot.cupIndex, slot.cupTotal, slot.hour, slot.minute);
        setAlarmManagerReminder(appContext, manager, triggerAtMillis, pi);
        scheduleWorkFallback(appContext, slot, triggerAtMillis);
        Log.d(TAG, "Scheduled cup " + slot.cupIndex + " at " + triggerAtMillis + " (" + formatTime(slot.hour, slot.minute) + ")");
    }

    private static void setAlarmManagerReminder(Context context, AlarmManager manager, long triggerAtMillis, PendingIntent pi) {
        if (manager == null) return;
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

    private static void scheduleWorkFallback(Context context, Slot slot, long triggerAtMillis) {
        long delayMillis = Math.max(0L, triggerAtMillis - System.currentTimeMillis());
        Data data = new Data.Builder()
                .putInt(EXTRA_CUP_INDEX, slot.cupIndex)
                .putInt(EXTRA_CUP_TOTAL, slot.cupTotal)
                .putInt(EXTRA_HOUR, slot.hour)
                .putInt(EXTRA_MINUTE, slot.minute)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WaterReminderWorker.class)
                .setInputData(data)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(workName(slot.cupIndex), ExistingWorkPolicy.REPLACE, request);
    }

    private static void scheduleDefaultActivationAlarm(Context context) {
        Context appContext = context.getApplicationContext();
        long triggerAtMillis = nextTriggerMillis(DEFAULT_START_HOUR, 0);
        PendingIntent pi = defaultActivationPendingIntent(appContext);
        AlarmManager manager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        setAlarmManagerReminder(appContext, manager, triggerAtMillis, pi);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WaterReminderWorker.class)
                .setInitialDelay(Math.max(0L, triggerAtMillis - System.currentTimeMillis()), TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder().putBoolean("activate_default", true).build())
                .build();
        WorkManager.getInstance(appContext).enqueueUniqueWork("focuslife_water_default_activation", ExistingWorkPolicy.REPLACE, request);
    }

    private static PendingIntent pendingIntent(Context context, int requestCode, int cupIndex, int cupTotal, int hour, int minute) {
        Intent intent = new Intent(context, WaterReminderReceiver.class);
        intent.setAction(ACTION_REMIND);
        intent.putExtra(EXTRA_CUP_INDEX, cupIndex);
        intent.putExtra(EXTRA_CUP_TOTAL, cupTotal);
        intent.putExtra(EXTRA_HOUR, hour);
        intent.putExtra(EXTRA_MINUTE, minute);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, BASE_REQUEST_CODE + requestCode, intent, flags);
    }

    private static PendingIntent defaultActivationPendingIntent(Context context) {
        Intent intent = new Intent(context, WaterReminderReceiver.class);
        intent.setAction(ACTION_ACTIVATE_DEFAULT);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, BASE_REQUEST_CODE + 99, intent, flags);
    }

    @NonNull
    private static String workName(int cupIndex) {
        return WORK_PREFIX + cupIndex;
    }

    private static long nextTriggerMillis(int hour, int minute) {
        Calendar trigger = Calendar.getInstance();
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        if (!trigger.after(Calendar.getInstance())) trigger.add(Calendar.DAY_OF_YEAR, 1);
        return trigger.getTimeInMillis();
    }

    private static String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int clamp(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }

    private static class Slot {
        final int cupIndex;
        final int cupTotal;
        final int hour;
        final int minute;

        Slot(int cupIndex, int cupTotal, int hour, int minute) {
            this.cupIndex = cupIndex;
            this.cupTotal = cupTotal;
            this.hour = hour;
            this.minute = minute;
        }
    }
}
