package com.hcmute.edu.vn.focus_life.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

public class OnboardingPreferences {
    private static final String PREF_NAME = "onboarding_pref";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PRIMARY_GOAL = "primary_goal";
    private static final String KEY_SELECTED_GOALS = "selected_goals";

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
        return preferences.getString(KEY_DISPLAY_NAME, "");
    }

    public void setPrimaryGoal(String goal) {
        preferences.edit().putString(KEY_PRIMARY_GOAL, goal).apply();
    }

    public String getPrimaryGoal() {
        return preferences.getString(KEY_PRIMARY_GOAL, "Deep Work");
    }

    public void setSelectedGoals(Set<String> goals) {
        LinkedHashSet<String> safeGoals = new LinkedHashSet<>();
        if (goals != null) {
            for (String goal : goals) {
                if (goal != null && !goal.trim().isEmpty()) {
                    safeGoals.add(goal);
                }
            }
        }
        preferences.edit().putStringSet(KEY_SELECTED_GOALS, safeGoals).apply();
    }

    public Set<String> getSelectedGoals() {
        Set<String> stored = preferences.getStringSet(KEY_SELECTED_GOALS, null);
        return stored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stored);
    }

    public String joinGoals(Set<String> goals) {
        if (goals == null || goals.isEmpty()) {
            return "Deep Work";
        }

        StringBuilder builder = new StringBuilder();
        for (String goal : goals) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(goal);
        }
        return builder.toString();
    }
}
