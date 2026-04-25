package com.hcmute.edu.vn.focus_life.ui.focus;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.data.repository.FocusTaskRepository;
import com.hcmute.edu.vn.focus_life.domain.model.FocusTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FocusFragment extends Fragment implements FocusTaskAdapter.Listener {

    private static final int FILTER_ACTIVE = 0;
    private static final int FILTER_COMPLETED = 1;

    private final FocusTaskRepository repository = new FocusTaskRepository();
    private final List<FocusTask> allTasks = new ArrayList<>();

    private FocusTaskAdapter adapter;
    private TextView tvStatsPrimary;
    private TextView tvStatsSecondary;
    private TextView tvSelectionCount;
    private TextView tvEmptyState;
    private View selectionBar;
    private MaterialButton btnActive;
    private MaterialButton btnCompleted;
    private int currentFilter = FILTER_ACTIVE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_focus_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatsPrimary = view.findViewById(R.id.tvFocusStatsPrimary);
        tvStatsSecondary = view.findViewById(R.id.tvFocusStatsSecondary);
        tvSelectionCount = view.findViewById(R.id.tvSelectionCount);
        tvEmptyState = view.findViewById(R.id.tvFocusEmptyState);
        selectionBar = view.findViewById(R.id.layoutFocusSelection);
        btnActive = view.findViewById(R.id.btnFocusActive);
        btnCompleted = view.findViewById(R.id.btnFocusCompleted);

        MaterialButton btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        MaterialButton btnCancelSelection = view.findViewById(R.id.btnCancelSelection);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddTask);

        RecyclerView recyclerView = view.findViewById(R.id.rvFocusTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FocusTaskAdapter(this);
        recyclerView.setAdapter(adapter);

        btnActive.setOnClickListener(v -> setFilter(FILTER_ACTIVE));
        btnCompleted.setOnClickListener(v -> setFilter(FILTER_COMPLETED));

        fabAdd.setOnClickListener(v -> startActivity(new Intent(requireContext(), FocusTaskEditorActivity.class)));
        btnCancelSelection.setOnClickListener(v -> adapter.clearSelection());
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedTasks());

        updateFilterButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterButtons();
        renderTasks();
    }

    private void updateFilterButtons() {
        updateButton(btnActive, currentFilter == FILTER_ACTIVE);
        updateButton(btnCompleted, currentFilter == FILTER_COMPLETED);
    }

    private void updateButton(@NonNull MaterialButton button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_focus_tab_selected : R.drawable.bg_focus_tab_unselected);
        button.setTextColor(requireContext().getColor(selected ? R.color.primary : R.color.on_surface_variant));
        button.setElevation(0f);
        button.setStrokeWidth(0);
    }

    private void loadTasks() {
        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.loadTasks(uid, tasks -> {
            allTasks.clear();
            allTasks.addAll(tasks);
            Collections.sort(allTasks, Comparator.comparingLong(FocusTask::resolveAnchorTime));
            if (isAdded()) {
                renderTasks();
            }
        });
    }

    private void renderTasks() {
        List<FocusTask> filtered = new ArrayList<>();
        for (FocusTask task : allTasks) {
            if (currentFilter == FILTER_ACTIVE && task.completed) continue;
            if (currentFilter == FILTER_COMPLETED && !task.completed) continue;
            filtered.add(task);
        }

        tvEmptyState.setText(currentFilter == FILTER_ACTIVE
                ? "Không còn task đang làm. Nhấn + để tạo task mới."
                : "Chưa có task đã hoàn thành.");
        tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        updateStats(filtered);

        List<FocusTaskAdapter.ListItem> items = new ArrayList<>();
        String lastHeader = "";

        for (FocusTask task : filtered) {
            String header = buildGroupHeader(task);
            if (!header.equals(lastHeader)) {
                items.add(new FocusTaskAdapter.HeaderItem(header));
                lastHeader = header;
            }
            items.add(new FocusTaskAdapter.TaskItem(task));
        }

        adapter.submit(items);
    }

    private void updateStats(@NonNull List<FocusTask> visibleTasks) {
        int activeCount = 0;
        int completedCount = 0;
        int overdue = 0;
        int upcomingSevenDays = 0;

        long now = System.currentTimeMillis();
        long sevenDaysLater = now + 7L * 24L * 60L * 60L * 1000L;

        for (FocusTask task : allTasks) {
            if (task.completed) {
                completedCount++;
                continue;
            }

            activeCount++;

            if (task.dueAt > 0 && task.dueAt < now) {
                overdue++;
            }

            long anchor = task.resolveAnchorTime();
            if (anchor >= now && anchor <= sevenDaysLater) {
                upcomingSevenDays++;
            }
        }

        if (currentFilter == FILTER_ACTIVE) {
            tvStatsPrimary.setText(visibleTasks.size() + " task đang làm");
            tvStatsSecondary.setText(upcomingSevenDays + " việc trong 7 ngày tới · " + overdue + " trễ hạn");
        } else {
            tvStatsPrimary.setText(visibleTasks.size() + " task đã xong");
            tvStatsSecondary.setText(activeCount + " task chưa xong · " + completedCount + " đã hoàn thành");
        }
    }

    @NonNull
    private String buildGroupHeader(@NonNull FocusTask task) {
        long anchor = task.resolveAnchorTime();

        Calendar today = startOfDay(Calendar.getInstance());

        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(anchor);

        Calendar targetStart = startOfDay((Calendar) target.clone());

        long diffMillis = targetStart.getTimeInMillis() - today.getTimeInMillis();
        int diffDays = (int) (diffMillis / (24L * 60L * 60L * 1000L));

        if (diffDays < 0) {
            SimpleDateFormat monthFormat = new SimpleDateFormat("'Đã qua ·' MMMM/yyyy", new Locale("vi", "VN"));
            return capitalize(monthFormat.format(new Date(anchor)));
        }

        if (diffDays == 0) return "Hôm nay";
        if (diffDays == 1) return "Ngày mai";

        if (diffDays <= 7) {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, dd/MM", new Locale("vi", "VN"));
            return capitalize(dayFormat.format(new Date(anchor)));
        }

        if (diffDays <= 31) {
            int weekOfMonth = target.get(Calendar.WEEK_OF_MONTH);
            int month = target.get(Calendar.MONTH) + 1;
            int year = target.get(Calendar.YEAR);
            return "Tuần " + weekOfMonth + " · Tháng " + month + "/" + year;
        }

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM/yyyy", new Locale("vi", "VN"));
        return capitalize(monthFormat.format(new Date(anchor)));
    }

    @NonNull
    private Calendar startOfDay(@NonNull Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    @NonNull
    private String capitalize(@NonNull String text) {
        if (text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase(new Locale("vi", "VN")) + text.substring(1);
    }

    @Override
    public void onTaskClicked(@NonNull FocusTask task) {
        Intent intent = new Intent(requireContext(), FocusTaskDetailActivity.class);
        intent.putExtra(FocusTaskDetailActivity.EXTRA_TASK_ID, task.id);
        startActivity(intent);
    }

    @Override
    public void onTaskLongPressed(@NonNull FocusTask task) {
        // no-op, selection handled in adapter
    }

    @Override
    public void onTaskCheckedChanged(@NonNull FocusTask task, boolean checked) {
        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.updateCompleted(uid, task.id, checked, (success, error) -> {
            if (!success && isAdded()) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Không thể cập nhật task", Toast.LENGTH_SHORT).show());
            } else {
                loadTasks();
            }
        });
    }

    @Override
    public void onSelectionChanged(int count) {
        selectionBar.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        tvSelectionCount.setText(count + " task đang được chọn");
    }

    private void deleteSelectedTasks() {
        List<String> ids = adapter.getSelectedTaskIds();
        if (ids.isEmpty()) return;

        String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();
        repository.deleteTasks(uid, ids, (success, error) -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(requireContext(), "Đã xóa các task đã chọn", Toast.LENGTH_SHORT).show();
                        adapter.clearSelection();
                        loadTasks();
                    } else {
                        Toast.makeText(requireContext(), "Xóa task thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
