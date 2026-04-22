package com.hcmute.edu.vn.focus_life.ui.focus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class PomodoroPreferences {
    private static final String PREF_NAME = "pomodoro_pref";
    private static final String KEY_FOCUS_MINUTES = "focus_minutes";
    private static final String KEY_SHORT_BREAK_MINUTES = "short_break_minutes";
    private static final String KEY_LONG_BREAK_MINUTES = "long_break_minutes";
    private static final String KEY_CYCLES_UNTIL_LONG_BREAK = "cycles_until_long_break";
    private static final String KEY_AUTO_DND = "auto_dnd";

    private final SharedPreferences preferences;

    public PomodoroPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public Config getConfig() {
        Config config = new Config();
        config.focusMinutes = preferences.getInt(KEY_FOCUS_MINUTES, 25);
        config.shortBreakMinutes = preferences.getInt(KEY_SHORT_BREAK_MINUTES, 5);
        config.longBreakMinutes = preferences.getInt(KEY_LONG_BREAK_MINUTES, 15);
        config.cyclesUntilLongBreak = preferences.getInt(KEY_CYCLES_UNTIL_LONG_BREAK, 4);
        config.autoDnd = preferences.getBoolean(KEY_AUTO_DND, true);
        return config;
    }

    public void save(@NonNull Config config) {
        preferences.edit()
                .putInt(KEY_FOCUS_MINUTES, config.focusMinutes)
                .putInt(KEY_SHORT_BREAK_MINUTES, config.shortBreakMinutes)
                .putInt(KEY_LONG_BREAK_MINUTES, config.longBreakMinutes)
                .putInt(KEY_CYCLES_UNTIL_LONG_BREAK, config.cyclesUntilLongBreak)
                .putBoolean(KEY_AUTO_DND, config.autoDnd)
                .apply();
    }

    public static class Config {
        public int focusMinutes;
        public int shortBreakMinutes;
        public int longBreakMinutes;
        public int cyclesUntilLongBreak;
        public boolean autoDnd;
    }
}