package com.hcmute.edu.vn.focus_life.ui.focus;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.common.AppExecutors;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.PomodoroSessionEntity;
import com.hcmute.edu.vn.focus_life.data.repository.FocusTaskRepository;
import com.hcmute.edu.vn.focus_life.service.PomodoroService;

import java.util.UUID;

public class PomodoroTimerActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";

    private static final String STATE_REMAINING = "state_remaining";
    private static final String STATE_RUNNING = "state_running";
    private static final String STATE_PAUSED = "state_paused";
    private static final String STATE_STARTED_AT = "state_started_at";
    private static final String STATE_SESSION_TYPE = "state_session_type";
    private static final String STATE_COMPLETED_FOCUS_COUNT = "state_completed_focus_count";
    private static final String STATE_SESSION_SAVED = "state_session_saved";

    private final FocusTaskRepository repository = new FocusTaskRepository();
    private final AppExecutors executors = new AppExecutors();

    private TextView tvMuteStatus;
    private TextView tvTimer;
    private TextView tvTaskName;
    private TextView tvSessionMeta;
    private TextView tvProgress;
    private TextView btnPauseResume;

    private PomodoroPreferences.Config config;
    private CountDownTimer countDownTimer;
    private long sessionDurationMillis;
    private long remainingMillis;
    private long startedAt;
    private boolean running;
    private boolean paused;
    private boolean sessionSaved;
    private String taskId;
    private String taskTitle;
    private String currentSessionType = Constants.SESSION_TYPE_FOCUS;
    private int completedFocusCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_mode);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskTitle = getIntent().getStringExtra(EXTRA_TASK_TITLE);
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = "Phiên Focus";
        }

        tvMuteStatus = findViewById(R.id.tvMuteStatus);
        tvTimer = findViewById(R.id.tvFocusCountdown);
        tvTaskName = findViewById(R.id.tvFocusTaskTitle);
        tvSessionMeta = findViewById(R.id.tvFocusSessionMeta);
        tvProgress = findViewById(R.id.tvFocusProgress);
        btnPauseResume = findViewById(R.id.btnFocusPauseResume);
        TextView btnEnd = findViewById(R.id.btnFocusEndEarly);

        config = new PomodoroPreferences(this).getConfig();
        tvTaskName.setText(taskTitle);

        if (savedInstanceState != null) {
            currentSessionType = savedInstanceState.getString(STATE_SESSION_TYPE, Constants.SESSION_TYPE_FOCUS);
            completedFocusCount = savedInstanceState.getInt(STATE_COMPLETED_FOCUS_COUNT, 0);
            running = savedInstanceState.getBoolean(STATE_RUNNING, false);
            paused = savedInstanceState.getBoolean(STATE_PAUSED, false);
            startedAt = savedInstanceState.getLong(STATE_STARTED_AT, System.currentTimeMillis());
            sessionSaved = savedInstanceState.getBoolean(STATE_SESSION_SAVED, false);
            sessionDurationMillis = getDurationMillis(currentSessionType);
            remainingMillis = savedInstanceState.getLong(STATE_REMAINING, sessionDurationMillis);
            bindCurrentSessionUi();
        } else {
            startSession(Constants.SESSION_TYPE_FOCUS);
        }

        updateDndStatus();
        updateTimerUi(remainingMillis);
        btnPauseResume.setText(running ? "❚❚" : "▶");

        btnPauseResume.setOnClickListener(v -> {
            if (running) {
                pauseSession();
            } else {
                resumeSession();
            }
        });
        btnEnd.setOnClickListener(v -> endEarly());
        tvMuteStatus.setOnClickListener(v -> {
            if (config.autoDnd && isFocusSession() && !hasNotificationPolicyAccess()) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
        });

        if (running) {
            startCountdown(remainingMillis);
        }
    }

    private void startSession(@NonNull String sessionType) {
        currentSessionType = sessionType;
        sessionDurationMillis = getDurationMillis(sessionType);
        remainingMillis = sessionDurationMillis;
        startedAt = System.currentTimeMillis();
        running = true;
        paused = false;
        sessionSaved = false;

        bindCurrentSessionUi();
        updateDndStatus();
        updateTimerUi(remainingMillis);

        stopService(buildServiceIntent(PomodoroService.ACTION_STOP));
        startForegroundServiceCompat(buildServiceIntent(PomodoroService.ACTION_START));
        startCountdown(remainingMillis);
    }

    private void pauseSession() {
        running = false;
        paused = true;
        cancelTimer();
        btnPauseResume.setText("▶");
    }

    private void resumeSession() {
        running = true;
        paused = false;
        startForegroundServiceCompat(buildServiceIntent(PomodoroService.ACTION_START));
        startCountdown(remainingMillis);
    }

    private void startCountdown(long durationMillis) {
        cancelTimer();
        running = true;
        paused = false;
        btnPauseResume.setText("❚❚");
        countDownTimer = new CountDownTimer(durationMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updateTimerUi(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                remainingMillis = 0L;
                updateTimerUi(0L);
                handleSessionFinished();
            }
        };
        countDownTimer.start();
    }

    private void handleSessionFinished() {
        cancelTimer();
        running = false;
        paused = false;
        stopService(buildServiceIntent(PomodoroService.ACTION_STOP));

        if (!sessionSaved) {
            sessionSaved = true;
            saveSession(true);
        }

        if (isFocusSession()) {
            completedFocusCount++;
            String nextSession = shouldStartLongBreak()
                    ? Constants.SESSION_TYPE_LONG_BREAK
                    : Constants.SESSION_TYPE_SHORT_BREAK;
            Toast.makeText(this, "Pomodoro hoàn thành, tự chuyển sang nghỉ", Toast.LENGTH_SHORT).show();
            startSession(nextSession);
        } else {
            Toast.makeText(this, "Hết giờ nghỉ, tự chuyển sang phiên Focus mới", Toast.LENGTH_SHORT).show();
            startSession(Constants.SESSION_TYPE_FOCUS);
        }
    }

    private void endEarly() {
        cancelTimer();
        running = false;
        paused = false;
        stopService(buildServiceIntent(PomodoroService.ACTION_STOP));
        if (!sessionSaved) {
            sessionSaved = true;
            saveSession(false);
        }
        Toast.makeText(this, "Đã kết thúc phiên hiện tại", Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean shouldStartLongBreak() {
        int cycleTarget = Math.max(1, config.cyclesUntilLongBreak);
        return completedFocusCount > 0 && completedFocusCount % cycleTarget == 0;
    }

    private void saveSession(boolean completed) {
        PomodoroSessionEntity entity = new PomodoroSessionEntity();
        entity.sessionUuid = UUID.randomUUID().toString();
        entity.taskName = taskTitle;
        entity.sessionType = currentSessionType;
        entity.durationMinutes = getDurationMinutes(currentSessionType);
        entity.startedAt = startedAt;
        entity.endedAt = System.currentTimeMillis();
        entity.completed = completed;
        entity.synced = false;

        AppDatabase database = FocusLifeApp.getInstance().getDatabase();
        executors.diskIO().execute(() -> database.pomodoroDao().insert(entity));

        if (completed && isFocusSession() && taskId != null && !taskId.trim().isEmpty()) {
            String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
            repository.incrementCompletedPomodoro(uid, taskId, null);
        }
    }

    private Intent buildServiceIntent(String action) {
        Intent intent = new Intent(this, PomodoroService.class);
        intent.setAction(action);
        intent.putExtra(PomodoroService.EXTRA_TASK_TITLE, buildNotificationTitle());
        intent.putExtra(PomodoroService.EXTRA_DURATION_MINUTES, getDurationMinutes(currentSessionType));
        intent.putExtra(PomodoroService.EXTRA_ENABLE_DND, isFocusSession() && config.autoDnd);
        return intent;
    }

    private void bindCurrentSessionUi() {
        tvTaskName.setText(isFocusSession() ? taskTitle : getSessionTitle(currentSessionType));
        tvSessionMeta.setText(buildSessionMeta());
    }

    private String buildSessionMeta() {
        if (Constants.SESSION_TYPE_LONG_BREAK.equals(currentSessionType)) {
            return "Nghỉ dài " + config.longBreakMinutes + " phút · Sau " + Math.max(1, config.cyclesUntilLongBreak) + " phiên focus";
        }
        if (Constants.SESSION_TYPE_SHORT_BREAK.equals(currentSessionType)) {
            return "Nghỉ ngắn " + config.shortBreakMinutes + " phút · Phiên focus tiếp theo sẽ tự bắt đầu";
        }
        return "Focus " + config.focusMinutes + " phút · "
                + "Đã xong " + completedFocusCount + " phiên · "
                + "Break ngắn " + config.shortBreakMinutes + " phút";
    }

    private String buildNotificationTitle() {
        if (isFocusSession()) {
            return taskTitle;
        }
        return getSessionTitle(currentSessionType);
    }

    private String getSessionTitle(@NonNull String sessionType) {
        if (Constants.SESSION_TYPE_LONG_BREAK.equals(sessionType)) {
            return "Nghỉ dài";
        }
        if (Constants.SESSION_TYPE_SHORT_BREAK.equals(sessionType)) {
            return "Nghỉ ngắn";
        }
        return "Phiên Focus";
    }

    private long getDurationMillis(@NonNull String sessionType) {
        return getDurationMinutes(sessionType) * 60L * 1000L;
    }

    private int getDurationMinutes(@NonNull String sessionType) {
        if (Constants.SESSION_TYPE_LONG_BREAK.equals(sessionType)) {
            return Math.max(1, config.longBreakMinutes);
        }
        if (Constants.SESSION_TYPE_SHORT_BREAK.equals(sessionType)) {
            return Math.max(1, config.shortBreakMinutes);
        }
        return Math.max(1, config.focusMinutes);
    }

    private boolean isFocusSession() {
        return Constants.SESSION_TYPE_FOCUS.equals(currentSessionType);
    }

    private void updateTimerUi(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        tvTimer.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int progress = (int) ((sessionDurationMillis - millis) * 100 / Math.max(1L, sessionDurationMillis));
        String label;
        if (Constants.SESSION_TYPE_LONG_BREAK.equals(currentSessionType)) {
            label = "% nghỉ dài";
        } else if (Constants.SESSION_TYPE_SHORT_BREAK.equals(currentSessionType)) {
            label = "% nghỉ ngắn";
        } else {
            label = "% hoàn thành";
        }
        tvProgress.setText(progress + label);
    }

    private void updateDndStatus() {
        if (!isFocusSession()) {
            tvMuteStatus.setText("☕  Đang trong giờ nghỉ");
            return;
        }
        if (!config.autoDnd) {
            tvMuteStatus.setText("🔔  DND đang tắt trong setting");
            return;
        }
        if (hasNotificationPolicyAccess()) {
            tvMuteStatus.setText("🔕  Thông báo đã tắt");
        } else {
            tvMuteStatus.setText("⚙️  Cấp quyền DND để tắt app khác");
        }
    }

    private boolean hasNotificationPolicyAccess() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return manager != null && manager.isNotificationPolicyAccessGranted();
    }

    private void startForegroundServiceCompat(Intent intent) {
        androidx.core.content.ContextCompat.startForegroundService(this, intent);
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_REMAINING, remainingMillis);
        outState.putBoolean(STATE_RUNNING, running);
        outState.putBoolean(STATE_PAUSED, paused);
        outState.putLong(STATE_STARTED_AT, startedAt);
        outState.putString(STATE_SESSION_TYPE, currentSessionType);
        outState.putInt(STATE_COMPLETED_FOCUS_COUNT, completedFocusCount);
        outState.putBoolean(STATE_SESSION_SAVED, sessionSaved);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        if (isFinishing()) {
            stopService(buildServiceIntent(PomodoroService.ACTION_STOP));
        }
    }
}
