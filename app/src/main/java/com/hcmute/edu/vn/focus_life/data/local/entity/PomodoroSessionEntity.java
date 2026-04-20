package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pomodoro_sessions")
public class PomodoroSessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String sessionUuid;
    public String taskName;
    public String sessionType;
    public int durationMinutes;
    public long startedAt;
    public long endedAt;
    public boolean completed;
    public boolean synced;
}
