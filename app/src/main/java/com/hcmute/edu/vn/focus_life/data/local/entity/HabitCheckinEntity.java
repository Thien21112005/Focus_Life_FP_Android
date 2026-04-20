package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habit_checkins")
public class HabitCheckinEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long habitId;
    public String checkinDate;
    public boolean completed;
    public long completedAt;
    public String note;
    public boolean synced;
}
