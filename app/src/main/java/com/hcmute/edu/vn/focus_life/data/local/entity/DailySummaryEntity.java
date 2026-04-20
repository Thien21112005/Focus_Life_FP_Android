package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_daily_summary")
public class DailySummaryEntity {
    @PrimaryKey
    @NonNull
    public String date = "";
    public int steps;
    public float calories;
    public int focusMinutes;
    public int waterMl;
    public int completedHabits;
    public int nutritionCalories;
    public boolean synced;
}
