package com.hcmute.edu.vn.focus_life.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HealthMetricsRepository {
    public interface GoalCallback {
        void onResult(@NonNull MonthlyGoal goal, @Nullable Exception error);
    }

    public interface ReportCallback {
        void onResult(@NonNull MonthlyReport report, @Nullable Exception error);
    }

    public interface DailyCallback {
        void onResult(@NonNull DailyReport report, @Nullable Exception error);
    }

    public interface BasicCallback {
        void onResult(boolean success, @NonNull String message);
    }

    public interface IntCallback {
        void onResult(int value, @Nullable Exception error);
    }

    public interface LifetimeCallback {
        void onResult(@NonNull LifetimeReport report, @Nullable Exception error);
    }

    public static class MonthlyGoal {
        public String monthKey;
        public int stepTarget;
        public int runningDistanceKmTarget;
        public int netCalorieTarget;
        public int focusMinuteTarget;
        public int waterGlassTargetPerDay;
        public int reminderStartHour;
        public int reminderEndHour;
        public int reminderIntervalHours;
        public boolean hasSavedGoal;
        public boolean waterReminderEnabled;
        public long createdAt;
        public long updatedAt;

        public static MonthlyGoal defaultForCurrentMonth() {
            MonthlyGoal goal = new MonthlyGoal();
            goal.monthKey = currentMonthKey();
            goal.stepTarget = 0;
            goal.runningDistanceKmTarget = 0;
            goal.netCalorieTarget = 0;
            goal.focusMinuteTarget = 0;
            goal.waterGlassTargetPerDay = 8;
            goal.reminderStartHour = 8;
            goal.reminderEndHour = 22;
            goal.reminderIntervalHours = 2;
            return goal;
        }
    }

    public static class LifetimeReport {
        public float totalRunningKm;
        public int totalSteps;
        public int focusMinutes;
        public int waterGlasses;
        public long appActiveMillis;
    }

    public static class MonthlyReport {
        public MonthlyGoal goal;
        public String monthKey;
        public int totalSteps;
        public float runningDistanceKm;
        public int nutritionCalories;
        public int burnedCalories;
        public int netCalories;
        public int focusMinutes;
        public long appActiveMillis;
        public int todayWaterGlasses;
        public int todayWaterMl;
        public int monthlyWaterGlasses;
        public int monthlyWaterMl;
        public int daysInMonth;

        public int stepPercent() {
            return percent(totalSteps, goal == null ? 0 : goal.stepTarget);
        }

        public int runningDistancePercent() {
            return percent(runningDistanceKm, goal == null ? 0 : goal.runningDistanceKmTarget);
        }

        public int netCaloriePercent() {
            return percent(netCalories, goal == null ? 0 : goal.netCalorieTarget);
        }

        public int focusPercent() {
            return percent(focusMinutes, goal == null ? 0 : goal.focusMinuteTarget);
        }

        public int waterPercentToday() {
            return percent(todayWaterGlasses, 8);
        }

        public int monthlyWaterTarget() {
            return Math.max(0, daysInMonth) * 8;
        }

        public int monthlyWaterPercent() {
            return percent(monthlyWaterGlasses, monthlyWaterTarget());
        }
    }

    public static class DailyReport {
        public String dateKey;
        public int totalSteps;
        public float runningDistanceKm;
        public int nutritionCalories;
        public int burnedCalories;
        public int netCalories;
        public int focusMinutes;
        public long appActiveMillis;
        public int waterGlasses;
        public int waterMl;
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public void loadMonthlyGoal(@NonNull GoalCallback callback) {
        String uid = requireUid(callback);
        if (uid == null) return;
        String monthKey = currentMonthKey();
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_MONTHLY_GOALS)
                .document(monthKey)
                .get()
                .addOnSuccessListener(document -> callback.onResult(mapGoal(document), null))
                .addOnFailureListener(e -> callback.onResult(MonthlyGoal.defaultForCurrentMonth(), e));
    }

    public void saveMonthlyGoal(@NonNull MonthlyGoal goal, @NonNull BasicCallback callback) {
        String uid = requireUid(callback);
        if (uid == null) return;
        long now = System.currentTimeMillis();
        if (goal.monthKey == null || goal.monthKey.trim().isEmpty()) goal.monthKey = currentMonthKey();
        if (goal.createdAt <= 0L) goal.createdAt = now;
        goal.updatedAt = now;
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_MONTHLY_GOALS)
                .document(goal.monthKey)
                .set(goalToMap(goal), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onResult(true, "Đã lưu mục tiêu tháng"))
                .addOnFailureListener(e -> callback.onResult(false, friendly(e)));
    }

    public void deleteCurrentMonthlyGoal(@NonNull BasicCallback callback) {
        String uid = requireUid(callback);
        if (uid == null) return;
        String monthKey = currentMonthKey();
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_MONTHLY_GOALS)
                .document(monthKey)
                .delete()
                .addOnSuccessListener(unused -> callback.onResult(true, "Đã xóa mục tiêu tháng"))
                .addOnFailureListener(e -> callback.onResult(false, friendly(e)));
    }

    public void loadMonthlyReport(@NonNull ReportCallback callback) {
        String uid = requireUid(callback);
        if (uid == null) return;
        String monthKey = currentMonthKey();
        String today = todayKey();

        Task<DocumentSnapshot> goalTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection(Constants.FIRESTORE_MONTHLY_GOALS).document(monthKey).get();
        Task<QuerySnapshot> stepTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection(Constants.FIRESTORE_STEP_RECORDS).get();
        Task<QuerySnapshot> nutritionTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection(Constants.FIRESTORE_NUTRITION_ENTRIES).get();
        Task<QuerySnapshot> focusTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("pomodoro_sessions").get();
        Task<QuerySnapshot> usageTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("app_usage_records").get();
        Task<QuerySnapshot> waterTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("water_records").get();
        Task<QuerySnapshot> runningTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("running_sessions").get();

        Tasks.whenAllComplete(goalTask, stepTask, nutritionTask, focusTask, usageTask, waterTask, runningTask)
                .addOnSuccessListener(tasks -> {
                    MonthlyReport report = new MonthlyReport();
                    report.monthKey = monthKey;
                    report.daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
                    report.goal = goalTask.isSuccessful() ? mapGoal(goalTask.getResult()) : MonthlyGoal.defaultForCurrentMonth();
                    aggregateSteps(report, stepTask, monthKey);
                    aggregateRunning(report, runningTask, monthKey);
                    aggregateNutrition(report, nutritionTask, monthKey);
                    aggregateFocus(report, focusTask, monthKey);
                    aggregateUsage(report, usageTask, monthKey);
                    aggregateWater(report, waterTask, monthKey, today);
                    report.netCalories = Math.max(0, report.nutritionCalories - report.burnedCalories);
                    callback.onResult(report, null);
                })
                .addOnFailureListener(e -> callback.onResult(emptyReport(monthKey), e));
    }


    public void loadDailyReport(@NonNull String dateKey, @NonNull DailyCallback callback) {
        String uid = requireUid(callback);
        if (uid == null) return;
        String safeDate = (dateKey == null || dateKey.trim().isEmpty()) ? todayKey() : dateKey.trim();

        Task<QuerySnapshot> stepTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection(Constants.FIRESTORE_STEP_RECORDS).get();
        Task<QuerySnapshot> nutritionTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection(Constants.FIRESTORE_NUTRITION_ENTRIES).get();
        Task<QuerySnapshot> focusTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("pomodoro_sessions").get();
        Task<QuerySnapshot> usageTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("app_usage_records").get();
        Task<QuerySnapshot> waterTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("water_records").get();
        Task<QuerySnapshot> runningTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("running_sessions").get();

        Tasks.whenAllComplete(stepTask, nutritionTask, focusTask, usageTask, waterTask, runningTask)
                .addOnSuccessListener(tasks -> {
                    DailyReport report = new DailyReport();
                    report.dateKey = safeDate;
                    aggregateDailySteps(report, stepTask, safeDate);
                    aggregateDailyRunning(report, runningTask, safeDate);
                    aggregateDailyNutrition(report, nutritionTask, safeDate);
                    aggregateDailyFocus(report, focusTask, safeDate);
                    aggregateDailyUsage(report, usageTask, safeDate);
                    aggregateDailyWater(report, waterTask, safeDate);
                    report.netCalories = Math.max(0, report.nutritionCalories - report.burnedCalories);
                    callback.onResult(report, null);
                })
                .addOnFailureListener(e -> callback.onResult(emptyDailyReport(safeDate), e));
    }


    public void loadLifetimeReport(@NonNull LifetimeCallback callback) {
        String uid = requireUid(callback);
        if (uid == null) return;
        Task<QuerySnapshot> stepTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection(Constants.FIRESTORE_STEP_RECORDS).get();
        Task<QuerySnapshot> focusTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("pomodoro_sessions").get();
        Task<QuerySnapshot> usageTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("app_usage_records").get();
        Task<QuerySnapshot> waterTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("water_records").get();
        Task<QuerySnapshot> runningTask = firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid).collection("running_sessions").get();

        Tasks.whenAllComplete(stepTask, focusTask, usageTask, waterTask, runningTask)
                .addOnSuccessListener(tasks -> {
                    LifetimeReport report = new LifetimeReport();
                    aggregateLifetimeSteps(report, stepTask);
                    aggregateLifetimeRunning(report, runningTask);
                    aggregateLifetimeFocus(report, focusTask);
                    aggregateLifetimeUsage(report, usageTask);
                    aggregateLifetimeWater(report, waterTask);
                    callback.onResult(report, null);
                })
                .addOnFailureListener(e -> callback.onResult(new LifetimeReport(), e));
    }


    public void loadTodayWaterGlasses(@NonNull IntCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onResult(0, new IllegalStateException("Bạn cần đăng nhập để dùng chức năng này"));
            return;
        }
        String today = todayKey();
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(user.getUid())
                .collection("water_records")
                .document(today)
                .get()
                .addOnSuccessListener(document -> {
                    int glasses = document != null && document.exists() ? Math.max(0, intValue(document.get("glasses"))) : 0;
                    callback.onResult(glasses, null);
                })
                .addOnFailureListener(e -> callback.onResult(0, e));
    }

    public void logWaterGlass(@NonNull BasicCallback callback) {
        String uid = requireUid(callback);
        if (uid == null) return;
        String today = todayKey();
        String monthKey = currentMonthKey();
        long now = System.currentTimeMillis();

        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection("water_records")
                .document(today)
                .get()
                .addOnSuccessListener(document -> {
                    int current = document != null && document.exists() ? intValue(document.get("glasses")) : 0;
                    if (current >= 8) {
                        callback.onResult(true, "Hôm nay bạn đã đạt 8/8 ly nước");
                        return;
                    }
                    int next = current + 1;
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", today);
                    data.put("monthKey", monthKey);
                    data.put("glasses", next);
                    data.put("milliliters", next * 250);
                    data.put("targetGlasses", 8);
                    data.put("targetMilliliters", 2000);
                    data.put("completed", next >= 8);
                    data.put("updatedAt", now);
                    firestore.collection(Constants.FIRESTORE_USERS)
                            .document(uid)
                            .collection("water_records")
                            .document(today)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                saveWaterDetail(uid, today, monthKey, next, "manual", now);
                                callback.onResult(true, "Đã ghi nhận ly nước thứ " + next + "/8");
                            })
                            .addOnFailureListener(e -> callback.onResult(false, friendly(e)));
                })
                .addOnFailureListener(e -> callback.onResult(false, friendly(e)));
    }

    private void saveWaterDetail(@NonNull String uid, @NonNull String today, @NonNull String monthKey, int cupIndex, @NonNull String source, long now) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("date", today);
        detail.put("monthKey", monthKey);
        detail.put("cupIndex", cupIndex);
        detail.put("milliliters", 250);
        detail.put("source", source);
        detail.put("createdAt", now);
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection("water_record_details")
                .document(today + "_cup_" + cupIndex)
                .set(detail, com.google.firebase.firestore.SetOptions.merge());
    }


    public void setWaterReminderEnabled(boolean enabled) {
        saveWaterReminderSettings(enabled, 8, 22, 8);
    }

    public void saveWaterReminderSettings(boolean enabled, int startHour, int endHour, int glassTarget) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;
        long now = System.currentTimeMillis();

        Map<String, Object> settingsData = new HashMap<>();
        settingsData.put("enabled", enabled);
        settingsData.put("waterGlassTargetPerDay", 8);
        settingsData.put("reminderStartHour", clamp(startHour, 0, 23, 8));
        settingsData.put("reminderEndHour", clamp(endHour, 0, 23, 22));
        settingsData.put("updatedAt", now);

        firestore.collection(Constants.FIRESTORE_USERS)
                .document(user.getUid())
                .collection("water_reminder_settings")
                .document("current")
                .set(settingsData, com.google.firebase.firestore.SetOptions.merge());
    }

    public void recordAppUsage(long activeMillis) {
        if (activeMillis < 5000L) return;
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;
        String today = todayKey();
        Map<String, Object> data = new HashMap<>();
        data.put("date", today);
        data.put("totalActiveMillis", FieldValue.increment(activeMillis));
        data.put("sessions", FieldValue.increment(1));
        data.put("updatedAt", System.currentTimeMillis());
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(user.getUid())
                .collection("app_usage_records")
                .document(today)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    public void savePomodoroSession(@NonNull String sessionUuid, @NonNull Map<String, Object> data) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || sessionUuid.trim().isEmpty()) return;
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(user.getUid())
                .collection("pomodoro_sessions")
                .document(sessionUuid)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    private void aggregateDailySteps(DailyReport report, Task<QuerySnapshot> task, String dateKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (date.trim().isEmpty()) date = doc.getId();
            if (!dateKey.equals(date)) continue;
            report.totalSteps += intValue(doc.get("steps"));
            report.burnedCalories += Math.round(floatValue(doc.get("calories")));
        }
    }

    private void aggregateDailyRunning(DailyReport report, Task<QuerySnapshot> task, String dateKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (!dateKey.equals(date)) continue;
            float km = floatValue(doc.get("distanceKm"));
            if (km <= 0f) km = floatValue(doc.get("distanceMeters")) / 1000f;
            report.runningDistanceKm += Math.max(0f, km);
        }
    }

    private void aggregateDailyNutrition(DailyReport report, Task<QuerySnapshot> task, String dateKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("entryDate"));
            if (!dateKey.equals(date)) continue;
            Object deleted = doc.get("deleted");
            if (deleted instanceof Boolean && (Boolean) deleted) continue;
            report.nutritionCalories += intValue(doc.get("calories"));
        }
    }

    private void aggregateDailyFocus(DailyReport report, Task<QuerySnapshot> task, String dateKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            long endedAt = longValue(doc.get("endedAt"));
            if (!dateKey.equals(dateKeyFromMillis(endedAt))) continue;
            Object completed = doc.get("completed");
            if (completed instanceof Boolean && !(Boolean) completed) continue;
            report.focusMinutes += intValue(doc.get("durationMinutes"));
        }
    }

    private void aggregateDailyUsage(DailyReport report, Task<QuerySnapshot> task, String dateKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (!dateKey.equals(date)) continue;
            report.appActiveMillis += longValue(doc.get("totalActiveMillis"));
        }
    }

    private void aggregateDailyWater(DailyReport report, Task<QuerySnapshot> task, String dateKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (date.trim().isEmpty()) date = doc.getId();
            if (!dateKey.equals(date)) continue;
            report.waterGlasses += Math.max(0, intValue(doc.get("glasses")));
            report.waterMl += Math.max(0, intValue(doc.get("milliliters")));
        }
    }

    private void aggregateSteps(MonthlyReport report, Task<QuerySnapshot> task, String monthKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (date.trim().isEmpty()) date = doc.getId();
            if (!date.startsWith(monthKey)) continue;
            report.totalSteps += intValue(doc.get("steps"));
            report.burnedCalories += Math.round(floatValue(doc.get("calories")));
        }
    }

    private void aggregateRunning(MonthlyReport report, Task<QuerySnapshot> task, String monthKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (!date.startsWith(monthKey)) continue;
            float km = floatValue(doc.get("distanceKm"));
            if (km <= 0f) {
                km = floatValue(doc.get("distanceMeters")) / 1000f;
            }
            report.runningDistanceKm += Math.max(0f, km);
        }
    }

    private void aggregateNutrition(MonthlyReport report, Task<QuerySnapshot> task, String monthKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("entryDate"));
            if (!date.startsWith(monthKey)) continue;
            Object deleted = doc.get("deleted");
            if (deleted instanceof Boolean && (Boolean) deleted) continue;
            report.nutritionCalories += intValue(doc.get("calories"));
        }
    }

    private void aggregateFocus(MonthlyReport report, Task<QuerySnapshot> task, String monthKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        long monthStart = monthStartMillis(monthKey);
        long nextMonth = nextMonthStartMillis(monthKey);
        for (QueryDocumentSnapshot doc : task.getResult()) {
            long endedAt = longValue(doc.get("endedAt"));
            if (endedAt < monthStart || endedAt >= nextMonth) continue;
            Object completed = doc.get("completed");
            if (completed instanceof Boolean && !(Boolean) completed) continue;
            report.focusMinutes += intValue(doc.get("durationMinutes"));
        }
    }

    private void aggregateUsage(MonthlyReport report, Task<QuerySnapshot> task, String monthKey) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (!date.startsWith(monthKey)) continue;
            report.appActiveMillis += longValue(doc.get("totalActiveMillis"));
        }
    }

    private void aggregateWater(MonthlyReport report, Task<QuerySnapshot> task, String monthKey, String today) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            String date = stringValue(doc.get("date"));
            if (date.trim().isEmpty()) date = doc.getId();
            if (!date.startsWith(monthKey)) continue;
            int glasses = Math.max(0, intValue(doc.get("glasses")));
            int milliliters = Math.max(0, intValue(doc.get("milliliters")));
            report.monthlyWaterGlasses += glasses;
            report.monthlyWaterMl += milliliters;
            if (today.equals(date)) {
                report.todayWaterGlasses += glasses;
                report.todayWaterMl += milliliters;
            }
        }
    }


    private void aggregateLifetimeSteps(LifetimeReport report, Task<QuerySnapshot> task) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            report.totalSteps += intValue(doc.get("steps"));
        }
    }

    private void aggregateLifetimeRunning(LifetimeReport report, Task<QuerySnapshot> task) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            float km = floatValue(doc.get("distanceKm"));
            if (km <= 0f) km = floatValue(doc.get("distanceMeters")) / 1000f;
            report.totalRunningKm += Math.max(0f, km);
        }
    }

    private void aggregateLifetimeFocus(LifetimeReport report, Task<QuerySnapshot> task) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            Object completed = doc.get("completed");
            if (completed instanceof Boolean && !(Boolean) completed) continue;
            report.focusMinutes += intValue(doc.get("durationMinutes"));
        }
    }

    private void aggregateLifetimeUsage(LifetimeReport report, Task<QuerySnapshot> task) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            report.appActiveMillis += longValue(doc.get("totalActiveMillis"));
        }
    }

    private void aggregateLifetimeWater(LifetimeReport report, Task<QuerySnapshot> task) {
        if (!task.isSuccessful() || task.getResult() == null) return;
        for (QueryDocumentSnapshot doc : task.getResult()) {
            report.waterGlasses += Math.max(0, intValue(doc.get("glasses")));
        }
    }

    private DailyReport emptyDailyReport(String dateKey) {
        DailyReport report = new DailyReport();
        report.dateKey = dateKey;
        return report;
    }

    private MonthlyReport emptyReport(String monthKey) {
        MonthlyReport report = new MonthlyReport();
        report.monthKey = monthKey;
        report.goal = MonthlyGoal.defaultForCurrentMonth();
        report.daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        return report;
    }

    private MonthlyGoal mapGoal(DocumentSnapshot document) {
        MonthlyGoal goal = MonthlyGoal.defaultForCurrentMonth();
        if (document == null || !document.exists()) return goal;
        int schemaVersion = intValue(document.get("schemaVersion"));
        if (schemaVersion < 2) {
            return goal;
        }
        goal.hasSavedGoal = true;
        goal.monthKey = stringOrDefault(document.get("monthKey"), currentMonthKey());
        goal.stepTarget = 0;
        goal.runningDistanceKmTarget = positiveOrDefault(document.get("runningDistanceKmTarget"), 0);
        goal.netCalorieTarget = positiveOrDefault(document.get("netCalorieTarget"), 0);
        goal.focusMinuteTarget = positiveOrDefault(document.get("focusMinuteTarget"), 0);
        goal.waterGlassTargetPerDay = 8;
        goal.reminderStartHour = 8;
        goal.reminderEndHour = 22;
        goal.reminderIntervalHours = 0;
        goal.waterReminderEnabled = false;
        goal.createdAt = longValue(document.get("createdAt"));
        goal.updatedAt = longValue(document.get("updatedAt"));
        return goal;
    }

    private Map<String, Object> goalToMap(MonthlyGoal goal) {
        Map<String, Object> map = new HashMap<>();
        map.put("monthKey", goal.monthKey);
        map.put("schemaVersion", 2);
        map.put("stepTarget", null);
        map.put("runningDistanceKmTarget", goal.runningDistanceKmTarget > 0 ? goal.runningDistanceKmTarget : null);
        map.put("netCalorieTarget", goal.netCalorieTarget > 0 ? goal.netCalorieTarget : null);
        map.put("focusMinuteTarget", goal.focusMinuteTarget > 0 ? goal.focusMinuteTarget : null);
        map.put("waterGlassTargetPerDay", 8);
        map.put("createdAt", goal.createdAt);
        map.put("updatedAt", goal.updatedAt);
        return map;
    }

    private String requireUid(GoalCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onResult(MonthlyGoal.defaultForCurrentMonth(), new IllegalStateException("Bạn cần đăng nhập để dùng chức năng này"));
            return null;
        }
        return user.getUid();
    }

    private String requireUid(ReportCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onResult(emptyReport(currentMonthKey()), new IllegalStateException("Bạn cần đăng nhập để dùng chức năng này"));
            return null;
        }
        return user.getUid();
    }

    private String requireUid(DailyCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onResult(emptyDailyReport(todayKey()), new IllegalStateException("Bạn cần đăng nhập để xem sức khỏe"));
            return null;
        }
        return user.getUid();
    }

    private String requireUid(BasicCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onResult(false, "Bạn cần đăng nhập để dùng chức năng này");
            return null;
        }
        return user.getUid();
    }

    private String requireUid(LifetimeCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onResult(new LifetimeReport(), new IllegalStateException("Bạn cần đăng nhập để xem lịch sử"));
            return null;
        }
        return user.getUid();
    }

    private static String dateKeyFromMillis(long millis) {
        if (millis <= 0L) return "";
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    public static String currentMonthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }

    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static int percent(int value, int target) {
        if (target <= 0) return 0;
        return Math.min(999, Math.round(value * 100f / target));
    }

    public static int percent(float value, int target) {
        if (target <= 0) return 0;
        return Math.min(999, Math.round(value * 100f / target));
    }

    private static long monthStartMillis(String monthKey) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM", Locale.US).parse(monthKey);
            return date == null ? 0L : date.getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static long nextMonthStartMillis(String monthKey) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(monthStartMillis(monthKey));
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTimeInMillis();
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return 0; }
    }

    private static long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return 0L; }
    }

    private static float floatValue(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        try { return Float.parseFloat(String.valueOf(value)); } catch (Exception ignored) { return 0f; }
    }

    private static int positiveOrDefault(Object value, int fallback) {
        int parsed = intValue(value);
        return parsed > 0 ? parsed : fallback;
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value < min || value > max) return fallback;
        return value;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String stringOrDefault(Object value, String fallback) {
        String text = stringValue(value);
        return text.trim().isEmpty() ? fallback : text;
    }

    private static String friendly(Exception e) {
        return e == null || e.getMessage() == null ? "Không thể lưu dữ liệu" : e.getMessage();
    }
}
