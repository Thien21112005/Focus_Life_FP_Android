package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.PomodoroSessionEntity;

import java.util.List;

@Dao
public interface PomodoroDao {
    @Insert
    long insert(PomodoroSessionEntity entity);

    @Query("SELECT * FROM pomodoro_sessions ORDER BY startedAt DESC")
    List<PomodoroSessionEntity> getAll();

    @Query("SELECT * FROM pomodoro_sessions WHERE synced = 0")
    List<PomodoroSessionEntity> getUnsynced();
}
