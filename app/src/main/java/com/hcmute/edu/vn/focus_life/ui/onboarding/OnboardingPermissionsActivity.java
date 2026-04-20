package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.utils.PermissionManager;
import com.hcmute.edu.vn.focus_life.ui.auth.LoginActivity;

import java.util.Map;

public class OnboardingPermissionsActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_permissions);

        MaterialButton btnAllowAll = findViewById(R.id.btnAllowAll);
        TextView btnSkip = findViewById(R.id.btnSkip);

        btnAllowAll.setOnClickListener(v -> PermissionManager.requestOnboardingPermissions(permissionLauncher));
        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        boolean allGranted = true;
        for (Boolean granted : result.values()) {
            if (!Boolean.TRUE.equals(granted)) {
                allGranted = false;
                break;
            }
        }
        Toast.makeText(this, allGranted ? "Đã cấp quyền" : "Bạn có thể cấp quyền sau trong cài đặt", Toast.LENGTH_SHORT).show();
        completeOnboarding();
    }

    private void completeOnboarding() {
        new OnboardingPreferences(this).setCompleted(true);
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}
