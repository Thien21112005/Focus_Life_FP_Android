package com.hcmute.edu.vn.focus_life;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.hcmute.edu.vn.focus_life.core.session.AuthStatePreferences;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.core.session.SettingsPreferences;
import com.hcmute.edu.vn.focus_life.core.utils.NotificationHelper;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.worker.SyncScheduler;

public class FocusLifeApp extends Application {
    private static FocusLifeApp instance;
    private AppDatabase database;
    private SessionManager sessionManager;
    private AuthStatePreferences authStatePreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        new SettingsPreferences(this).applyToAppCompat();
        FirebaseApp.initializeApp(this);
        database = AppDatabase.getInstance(this);
        authStatePreferences = new AuthStatePreferences(this);
        sessionManager = new SessionManager(authStatePreferences);
        NotificationHelper.createChannels(this);
        SyncScheduler.schedule(this);
    }

    public static FocusLifeApp getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
