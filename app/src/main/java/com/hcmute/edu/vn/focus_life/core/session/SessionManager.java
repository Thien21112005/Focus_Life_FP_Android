package com.hcmute.edu.vn.focus_life.core.session;

public class SessionManager {
    private final AuthStatePreferences authStatePreferences;

    public SessionManager(AuthStatePreferences authStatePreferences) {
        this.authStatePreferences = authStatePreferences;
    }

    public boolean isUserLoggedIn() {
        return authStatePreferences.isLoggedIn();
    }

    public void onLoginSuccess(String uid, String email) {
        authStatePreferences.saveSession(uid, email);
    }

    public void logout() {
        authStatePreferences.clearSession();
    }

    public String requireUid() {
        return authStatePreferences.getUid();
    }
}

