package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "step_records")
public class StepRecordEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String date;
    public long timestamp;
    public int steps;
    public float calories;
    public String source;
    public boolean synced;
}
