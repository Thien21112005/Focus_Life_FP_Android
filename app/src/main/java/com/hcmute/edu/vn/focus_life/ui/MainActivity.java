package com.hcmute.edu.vn.focus_life.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.SettingsPreferences;
import com.hcmute.edu.vn.focus_life.ui.diary.NutritionDiaryFragment;
import com.hcmute.edu.vn.focus_life.ui.focus.FocusFragment;
import com.hcmute.edu.vn.focus_life.ui.home.HomeFragment;
import com.hcmute.edu.vn.focus_life.ui.profile.ProfileFragment;
import com.hcmute.edu.vn.focus_life.ui.running.RunningMapFragment;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_START_TAB = "extra_start_tab";
    public static final String TAB_HOME = "home";
    public static final String TAB_HEALTH = "health";
    public static final String TAB_FOCUS = "focus";
    public static final String TAB_MAP = "map";
    public static final String TAB_DIARY = "diary";
    public static final String TAB_AI_COACH = "ai_coach";
    public static final String TAB_NOTIFICATION = "notification";
    public static final String TAB_REPORT = "report";
    public static final String TAB_STATISTIC = "statistic";
    public static final String TAB_TARGET = "target";
    public static final String TAB_PROFILE = "profile";

    private static final String STATE_CURRENT_TAB = "state_current_tab";

    private final Map<String, TextView> navViews = new LinkedHashMap<>();
    private SettingsPreferences settingsPreferences;
    private String currentTab = TAB_HOME;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        settingsPreferences = new SettingsPreferences(this);
        settingsPreferences.applyToAppCompat();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        bindNavViews();
        applyNavLabels();
        setupNavClicks();

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getString(STATE_CURRENT_TAB, TAB_HOME);
        } else {
            String requestedTab = getIntent().getStringExtra(EXTRA_START_TAB);
            if (requestedTab != null && navViews.containsKey(requestedTab)) {
                currentTab = requestedTab;
            }
        }

        if (savedInstanceState == null) {
            switchTab(currentTab);
        } else {
            updateBottomNav(currentTab);
        }
    }

    private void bindNavViews() {
        navViews.put(TAB_HOME, findViewById(R.id.tvNavHome));
        navViews.put(TAB_HEALTH, findViewById(R.id.tvNavHealth));
        navViews.put(TAB_FOCUS, findViewById(R.id.tvNavFocus));
        navViews.put(TAB_MAP, findViewById(R.id.tvNavMap));
        navViews.put(TAB_DIARY, findViewById(R.id.tvNavDiary));
        navViews.put(TAB_AI_COACH, findViewById(R.id.tvNavAiCoach));
        navViews.put(TAB_NOTIFICATION, findViewById(R.id.tvNavNotification));
        navViews.put(TAB_REPORT, findViewById(R.id.tvNavReport));
        navViews.put(TAB_STATISTIC, findViewById(R.id.tvNavStatistic));
        navViews.put(TAB_TARGET, findViewById(R.id.tvNavTarget));
        navViews.put(TAB_PROFILE, findViewById(R.id.tvNavProfile));
    }

    private void setupNavClicks() {
        for (Map.Entry<String, TextView> entry : navViews.entrySet()) {
            entry.getValue().setOnClickListener(v -> switchTab(entry.getKey()));
        }
    }

    private void applyNavLabels() {
        setNavLabel(TAB_HOME, R.string.nav_home);
        setNavLabel(TAB_HEALTH, R.string.nav_health);
        setNavLabel(TAB_FOCUS, R.string.nav_focus);
        setNavLabel(TAB_MAP, R.string.nav_map);
        setNavLabel(TAB_DIARY, R.string.nav_diary);
        setNavLabel(TAB_AI_COACH, R.string.nav_ai_coach);
        setNavLabel(TAB_NOTIFICATION, R.string.nav_notification);
        setNavLabel(TAB_REPORT, R.string.nav_report);
        setNavLabel(TAB_STATISTIC, R.string.nav_statistic);
        setNavLabel(TAB_TARGET, R.string.nav_target);
        setNavLabel(TAB_PROFILE, R.string.nav_profile);
    }

    private void setNavLabel(String tab, int stringRes) {
        TextView view = navViews.get(tab);
        if (view != null) view.setText(stringRes);
    }

    public void openTab(String tab) {
        if (tab == null || !navViews.containsKey(tab)) return;
        switchTab(tab);
    }

    private Fragment createFragmentForTab(String tab) {
        switch (tab) {
            case TAB_HEALTH:
                return DashboardScreenFragment.newInstance(R.layout.activity_health_tracker);
            case TAB_FOCUS:
                return new FocusFragment();
            case TAB_MAP:
                return new RunningMapFragment();
            case TAB_DIARY:
                return new NutritionDiaryFragment();
            case TAB_AI_COACH:
                return DashboardScreenFragment.newInstance(R.layout.activity_ai_coach_chat);
            case TAB_NOTIFICATION:
                return DashboardScreenFragment.newInstance(R.layout.activity_notification_center);
            case TAB_REPORT:
                return DashboardScreenFragment.newInstance(R.layout.activity_monthly_report);
            case TAB_STATISTIC:
                return DashboardScreenFragment.newInstance(R.layout.activity_activity_history);
            case TAB_TARGET:
                return DashboardScreenFragment.newInstance(R.layout.activity_set_month_goal);
            case TAB_PROFILE:
                return new ProfileFragment();
            case TAB_HOME:
            default:
                return new HomeFragment();
        }
    }

    private void switchTab(String tab) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment current = fm.findFragmentByTag(currentTab);
        if (current != null) ft.hide(current);

        Fragment target = fm.findFragmentByTag(tab);
        if (target == null) {
            target = createFragmentForTab(tab);
            ft.add(R.id.mainContainer, target, tab);
        } else {
            ft.show(target);
        }

        currentTab = tab;
        ft.commit();
        updateBottomNav(tab);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_TAB, currentTab);
    }

    private void updateBottomNav(String selectedTab) {
        int accent = settingsPreferences != null ? settingsPreferences.getAccentColor(this) : getColor(R.color.primary);
        int selectedBg = ColorUtils.setAlphaComponent(accent, 32);

        for (Map.Entry<String, TextView> entry : navViews.entrySet()) {
            TextView view = entry.getValue();
            boolean selected = entry.getKey().equals(selectedTab);
            if (selected) {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(selectedBg);
                drawable.setCornerRadius(getResources().getDisplayMetrics().density * 24f);
                view.setBackground(drawable);
            } else {
                view.setBackground(null);
            }
            view.setTextColor(selected ? accent : getColor(R.color.on_surface_variant));
            view.setAllCaps(false);
            view.setTypeface(view.getTypeface(), selected ? Typeface.BOLD : Typeface.NORMAL);
            view.setAlpha(selected ? 1f : 0.9f);
        }
    }
}