package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "nutrition_entries",
        indices = {
                @Index(value = {"uid", "entryDate"}),
                @Index(value = {"entryUuid"}, unique = true)
        }
)
public class NutritionEntryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String entryUuid = "";

    @NonNull
    public String uid = "";

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
    public String imageUri;
    public String imageUrl;
    public String imagePublicId;
    public String source;
    public float mlConfidence;
    public String healthFlags;
    public long createdAt;
    public long updatedAt;
    public boolean deleted;
    public boolean synced;
}
