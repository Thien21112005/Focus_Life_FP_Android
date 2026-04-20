package com.hcmute.edu.vn.focus_life.data.mapper;

import com.hcmute.edu.vn.focus_life.data.local.entity.DailySummaryEntity;
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
}