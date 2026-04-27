package com.hcmute.edu.vn.focus_life.core.motivation;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MotivationPreferences {
    private static final String PREF_NAME = "motivation_preferences";
    private static final String KEY_ENABLED = "motivation_enabled";
    private static final String KEY_HOUR = "motivation_hour";
    private static final String KEY_MINUTE = "motivation_minute";
    private static final String KEY_QUOTE_CURSOR = "motivation_quote_cursor";
    private static final String KEY_LAST_NOTIFIED_PREFIX = "last_notified_at_slot_";

    private final SharedPreferences preferences;

    public MotivationPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return preferences.getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public int getHour() {
        return preferences.getInt(KEY_HOUR, 7);
    }

    public int getMinute() {
        return preferences.getInt(KEY_MINUTE, 30);
    }

    public void setReminderTime(int hour, int minute) {
        if (hour < 0) hour = 0;
        if (hour > 23) hour = 23;
        if (minute < 0) minute = 0;
        if (minute > 59) minute = 59;
        preferences.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply();
    }

    public String getReminderTimeText() {
        return String.format(Locale.getDefault(), "%02d:%02d", getHour(), getMinute());
    }

    public int getQuoteCursor() {
        return preferences.getInt(KEY_QUOTE_CURSOR, 0);
    }

    public int nextQuoteCursor(int quoteCount) {
        int count = Math.max(1, quoteCount);
        int next = (getQuoteCursor() + 1) % count;
        preferences.edit().putInt(KEY_QUOTE_CURSOR, next).apply();
        return next;
    }

    public boolean wasRecentlyNotified(int slot, long duplicateWindowMillis) {
        long last = preferences.getLong(KEY_LAST_NOTIFIED_PREFIX + slot, 0L);
        return last > 0L && System.currentTimeMillis() - last < Math.max(1L, duplicateWindowMillis);
    }

    public void markSlotNotifiedNow(int slot) {
        preferences.edit().putLong(KEY_LAST_NOTIFIED_PREFIX + slot, System.currentTimeMillis()).apply();
    }

    // Kept for older code paths. New receiver uses the short duplicate window above so
    // changing test time within the same day is not blocked.
    public boolean wasDailySlotNotifiedToday(int slot) {
        return preferences.getBoolean("daily_notified_" + todayKey() + "_" + slot, false);
    }

    public void markDailySlotNotifiedToday(int slot) {
        preferences.edit().putBoolean("daily_notified_" + todayKey() + "_" + slot, true).apply();
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
