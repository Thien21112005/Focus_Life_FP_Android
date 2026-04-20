package com.hcmute.edu.vn.focus_life.ui.running;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.service.RunningTrackerService;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunningMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_RUNNING_PERMISSIONS = 2001;
    private static final double DEFAULT_ZOOM = 16.5;

    private static final String[] MAP_STYLES = new String[]{
            Style.OUTDOORS,
            Style.MAPBOX_STREETS,
            Style.SATELLITE_STREETS
    };

    private MapView mapView;
    private MapboxMap mapboxMap;

    private TextView btnBackRunning;
    private TextView btnStyleToggle;
    private TextView btnCenterMap;
    private TextView tvFollowBadge;
    private TextView tvRunState;
    private TextView tvDistance;
    private TextView tvPace;
    private TextView tvDuration;
    private TextView tvSpeed;
    private TextView tvSteps;
    private TextView tvCalories;
    private TextView tvRunningHint;
    private TextView tvSyncStatus;
    private MaterialButton btnPrimaryAction;
    private MaterialButton btnSecondaryAction;

    private RunningTrackerService runningService;
    private boolean isServiceBound = false;

    private boolean isMapReady = false;
    private boolean isFollowingUser = true;
    private int currentStyleIndex = 0;
    private int lastRenderedRouteCount = -1;
    private String lastKnownSyncStatus = "Chưa đồng bộ Firebase";

    private Polyline routePolyline;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable uiTicker = new Runnable() {
        @Override
        public void run() {
            renderSessionMetrics();
            redrawRouteFromServiceIfNeeded();
            centerToCurrentLocationIfNeeded();
            uiHandler.postDelayed(this, 1000L);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RunningTrackerService.LocalBinder binder = (RunningTrackerService.LocalBinder) service;
            runningService = binder.getService();
            isServiceBound = true;
            renderSessionMetrics();
            redrawRouteFromServiceIfNeeded();
            centerToCurrentLocationIfNeeded();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            runningService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        } catch (Exception e) {
            Toast.makeText(this, "Mapbox init lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_running_map);

        bindViews();
        initMap(savedInstanceState);
        setupInteractions();
        renderSessionMetrics();
    }

    private void bindViews() {
        mapView = findViewById(R.id.mapView);
        btnBackRunning = findViewById(R.id.btnBackRunning);
        btnStyleToggle = findViewById(R.id.btnStyleToggle);
        btnCenterMap = findViewById(R.id.btnCenterMap);
        tvFollowBadge = findViewById(R.id.tvFollowBadge);
        tvRunState = findViewById(R.id.tvRunState);
        tvDistance = findViewById(R.id.tvDistance);
        tvPace = findViewById(R.id.tvPace);
        tvDuration = findViewById(R.id.tvDuration);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvSteps = findViewById(R.id.tvSteps);
        tvCalories = findViewById(R.id.tvCalories);
        tvRunningHint = findViewById(R.id.tvRunningHint);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnSecondaryAction = findViewById(R.id.btnSecondaryAction);
    }

    private void initMap(@Nullable Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void setupInteractions() {
        btnBackRunning.setOnClickListener(v -> finish());

        btnStyleToggle.setOnClickListener(v -> {
            currentStyleIndex = (currentStyleIndex + 1) % MAP_STYLES.length;
            loadCurrentMapStyle();
        });

        btnCenterMap.setOnClickListener(v -> {
            isFollowingUser = true;
            updateFollowBadge();
            focusRouteOrCurrentPosition(true);
        });

        btnPrimaryAction.setOnClickListener(v -> {
            RunningSessionSnapshot snapshot = getCurrentSnapshot();

            if (!snapshot.sessionStarted || snapshot.paused) {
                startOrResumeRun();
            } else {
                pauseRun();
            }
        });

        btnSecondaryAction.setOnClickListener(v -> stopRun());
    }

    @Override
    public void onMapReady(@NonNull MapboxMap readyMap) {
        mapboxMap = readyMap;
        isMapReady = true;

        mapboxMap.addOnCameraMoveStartedListener(reason -> {
            if (reason == MapboxMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                isFollowingUser = false;
                updateFollowBadge();
            }
        });

        loadCurrentMapStyle();
    }

    private void loadCurrentMapStyle() {
        if (!isMapReady || mapboxMap == null) return;

        mapboxMap.setStyle(MAP_STYLES[currentStyleIndex], style -> {
            routePolyline = null;
            redrawRouteFromServiceIfNeeded();
            centerToCurrentLocationIfNeeded();
        });
    }

    private void startOrResumeRun() {
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
            return;
        }

        isFollowingUser = true;
        updateFollowBadge();

        Intent intent = new Intent(this, RunningTrackerService.class);
        intent.setAction(RunningTrackerService.ACTION_START_OR_RESUME);
        ContextCompat.startForegroundService(this, intent);
    }

    private void pauseRun() {
        Intent intent = new Intent(this, RunningTrackerService.class);
        intent.setAction(RunningTrackerService.ACTION_PAUSE);
        startService(intent);
    }

    private void stopRun() {
        Intent intent = new Intent(this, RunningTrackerService.class);
        intent.setAction(RunningTrackerService.ACTION_STOP);
        startService(intent);
        Toast.makeText(this, "Đang kết thúc và lưu phiên chạy...", Toast.LENGTH_SHORT).show();
    }

    private RunningSessionSnapshot getCurrentSnapshot() {
        if (runningService == null) {
            return RunningSessionSnapshot.idle();
        }
        return runningService.getSnapshot();
    }

    private void renderSessionMetrics() {
        RunningSessionSnapshot snapshot = getCurrentSnapshot();

        tvDistance.setText(formatDistance(snapshot.distanceMeters));
        tvPace.setText(formatPaceFromSeconds(snapshot.paceSecondsPerKm));
        tvDuration.setText(formatDuration(snapshot.durationMillis));
        tvSpeed.setText("🚀 " + String.format(Locale.getDefault(), "%.1f km/h", snapshot.avgSpeedKmh));
        tvSteps.setText(String.format(Locale.getDefault(), "👟 %,d bước", snapshot.steps));
        tvCalories.setText(String.format(Locale.getDefault(), "⚡ %.0f kcal", snapshot.calories));

        if (runningService != null) {
            lastKnownSyncStatus = runningService.getSyncStatus();
        }
        if (tvSyncStatus != null) {
            tvSyncStatus.setText(lastKnownSyncStatus);
        }

        if (!snapshot.sessionStarted) {
            tvRunState.setText("Sẵn sàng bắt đầu");
            tvRunningHint.setText("Bấm Bắt đầu chạy để ghi lại GPS, quãng đường và số bước.");
            btnPrimaryAction.setText("Bắt đầu chạy");
            btnSecondaryAction.setVisibility(android.view.View.GONE);
        } else if (!snapshot.paused) {
            tvRunState.setText(snapshot.gpsLocked ? "Đang ghi lại phiên chạy" : "Đang tìm tín hiệu GPS...");
            tvRunningHint.setText("Foreground service đang chạy, tắt màn hình vẫn tiếp tục ghi.");
            btnPrimaryAction.setText("Tạm dừng");
            btnSecondaryAction.setVisibility(android.view.View.VISIBLE);
        } else {
            tvRunState.setText("Đang tạm dừng");
            tvRunningHint.setText("Bấm Tiếp tục để chạy tiếp hoặc Kết thúc để lưu.");
            btnPrimaryAction.setText("Tiếp tục");
            btnSecondaryAction.setVisibility(android.view.View.VISIBLE);
        }

        updateFollowBadge();
    }

    private void redrawRouteFromServiceIfNeeded() {
        if (!isMapReady || mapboxMap == null || runningService == null) return;

        List<Location> routeLocations = runningService.getRoutePointsSnapshot();
        if (routeLocations.size() == lastRenderedRouteCount && routePolyline != null) {
            return;
        }

        lastRenderedRouteCount = routeLocations.size();

        if (routePolyline != null) {
            mapboxMap.removePolyline(routePolyline);
            routePolyline = null;
        }

        if (routeLocations.size() < 2) {
            return;
        }

        List<LatLng> latLngs = new ArrayList<>();
        for (Location location : routeLocations) {
            latLngs.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        routePolyline = mapboxMap.addPolyline(new PolylineOptions()
                .addAll(latLngs)
                .color(ContextCompat.getColor(this, R.color.primary))
                .alpha(0.88f)
                .width(6f));

        if (isFollowingUser) {
            focusRouteOrCurrentPosition(false);
        }
    }

    private void centerToCurrentLocationIfNeeded() {
        if (!isMapReady || mapboxMap == null || runningService == null || !isFollowingUser) return;

        Location current = runningService.getLastKnownLocationSnapshot();
        if (current == null) return;

        RunningSessionSnapshot snapshot = runningService.getSnapshot();

        if (snapshot.routePointCount < 2) {
            animateCamera(current, DEFAULT_ZOOM);
        }
    }

    private void focusRouteOrCurrentPosition(boolean forceOverview) {
        if (mapboxMap == null || runningService == null) return;

        List<Location> routeLocations = runningService.getRoutePointsSnapshot();

        if (routeLocations.size() >= 2 && forceOverview) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Location location : routeLocations) {
                builder.include(new LatLng(location.getLatitude(), location.getLongitude()));
            }
            mapboxMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 140),
                    900
            );
            return;
        }

        if (!routeLocations.isEmpty()) {
            Location last = routeLocations.get(routeLocations.size() - 1);
            animateCamera(last, DEFAULT_ZOOM);
            return;
        }

        Location current = runningService.getLastKnownLocationSnapshot();
        if (current != null) {
            animateCamera(current, DEFAULT_ZOOM);
        }
    }

    private void animateCamera(@NonNull Location location, double zoom) {
        if (mapboxMap == null) return;

        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(zoom)
                .tilt(30d)
                .bearing(location.hasBearing() ? location.getBearing() : 0d)
                .build();

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 700);
    }

    private void updateFollowBadge() {
        tvFollowBadge.setText(isFollowingUser ? "FOLLOW" : "FREE PAN");
        tvFollowBadge.setTextColor(ContextCompat.getColor(
                this,
                isFollowingUser ? R.color.primary : R.color.on_surface_variant
        ));
    }

    private boolean hasRequiredPermissions() {
        boolean hasFineLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean hasActivityRecognition = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;

        return hasFineLocation && hasActivityRecognition && hasNotifications;
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissions.toArray(new String[0]),
                    REQUEST_CODE_RUNNING_PERMISSIONS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_RUNNING_PERMISSIONS) return;

        if (hasRequiredPermissions()) {
            Toast.makeText(this, "Đã cấp quyền. Bạn có thể bắt đầu chạy rồi.", Toast.LENGTH_SHORT).show();
            startOrResumeRun();
        } else {
            Toast.makeText(this, "Thiếu quyền để theo dõi phiên chạy.", Toast.LENGTH_LONG).show();
        }
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

    private String formatPaceFromSeconds(float secondsPerKm) {
        if (secondsPerKm <= 0f) {
            return "--'--\"";
        }

        long minutes = (long) (secondsPerKm / 60f);
        long seconds = Math.round(secondsPerKm - (minutes * 60f));

        if (seconds == 60) {
            minutes += 1;
            seconds = 0;
        }

        return String.format(Locale.getDefault(), "%d'%02d\"", minutes, seconds);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();

        Intent intent = new Intent(this, RunningTrackerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        uiHandler.post(uiTicker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        uiHandler.removeCallbacks(uiTicker);

        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }

        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(uiTicker);
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}