package com.hcmute.edu.vn.focus_life.ui.focus;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FocusTaskEditorActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";

    private final FocusTaskRepository repository = new FocusTaskRepository();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, dd/MM/yyyy · HH:mm", new Locale("vi", "VN"));

    private EditText etTitle;
    private EditText etDescription;
    private EditText etCategory;
    private EditText etPriority;
    private EditText etPomodoroEstimate;
    private TextView tvStartAt;
    private TextView tvDueAt;
    private MaterialButton btnDelete;

    private FocusTask currentTask;
    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar dueCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_task_editor);

        etTitle = findViewById(R.id.etFocusTaskTitle);
        etDescription = findViewById(R.id.etFocusTaskDescription);
        etCategory = findViewById(R.id.etFocusTaskCategory);
        etPriority = findViewById(R.id.etFocusTaskPriority);
        etPomodoroEstimate = findViewById(R.id.etFocusTaskPomodoros);
        tvStartAt = findViewById(R.id.tvFocusTaskStartValue);
        tvDueAt = findViewById(R.id.tvFocusTaskDueValue);
        btnDelete = findViewById(R.id.btnDeleteTaskDraft);
        TextView tvHeader = findViewById(R.id.tvFocusEditorTitle);
        MaterialButton btnSave = findViewById(R.id.btnSaveTask);
        MaterialButton btnCancel = findViewById(R.id.btnCancelTask);

        long now = System.currentTimeMillis();
        startCalendar.setTimeInMillis(now);
        dueCalendar.setTimeInMillis(now + 60L * 60L * 1000L);
        bindDateViews();

        findViewById(R.id.cardFocusTaskStart).setOnClickListener(v -> pickDateTime(startCalendar, this::bindDateViews));
        findViewById(R.id.cardFocusTaskDue).setOnClickListener(v -> pickDateTime(dueCalendar, this::bindDateViews));

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTask());
        btnDelete.setOnClickListener(v -> deleteTask());

        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId == null || taskId.trim().isEmpty()) {
            tvHeader.setText("Tạo task mới");
            currentTask = FocusTask.createEmpty();
            btnDelete.setVisibility(android.view.View.GONE);
        } else {
            tvHeader.setText("Chỉnh sửa task");
            loadTask(taskId);
        }
    }

    private void loadTask(@NonNull String taskId) {
        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.getTask(uid, taskId, task -> {
            if (task == null) {
                Toast.makeText(this, "Không tải được task", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentTask = task;
            etTitle.setText(task.title);
            etDescription.setText(task.description);
            etCategory.setText(task.category);
            etPriority.setText(task.priority);
            etPomodoroEstimate.setText(String.valueOf(Math.max(1, task.estimatedPomodoros)));
            startCalendar.setTimeInMillis(task.startAt > 0 ? task.startAt : System.currentTimeMillis());
            dueCalendar.setTimeInMillis(task.dueAt > 0 ? task.dueAt : startCalendar.getTimeInMillis());
            bindDateViews();
        });
    }

    private void saveTask() {
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Nhập tên task giúp mình");
            return;
        }

        if (currentTask == null) {
            currentTask = FocusTask.createEmpty();
        }

        currentTask.title = title;
        currentTask.description = etDescription.getText().toString().trim();
        currentTask.category = emptyToDefault(etCategory.getText().toString().trim(), "Focus");
        currentTask.priority = emptyToDefault(etPriority.getText().toString().trim().toLowerCase(Locale.ROOT), "medium");
        currentTask.startAt = startCalendar.getTimeInMillis();
        currentTask.dueAt = dueCalendar.getTimeInMillis();
        currentTask.estimatedPomodoros = safeInt(etPomodoroEstimate.getText().toString().trim(), 1);
        currentTask.status = currentTask.resolveStatus(System.currentTimeMillis());

        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.saveTask(uid, currentTask, (success, error) -> {
            if (success) {
                Toast.makeText(this, "Đã lưu task", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Lưu task thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTask() {
        if (currentTask == null || currentTask.id == null) {
            finish();
            return;
        }
        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.deleteTask(uid, currentTask.id, (success, error) -> {
            if (success) {
                Toast.makeText(this, "Đã xóa task", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Xóa task thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDateViews() {
        tvStartAt.setText(dateTimeFormat.format(new Date(startCalendar.getTimeInMillis())));
        tvDueAt.setText(dateTimeFormat.format(new Date(dueCalendar.getTimeInMillis())));
    }

    private void pickDateTime(@NonNull Calendar calendar, @NonNull Runnable onComplete) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                onComplete.run();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @NonNull
    private String emptyToDefault(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value;
    }

    private int safeInt(@Nullable String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(1, parsed);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}