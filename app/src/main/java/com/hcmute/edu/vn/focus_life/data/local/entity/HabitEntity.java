package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habits")
public class HabitEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String remoteId;
    public String title;
    public String description;
    public String icon;
    public String colorKey;
    public String reminderTime;
    public boolean active;
    public long createdAt;
    public long updatedAt;
    public boolean synced;
}
