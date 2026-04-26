package com.hcmute.edu.vn.focus_life.core.usage;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;

import com.hcmute.edu.vn.focus_life.data.repository.HealthMetricsRepository;

public class AppUsageTracker implements Application.ActivityLifecycleCallbacks {
    private int startedActivityCount = 0;
    private long foregroundStartedAt = 0L;
    private final HealthMetricsRepository repository = new HealthMetricsRepository();

    public void register(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        startedActivityCount++;
        if (startedActivityCount == 1) {
            foregroundStartedAt = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedActivityCount = Math.max(0, startedActivityCount - 1);
        if (startedActivityCount == 0 && foregroundStartedAt > 0L) {
            long activeMillis = SystemClock.elapsedRealtime() - foregroundStartedAt;
            foregroundStartedAt = 0L;
            repository.recordAppUsage(activeMillis);
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
