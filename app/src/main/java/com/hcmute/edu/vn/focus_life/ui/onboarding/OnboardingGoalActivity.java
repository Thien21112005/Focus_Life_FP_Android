package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class OnboardingGoalActivity extends AppCompatActivity {

    private final Set<String> selectedGoals = new LinkedHashSet<>();
    private MaterialButton btnContinue;
    private final Map<Integer, String> goalMap = Map.of(
            R.id.cardGoalFocus, "Deep Work",
            R.id.cardGoalHealth, "Health First",
            R.id.cardGoalAll, "Holistic Life"
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar();
        setContentView(R.layout.activity_onboarding_goal);

        OnboardingPreferences preferences = new OnboardingPreferences(this);
        selectedGoals.addAll(preferences.getSelectedGoals());

        btnContinue = findViewById(R.id.btnGoalContinue);
        MaterialButton btnSkip = findViewById(R.id.btnGoalSkip);

        for (Integer cardId : goalMap.keySet()) {
            LinearLayout card = findViewById(cardId);
            card.setOnClickListener(v -> toggleGoal(cardId));
        }

        btnSkip.setOnClickListener(v -> {
            if (selectedGoals.isEmpty()) {
                selectedGoals.add("Deep Work");
            }
            saveGoalsAndContinue(preferences);
        });

        btnContinue.setOnClickListener(v -> {
            if (selectedGoals.isEmpty()) {
                Toast.makeText(this, "Chọn ít nhất 1 mục tiêu nhé", Toast.LENGTH_SHORT).show();
                return;
            }
            saveGoalsAndContinue(preferences);
        });

        refreshGoalStates();
    }

    private void toggleGoal(int cardId) {
        String goal = goalMap.get(cardId);
        if (goal == null) return;

        if ("Holistic Life".equals(goal)) {
            if (selectedGoals.contains(goal)) {
                selectedGoals.remove(goal);
            } else {
                selectedGoals.clear();
                selectedGoals.add(goal);
            }
        } else {
            selectedGoals.remove("Holistic Life");
            if (selectedGoals.contains(goal)) {
                selectedGoals.remove(goal);
            } else {
                selectedGoals.add(goal);
            }
        }

        refreshGoalStates();
    }

    private void refreshGoalStates() {
        updateCard(R.id.cardGoalFocus, R.id.goalCheckFocus, selectedGoals.contains("Deep Work"));
        updateCard(R.id.cardGoalHealth, R.id.goalCheckHealth, selectedGoals.contains("Health First"));
        updateCard(R.id.cardGoalAll, R.id.goalCheckAll, selectedGoals.contains("Holistic Life"));

        btnContinue.setEnabled(!selectedGoals.isEmpty());
        btnContinue.setAlpha(selectedGoals.isEmpty() ? 0.55f : 1f);
    }

    private void updateCard(int cardId, int checkId, boolean selected) {
        LinearLayout card = findViewById(cardId);
        TextView check = findViewById(checkId);
        card.setBackgroundResource(selected ? R.drawable.bg_goal_card_selected : R.drawable.bg_goal_card_unselected);
        check.animate().alpha(selected ? 1f : 0f).setDuration(150L).start();
        card.animate().translationY(selected ? -2f : 0f).setDuration(150L).start();
    }

    private void saveGoalsAndContinue(OnboardingPreferences preferences) {
        preferences.setSelectedGoals(selectedGoals);
        preferences.setPrimaryGoal(preferences.joinGoals(selectedGoals));
        startActivity(new Intent(this, OnboardingPermissionsActivity.class));
        finish();
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
