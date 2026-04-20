package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "water_entries")
public class WaterEntryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int amountMl;
    public String entryDate;
    public long createdAt;
    public boolean synced;
}
