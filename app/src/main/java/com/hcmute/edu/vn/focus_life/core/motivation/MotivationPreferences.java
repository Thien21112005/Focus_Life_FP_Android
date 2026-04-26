package com.hcmute.edu.vn.focus_life.core.motivation;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class MotivationPreferences {
    private static final String PREF_NAME = "motivation_preferences";
    private static final String KEY_ENABLED = "motivation_enabled";
    private static final String KEY_HOUR = "motivation_hour";
    private static final String KEY_MINUTE = "motivation_minute";
    private static final String KEY_QUOTE_CURSOR = "motivation_quote_cursor";

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
}
