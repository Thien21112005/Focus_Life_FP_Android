package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;

public class OnboardingGoalActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_goal);

        MaterialButton btnContinue = findViewById(R.id.btnGoalContinue);
        btnContinue.setOnClickListener(v -> {
            new OnboardingPreferences(this).setPrimaryGoal("Deep Work");
            startActivity(new Intent(this, OnboardingPermissionsActivity.class));
        });
    }
}
