package com.hcmute.edu.vn.focus_life.core.exception;

import android.util.Log;

public final class AppExceptionLogger {

    private static final String TAG = "FocusLifeError";

    private AppExceptionLogger() {
    }

    public static void log(String scope, Exception e) {
        if (e == null) {
            Log.e(TAG, "[" + scope + "] unknown error");
            return;
        }
        Log.e(TAG, "[" + scope + "] " + e.getMessage(), e);
    }
}
