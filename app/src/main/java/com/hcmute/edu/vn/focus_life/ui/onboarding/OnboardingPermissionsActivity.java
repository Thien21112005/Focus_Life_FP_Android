package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.utils.PermissionManager;
import com.hcmute.edu.vn.focus_life.ui.auth.LoginActivity;

import java.util.Map;

public class OnboardingPermissionsActivity extends AppCompatActivity {

    private static final String PREFS_PERMISSION_STATE = "permission_state";
    private static final String KEY_ASKED_PREFIX = "asked_";

    private LinearLayout cardPermissionHealth;
    private LinearLayout cardPermissionLocation;
    private LinearLayout cardPermissionNotification;

    private Switch switchHealth;
    private Switch switchLocation;
    private Switch switchNotification;

    private MaterialButton btnContinue;
    private TextView btnSkip;

    private String pendingPermissionType = null;
    private SharedPreferences permissionPrefs;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onPermissionResult
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_permissions);

        permissionPrefs = getSharedPreferences(PREFS_PERMISSION_STATE, MODE_PRIVATE);

        bindViews();
        setupInteractions();
        syncPermissionStates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncPermissionStates();
    }

    private void bindViews() {
        cardPermissionHealth = findViewById(R.id.cardPermissionHealth);
        cardPermissionLocation = findViewById(R.id.cardPermissionLocation);
        cardPermissionNotification = findViewById(R.id.cardPermissionNotification);

        switchHealth = findViewById(R.id.switchHealth);
        switchLocation = findViewById(R.id.switchLocation);
        switchNotification = findViewById(R.id.switchNotification);

        btnContinue = findViewById(R.id.btnPermissionContinue);
        btnSkip = findViewById(R.id.btnSkip);
    }

    private void setupInteractions() {
        switchHealth.setClickable(false);
        switchHealth.setFocusable(false);
        switchLocation.setClickable(false);
        switchLocation.setFocusable(false);
        switchNotification.setClickable(false);
        switchNotification.setFocusable(false);

        cardPermissionHealth.setOnClickListener(v -> handlePermissionTap(PermissionManager.TYPE_HEALTH));
        cardPermissionLocation.setOnClickListener(v -> handlePermissionTap(PermissionManager.TYPE_LOCATION));
        cardPermissionNotification.setOnClickListener(v -> handlePermissionTap(PermissionManager.TYPE_NOTIFICATION));

        btnContinue.setOnClickListener(v -> completeOnboarding());
        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void handlePermissionTap(String type) {
        if (PermissionManager.hasPermissionType(this, type)) {
            Toast.makeText(this, "Quyền này đã được cấp rồi", Toast.LENGTH_SHORT).show();
            syncPermissionStates();
            return;
        }

        String[] permissions = PermissionManager.getPermissionsForType(type);
        if (permissions.length == 0) {
            Toast.makeText(this, "Thiết bị này không cần xin quyền này", Toast.LENGTH_SHORT).show();
            syncPermissionStates();
            return;
        }

        boolean wasAskedBefore = wasPermissionAskedBefore(type);
        if (PermissionManager.shouldOpenSettings(this, type, wasAskedBefore)) {
            showOpenSettingsDialog(type);
            return;
        }

        pendingPermissionType = type;
        markPermissionAsked(type);
        PermissionManager.requestPermissionType(permissionLauncher, type);
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        boolean granted = true;
        for (Boolean value : result.values()) {
            if (!Boolean.TRUE.equals(value)) {
                granted = false;
                break;
            }
        }

        syncPermissionStates();

        if (pendingPermissionType == null) {
            return;
        }

        String label = getPermissionLabel(pendingPermissionType);
        if (granted) {
            Toast.makeText(this, "Đã cho phép " + label, Toast.LENGTH_SHORT).show();
        } else if (PermissionManager.shouldOpenSettings(this, pendingPermissionType, true)) {
            showOpenSettingsDialog(pendingPermissionType);
        } else {
            Toast.makeText(this, "Bạn chưa cho phép " + label + ", nên công tắc vẫn tắt", Toast.LENGTH_SHORT).show();
        }

        pendingPermissionType = null;
    }

    private void syncPermissionStates() {
        boolean healthGranted = PermissionManager.hasPermissionType(this, PermissionManager.TYPE_HEALTH);
        boolean locationGranted = PermissionManager.hasPermissionType(this, PermissionManager.TYPE_LOCATION);
        boolean notificationGranted = PermissionManager.hasPermissionType(this, PermissionManager.TYPE_NOTIFICATION);

        switchHealth.setChecked(healthGranted);
        switchLocation.setChecked(locationGranted);
        switchNotification.setChecked(notificationGranted);

        updateCardState(cardPermissionHealth, healthGranted);
        updateCardState(cardPermissionLocation, locationGranted);
        updateCardState(cardPermissionNotification, notificationGranted);
    }

    private void updateCardState(View card, boolean granted) {
        card.setAlpha(granted ? 1f : 0.95f);
    }

    private boolean wasPermissionAskedBefore(String type) {
        return permissionPrefs.getBoolean(KEY_ASKED_PREFIX + type, false);
    }

    private void markPermissionAsked(String type) {
        permissionPrefs.edit().putBoolean(KEY_ASKED_PREFIX + type, true).apply();
    }

    private void showOpenSettingsDialog(String type) {
        String label = getPermissionLabel(type);
        new AlertDialog.Builder(this)
                .setTitle("Bật quyền trong cài đặt")
                .setMessage("Android hiện không hiện lại hộp thoại cho " + label + ". Bạn mở Cài đặt ứng dụng để bật quyền này nhé.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Mở cài đặt", (dialog, which) -> openAppSettings())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private String getPermissionLabel(String type) {
        switch (type) {
            case PermissionManager.TYPE_HEALTH:
                return "quyền dữ liệu sức khỏe";
            case PermissionManager.TYPE_LOCATION:
                return "quyền vị trí";
            case PermissionManager.TYPE_NOTIFICATION:
                return "quyền thông báo";
            default:
                return "quyền truy cập";
        }
    }

    private void completeOnboarding() {
        new OnboardingPreferences(this).setCompleted(true);
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}
