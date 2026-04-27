package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.utils.PermissionManager;
import com.hcmute.edu.vn.focus_life.ui.auth.LoginActivity;

import java.util.Map;

public class PermissionSlideFragment extends Fragment {

    private LinearLayout cardPermissionHealth;
    private LinearLayout cardPermissionLocation;
    private LinearLayout cardPermissionNotification;
    private LinearLayout cardPermissionExactAlarm;
    private Switch switchHealth;
    private Switch switchLocation;
    private Switch switchNotification;
    private Switch switchExactAlarm;
    private String pendingPermissionType;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_onboarding_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardPermissionHealth = view.findViewById(R.id.cardPermissionHealth);
        cardPermissionLocation = view.findViewById(R.id.cardPermissionLocation);
        cardPermissionNotification = view.findViewById(R.id.cardPermissionNotification);
        cardPermissionExactAlarm = view.findViewById(R.id.cardPermissionExactAlarm);
        switchHealth = view.findViewById(R.id.switchHealth);
        switchLocation = view.findViewById(R.id.switchLocation);
        switchNotification = view.findViewById(R.id.switchNotification);
        switchExactAlarm = view.findViewById(R.id.switchExactAlarm);
        MaterialButton btnContinue = view.findViewById(R.id.btnPermissionContinue);
        TextView btnSkip = view.findViewById(R.id.btnSkip);

        switchHealth.setClickable(false);
        switchLocation.setClickable(false);
        switchNotification.setClickable(false);
        switchExactAlarm.setClickable(false);

        cardPermissionHealth.setOnClickListener(v -> requestPermission(PermissionManager.TYPE_HEALTH));
        cardPermissionLocation.setOnClickListener(v -> requestPermission(PermissionManager.TYPE_LOCATION));
        cardPermissionNotification.setOnClickListener(v -> requestPermission(PermissionManager.TYPE_NOTIFICATION));
        cardPermissionExactAlarm.setOnClickListener(v -> requestPermission(PermissionManager.TYPE_EXACT_ALARM));

        btnContinue.setOnClickListener(v -> completeOnboarding());
        btnSkip.setOnClickListener(v -> completeOnboarding());

        syncPermissionStates();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncPermissionStates();
    }

    private void requestPermission(String type) {
        if (PermissionManager.hasPermissionType(requireContext(), type)) {
            Toast.makeText(requireContext(), "Quyền này đã được cấp rồi", Toast.LENGTH_SHORT).show();
            syncPermissionStates();
            return;
        }

        if (PermissionManager.TYPE_EXACT_ALARM.equals(type)) {
            showExactAlarmPermissionDialog();
            return;
        }

        pendingPermissionType = type;
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
        if (pendingPermissionType != null) {
            Toast.makeText(requireContext(), granted ? "Đã cấp quyền" : "Bạn có thể cấp quyền sau trong cài đặt", Toast.LENGTH_SHORT).show();
            pendingPermissionType = null;
        }
    }

    private void syncPermissionStates() {
        if (getContext() == null) return;

        boolean healthGranted = PermissionManager.hasPermissionType(requireContext(), PermissionManager.TYPE_HEALTH);
        boolean locationGranted = PermissionManager.hasPermissionType(requireContext(), PermissionManager.TYPE_LOCATION);
        boolean notificationGranted = PermissionManager.hasPermissionType(requireContext(), PermissionManager.TYPE_NOTIFICATION);
        boolean exactAlarmGranted = PermissionManager.hasPermissionType(requireContext(), PermissionManager.TYPE_EXACT_ALARM);

        switchHealth.setChecked(healthGranted);
        switchLocation.setChecked(locationGranted);
        switchNotification.setChecked(notificationGranted);
        switchExactAlarm.setChecked(exactAlarmGranted);

        cardPermissionHealth.setAlpha(healthGranted ? 1f : 0.95f);
        cardPermissionLocation.setAlpha(locationGranted ? 1f : 0.95f);
        cardPermissionNotification.setAlpha(notificationGranted ? 1f : 0.95f);
        cardPermissionExactAlarm.setAlpha(exactAlarmGranted ? 1f : 0.95f);
    }

    private void showExactAlarmPermissionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Bật Báo thức & lời nhắc")
                .setMessage("Quyền này giúp FocusLife đặt lịch nhắc uống nước, động lực và Focus đúng giờ ngay cả khi bạn đã thoát app.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Mở cài đặt", (dialog, which) -> PermissionManager.openExactAlarmSettings(requireContext()))
                .show();
    }

    private void completeOnboarding() {
        new OnboardingPreferences(requireContext()).setCompleted(true);
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finishAffinity();
    }
}
