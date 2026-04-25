package com.hcmute.edu.vn.focus_life.ui.profile;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.SettingsPreferences;

public class AboutFocusLifeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_focuslife);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        SettingsPreferences preferences = new SettingsPreferences(this);
        TextView tvHeroChip = findViewById(R.id.tvHeroChip);
        int accent = preferences.getAccentColor(this);
        int accentSoft = preferences.getAccentContainerColor(this);
        GradientDrawable chip = new GradientDrawable();
        chip.setShape(GradientDrawable.RECTANGLE);
        chip.setCornerRadius(getResources().getDisplayMetrics().density * 999);
        chip.setColor(accentSoft);
        tvHeroChip.setBackground(chip);
        tvHeroChip.setTextColor(accent);
    }
}
