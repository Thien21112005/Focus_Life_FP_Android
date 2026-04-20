package com.hcmute.edu.vn.focus_life.core.session;

import android.content.Context;
import android.content.SharedPreferences;

public class OnboardingPreferences {
    private static final String PREF_NAME = "onboarding_pref";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PRIMARY_GOAL = "primary_goal";

    private final SharedPreferences preferences;

    public OnboardingPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isCompleted() {
        return preferences.getBoolean(KEY_COMPLETED, false);
    }

    public void setCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_COMPLETED, completed).apply();
    }

    public void setDisplayName(String displayName) {
        preferences.edit().putString(KEY_DISPLAY_NAME, displayName).apply();
    }

    public String getDisplayName() {
        return preferences.getString(KEY_DISPLAY_NAME, "Minh");
    }

    public void setPrimaryGoal(String goal) {
        preferences.edit().putString(KEY_PRIMARY_GOAL, goal).apply();
    }

    public String getPrimaryGoal() {
        return preferences.getString(KEY_PRIMARY_GOAL, "Deep Work");
    }
}
