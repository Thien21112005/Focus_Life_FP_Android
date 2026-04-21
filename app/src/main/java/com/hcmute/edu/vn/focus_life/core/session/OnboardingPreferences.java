package com.hcmute.edu.vn.focus_life.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class OnboardingPreferences {
    private static final String PREF_NAME = "onboarding_pref";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PRIMARY_GOAL = "primary_goal";
    private static final String KEY_SELECTED_GOALS = "selected_goals";
    private static final String KEY_PENDING_AVATAR_URI = "pending_avatar_uri";
    private static final String KEY_AVATAR_URL = "avatar_url";

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

    public void setSelectedGoals(Set<String> goals) {
        Set<String> safeGoals = goals == null ? new HashSet<>() : new HashSet<>(goals);
        preferences.edit().putStringSet(KEY_SELECTED_GOALS, safeGoals).apply();
        if (!safeGoals.isEmpty()) {
            setPrimaryGoal(joinGoals(safeGoals));
        }
    }

    public Set<String> getSelectedGoals() {
        Set<String> goals = preferences.getStringSet(KEY_SELECTED_GOALS, null);
        if (goals == null || goals.isEmpty()) {
            Set<String> fallback = new HashSet<>();
            fallback.add(getPrimaryGoal());
            return fallback;
        }
        return new HashSet<>(goals);
    }

    public String joinGoals(Set<String> goals) {
        if (goals == null || goals.isEmpty()) {
            return "Deep Work";
        }
        return TextUtils.join(", ", goals);
    }

    public void setPendingAvatarUri(String avatarUri) {
        preferences.edit().putString(KEY_PENDING_AVATAR_URI, avatarUri).apply();
    }

    public String getPendingAvatarUri() {
        return preferences.getString(KEY_PENDING_AVATAR_URI, "");
    }

    public void setAvatarUrl(String avatarUrl) {
        preferences.edit().putString(KEY_AVATAR_URL, avatarUrl).apply();
    }

    public String getAvatarUrl() {
        return preferences.getString(KEY_AVATAR_URL, "");
    }

    public String getBestAvatarSource() {
        String remoteUrl = getAvatarUrl();
        if (remoteUrl != null && !remoteUrl.trim().isEmpty()) {
            return remoteUrl;
        }
        String localUri = getPendingAvatarUri();
        return localUri != null ? localUri : "";
    }

    public boolean hasPendingProfile() {
        return !getDisplayName().trim().isEmpty() || !getBestAvatarSource().trim().isEmpty();
    }

    public void clearPendingAvatarUri() {
        preferences.edit().remove(KEY_PENDING_AVATAR_URI).apply();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
