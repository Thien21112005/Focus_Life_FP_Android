package com.hcmute.edu.vn.focus_life.ui.running;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.utils.PermissionManager;
import com.hcmute.edu.vn.focus_life.service.RunningTrackerService;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunningMapFragment extends Fragment {

    private FrameLayout mapContainer;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Polyline routePolyline;
    private Marker userLocationMarker;
    private Location userMarkerLocation;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView btnBackRunning;
    private TextView tvRunState;
    private TextView btnStyleToggle;
    private TextView btnCenterMap;
    private TextView tvFollowBadge;
    private TextView tvDistance;
    private TextView tvPace;
    private TextView tvDuration;
    private TextView tvSpeed;
    private TextView tvSteps;
    private TextView tvCalories;
    private TextView tvRunningHint;
    private TextView tvSyncStatus;
    private MaterialButton btnSecondaryAction;
    private MaterialButton btnPrimaryAction;

    private RunningTrackerService runningService;
    private boolean serviceBound = false;
    private boolean followUser = true;
    private boolean satelliteStyle = false;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable snapshotUpdater = new Runnable() {
        @Override
        public void run() {
            renderSnapshot();
            uiHandler.postDelayed(this, 1000L);
        }
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = true;
                for (Boolean value : result.values()) {
                    if (!Boolean.TRUE.equals(value)) {
                        granted = false;
                        break;
                    }
                }
                if (granted) {
                    startOrResumeRun();
                    locateAndMarkUser(true);
                } else {
                    Toast.makeText(requireContext(), "Cần quyền vị trí và dữ liệu sức khỏe để ghi phiên chạy", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> locatePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = true;
                for (Boolean value : result.values()) {
                    if (!Boolean.TRUE.equals(value)) {
                        granted = false;
                        break;
                    }
                }
                if (granted) {
                    locateAndMarkUser(true);
                } else {
                    Toast.makeText(requireContext(), "Cần quyền vị trí để định vị trên bản đồ", Toast.LENGTH_SHORT).show();
                }
            });

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RunningTrackerService.LocalBinder binder = (RunningTrackerService.LocalBinder) service;
            runningService = binder.getService();
            serviceBound = true;
            renderSnapshot();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            runningService = null;
            renderSnapshot();
        }
    };

    public RunningMapFragment() {
        super(R.layout.activity_running_map);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            Mapbox.getInstance(requireContext().getApplicationContext(), getString(R.string.mapbox_access_token));
        }
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapContainer = view.findViewById(R.id.mapContainer);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        mapView = new MapView(requireContext());
        mapView.setId(R.id.mapView);
        mapContainer.addView(mapView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        btnBackRunning = view.findViewById(R.id.btnBackRunning);
        tvRunState = view.findViewById(R.id.tvRunState);
        btnStyleToggle = view.findViewById(R.id.btnStyleToggle);
        btnCenterMap = view.findViewById(R.id.btnCenterMap);
        tvFollowBadge = view.findViewById(R.id.tvFollowBadge);
        if (tvFollowBadge != null) tvFollowBadge.setVisibility(android.view.View.GONE);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvPace = view.findViewById(R.id.tvPace);
        tvDuration = view.findViewById(R.id.tvDuration);
        tvSpeed = view.findViewById(R.id.tvSpeed);
        tvSteps = view.findViewById(R.id.tvSteps);
        tvCalories = view.findViewById(R.id.tvCalories);
        tvRunningHint = view.findViewById(R.id.tvRunningHint);
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus);
        if (tvSyncStatus != null) tvSyncStatus.setVisibility(android.view.View.GONE);
        btnSecondaryAction = view.findViewById(R.id.btnSecondaryAction);
        btnPrimaryAction = view.findViewById(R.id.btnPrimaryAction);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::onMapReady);

        btnBackRunning.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openTab(MainActivity.TAB_HOME);
            }
        });

        if (btnStyleToggle != null) {
            btnStyleToggle.setOnClickListener(v -> toggleMapStyle());
        }
        btnCenterMap.setOnClickListener(v -> {
            followUser = true;
            updateFollowBadge();
            if (ensureLocationPermissionForLocate(true)) {
                locateAndMarkUser(true);
            }
        });

        btnPrimaryAction.setOnClickListener(v -> handlePrimaryAction());
        btnSecondaryAction.setOnClickListener(v -> stopRun());

        renderIdleState();
    }

    private void onMapReady(@NonNull MapboxMap readyMap) {
        mapboxMap = readyMap;
        applyMapStyle();
        mapboxMap.addOnCameraMoveStartedListener(reason -> {
            if (reason == MapboxMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                followUser = false;
                updateFollowBadge();
            }
        });
    }

    private void applyMapStyle() {
        if (mapboxMap == null) return;
        String style = satelliteStyle ? Style.SATELLITE_STREETS : Style.MAPBOX_STREETS;
        mapboxMap.setStyle(style, loadedStyle -> {
            if (btnStyleToggle != null) btnStyleToggle.setText(satelliteStyle ? "☀" : "▦");
            renderRoute();
            renderUserLocationMarker(false);
            centerOnUser(false);
        });
    }

    private void toggleMapStyle() {
        satelliteStyle = !satelliteStyle;
        applyMapStyle();
    }

    private void handlePrimaryAction() {
        RunningSessionSnapshot snapshot = runningService != null ? runningService.getSnapshot() : RunningSessionSnapshot.idle();
        if (!snapshot.sessionStarted) {
            if (ensureRunPermissions(true)) {
                startOrResumeRun();
            }
            return;
        }

        if (snapshot.paused) {
            if (ensureRunPermissions(true)) {
                startOrResumeRun();
            }
        } else {
            pauseRun();
        }
    }

    private boolean ensureLocationPermissionForLocate(boolean requestIfMissing) {
        Context context = getContext();
        if (context == null) return false;

        boolean granted = PermissionManager.hasPermissionType(context, PermissionManager.TYPE_LOCATION);
        if (!granted && requestIfMissing) {
            List<String> missing = new ArrayList<>();
            for (String permission : PermissionManager.getLocationPermissions()) {
                if (!PermissionManager.hasPermission(context, permission)) {
                    missing.add(permission);
                }
            }
            locatePermissionLauncher.launch(missing.toArray(new String[0]));
        }
        return granted;
    }

    @SuppressWarnings("MissingPermission")
    private void locateAndMarkUser(boolean moveCamera) {
        if (mapboxMap == null) return;

        Location serviceLocation = runningService != null ? runningService.getLastKnownLocationSnapshot() : null;
        if (serviceLocation != null) {
            showUserLocationMarker(serviceLocation, moveCamera);
            return;
        }

        if (fusedLocationClient == null || !ensureLocationPermissionForLocate(false)) {
            Toast.makeText(requireContext(), "Chưa có quyền vị trí để định vị", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) return;
                    if (location != null) {
                        showUserLocationMarker(location, moveCamera);
                    } else {
                        Toast.makeText(requireContext(), "Chưa lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Không thể định vị lúc này", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showUserLocationMarker(@NonNull Location location, boolean moveCamera) {
        if (mapboxMap == null) return;

        userMarkerLocation = new Location(location);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userLocationMarker != null) {
            mapboxMap.removeMarker(userLocationMarker);
            userLocationMarker = null;
        }

        userLocationMarker = mapboxMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Vị trí của bạn"));

        if (moveCamera) {
            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.5), 700);
        }
    }

    private void renderUserLocationMarker(boolean moveCamera) {
        if (userMarkerLocation != null) {
            showUserLocationMarker(userMarkerLocation, moveCamera);
        }
    }

    private boolean ensureRunPermissions(boolean requestIfMissing) {
        Context context = getContext();
        if (context == null) return false;

        boolean hasLocation = PermissionManager.hasPermissionType(context, PermissionManager.TYPE_LOCATION);
        boolean hasHealth = PermissionManager.hasPermissionType(context, PermissionManager.TYPE_HEALTH);
        boolean granted = hasLocation && hasHealth;

        if (!granted && requestIfMissing) {
            List<String> missing = new ArrayList<>();
            for (String permission : PermissionManager.getLocationPermissions()) {
                if (!PermissionManager.hasPermission(context, permission)) {
                    missing.add(permission);
                }
            }
            for (String permission : PermissionManager.getHealthPermissions()) {
                if (!PermissionManager.hasPermission(context, permission)) {
                    missing.add(permission);
                }
            }
            permissionLauncher.launch(missing.toArray(new String[0]));
        }
        return granted;
    }

    private void startOrResumeRun() {
        if (getContext() == null) return;
        Intent intent = new Intent(requireContext(), RunningTrackerService.class);
        intent.setAction(RunningTrackerService.ACTION_START_OR_RESUME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(requireContext(), intent);
        } else {
            requireContext().startService(intent);
        }
        bindRunningService();
    }

    private void pauseRun() {
        if (getContext() == null) return;
        Intent intent = new Intent(requireContext(), RunningTrackerService.class);
        intent.setAction(RunningTrackerService.ACTION_PAUSE);
        requireContext().startService(intent);
        renderSnapshot();
    }

    private void stopRun() {
        if (getContext() == null) return;
        RunningSessionSnapshot snapshot = runningService != null ? runningService.getSnapshot() : RunningSessionSnapshot.idle();
        boolean tooShortToSave = isRunTooShortToSave(snapshot);

        Intent intent = new Intent(requireContext(), RunningTrackerService.class);
        intent.setAction(RunningTrackerService.ACTION_STOP);
        requireContext().startService(intent);

        if (tooShortToSave) {
            Toast.makeText(requireContext(), "Phiên chạy quá ngắn nên không lưu.", Toast.LENGTH_LONG).show();
            resetMapAfterFinish();
        } else {
            Toast.makeText(requireContext(), "Đang lưu phiên chạy lên Firestore...", Toast.LENGTH_SHORT).show();
            uiHandler.postDelayed(this::resetMapAfterFinish, 1800L);
        }
    }

    private void resetMapAfterFinish() {
        unbindRunningService();
        runningService = null;
        serviceBound = false;
        if (routePolyline != null && mapboxMap != null) {
            mapboxMap.removePolyline(routePolyline);
            routePolyline = null;
        }
        renderIdleState();
    }

    private boolean isRunTooShortToSave(RunningSessionSnapshot snapshot) {
        if (snapshot == null) return true;
        return snapshot.steps <= 0 && snapshot.distanceMeters < 30f;
    }

    private void bindRunningService() {
        if (serviceBound || getContext() == null) return;
        Intent serviceIntent = new Intent(requireContext(), RunningTrackerService.class);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindRunningService() {
        if (!serviceBound || getContext() == null) return;
        try {
            requireContext().unbindService(serviceConnection);
        } catch (Exception ignored) {
        }
        serviceBound = false;
        runningService = null;
    }

    private void renderSnapshot() {
        RunningSessionSnapshot snapshot = runningService != null ? runningService.getSnapshot() : RunningSessionSnapshot.idle();

        if (!snapshot.sessionStarted) {
            renderIdleState();
        } else {
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", snapshot.distanceMeters / 1000f));
            tvPace.setText(snapshot.paceSecondsPerKm <= 0f ? "--'--\"" : formatPace(snapshot.paceSecondsPerKm));
            tvDuration.setText(formatDuration(snapshot.durationMillis));
            tvSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", snapshot.avgSpeedKmh));
            tvSteps.setText(String.format(Locale.getDefault(), "%d bước", snapshot.steps));
            tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", snapshot.calories));
            tvRunningHint.setText(snapshot.gpsLocked
                    ? "GPS đang hoạt động, quãng đường sẽ được đo theo lộ trình thật."
                    : "Đang chờ GPS khóa vị trí chính xác hơn...");
            tvRunState.setText(snapshot.paused ? "Đang tạm dừng" : "Đang ghi phiên chạy");
            btnPrimaryAction.setText(snapshot.paused ? "Tiếp tục chạy" : "Tạm dừng");
            btnSecondaryAction.setVisibility(android.view.View.VISIBLE);
            tvFollowBadge.setText(followUser ? "FOLLOW" : "FREE PAN");
        }

        tvSyncStatus.setText(runningService != null ? runningService.getSyncStatus() : "Chưa có phiên chạy");
        renderRoute();
        centerOnUser(false);
    }

    private void renderIdleState() {
        tvDistance.setText("0.00 km");
        tvPace.setText("--'--\"");
        tvDuration.setText("00:00");
        tvSpeed.setText("0.0 km/h");
        tvSteps.setText("0 bước");
        tvCalories.setText("0 kcal");
        tvRunState.setText("Sẵn sàng bắt đầu");
        tvRunningHint.setText("Bấm Bắt đầu chạy để ghi lại GPS, quãng đường và số bước.");
        tvSyncStatus.setText(runningService != null ? runningService.getSyncStatus() : "Chưa có phiên chạy");
        btnPrimaryAction.setText("Bắt đầu chạy");
        btnSecondaryAction.setVisibility(android.view.View.GONE);
        updateFollowBadge();
        renderRoute();
    }

    private void renderRoute() {
        if (mapboxMap == null) return;

        if (routePolyline != null) {
            mapboxMap.removePolyline(routePolyline);
            routePolyline = null;
        }

        if (runningService == null) return;
        RunningSessionSnapshot snapshot = runningService.getSnapshot();
        if (snapshot == null || !snapshot.sessionStarted) return;

        List<Location> points = runningService.getRoutePointsSnapshot();
        if (points.size() < 2) {
            return;
        }

        List<LatLng> latLngs = new ArrayList<>();
        for (Location point : points) {
            latLngs.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }

        routePolyline = mapboxMap.addPolyline(new PolylineOptions()
                .addAll(latLngs)
                .width(6f)
                .color(ContextCompat.getColor(requireContext(), R.color.primary)));
    }

    private void centerOnUser(boolean force) {
        if (mapboxMap == null || runningService == null) return;
        if (!force && !followUser) return;

        Location location = runningService.getLastKnownLocationSnapshot();
        if (location == null) return;

        if (userLocationMarker != null) {
            showUserLocationMarker(location, false);
        }

        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()),
                16.5
        ), 700);
    }

    private void updateFollowBadge() {
        if (tvFollowBadge == null) return;
        tvFollowBadge.setText(followUser ? "FOLLOW" : "FREE PAN");
        tvFollowBadge.setAlpha(followUser ? 1f : 0.72f);
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

    private String formatPace(float secondsPerKm) {
        long minutes = (long) (secondsPerKm / 60f);
        long seconds = Math.round(secondsPerKm - minutes * 60f);
        if (seconds == 60) {
            minutes += 1;
            seconds = 0;
        }
        return String.format(Locale.getDefault(), "%d'%02d\"", minutes, seconds);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
        bindRunningService();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        uiHandler.removeCallbacks(snapshotUpdater);
        uiHandler.post(snapshotUpdater);
    }

    @Override
    public void onPause() {
        uiHandler.removeCallbacks(snapshotUpdater);
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mapView != null) mapView.onStop();
        unbindRunningService();
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        uiHandler.removeCallbacks(snapshotUpdater);
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
