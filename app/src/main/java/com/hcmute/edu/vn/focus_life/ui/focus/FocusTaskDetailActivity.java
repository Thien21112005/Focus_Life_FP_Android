package com.hcmute.edu.vn.focus_life.ui.focus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
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
    private FocusTask currentTask;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvStatus;
    private TextView tvSchedule;
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

        tvTitle = findViewById(R.id.tvDetailTaskTitle);
        tvDescription = findViewById(R.id.tvDetailTaskDescription);
        tvStatus = findViewById(R.id.tvDetailTaskStatus);
        tvSchedule = findViewById(R.id.tvDetailTaskSchedule);
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
    protected void onResume() {
        super.onResume();
        loadTask();
    }

    private void loadTask() {
        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.getTask(uid, taskId, task -> {
            if (task == null) {
                Toast.makeText(this, "Task không còn tồn tại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentTask = task;
            bindTask(task);
        });
    }

    private void bindTask(@NonNull FocusTask task) {
        tvTitle.setText(task.title);
        tvDescription.setText(task.description == null || task.description.trim().isEmpty()
                ? "Task này chưa có ghi chú chi tiết."
                : task.description);
        tvStatus.setText(resolveStatus(task));
        tvSchedule.setText(buildSchedule(task));
        tvCategory.setText(task.category);
        tvPriority.setText(resolvePriority(task.priority));
        tvPomodoroStats.setText(task.completedPomodoros + "/" + Math.max(1, task.estimatedPomodoros) + " phiên đã hoàn thành");
        cbCompleted.setChecked(task.completed);
    }

    private void toggleCompleted() {
        if (currentTask == null) return;
        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        boolean next = cbCompleted.isChecked();
        repository.updateCompleted(uid, currentTask.id, next, (success, error) -> {
            if (success) {
                loadTask();
            } else {
                cbCompleted.setChecked(!next);
                Toast.makeText(this, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTask() {
        if (currentTask == null) return;
        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.deleteTask(uid, currentTask.id, (success, error) -> {
            if (success) {
                Toast.makeText(this, "Đã xóa task", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Xóa task thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPomodoro() {
        if (currentTask == null) return;
        Intent intent = new Intent(this, PomodoroTimerActivity.class);
        intent.putExtra(PomodoroTimerActivity.EXTRA_TASK_ID, currentTask.id);
        intent.putExtra(PomodoroTimerActivity.EXTRA_TASK_TITLE, currentTask.title);
        startActivity(intent);
    }

    @NonNull
    private String buildSchedule(@NonNull FocusTask task) {
        if (task.startAt > 0 && task.dueAt > 0) {
            return dateTimeFormat.format(new Date(task.startAt)) + " → " + dateTimeFormat.format(new Date(task.dueAt));
        }
        return dateTimeFormat.format(new Date(task.resolveAnchorTime()));
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
}