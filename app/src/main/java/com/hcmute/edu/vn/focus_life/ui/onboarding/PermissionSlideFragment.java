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

    private Switch switchHealth;
    private Switch switchLocation;
    private Switch switchNotification;

    private MaterialButton btnContinue;
    private TextView btnSkip;

    private String pendingPermissionType = null;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onPermissionResult
            );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_onboarding_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardPermissionHealth = view.findViewById(R.id.cardPermissionHealth);
        cardPermissionLocation = view.findViewById(R.id.cardPermissionLocation);
        cardPermissionNotification = view.findViewById(R.id.cardPermissionNotification);

        switchHealth = view.findViewById(R.id.switchHealth);
        switchLocation = view.findViewById(R.id.switchLocation);
        switchNotification = view.findViewById(R.id.switchNotification);

        btnContinue = view.findViewById(R.id.btnPermissionContinue);
        btnSkip = view.findViewById(R.id.btnSkip);

        setupInteractions();
        syncPermissionStates();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncPermissionStates();
    }

    private void setupInteractions() {
        // Switch chỉ hiển thị trạng thái thật, không cho tự toggle giả
        switchHealth.setClickable(false);
        switchHealth.setFocusable(false);

        switchLocation.setClickable(false);
        switchLocation.setFocusable(false);

        switchNotification.setClickable(false);
        switchNotification.setFocusable(false);

        cardPermissionHealth.setOnClickListener(v ->
                requestPermission(PermissionManager.TYPE_HEALTH));

        cardPermissionLocation.setOnClickListener(v ->
                requestPermission(PermissionManager.TYPE_LOCATION));

        cardPermissionNotification.setOnClickListener(v ->
                requestPermission(PermissionManager.TYPE_NOTIFICATION));

        btnContinue.setOnClickListener(v -> completeOnboarding());
        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void requestPermission(String type) {
        if (getContext() == null) return;

        boolean alreadyGranted = PermissionManager.hasPermissionType(requireContext(), type);
        if (alreadyGranted) {
            Toast.makeText(
                    requireContext(),
                    "Quyền này đã được cấp rồi",
                    Toast.LENGTH_SHORT
            ).show();
            syncPermissionStates();
            return;
        }

        String[] permissions = PermissionManager.getPermissionsForType(type);
        if (permissions == null || permissions.length == 0) {
            Toast.makeText(
                    requireContext(),
                    "Thiết bị này không cần xin quyền này",
                    Toast.LENGTH_SHORT
            ).show();
            syncPermissionStates();
            return;
        }

        pendingPermissionType = type;
        permissionLauncher.launch(permissions);
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

        if (pendingPermissionType == null || getContext() == null) {
            return;
        }

        String label = getPermissionLabel(pendingPermissionType);

        if (granted) {
            Toast.makeText(
                    requireContext(),
                    "Đã cho phép " + label,
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    requireContext(),
                    "Bạn chưa cho phép " + label + ", nên công tắc vẫn tắt",
                    Toast.LENGTH_SHORT
            ).show();
        }

        pendingPermissionType = null;
    }

    private void syncPermissionStates() {
        if (getContext() == null) return;

        boolean healthGranted = PermissionManager.hasPermissionType(
                requireContext(), PermissionManager.TYPE_HEALTH
        );
        boolean locationGranted = PermissionManager.hasPermissionType(
                requireContext(), PermissionManager.TYPE_LOCATION
        );
        boolean notificationGranted = PermissionManager.hasPermissionType(
                requireContext(), PermissionManager.TYPE_NOTIFICATION
        );

        switchHealth.setChecked(healthGranted);
        switchLocation.setChecked(locationGranted);
        switchNotification.setChecked(notificationGranted);

        updateCardState(cardPermissionHealth, healthGranted);
        updateCardState(cardPermissionLocation, locationGranted);
        updateCardState(cardPermissionNotification, notificationGranted);
    }

    private void updateCardState(View card, boolean granted) {
        if (card == null) return;
        card.setAlpha(granted ? 1f : 0.94f);
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
        if (getContext() == null || getActivity() == null) return;

        new OnboardingPreferences(requireContext()).setCompleted(true);
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finishAffinity();
    }
}