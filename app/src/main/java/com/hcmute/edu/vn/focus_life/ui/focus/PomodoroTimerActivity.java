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
import com.hcmute.edu.vn.focus_life.core.focus.PomodoroAlarmScheduler;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.PomodoroSessionEntity;
import com.hcmute.edu.vn.focus_life.data.repository.FocusTaskRepository;
import com.hcmute.edu.vn.focus_life.data.repository.HealthMetricsRepository;
import com.hcmute.edu.vn.focus_life.service.PomodoroService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PomodoroTimerActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";

    private static final String STATE_REMAINING = "state_remaining";
    private static final String STATE_RUNNING = "state_running";
    private static final String STATE_PAUSED = "state_paused";
    private static final String STATE_STARTED_AT = "state_started_at";

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
        sessionDurationMillis = config.focusMinutes * 60L * 1000L;
        remainingMillis = sessionDurationMillis;
        startedAt = System.currentTimeMillis();
        tvTaskName.setText(taskTitle);
        tvSessionMeta.setText("Focus " + config.focusMinutes + " phút · Break ngắn " + config.shortBreakMinutes + " phút");

        if (savedInstanceState != null) {
            remainingMillis = savedInstanceState.getLong(STATE_REMAINING, sessionDurationMillis);
            running = savedInstanceState.getBoolean(STATE_RUNNING, false);
            paused = savedInstanceState.getBoolean(STATE_PAUSED, false);
            startedAt = savedInstanceState.getLong(STATE_STARTED_AT, System.currentTimeMillis());
        } else {
            startFocusSession();
        }

        updateDndStatus();
        updateTimerUi(remainingMillis);

        btnPauseResume.setOnClickListener(v -> {
            if (running) {
                pauseSession();
            } else {
                resumeSession();
            }
        });
        btnEnd.setOnClickListener(v -> finishSession(false));
        tvMuteStatus.setOnClickListener(v -> {
            if (config.autoDnd && !hasNotificationPolicyAccess()) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
        });

        if (running) {
            startCountdown(remainingMillis);
        }
    }

    private void startFocusSession() {
        running = true;
        paused = false;
        startedAt = System.currentTimeMillis();
        sessionSaved = false;
        startForegroundServiceCompat(buildServiceIntent(PomodoroService.ACTION_START));
        PomodoroAlarmScheduler.scheduleCompletion(this, taskTitle, config.focusMinutes, remainingMillis);
        startCountdown(remainingMillis);
    }

    private void pauseSession() {
        running = false;
        paused = true;
        cancelTimer();
        PomodoroAlarmScheduler.cancel(this);
        btnPauseResume.setText("▶");
    }

    private void resumeSession() {
        running = true;
        paused = false;
        PomodoroAlarmScheduler.scheduleCompletion(this, taskTitle, config.focusMinutes, remainingMillis);
        startCountdown(remainingMillis);
    }

    private void startCountdown(long durationMillis) {
        cancelTimer();
        running = true;
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
                finishSession(true);
            }
        };
        countDownTimer.start();
    }

    private void finishSession(boolean completed) {
        cancelTimer();
        PomodoroAlarmScheduler.cancel(this);
        running = false;
        paused = false;
        stopService(buildServiceIntent(PomodoroService.ACTION_STOP));
        if (!sessionSaved) {
            sessionSaved = true;
            saveSession(completed);
        }
        Toast.makeText(this,
                completed ? "Pomodoro hoàn thành, giữ nhịp rất tốt" : "Đã kết thúc phiên focus",
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveSession(boolean completed) {
        PomodoroSessionEntity entity = new PomodoroSessionEntity();
        entity.sessionUuid = UUID.randomUUID().toString();
        entity.taskName = taskTitle;
        entity.sessionType = Constants.SESSION_TYPE_FOCUS;
        entity.durationMinutes = config.focusMinutes;
        entity.startedAt = startedAt;
        entity.endedAt = System.currentTimeMillis();
        entity.completed = completed;
        entity.synced = false;

        AppDatabase database = FocusLifeApp.getInstance().getDatabase();
        executors.diskIO().execute(() -> database.pomodoroDao().insert(entity));

        Map<String, Object> firestoreData = new HashMap<>();
        firestoreData.put("sessionUuid", entity.sessionUuid);
        firestoreData.put("taskName", entity.taskName);
        firestoreData.put("sessionType", entity.sessionType);
        firestoreData.put("durationMinutes", entity.durationMinutes);
        firestoreData.put("startedAt", entity.startedAt);
        firestoreData.put("endedAt", entity.endedAt);
        firestoreData.put("completed", entity.completed);
        firestoreData.put("updatedAt", System.currentTimeMillis());
        new HealthMetricsRepository().savePomodoroSession(entity.sessionUuid, firestoreData);

        if (completed && taskId != null && !taskId.trim().isEmpty()) {
            String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
            repository.incrementCompletedPomodoro(uid, taskId, null);
        }
    }

    private Intent buildServiceIntent(String action) {
        Intent intent = new Intent(this, PomodoroService.class);
        intent.setAction(action);
        intent.putExtra(PomodoroService.EXTRA_TASK_TITLE, taskTitle);
        intent.putExtra(PomodoroService.EXTRA_DURATION_MINUTES, config.focusMinutes);
        intent.putExtra(PomodoroService.EXTRA_ENABLE_DND, config.autoDnd);
        return intent;
    }

    private void updateTimerUi(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        tvTimer.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int progress = (int) ((sessionDurationMillis - millis) * 100 / Math.max(1L, sessionDurationMillis));
        tvProgress.setText(progress + "% hoàn thành");
    }

    private void updateDndStatus() {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        if (isFinishing() && !sessionSaved) {
            PomodoroAlarmScheduler.cancel(this);
            stopService(buildServiceIntent(PomodoroService.ACTION_STOP));
        }
    }
}