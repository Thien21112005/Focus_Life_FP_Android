package com.hcmute.edu.vn.focus_life.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FocusTask {
    public String id;
    public String title;
    public String description;
    public String category;
    public String priority;
    public String status;
    public long startAt;
    public long dueAt;
    public long createdAt;
    public long updatedAt;
    public long completedAt;
    public int estimatedPomodoros;
    public int completedPomodoros;
    public boolean completed;
    public boolean deleted;

    public FocusTask() {
    }

    @NonNull
    public static FocusTask createEmpty() {
        FocusTask task = new FocusTask();
        long now = System.currentTimeMillis();
        task.id = UUID.randomUUID().toString();
        task.title = "";
        task.description = "";
        task.category = "Focus";
        task.priority = "medium";
        task.status = "planned";
        task.startAt = now;
        task.dueAt = now;
        task.createdAt = now;
        task.updatedAt = now;
        task.completedAt = 0L;
        task.estimatedPomodoros = 1;
        task.completedPomodoros = 0;
        task.completed = false;
        task.deleted = false;
        return task;
    }

    @Nullable
    public static FocusTask fromSnapshot(@Nullable DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        FocusTask task = new FocusTask();
        task.id = snapshot.getId();
        task.title = safeString(snapshot.getString("title"));
        task.description = safeString(snapshot.getString("description"));
        task.category = safeString(snapshot.getString("category"), "Focus");
        task.priority = safeString(snapshot.getString("priority"), "medium");
        task.status = safeString(snapshot.getString("status"), "planned");
        task.startAt = safeLong(snapshot.getLong("startAt"));
        task.dueAt = safeLong(snapshot.getLong("dueAt"));
        task.createdAt = safeLong(snapshot.getLong("createdAt"));
        task.updatedAt = safeLong(snapshot.getLong("updatedAt"));
        task.completedAt = safeLong(snapshot.getLong("completedAt"));
        task.estimatedPomodoros = safeInt(snapshot.getLong("estimatedPomodoros"), 1);
        task.completedPomodoros = safeInt(snapshot.getLong("completedPomodoros"), 0);
        Boolean completedValue = snapshot.getBoolean("completed");
        task.completed = completedValue != null && completedValue;
        Boolean deletedValue = snapshot.getBoolean("deleted");
        task.deleted = deletedValue != null && deletedValue;
        return task;
    }

    @NonNull
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", description);
        data.put("category", category);
        data.put("priority", priority);
        data.put("status", status);
        data.put("startAt", startAt);
        data.put("dueAt", dueAt);
        data.put("createdAt", createdAt);
        data.put("updatedAt", updatedAt);
        data.put("completedAt", completedAt);
        data.put("estimatedPomodoros", estimatedPomodoros);
        data.put("completedPomodoros", completedPomodoros);
        data.put("completed", completed);
        data.put("deleted", deleted);
        return data;
    }

    @NonNull
    public String resolveStatus(long now) {
        if (completed) return "completed";
        if (dueAt > 0 && dueAt < now) return "overdue";
        if (startAt > now) return "upcoming";
        return "in_progress";
    }

    public long resolveAnchorTime() {
        if (dueAt > 0) return dueAt;
        if (startAt > 0) return startAt;
        if (createdAt > 0) return createdAt;
        return System.currentTimeMillis();
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static int safeInt(Long value, int fallback) {
        return value == null ? fallback : value.intValue();
    }

    @NonNull
    private static String safeString(@Nullable String value) {
        return safeString(value, "");
    }

    @NonNull
    private static String safeString(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}