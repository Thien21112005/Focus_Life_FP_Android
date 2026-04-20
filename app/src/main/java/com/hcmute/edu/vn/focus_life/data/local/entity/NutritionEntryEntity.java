package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "nutrition_entries")
public class NutritionEntryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String mealType;
    public String foodName;
    public double quantity;
    public String unit;
    public int calories;
    public double protein;
    public double carbs;
    public double fat;
    public double fiber;
    public double sugar;
    public double sodium;
    public String entryDate;
    public long createdAt;
    public boolean synced;
}
