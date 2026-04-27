package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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
    private LinearLayout cardPermissionExactAlarm;

    private Switch switchHealth;
    private Switch switchLocation;
    private Switch switchNotification;
    private Switch switchExactAlarm;

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
        hideStatusBar();
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
        cardPermissionExactAlarm = findViewById(R.id.cardPermissionExactAlarm);

        switchHealth = findViewById(R.id.switchHealth);
        switchLocation = findViewById(R.id.switchLocation);
        switchNotification = findViewById(R.id.switchNotification);
        switchExactAlarm = findViewById(R.id.switchExactAlarm);

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
        switchExactAlarm.setClickable(false);
        switchExactAlarm.setFocusable(false);

        cardPermissionHealth.setOnClickListener(v -> handlePermissionTap(PermissionManager.TYPE_HEALTH));
        cardPermissionLocation.setOnClickListener(v -> handlePermissionTap(PermissionManager.TYPE_LOCATION));
        cardPermissionNotification.setOnClickListener(v -> handlePermissionTap(PermissionManager.TYPE_NOTIFICATION));
        cardPermissionExactAlarm.setOnClickListener(v -> handlePermissionTap(PermissionManager.TYPE_EXACT_ALARM));

        btnContinue.setOnClickListener(v -> completeOnboarding());
        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void handlePermissionTap(String type) {
        if (PermissionManager.hasPermissionType(this, type)) {
            Toast.makeText(this, "Quyền này đã được cấp rồi", Toast.LENGTH_SHORT).show();
            syncPermissionStates();
            return;
        }

        if (PermissionManager.TYPE_EXACT_ALARM.equals(type)) {
            showExactAlarmPermissionDialog();
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
        boolean exactAlarmGranted = PermissionManager.hasPermissionType(this, PermissionManager.TYPE_EXACT_ALARM);

        switchHealth.setChecked(healthGranted);
        switchLocation.setChecked(locationGranted);
        switchNotification.setChecked(notificationGranted);
        switchExactAlarm.setChecked(exactAlarmGranted);

        updateCardState(cardPermissionHealth, healthGranted);
        updateCardState(cardPermissionLocation, locationGranted);
        updateCardState(cardPermissionNotification, notificationGranted);
        updateCardState(cardPermissionExactAlarm, exactAlarmGranted);
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
                .setPositiveButton("Mở cài đặt", (dialog, which) -> openSettingsForType(type))
                .show();
    }

    private void showExactAlarmPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Bật Báo thức & lời nhắc")
                .setMessage("Quyền này giúp FocusLife đặt lịch nhắc uống nước, động lực và Focus đúng giờ ngay cả khi bạn đã thoát app. Hãy bật quyền Cho phép đặt báo thức và lời nhắc cho FocusLife.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Mở cài đặt", (dialog, which) -> PermissionManager.openExactAlarmSettings(this))
                .show();
    }

    private void openSettingsForType(String type) {
        if (PermissionManager.TYPE_NOTIFICATION.equals(type)) {
            PermissionManager.openNotificationSettings(this);
        } else if (PermissionManager.TYPE_EXACT_ALARM.equals(type)) {
            PermissionManager.openExactAlarmSettings(this);
        } else {
            PermissionManager.openAppDetailsSettings(this);
        }
    }

    private void openAppSettings() {
        PermissionManager.openAppDetailsSettings(this);
    }

    private String getPermissionLabel(String type) {
        switch (type) {
            case PermissionManager.TYPE_HEALTH:
                return "quyền dữ liệu sức khỏe";
            case PermissionManager.TYPE_LOCATION:
                return "quyền vị trí";
            case PermissionManager.TYPE_NOTIFICATION:
                return "quyền thông báo";
            case PermissionManager.TYPE_EXACT_ALARM:
                return "quyền Báo thức & lời nhắc";
            default:
                return "quyền truy cập";
        }
    }

    private void completeOnboarding() {
        new OnboardingPreferences(this).setCompleted(true);
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }

    private void hideStatusBar() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

}
