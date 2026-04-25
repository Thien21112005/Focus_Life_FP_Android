package com.hcmute.edu.vn.focus_life.ui.focus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.data.repository.FocusTaskRepository;
import com.hcmute.edu.vn.focus_life.domain.model.FocusTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FocusTaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";

    private final FocusTaskRepository repository = new FocusTaskRepository();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, dd/MM/yyyy · HH:mm", new Locale("vi", "VN"));

    private String taskId;
    private String uid;
    private FocusTask currentTask;
    private ListenerRegistration taskRegistration;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvStatus;
    private TextView tvStartAt;
    private TextView tvDueAt;
    private TextView tvCategory;
    private TextView tvPriority;
    private TextView tvPomodoroStats;
    private CheckBox cbCompleted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_task_detail);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId == null || taskId.trim().isEmpty()) {
            finish();
            return;
        }

        uid = FocusLifeApp.getInstance().getSessionManager().requireUid();

        tvTitle = findViewById(R.id.tvDetailTaskTitle);
        tvDescription = findViewById(R.id.tvDetailTaskDescription);
        tvStatus = findViewById(R.id.tvDetailTaskStatus);
        tvStartAt = findViewById(R.id.tvDetailTaskStartValue);
        tvDueAt = findViewById(R.id.tvDetailTaskDueValue);
        tvCategory = findViewById(R.id.tvDetailTaskCategory);
        tvPriority = findViewById(R.id.tvDetailTaskPriority);
        tvPomodoroStats = findViewById(R.id.tvDetailTaskPomodoroStats);
        cbCompleted = findViewById(R.id.cbDetailTaskCompleted);

        MaterialButton btnEdit = findViewById(R.id.btnEditTask);
        MaterialButton btnDelete = findViewById(R.id.btnDeleteTask);
        MaterialButton btnStartPomodoro = findViewById(R.id.btnStartTaskPomodoro);
        TextView btnBack = findViewById(R.id.tvBackFocusDetail);

        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, FocusTaskEditorActivity.class);
            intent.putExtra(FocusTaskEditorActivity.EXTRA_TASK_ID, taskId);
            startActivity(intent);
        });
        btnDelete.setOnClickListener(v -> deleteTask());
        btnStartPomodoro.setOnClickListener(v -> startPomodoro());
        cbCompleted.setOnClickListener(v -> toggleCompleted());
    }

    @Override
    protected void onStart() {
        super.onStart();
        observeTask();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (taskRegistration != null) {
            taskRegistration.remove();
            taskRegistration = null;
        }
    }

    private void observeTask() {
        if (taskRegistration != null) {
            taskRegistration.remove();
        }
        taskRegistration = repository.observeTask(uid, taskId, task -> runOnUiThread(() -> {
            if (task == null) {
                Toast.makeText(this, "Task không còn tồn tại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentTask = task;
            bindTask(task);
        }));
    }

    private void bindTask(@NonNull FocusTask task) {
        tvTitle.setText(task.title);
        tvDescription.setText(task.description == null || task.description.trim().isEmpty()
                ? "Task này chưa có ghi chú chi tiết."
                : task.description);
        tvStatus.setText(resolveStatus(task));
        tvStartAt.setText(formatDate(task.startAt));
        tvDueAt.setText(formatDate(task.dueAt));
        tvCategory.setText(task.category == null || task.category.trim().isEmpty() ? "Chưa chọn" : task.category);
        tvPriority.setText(resolvePriority(task.priority));
        applyPriorityStyle(task.priority, tvPriority);
        tvPomodoroStats.setText(task.completedPomodoros + "/" + Math.max(1, task.estimatedPomodoros) + " phiên đã hoàn thành");
        cbCompleted.setChecked(task.completed);
    }

    private void toggleCompleted() {
        if (currentTask == null) return;
        boolean next = cbCompleted.isChecked();
        repository.updateCompleted(uid, currentTask.id, next, (success, error) -> runOnUiThread(() -> {
            if (!success) {
                cbCompleted.setChecked(!next);
                Toast.makeText(this, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void deleteTask() {
        if (currentTask == null) return;
        repository.deleteTask(uid, currentTask.id, (success, error) -> runOnUiThread(() -> {
            if (success) {
                Toast.makeText(this, "Đã xóa task", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Xóa task thất bại", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void startPomodoro() {
        if (currentTask == null) return;
        Intent intent = new Intent(this, PomodoroTimerActivity.class);
        intent.putExtra(PomodoroTimerActivity.EXTRA_TASK_ID, currentTask.id);
        intent.putExtra(PomodoroTimerActivity.EXTRA_TASK_TITLE, currentTask.title);
        startActivity(intent);
    }

    @NonNull
    private String formatDate(long timeMillis) {
        if (timeMillis <= 0L) {
            return "--";
        }
        return dateTimeFormat.format(new Date(timeMillis));
    }

    @NonNull
    private String resolveStatus(@NonNull FocusTask task) {
        String status = task.resolveStatus(System.currentTimeMillis());
        switch (status) {
            case "completed":
                return "Đã hoàn thành";
            case "overdue":
                return "Đang trễ hạn";
            case "upcoming":
                return "Sắp tới";
            default:
                return "Đang thực hiện";
        }
    }

    @NonNull
    private String resolvePriority(@Nullable String priority) {
        if (priority == null) return "Ưu tiên vừa";
        switch (priority.toLowerCase(Locale.ROOT)) {
            case "high":
                return "Ưu tiên cao";
            case "low":
                return "Ưu tiên thấp";
            default:
                return "Ưu tiên vừa";
        }
    }

    private void applyPriorityStyle(@Nullable String priority, @NonNull TextView textView) {
        int backgroundRes;
        int textColor;

        String normalized = priority == null ? "medium" : priority.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "high":
                backgroundRes = R.drawable.bg_chip_priority_high;
                textColor = ContextCompat.getColor(this, R.color.priority_high_text);
                break;
            case "low":
                backgroundRes = R.drawable.bg_chip_priority_low;
                textColor = ContextCompat.getColor(this, R.color.priority_low_text);
                break;
            default:
                backgroundRes = R.drawable.bg_chip_priority_medium;
                textColor = ContextCompat.getColor(this, R.color.priority_medium_text);
                break;
        }

        textView.setBackgroundResource(backgroundRes);
        textView.setTextColor(textColor);
    }
}
