package com.hcmute.edu.vn.focus_life.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.hcmute.edu.vn.focus_life.data.local.entity.DailySummaryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.PomodoroSessionEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.StepRecordEntity;

import java.util.HashMap;
import java.util.Map;

public class FirestoreMapper {
    public static Map<String, Object> mapDailySummary(DailySummaryEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("date", entity.date);
        map.put("steps", entity.steps);
        map.put("calories", entity.calories);
        map.put("focusMinutes", entity.focusMinutes);
        map.put("waterMl", entity.waterMl);
        map.put("completedHabits", entity.completedHabits);
        map.put("nutritionCalories", entity.nutritionCalories);
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    public static Map<String, Object> mapPomodoroSession(PomodoroSessionEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskName", entity.taskName);
        map.put("sessionType", entity.sessionType);
        map.put("durationMinutes", entity.durationMinutes);
        map.put("startedAt", entity.startedAt);
        map.put("endedAt", entity.endedAt);
        map.put("completed", entity.completed);
        return map;
    }

    public static Map<String, Object> mapStepRecord(StepRecordEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("date", entity.date);
        map.put("timestamp", entity.timestamp);
        map.put("steps", entity.steps);
        map.put("calories", entity.calories);
        map.put("source", entity.source);
        map.put("syncedAt", System.currentTimeMillis());
        return map;
    }

    public static Map<String, Object> mapNutritionEntry(NutritionEntryEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("entryUuid", entity.entryUuid);
        map.put("uid", entity.uid);
        map.put("mealType", entity.mealType);
        map.put("foodName", entity.foodName);
        map.put("quantity", entity.quantity);
        map.put("unit", entity.unit);
        map.put("calories", entity.calories);
        map.put("protein", entity.protein);
        map.put("carbs", entity.carbs);
        map.put("fat", entity.fat);
        map.put("fiber", entity.fiber);
        map.put("sugar", entity.sugar);
        map.put("sodium", entity.sodium);
        map.put("entryDate", entity.entryDate);
        map.put("imageUri", entity.imageUri);
        map.put("imageUrl", entity.imageUrl);
        map.put("imagePublicId", entity.imagePublicId);
        map.put("source", entity.source);
        map.put("mlConfidence", (double) entity.mlConfidence);
        map.put("healthFlags", entity.healthFlags);
        map.put("createdAt", entity.createdAt);
        map.put("updatedAt", entity.updatedAt);
        map.put("deleted", entity.deleted);
        map.put("syncedAt", System.currentTimeMillis());
        return map;
    }

    @Nullable
    public static NutritionEntryEntity mapNutritionEntry(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return null;

        NutritionEntryEntity entity = new NutritionEntryEntity();
        entity.entryUuid = stringValue(snapshot, "entryUuid", snapshot.getId());
        entity.uid = stringValue(snapshot, "uid", "");
        entity.mealType = stringValue(snapshot, "mealType", "");
        entity.foodName = stringValue(snapshot, "foodName", "");
        entity.quantity = doubleValue(snapshot, "quantity");
        entity.unit = stringValue(snapshot, "unit", "");
        entity.calories = (int) Math.round(doubleValue(snapshot, "calories"));
        entity.protein = doubleValue(snapshot, "protein");
        entity.carbs = doubleValue(snapshot, "carbs");
        entity.fat = doubleValue(snapshot, "fat");
        entity.fiber = doubleValue(snapshot, "fiber");
        entity.sugar = doubleValue(snapshot, "sugar");
        entity.sodium = doubleValue(snapshot, "sodium");
        entity.entryDate = stringValue(snapshot, "entryDate", "");
        entity.imageUri = stringValue(snapshot, "imageUri", "");
        entity.imageUrl = stringValue(snapshot, "imageUrl", "");
        entity.imagePublicId = stringValue(snapshot, "imagePublicId", "");
        entity.source = stringValue(snapshot, "source", "");
        entity.mlConfidence = (float) doubleValue(snapshot, "mlConfidence");
        entity.healthFlags = stringValue(snapshot, "healthFlags", "");
        entity.createdAt = longValue(snapshot, "createdAt");
        entity.updatedAt = longValue(snapshot, "updatedAt");
        entity.deleted = booleanValue(snapshot, "deleted");
        entity.synced = true;
        return entity;
    }

    private static String stringValue(DocumentSnapshot snapshot, String key, String fallback) {
        String value = snapshot.getString(key);
        return value == null ? fallback : value;
    }

    private static double doubleValue(DocumentSnapshot snapshot, String key) {
        Double value = snapshot.getDouble(key);
        if (value != null) return value;
        Long asLong = snapshot.getLong(key);
        return asLong == null ? 0d : asLong.doubleValue();
    }

    private static long longValue(DocumentSnapshot snapshot, String key) {
        Long value = snapshot.getLong(key);
        if (value != null) return value;
        Double asDouble = snapshot.getDouble(key);
        return asDouble == null ? 0L : asDouble.longValue();
    }

    private static boolean booleanValue(DocumentSnapshot snapshot, String key) {
        Boolean value = snapshot.getBoolean(key);
        return value != null && value;
    }
}
