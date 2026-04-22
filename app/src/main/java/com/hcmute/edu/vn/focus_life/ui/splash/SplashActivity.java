package com.hcmute.edu.vn.focus_life.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;
import com.hcmute.edu.vn.focus_life.ui.auth.LoginActivity;
import com.hcmute.edu.vn.focus_life.ui.onboarding.OnboardingGoalActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, 1200L);
    }

    private void routeNext() {
        OnboardingPreferences onboardingPreferences = new OnboardingPreferences(this);
        Intent intent;
        if (!onboardingPreferences.isCompleted()) {
            intent = new Intent(this, OnboardingGoalActivity.class);
        } else if (FocusLifeApp.getInstance().getSessionManager().isUserLoggedIn()) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
