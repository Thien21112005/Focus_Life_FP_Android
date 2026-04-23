package com.hcmute.edu.vn.focus_life.core.nutrition;

import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;

import java.util.List;

public class NutritionDaySummary {
    public int calorieGoal;
    public int calories;
    public double protein;
    public double carbs;
    public double fat;
    public double fiber;
    public double sugar;
    public double sodium;
    public int entryCount;

    public static NutritionDaySummary fromEntries(List<NutritionEntryEntity> entries, int goalCalories) {
        NutritionDaySummary summary = new NutritionDaySummary();
        summary.calorieGoal = goalCalories;
        if (entries == null) return summary;

        for (NutritionEntryEntity entry : entries) {
            if (entry == null || entry.deleted) continue;
            summary.entryCount++;
            summary.calories += entry.calories;
            summary.protein += entry.protein;
            summary.carbs += entry.carbs;
            summary.fat += entry.fat;
            summary.fiber += entry.fiber;
            summary.sugar += entry.sugar;
            summary.sodium += entry.sodium;
        }
        return summary;
    }

    public int remainingCalories() {
        return calorieGoal - calories;
    }

    public int progressPercent() {
        if (calorieGoal <= 0) return 0;
        return Math.max(0, Math.min(100, (int) Math.round((calories * 100f) / calorieGoal)));
    }
}
