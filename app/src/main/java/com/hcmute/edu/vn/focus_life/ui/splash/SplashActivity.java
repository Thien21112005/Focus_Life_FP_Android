package com.hcmute.edu.vn.focus_life.ui.splash;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;
import com.hcmute.edu.vn.focus_life.ui.auth.LoginActivity;
import com.hcmute.edu.vn.focus_life.ui.onboarding.OnboardingGoalActivity;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private final List<AnimatorSet> dotAnimators = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        startDotsAnimation();
        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, 1200L);
    }

    private void startDotsAnimation() {
        animateDot(findViewById(R.id.dotSplashOne), 0L, 0.40f);
        animateDot(findViewById(R.id.dotSplashTwo), 140L, 0.65f);
        animateDot(findViewById(R.id.dotSplashThree), 280L, 0.85f);
    }

    private void animateDot(View dot, long delay, float minAlpha) {
        if (dot == null) return;
        float jump = -dp(10);
        ObjectAnimator translate = ObjectAnimator.ofFloat(dot, View.TRANSLATION_Y, 0f, jump, 0f);
        translate.setDuration(720L);
        translate.setRepeatCount(ValueAnimator.INFINITE);
        translate.setRepeatMode(ValueAnimator.RESTART);
        translate.setStartDelay(delay);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(dot, View.SCALE_X, 1f, 1.22f, 1f);
        scaleX.setDuration(720L);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.RESTART);
        scaleX.setStartDelay(delay);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(dot, View.SCALE_Y, 1f, 1.22f, 1f);
        scaleY.setDuration(720L);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.RESTART);
        scaleY.setStartDelay(delay);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(dot, View.ALPHA, minAlpha, 1f, minAlpha);
        alpha.setDuration(720L);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.RESTART);
        alpha.setStartDelay(delay);

        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.playTogether(translate, scaleX, scaleY, alpha);
        set.start();
        dotAnimators.add(set);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    @Override
    protected void onDestroy() {
        for (AnimatorSet animator : dotAnimators) {
            if (animator != null) animator.cancel();
        }
        dotAnimators.clear();
        super.onDestroy();
    }
}
