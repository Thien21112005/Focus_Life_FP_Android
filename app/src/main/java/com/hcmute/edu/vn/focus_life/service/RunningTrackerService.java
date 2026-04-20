package com.hcmute.edu.vn.focus_life.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.common.AppExecutors;
import com.hcmute.edu.vn.focus_life.core.utils.DateUtils;
import com.hcmute.edu.vn.focus_life.data.local.entity.StepRecordEntity;
import com.hcmute.edu.vn.focus_life.ui.running.RunningMapActivity;
import com.hcmute.edu.vn.focus_life.ui.running.RunningSessionSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RunningTrackerService extends Service implements SensorEventListener {

    public static final String ACTION_START_OR_RESUME = "com.hcmute.edu.vn.focus_life.action.START_OR_RESUME";
    public static final String ACTION_PAUSE = "com.hcmute.edu.vn.focus_life.action.PAUSE";
    public static final String ACTION_STOP = "com.hcmute.edu.vn.focus_life.action.STOP";

    private static final String CHANNEL_ID = "running_tracker_channel";
    private static final int NOTIFICATION_ID = 4102;

    private static final long LOCATION_INTERVAL_MS = 1500L;
    private static final float MIN_DISTANCE_TO_ACCEPT_METERS = 2.5f;
    private static final float MAX_DISTANCE_SPIKE_METERS = 120f;
    private static final float MAX_ACCEPTED_ACCURACY_METERS = 35f;

    private final IBinder binder = new LocalBinder();

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private boolean isLocationUpdatesActive = false;

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {
            for (Location location : result.getLocations()) {
                onLocationUpdate(location);
            }
        }
    };

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private boolean sensorRegistered = false;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private boolean isSessionRunning = false;
    private boolean isPaused = false;
    private boolean isStoppingAfterSync = false;

    private long runStartedAtElapsed = 0L;
    private long pauseStartedAtElapsed = 0L;
    private long pausedDurationMillis = 0L;
    private long finishedDurationMillis = 0L;

    private float totalDistanceMeters = 0f;
    private int sessionSteps = 0;
    private int sessionStepsOffset = 0;
    private float stepCounterBase = -1f;
    private float estimatedCalories = 0f;

    private Location lastKnownLocation;
    private Location lastAcceptedLocation;
    private final List<Location> routePoints = new ArrayList<>();

    private String syncStatus = "Chưa đồng bộ Firebase";

    public class LocalBinder extends Binder {
        public RunningTrackerService getService() {
            return RunningTrackerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(1000L)
                .setWaitForAccurateLocation(false)
                .build();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_PAUSE.equals(action)) {
            pauseRun();
        } else if (ACTION_STOP.equals(action)) {
            finishRunAndStopService();
        } else {
            startOrResumeRun();
        }

        return START_STICKY;
    }

    public synchronized RunningSessionSnapshot getSnapshot() {
        if (!isSessionRunning && finishedDurationMillis == 0L && totalDistanceMeters == 0f && sessionSteps == 0) {
            return RunningSessionSnapshot.idle();
        }

        long activeMillis = getActiveDurationMillis();
        float avgSpeed = calculateAverageSpeed(totalDistanceMeters, activeMillis);
        float paceSeconds = calculatePaceSecondsPerKm(totalDistanceMeters, activeMillis);

        return new RunningSessionSnapshot(
                isSessionRunning || finishedDurationMillis > 0L,
                isPaused,
                lastKnownLocation != null,
                activeMillis,
                totalDistanceMeters,
                sessionSteps,
                estimatedCalories,
                avgSpeed,
                paceSeconds,
                routePoints.size()
        );
    }

    public synchronized List<Location> getRoutePointsSnapshot() {
        List<Location> copy = new ArrayList<>();
        for (Location location : routePoints) {
            copy.add(new Location(location));
        }
        return copy;
    }

    public synchronized Location getLastKnownLocationSnapshot() {
        return lastKnownLocation == null ? null : new Location(lastKnownLocation);
    }

    public synchronized String getSyncStatus() {
        return syncStatus;
    }

    private synchronized void startOrResumeRun() {
        if (!hasRequiredPermissions()) {
            syncStatus = "Thiếu quyền vị trí hoặc nhận diện hoạt động";
            updateNotification();
            return;
        }

        if (!isSessionRunning) {
            resetSessionData();
            isSessionRunning = true;
            isPaused = false;
            runStartedAtElapsed = SystemClock.elapsedRealtime();
            pausedDurationMillis = 0L;
            finishedDurationMillis = 0L;
            syncStatus = "Phiên chạy chưa kết thúc";
        } else if (isPaused) {
            pausedDurationMillis += SystemClock.elapsedRealtime() - pauseStartedAtElapsed;
            pauseStartedAtElapsed = 0L;
            isPaused = false;
            sessionStepsOffset = sessionSteps;
            stepCounterBase = -1f;
            syncStatus = "Đang tiếp tục phiên chạy";
        }

        startForegroundInternal();
        registerStepSensorIfSupported();
        ensureLocationUpdatesActive();
        updateNotification();
    }

    private synchronized void pauseRun() {
        if (!isSessionRunning || isPaused) return;

        isPaused = true;
        pauseStartedAtElapsed = SystemClock.elapsedRealtime();
        sessionStepsOffset = sessionSteps;
        stepCounterBase = -1f;

        unregisterStepSensor();
        stopLocationUpdatesIfNeeded();

        syncStatus = "Phiên chạy đang tạm dừng";
        updateNotification();
    }

    private synchronized void finishRunAndStopService() {
        if (!isSessionRunning || isStoppingAfterSync) {
            return;
        }

        if (isPaused) {
            pausedDurationMillis += SystemClock.elapsedRealtime() - pauseStartedAtElapsed;
            pauseStartedAtElapsed = 0L;
        }

        finishedDurationMillis = getActiveDurationMillis();
        isSessionRunning = false;
        isPaused = false;
        isStoppingAfterSync = true;

        unregisterStepSensor();
        stopLocationUpdatesIfNeeded();
        recalculateCalories();

        syncStatus = "Đang lưu local...";
        updateNotification();

        persistRunToLocalAndFirebase();
    }

    private void startForegroundInternal() {
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void updateNotification() {
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification());
        } catch (Exception ignored) {
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, RunningMapActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                900,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent pauseIntent = new Intent(this, RunningTrackerService.class);
        pauseIntent.setAction(isPaused ? ACTION_START_OR_RESUME : ACTION_PAUSE);

        PendingIntent pausePendingIntent = PendingIntent.getService(
                this,
                901,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, RunningTrackerService.class);
        stopIntent.setAction(ACTION_STOP);

        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                902,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title;
        if (isStoppingAfterSync) {
            title = "Đang lưu phiên chạy";
        } else if (isPaused) {
            title = "Phiên chạy đang tạm dừng";
        } else {
            title = "Đang ghi phiên chạy";
        }

        String detailText = formatDistance(totalDistanceMeters)
                + " • " + formatDuration(getActiveDurationMillis())
                + " • " + sessionSteps + " bước";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(detailText)
                .setSubText(syncStatus)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(!isPaused && !isStoppingAfterSync);

        if (!isStoppingAfterSync) {
            builder.addAction(
                    android.R.drawable.ic_media_pause,
                    isPaused ? "Tiếp tục" : "Tạm dừng",
                    pausePendingIntent
            );
            builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Kết thúc",
                    stopPendingIntent
            );
        }

        return builder.build();
    }

    @SuppressWarnings("MissingPermission")
    private void ensureLocationUpdatesActive() {
        if (!hasRequiredPermissions()) return;
        if (isLocationUpdatesActive) return;

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                onLocationUpdate(location);
            }
        });

        isLocationUpdatesActive = true;
    }

    private void stopLocationUpdatesIfNeeded() {
        if (!isLocationUpdatesActive) return;

        fusedLocationClient.removeLocationUpdates(locationCallback);
        isLocationUpdatesActive = false;
    }

    private synchronized void onLocationUpdate(@Nullable Location location) {
        if (location == null) return;

        lastKnownLocation = new Location(location);

        if (!isSessionRunning || isPaused) {
            updateNotification();
            return;
        }

        if (location.hasAccuracy() && location.getAccuracy() > MAX_ACCEPTED_ACCURACY_METERS) {
            return;
        }

        if (lastAcceptedLocation == null) {
            lastAcceptedLocation = new Location(location);
            routePoints.add(new Location(location));
            updateNotification();
            return;
        }

        float delta = lastAcceptedLocation.distanceTo(location);
        if (delta < MIN_DISTANCE_TO_ACCEPT_METERS || delta > MAX_DISTANCE_SPIKE_METERS) {
            return;
        }

        totalDistanceMeters += delta;
        lastAcceptedLocation = new Location(location);
        routePoints.add(new Location(location));
        recalculateCalories();
        updateNotification();
    }

    private void registerStepSensorIfSupported() {
        if (sensorManager != null && stepCounterSensor != null && !sensorRegistered) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            sensorRegistered = true;
        }
    }

    private void unregisterStepSensor() {
        if (sensorManager != null && sensorRegistered) {
            sensorManager.unregisterListener(this);
            sensorRegistered = false;
        }
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
        if (!isSessionRunning || isPaused) return;

        float rawValue = event.values[0];

        if (stepCounterBase < 0f) {
            stepCounterBase = rawValue;
            sessionSteps = sessionStepsOffset;
        } else {
            int deltaSteps = Math.max(0, Math.round(rawValue - stepCounterBase));
            sessionSteps = sessionStepsOffset + deltaSteps;
        }

        recalculateCalories();
        updateNotification();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean hasRequiredPermissions() {
        boolean hasFineLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean hasActivityRecognition = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED;

        return hasFineLocation && hasActivityRecognition;
    }

    private synchronized void persistRunToLocalAndFirebase() {
        if (sessionSteps <= 0 && totalDistanceMeters < 30f) {
            syncStatus = "Phiên chạy quá ngắn, chưa lưu";
            updateNotification();
            finishServiceSafely();
            return;
        }

        final long now = System.currentTimeMillis();
        final StepRecordEntity entity = new StepRecordEntity();
        entity.date = DateUtils.todayKey();
        entity.timestamp = now;
        entity.steps = sessionSteps;
        entity.calories = estimatedCalories;
        entity.source = "running_foreground_service";
        entity.synced = false;

        final float distanceMeters = totalDistanceMeters;
        final long durationMillis = finishedDurationMillis;
        final float calories = estimatedCalories;
        final int steps = sessionSteps;
        final int routePointCount = routePoints.size();

        new AppExecutors().diskIO().execute(() -> {
            try {
                FocusLifeApp.getInstance().getDatabase().stepDao().insert(entity);
            } catch (Exception ignored) {
            }

            syncStatus = "Đã lưu local, đang đồng bộ Firebase...";
            updateNotification();

            try {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    syncStatus = "Đã lưu local, chưa sync Firebase vì chưa đăng nhập";
                    updateNotification();
                    finishServiceSafely();
                    return;
                }

                double avgSpeedKmh = calculateAverageSpeed(distanceMeters, durationMillis);

                Map<String, Object> data = new HashMap<>();
                data.put("date", entity.date);
                data.put("timestamp", entity.timestamp);
                data.put("steps", steps);
                data.put("calories", calories);
                data.put("source", entity.source);
                data.put("distanceMeters", distanceMeters);
                data.put("distanceKm", distanceMeters / 1000f);
                data.put("durationMillis", durationMillis);
                data.put("durationText", formatDuration(durationMillis));
                data.put("paceText", formatPace(distanceMeters, durationMillis));
                data.put("avgSpeedKmh", avgSpeedKmh);
                data.put("routePointCount", routePointCount);

                firestore.collection("users")
                        .document(user.getUid())
                        .collection("step_records")
                        .document("run_" + now)
                        .set(data)
                        .addOnSuccessListener(unused -> {
                            syncStatus = "Đã đồng bộ Firebase";
                            updateNotification();
                            finishServiceSafely();
                        })
                        .addOnFailureListener(e -> {
                            syncStatus = "Lưu local OK, sync Firebase lỗi";
                            updateNotification();
                            finishServiceSafely();
                        });

            } catch (Exception ignored) {
                syncStatus = "Lưu local OK, sync Firebase lỗi";
                updateNotification();
                finishServiceSafely();
            }
        });
    }

    private void finishServiceSafely() {
        stopForeground(true);
        stopSelf();
    }

    private synchronized void resetSessionData() {
        totalDistanceMeters = 0f;
        sessionSteps = 0;
        sessionStepsOffset = 0;
        stepCounterBase = -1f;
        estimatedCalories = 0f;
        lastKnownLocation = null;
        lastAcceptedLocation = null;
        routePoints.clear();
        runStartedAtElapsed = 0L;
        pauseStartedAtElapsed = 0L;
        pausedDurationMillis = 0L;
        finishedDurationMillis = 0L;
        isStoppingAfterSync = false;
    }

    private synchronized long getActiveDurationMillis() {
        if (runStartedAtElapsed == 0L) {
            return finishedDurationMillis;
        }

        if (!isSessionRunning) {
            return finishedDurationMillis;
        }

        long now = isPaused ? pauseStartedAtElapsed : SystemClock.elapsedRealtime();
        long active = now - runStartedAtElapsed - pausedDurationMillis;
        return Math.max(active, 0L);
    }

    private synchronized void recalculateCalories() {
        float distanceKm = totalDistanceMeters / 1000f;
        estimatedCalories = (distanceKm * 62f) + (sessionSteps * 0.02f);
    }

    private float calculateAverageSpeed(float meters, long activeMillis) {
        if (meters < 1f || activeMillis <= 0L) return 0f;
        double kmPerHour = (meters / 1000d) / (activeMillis / 3600000d);
        return (float) kmPerHour;
    }

    private float calculatePaceSecondsPerKm(float meters, long activeMillis) {
        if (meters < 20f || activeMillis <= 0L) return 0f;
        return (float) ((activeMillis / 1000d) / (meters / 1000d));
    }

    private String formatDistance(float meters) {
        return String.format(Locale.getDefault(), "%.2f km", meters / 1000f);
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private String formatPace(float meters, long activeMillis) {
        if (meters < 20f || activeMillis <= 0L) {
            return "--'--\"";
        }
        double secondsPerKm = (activeMillis / 1000d) / (meters / 1000d);
        long minutes = (long) (secondsPerKm / 60d);
        long seconds = Math.round(secondsPerKm - (minutes * 60d));
        if (seconds == 60) {
            minutes += 1;
            seconds = 0;
        }
        return String.format(Locale.getDefault(), "%d'%02d\"", minutes, seconds);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Running Tracker",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Theo dõi phiên chạy nền");
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        unregisterStepSensor();
        stopLocationUpdatesIfNeeded();
        super.onDestroy();
    }
}