package com.hcmute.edu.vn.focus_life.ui.focus;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FocusTaskEditorActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";

    private static final String ADD_CATEGORY_OPTION = "+ Thêm category mới";
    private static final String DEFAULT_CATEGORY = "Học tập";
    private static final String DEFAULT_PRIORITY = "medium";
    private static final String INVALID_TIME_MESSAGE = "Thời gian kết thúc không được bé hơn thời gian bắt đầu";
    private static final long DEFAULT_TASK_DURATION_MS = 60L * 60L * 1000L;

    private static final String PRIORITY_LOW_VALUE = "low";
    private static final String PRIORITY_MEDIUM_VALUE = "medium";
    private static final String PRIORITY_HIGH_VALUE = "high";
    private static final String PRIORITY_LOW_LABEL = "Thấp";
    private static final String PRIORITY_MEDIUM_LABEL = "Trung bình";
    private static final String PRIORITY_HIGH_LABEL = "Cao";

    private final FocusTaskRepository repository = new FocusTaskRepository();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, dd/MM/yyyy · HH:mm", new Locale("vi", "VN"));

    private EditText etTitle;
    private EditText etDescription;
    private AutoCompleteTextView etCategory;
    private AutoCompleteTextView etPriority;
    private EditText etPomodoroEstimate;
    private TextView tvStartAt;
    private TextView tvDueAt;
    private MaterialButton btnDelete;
    private FocusCategoryManager categoryManager;

    private FocusTask currentTask;
    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar dueCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_task_editor);

        categoryManager = new FocusCategoryManager(this);

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

        setupCategoryDropdown();
        setupPriorityDropdown();

        long now = System.currentTimeMillis();
        startCalendar.setTimeInMillis(now);
        dueCalendar.setTimeInMillis(now + DEFAULT_TASK_DURATION_MS);
        bindDateViews();

        findViewById(R.id.cardFocusTaskStart).setOnClickListener(v ->
                pickDateTime(startCalendar, pickedCalendar -> {
                    if (pickedCalendar.getTimeInMillis() >= dueCalendar.getTimeInMillis()) {
                        showInvalidTimeMessage();
                        return;
                    }
                    startCalendar.setTimeInMillis(pickedCalendar.getTimeInMillis());
                    bindDateViews();
                })
        );
        findViewById(R.id.cardFocusTaskDue).setOnClickListener(v ->
                pickDateTime(dueCalendar, pickedCalendar -> {
                    if (pickedCalendar.getTimeInMillis() <= startCalendar.getTimeInMillis()) {
                        showInvalidTimeMessage();
                        return;
                    }
                    dueCalendar.setTimeInMillis(pickedCalendar.getTimeInMillis());
                    bindDateViews();
                })
        );

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTask());
        btnDelete.setOnClickListener(v -> deleteTask());

        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId == null || taskId.trim().isEmpty()) {
            tvHeader.setText("Tạo task mới");
            currentTask = FocusTask.createEmpty();
            etCategory.setText(getDefaultCategory(), false);
            setPrioritySelection(DEFAULT_PRIORITY);
            etPomodoroEstimate.setText("1");
            btnDelete.setVisibility(View.GONE);
        } else {
            tvHeader.setText("Chỉnh sửa task");
            loadTask(taskId);
        }
    }

    private void setupCategoryDropdown() {
        List<String> options = new ArrayList<>(categoryManager.getCategories());
        options.add(ADD_CATEGORY_OPTION);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_focus_dropdown, options);
        etCategory.setAdapter(adapter);
        etCategory.setThreshold(0);
        etCategory.setOnClickListener(v -> etCategory.showDropDown());
        etCategory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etCategory.showDropDown();
        });
        etCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = String.valueOf(parent.getItemAtPosition(position));
            if (ADD_CATEGORY_OPTION.equals(selected)) {
                etCategory.setText("", false);
                showAddCategoryDialog(null);
            } else {
                etCategory.setText(selected, false);
            }
        });
    }

    private void setupPriorityDropdown() {
        List<String> priorities = new ArrayList<>();
        priorities.add(PRIORITY_LOW_LABEL);
        priorities.add(PRIORITY_MEDIUM_LABEL);
        priorities.add(PRIORITY_HIGH_LABEL);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_focus_dropdown, priorities);
        etPriority.setAdapter(adapter);
        etPriority.setThreshold(0);
        etPriority.setOnClickListener(v -> etPriority.showDropDown());
        etPriority.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etPriority.showDropDown();
        });
        etPriority.setOnItemClickListener((parent, view, position, id) -> {
            String selected = String.valueOf(parent.getItemAtPosition(position));
            setPrioritySelection(selected);
        });
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
            bindCategoryValue(task.category);
            setPrioritySelection(task.priority);
            etPomodoroEstimate.setText(String.valueOf(Math.max(1, task.estimatedPomodoros)));
            startCalendar.setTimeInMillis(task.startAt > 0 ? task.startAt : System.currentTimeMillis());
            dueCalendar.setTimeInMillis(task.dueAt > 0 ? task.dueAt : startCalendar.getTimeInMillis() + DEFAULT_TASK_DURATION_MS);
            if (dueCalendar.getTimeInMillis() <= startCalendar.getTimeInMillis()) {
                dueCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + DEFAULT_TASK_DURATION_MS);
            }
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

        String category = etCategory.getText().toString().trim();
        if (TextUtils.isEmpty(category) || ADD_CATEGORY_OPTION.equals(category)) {
            category = getDefaultCategory();
        }
        categoryManager.addCategory(category);

        if (dueCalendar.getTimeInMillis() <= startCalendar.getTimeInMillis()) {
            showInvalidTimeMessage();
            return;
        }

        currentTask.title = title;
        currentTask.description = etDescription.getText().toString().trim();
        currentTask.category = category;
        currentTask.priority = toPriorityValue(etPriority.getText().toString());
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

    private void pickDateTime(@NonNull Calendar currentValue, @NonNull DateTimeCallback callback) {
        Calendar pickedCalendar = (Calendar) currentValue.clone();

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            pickedCalendar.set(Calendar.YEAR, year);
            pickedCalendar.set(Calendar.MONTH, month);
            pickedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                pickedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                pickedCalendar.set(Calendar.MINUTE, minute);
                pickedCalendar.set(Calendar.SECOND, 0);
                pickedCalendar.set(Calendar.MILLISECOND, 0);
                callback.onPicked(pickedCalendar);
            }, pickedCalendar.get(Calendar.HOUR_OF_DAY), pickedCalendar.get(Calendar.MINUTE), true).show();
        }, pickedCalendar.get(Calendar.YEAR), pickedCalendar.get(Calendar.MONTH), pickedCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showInvalidTimeMessage() {
        Toast.makeText(this, INVALID_TIME_MESSAGE, Toast.LENGTH_SHORT).show();
    }

    private interface DateTimeCallback {
        void onPicked(@NonNull Calendar pickedCalendar);
    }

    private void showAddCategoryDialog(@Nullable String prefill) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Ví dụ: Android, Đọc sách, Sức khỏe");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        if (prefill != null) {
            input.setText(prefill);
            input.setSelection(prefill.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thêm category mới")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Thêm", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (TextUtils.isEmpty(value)) {
                input.setError("Nhập tên category");
                return;
            }
            categoryManager.addCategory(value);
            setupCategoryDropdown();
            bindCategoryValue(value);
            Toast.makeText(this, "Đã thêm category", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void bindCategoryValue(@Nullable String value) {
        String category = emptyToDefault(value, getDefaultCategory());
        categoryManager.addCategory(category);
        setupCategoryDropdown();
        etCategory.setText(category, false);
    }

    private void setPrioritySelection(@Nullable String value) {
        String label = toPriorityLabel(value);
        etPriority.setText(label, false);
        applyPriorityStyle(label);
    }

    private void applyPriorityStyle(@Nullable String value) {
        String priority = toPriorityValue(value);
        if (PRIORITY_LOW_VALUE.equals(priority)) {
            etPriority.setTextColor(Color.parseColor("#15803D"));
        } else if (PRIORITY_HIGH_VALUE.equals(priority)) {
            etPriority.setTextColor(Color.parseColor("#DC2626"));
        } else {
            etPriority.setTextColor(Color.parseColor("#B45309"));
        }
    }

    @NonNull
    private String getDefaultCategory() {
        List<String> categories = categoryManager.getCategories();
        if (categories.isEmpty()) return DEFAULT_CATEGORY;
        return categories.get(0);
    }

    @NonNull
    private String toPriorityValue(@Nullable String value) {
        if (value == null) return DEFAULT_PRIORITY;
        String priority = value.trim().toLowerCase(new Locale("vi", "VN"));

        if (PRIORITY_LOW_VALUE.equals(priority) || "thấp".equals(priority) || "thap".equals(priority)) {
            return PRIORITY_LOW_VALUE;
        }
        if (PRIORITY_HIGH_VALUE.equals(priority) || "cao".equals(priority)) {
            return PRIORITY_HIGH_VALUE;
        }
        if (PRIORITY_MEDIUM_VALUE.equals(priority)
                || "trung bình".equals(priority)
                || "trung binh".equals(priority)
                || "vừa".equals(priority)
                || "vua".equals(priority)) {
            return PRIORITY_MEDIUM_VALUE;
        }
        return DEFAULT_PRIORITY;
    }

    @NonNull
    private String toPriorityLabel(@Nullable String value) {
        String priority = toPriorityValue(value);
        if (PRIORITY_LOW_VALUE.equals(priority)) return PRIORITY_LOW_LABEL;
        if (PRIORITY_HIGH_VALUE.equals(priority)) return PRIORITY_HIGH_LABEL;
        return PRIORITY_MEDIUM_LABEL;
    }

    @NonNull
    private String emptyToDefault(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
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
