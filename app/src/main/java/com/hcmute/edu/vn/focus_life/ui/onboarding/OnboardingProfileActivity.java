package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;

public class OnboardingProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_profile);

        OnboardingPreferences preferences = new OnboardingPreferences(this);
        EditText etDisplayName = findViewById(R.id.etDisplayName);
        MaterialButton btnContinue = findViewById(R.id.btnProfileContinue);
        TextView btnBack = findViewById(R.id.btnBack);

        etDisplayName.setText(preferences.getDisplayName());

        btnContinue.setOnClickListener(v -> {
            String displayName = etDisplayName.getText().toString().trim();
            if (displayName.isEmpty()) {
                displayName = "Minh";
            }
            preferences.setDisplayName(displayName);
            startActivity(new Intent(this, OnboardingGoalActivity.class));
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
