package com.hcmute.edu.vn.focus_life.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import com.hcmute.edu.vn.focus_life.R;

public class SettingsPreferences {
    private static final String PREF_NAME = "settings_pref";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LANGUAGE = "language";

    public static final String THEME_INDIGO = "indigo";
    public static final String THEME_EMERALD = "emerald";
    public static final String THEME_ROSE = "rose";
    public static final String THEME_AMBER = "amber";
    public static final String THEME_OCEAN = "ocean";

    public static final String LANGUAGE_VI = "vi";
    public static final String LANGUAGE_EN = "en";

    private final SharedPreferences preferences;

    public SettingsPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(enabled
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public String getThemeKey() {
        return preferences.getString(KEY_THEME, THEME_INDIGO);
    }

    public void setThemeKey(String themeKey) {
        if (!isValidTheme(themeKey)) {
            themeKey = THEME_INDIGO;
        }
        preferences.edit().putString(KEY_THEME, themeKey).apply();
    }

    public int getAccentColor(Context context) {
        switch (getThemeKey()) {
            case THEME_EMERALD:
                return ContextCompat.getColor(context, R.color.theme_emerald);
            case THEME_ROSE:
                return ContextCompat.getColor(context, R.color.theme_rose);
            case THEME_AMBER:
                return ContextCompat.getColor(context, R.color.theme_amber);
            case THEME_OCEAN:
                return ContextCompat.getColor(context, R.color.theme_ocean);
            case THEME_INDIGO:
            default:
                return ContextCompat.getColor(context, R.color.theme_indigo);
        }
    }

    public int getAccentContainerColor(Context context) {
        switch (getThemeKey()) {
            case THEME_EMERALD:
                return ContextCompat.getColor(context, R.color.theme_emerald_container);
            case THEME_ROSE:
                return ContextCompat.getColor(context, R.color.theme_rose_container);
            case THEME_AMBER:
                return ContextCompat.getColor(context, R.color.theme_amber_container);
            case THEME_OCEAN:
                return ContextCompat.getColor(context, R.color.theme_ocean_container);
            case THEME_INDIGO:
            default:
                return ContextCompat.getColor(context, R.color.theme_indigo_container);
        }
    }

    public String getThemeDisplayName(Context context) {
        switch (getThemeKey()) {
            case THEME_EMERALD:
                return context.getString(R.string.theme_emerald);
            case THEME_ROSE:
                return context.getString(R.string.theme_rose);
            case THEME_AMBER:
                return context.getString(R.string.theme_amber);
            case THEME_OCEAN:
                return context.getString(R.string.theme_ocean);
            case THEME_INDIGO:
            default:
                return context.getString(R.string.theme_indigo);
        }
    }

    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, LANGUAGE_VI);
    }

    public void setLanguage(String language) {
        if (!LANGUAGE_EN.equals(language)) {
            language = LANGUAGE_VI;
        }
        preferences.edit().putString(KEY_LANGUAGE, language).apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    }

    public String getLanguageDisplayName(Context context) {
        return LANGUAGE_EN.equals(getLanguage())
                ? context.getString(R.string.language_english)
                : context.getString(R.string.language_vietnamese);
    }

    public void applyToAppCompat() {
        AppCompatDelegate.setDefaultNightMode(isDarkModeEnabled()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(getLanguage()));
    }

    private boolean isValidTheme(String themeKey) {
        return THEME_INDIGO.equals(themeKey)
                || THEME_EMERALD.equals(themeKey)
                || THEME_ROSE.equals(themeKey)
                || THEME_AMBER.equals(themeKey)
                || THEME_OCEAN.equals(themeKey);
    }
}
