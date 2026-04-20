package com.hcmute.edu.vn.focus_life.ui.running;

public class RunningSessionSnapshot {
    public final boolean sessionStarted;
    public final boolean paused;
    public final boolean gpsLocked;
    public final long durationMillis;
    public final float distanceMeters;
    public final int steps;
    public final float calories;
    public final float avgSpeedKmh;
    public final float paceSecondsPerKm;
    public final int routePointCount;

    public RunningSessionSnapshot(
            boolean sessionStarted,
            boolean paused,
            boolean gpsLocked,
            long durationMillis,
            float distanceMeters,
            int steps,
            float calories,
            float avgSpeedKmh,
            float paceSecondsPerKm,
            int routePointCount
    ) {
        this.sessionStarted = sessionStarted;
        this.paused = paused;
        this.gpsLocked = gpsLocked;
        this.durationMillis = durationMillis;
        this.distanceMeters = distanceMeters;
        this.steps = steps;
        this.calories = calories;
        this.avgSpeedKmh = avgSpeedKmh;
        this.paceSecondsPerKm = paceSecondsPerKm;
        this.routePointCount = routePointCount;
    }

    public static RunningSessionSnapshot idle() {
        return new RunningSessionSnapshot(
                false,
                false,
                false,
                0L,
                0f,
                0,
                0f,
                0f,
                0f,
                0
        );
    }
}