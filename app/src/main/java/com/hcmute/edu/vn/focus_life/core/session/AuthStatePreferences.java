package com.hcmute.edu.vn.focus_life.core.session;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthStatePreferences {
    private static final String PREF_NAME = "session_pref";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences preferences;

    public AuthStatePreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String uid, String email) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_UID, uid)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUid() {
        return preferences.getString(KEY_UID, null);
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, null);
    }
}
