package com.hcmute.edu.vn.focus_life.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.data.repository.AuthRepository;
import com.hcmute.edu.vn.focus_life.ui.auth.LoginActivity;

public class ProfileSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        TextView btnLogout = findViewById(R.id.btnLogout);

        OnboardingPreferences preferences = new OnboardingPreferences(this);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvProfileName.setText(preferences.getDisplayName());
        tvProfileEmail.setText(currentUser != null && currentUser.getEmail() != null
                ? currentUser.getEmail()
                : "focuslife.user@example.com");

        btnLogout.setOnClickListener(v -> {
            new AuthRepository(this).logout();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }
}
