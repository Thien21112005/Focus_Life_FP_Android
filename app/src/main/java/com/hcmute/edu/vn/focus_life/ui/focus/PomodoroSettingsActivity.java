package com.hcmute.edu.vn.focus_life.ui.focus;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;

public class PomodoroSettingsActivity extends AppCompatActivity {

    private EditText etFocusMinutes;
    private EditText etShortBreakMinutes;
    private EditText etLongBreakMinutes;
    private EditText etCycles;
    private Switch switchAutoDnd;
    private PomodoroPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro_settings);

        preferences = new PomodoroPreferences(this);
        etFocusMinutes = findViewById(R.id.etSettingFocusMinutes);
        etShortBreakMinutes = findViewById(R.id.etSettingShortBreakMinutes);
        etLongBreakMinutes = findViewById(R.id.etSettingLongBreakMinutes);
        etCycles = findViewById(R.id.etSettingCycles);
        switchAutoDnd = findViewById(R.id.switchSettingAutoDnd);
        MaterialButton btnSave = findViewById(R.id.btnSavePomodoroSettings);
        MaterialButton btnOpenDnd = findViewById(R.id.btnOpenDndAccess);
        findViewById(R.id.tvBackPomodoroSettings).setOnClickListener(v -> finish());

        bindConfig();

        btnSave.setOnClickListener(v -> saveConfig());
        btnOpenDnd.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            Toast.makeText(this, "Bật quyền Do Not Disturb để app tắt thông báo khi focus", Toast.LENGTH_LONG).show();
        });
    }

    private void bindConfig() {
        PomodoroPreferences.Config config = preferences.getConfig();
        etFocusMinutes.setText(String.valueOf(config.focusMinutes));
        etShortBreakMinutes.setText(String.valueOf(config.shortBreakMinutes));
        etLongBreakMinutes.setText(String.valueOf(config.longBreakMinutes));
        etCycles.setText(String.valueOf(config.cyclesUntilLongBreak));
        switchAutoDnd.setChecked(config.autoDnd);
    }

    private void saveConfig() {
        PomodoroPreferences.Config config = new PomodoroPreferences.Config();
        config.focusMinutes = positive(etFocusMinutes.getText().toString(), 25);
        config.shortBreakMinutes = positive(etShortBreakMinutes.getText().toString(), 5);
        config.longBreakMinutes = positive(etLongBreakMinutes.getText().toString(), 15);
        config.cyclesUntilLongBreak = positive(etCycles.getText().toString(), 4);
        config.autoDnd = switchAutoDnd.isChecked();
        preferences.save(config);
        Toast.makeText(this, "Đã lưu cấu hình Pomodoro", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int positive(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(1, parsed);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}