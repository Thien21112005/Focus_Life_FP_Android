package com.hcmute.edu.vn.focus_life.ui.focus;

import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.domain.model.FocusTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FocusTaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onTaskClicked(@NonNull FocusTask task);
        void onTaskLongPressed(@NonNull FocusTask task);
        void onTaskCheckedChanged(@NonNull FocusTask task, boolean checked);
        void onSelectionChanged(int count);
    }

    public abstract static class ListItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_TASK = 1;
        final int type;

        ListItem(int type) {
            this.type = type;
        }
    }

    public static class HeaderItem extends ListItem {
        public final String title;

        public HeaderItem(String title) {
            super(TYPE_HEADER);
            this.title = title;
        }
    }

    public static class TaskItem extends ListItem {
        public final FocusTask task;

        public TaskItem(FocusTask task) {
            super(TYPE_TASK);
            this.task = task;
        }
    }

    private final List<ListItem> items = new ArrayList<>();
    private final Set<String> selectedTaskIds = new HashSet<>();
    private final Listener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
    private boolean selectionMode = false;

    public FocusTaskAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<ListItem> newItems) {
        items.clear();
        items.addAll(newItems);

        Set<String> validIds = new HashSet<>();
        for (ListItem item : items) {
            if (item instanceof TaskItem) {
                validIds.add(((TaskItem) item).task.id);
            }
        }
        selectedTaskIds.retainAll(validIds);
        if (selectedTaskIds.isEmpty()) {
            selectionMode = false;
        }
        notifyDataSetChanged();
        listener.onSelectionChanged(selectedTaskIds.size());
    }

    @NonNull
    public List<String> getSelectedTaskIds() {
        return new ArrayList<>(selectedTaskIds);
    }

    public void clearSelection() {
        selectedTaskIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ListItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_focus_section_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_focus_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HeaderItem) items.get(position));
        } else if (holder instanceof TaskViewHolder) {
            ((TaskViewHolder) holder).bind((TaskItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void toggleSelection(@NonNull FocusTask task) {
        if (selectedTaskIds.contains(task.id)) {
            selectedTaskIds.remove(task.id);
        } else {
            selectedTaskIds.add(task.id);
        }
        selectionMode = !selectedTaskIds.isEmpty();
        notifyDataSetChanged();
        listener.onSelectionChanged(selectedTaskIds.size());
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSectionHeader);
        }

        void bind(@NonNull HeaderItem item) {
            tvTitle.setText(item.title);
        }
    }

    private class TaskViewHolder extends RecyclerView.ViewHolder {
        private final View root;
        private final CheckBox cbComplete;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final TextView tvSchedule;
        private final TextView tvMeta;
        private final TextView tvPriority;
        private final TextView tvSelection;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.cardTaskRoot);
            cbComplete = itemView.findViewById(R.id.cbTaskComplete);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvSchedule = itemView.findViewById(R.id.tvTaskSchedule);
            tvMeta = itemView.findViewById(R.id.tvTaskMeta);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvSelection = itemView.findViewById(R.id.tvTaskSelected);
        }

        void bind(@NonNull TaskItem item) {
            FocusTask task = item.task;
            tvTitle.setText(task.title);
            tvDescription.setText(TextUtils.isEmpty(task.description) ? "Không có mô tả thêm" : task.description);
            tvDescription.setMaxLines(2);
            tvDescription.setEllipsize(TextUtils.TruncateAt.END);
            tvMeta.setText(buildMeta(task));
            tvSchedule.setText(buildSchedule(task));
            tvPriority.setText(buildPriorityText(task));
            applyPriorityStyle(task.priority);

            cbComplete.setOnCheckedChangeListener(null);
            cbComplete.setChecked(task.completed);
            cbComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    listener.onTaskCheckedChanged(task, isChecked);
                }
            });

            if (task.completed) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setAlpha(0.7f);
                tvDescription.setAlpha(0.6f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setAlpha(1f);
                tvDescription.setAlpha(1f);
            }

            boolean selected = selectedTaskIds.contains(task.id);
            root.setBackgroundResource(selected ? R.drawable.bg_card_primary_soft : R.drawable.bg_focus_task_card);
            tvSelection.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            tvSelection.setText(selected ? "Đã chọn" : "Chọn");
            tvSelection.setAlpha(selected ? 1f : 0.45f);

            root.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(task);
                } else {
                    listener.onTaskClicked(task);
                }
            });
            root.setOnLongClickListener(v -> {
                listener.onTaskLongPressed(task);
                toggleSelection(task);
                return true;
            });
        }

        private void applyPriorityStyle(@Nullable String priority) {
            int backgroundRes;
            int textColor;
            String normalized = priority == null ? "medium" : priority.toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "high":
                    backgroundRes = R.drawable.bg_chip_priority_high;
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_high_text);
                    break;
                case "low":
                    backgroundRes = R.drawable.bg_chip_priority_low;
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_low_text);
                    break;
                default:
                    backgroundRes = R.drawable.bg_chip_priority_medium;
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_medium_text);
                    break;
            }
            tvPriority.setBackgroundResource(backgroundRes);
            tvPriority.setTextColor(textColor);
        }

        @NonNull
        private String buildPriorityText(@NonNull FocusTask task) {
            String priority = task.priority == null ? "medium" : task.priority.toLowerCase(Locale.ROOT);
            switch (priority) {
                case "high":
                    return "Ưu tiên cao";
                case "low":
                    return "Ưu tiên thấp";
                default:
                    return "Ưu tiên vừa";
            }
        }

        @NonNull
        private String buildMeta(@NonNull FocusTask task) {
            return task.category + " · " + task.completedPomodoros + "/" + Math.max(1, task.estimatedPomodoros) + " Pomodoro";
        }

        @NonNull
        private String buildSchedule(@NonNull FocusTask task) {
            long anchor = task.resolveAnchorTime();
            Date date = new Date(anchor);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd/MM · HH:mm", new Locale("vi", "VN"));
            if (task.startAt > 0 && task.dueAt > 0 && task.startAt != task.dueAt) {
                return dateFormat.format(new Date(task.startAt)) + " → " + timeFormat.format(new Date(task.dueAt));
            }
            return dateFormat.format(date);
        }
    }
}
