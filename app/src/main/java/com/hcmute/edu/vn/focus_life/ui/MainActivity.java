package com.hcmute.edu.vn.focus_life.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.AuthStatePreferences;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.ui.profile.ProfileSettingsActivity;
import com.hcmute.edu.vn.focus_life.ui.running.RunningMapActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        OnboardingPreferences onboardingPreferences = new OnboardingPreferences(this);
        AuthStatePreferences authStatePreferences = new AuthStatePreferences(this);

        TextView tvBrand = findViewById(R.id.tvHomeBrand);
        TextView tvNavProfile = findViewById(R.id.tvNavProfile);
        EditText etTaskName = findViewById(R.id.etTaskName);
        MaterialButton btnStartFocus = findViewById(R.id.btnStartFocus);

        String displayName = onboardingPreferences.getDisplayName();
        String goal = onboardingPreferences.getPrimaryGoal();
        if (tvBrand != null) {
            tvBrand.setText("FocusLife · " + displayName);
        }
        if (etTaskName != null && etTaskName.getText().toString().trim().isEmpty()) {
            etTaskName.setText(goal + " cho " + displayName);
        }

        btnStartFocus.setOnClickListener(v -> {
            String task = etTaskName.getText().toString().trim();
            if (task.isEmpty()) task = "Phiên Focus";
            Toast.makeText(this, "Bắt đầu: " + task, Toast.LENGTH_SHORT).show();
        });

        tvNavProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileSettingsActivity.class)));

        if (!FocusLifeApp.getInstance().getSessionManager().isUserLoggedIn()
                && (authStatePreferences.getEmail() == null || authStatePreferences.getEmail().isEmpty())) {
            Toast.makeText(this, "Phiên đăng nhập chưa tồn tại", Toast.LENGTH_SHORT).show();
        }
        TextView tvNavMap = findViewById(R.id.tvNavMap);

        tvNavMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RunningMapActivity.class)));
    }
}
