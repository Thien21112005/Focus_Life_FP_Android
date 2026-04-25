package com.hcmute.edu.vn.focus_life.ui.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.SettingsPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppearanceSettingsActivity extends AppCompatActivity {
    private SettingsPreferences settingsPreferences;
    private SwitchCompat switchDarkMode;
    private final Map<String, View> themeCards = new LinkedHashMap<>();
    private final Map<String, TextView> themeStateLabels = new LinkedHashMap<>();
    private TextView cardLanguageVi;
    private TextView cardLanguageEn;
    private boolean syncingSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appearance_settings);

        settingsPreferences = new SettingsPreferences(this);
        bindViews();
        setupActions();
        applyAccent();
        bindState();
    }

    private void bindViews() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
        themeCards.put(SettingsPreferences.THEME_INDIGO, findViewById(R.id.cardThemeIndigo));
        themeCards.put(SettingsPreferences.THEME_EMERALD, findViewById(R.id.cardThemeEmerald));
        themeCards.put(SettingsPreferences.THEME_OCEAN, findViewById(R.id.cardThemeOcean));
        themeCards.put(SettingsPreferences.THEME_ROSE, findViewById(R.id.cardThemeRose));
        themeCards.put(SettingsPreferences.THEME_AMBER, findViewById(R.id.cardThemeAmber));

        themeStateLabels.put(SettingsPreferences.THEME_INDIGO, findViewById(R.id.tvThemeIndigoState));
        themeStateLabels.put(SettingsPreferences.THEME_EMERALD, findViewById(R.id.tvThemeEmeraldState));
        themeStateLabels.put(SettingsPreferences.THEME_OCEAN, findViewById(R.id.tvThemeOceanState));
        themeStateLabels.put(SettingsPreferences.THEME_ROSE, findViewById(R.id.tvThemeRoseState));
        themeStateLabels.put(SettingsPreferences.THEME_AMBER, findViewById(R.id.tvThemeAmberState));

        cardLanguageVi = findViewById(R.id.cardLanguageVi);
        cardLanguageEn = findViewById(R.id.cardLanguageEn);

        findViewById(R.id.dotThemeEmerald).setBackground(circle(getColor(R.color.theme_emerald)));
        findViewById(R.id.dotThemeOcean).setBackground(circle(getColor(R.color.theme_ocean)));
        findViewById(R.id.dotThemeRose).setBackground(circle(getColor(R.color.theme_rose)));
        findViewById(R.id.dotThemeAmber).setBackground(circle(getColor(R.color.theme_amber)));
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.cardThemeIndigo).setOnClickListener(v -> selectTheme(SettingsPreferences.THEME_INDIGO));
        findViewById(R.id.cardThemeEmerald).setOnClickListener(v -> selectTheme(SettingsPreferences.THEME_EMERALD));
        findViewById(R.id.cardThemeOcean).setOnClickListener(v -> selectTheme(SettingsPreferences.THEME_OCEAN));
        findViewById(R.id.cardThemeRose).setOnClickListener(v -> selectTheme(SettingsPreferences.THEME_ROSE));
        findViewById(R.id.cardThemeAmber).setOnClickListener(v -> selectTheme(SettingsPreferences.THEME_AMBER));
        cardLanguageVi.setOnClickListener(v -> selectLanguage(SettingsPreferences.LANGUAGE_VI));
        cardLanguageEn.setOnClickListener(v -> selectLanguage(SettingsPreferences.LANGUAGE_EN));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (syncingSwitch) return;
            settingsPreferences.setDarkModeEnabled(isChecked);
            setResult(RESULT_OK);
        });
    }

    private void bindState() {
        syncingSwitch = true;
        switchDarkMode.setChecked(settingsPreferences.isDarkModeEnabled());
        syncingSwitch = false;

        String selectedTheme = settingsPreferences.getThemeKey();
        int accent = settingsPreferences.getAccentColor(this);
        for (Map.Entry<String, View> entry : themeCards.entrySet()) {
            boolean selected = entry.getKey().equals(selectedTheme);
            entry.getValue().setBackground(optionBackground(selected, accent));
            themeStateLabels.get(entry.getKey()).setText(selected ? R.string.selected_label : R.string.not_selected);
            themeStateLabels.get(entry.getKey()).setTextColor(selected ? accent : getColor(R.color.on_surface_variant));
        }

        String language = settingsPreferences.getLanguage();
        styleLanguageCard(cardLanguageVi, SettingsPreferences.LANGUAGE_VI.equals(language), accent);
        styleLanguageCard(cardLanguageEn, SettingsPreferences.LANGUAGE_EN.equals(language), accent);
    }

    private void selectTheme(String themeKey) {
        settingsPreferences.setThemeKey(themeKey);
        setResult(RESULT_OK);
        recreate();
    }

    private void selectLanguage(String language) {
        settingsPreferences.setLanguage(language);
        setResult(RESULT_OK);
        recreate();
    }

    private void applyAccent() {
        int accent = settingsPreferences.getAccentColor(this);
        switchDarkMode.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        switchDarkMode.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{accent, getColor(R.color.surface_container_highest)}
        ));
    }

    private void styleLanguageCard(TextView view, boolean selected, int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(18));
        drawable.setColor(selected ? settingsPreferences.getAccentContainerColor(this) : getColor(R.color.surface_container_low));
        drawable.setStroke(dp(1), selected ? accent : getColor(R.color.surface_container_high));
        view.setBackground(drawable);
        view.setTextColor(selected ? accent : getColor(R.color.on_surface));
    }

    private GradientDrawable optionBackground(boolean selected, int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(24));
        drawable.setColor(selected ? settingsPreferences.getAccentContainerColor(this) : getColor(R.color.surface_container_low));
        drawable.setStroke(dp(1), selected ? accent : getColor(R.color.surface_container_high));
        return drawable;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
