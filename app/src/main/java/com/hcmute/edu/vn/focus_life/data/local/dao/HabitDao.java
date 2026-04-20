package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.HabitCheckinEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.HabitEntity;

import java.util.List;

@Dao
public interface HabitDao {
    @Insert
    long insertHabit(HabitEntity entity);

    @Insert
    long insertCheckin(HabitCheckinEntity entity);

    @Query("SELECT * FROM habits WHERE active = 1 ORDER BY createdAt ASC")
    List<HabitEntity> getActiveHabits();

    @Query("SELECT * FROM habit_checkins WHERE checkinDate = :date")
    List<HabitCheckinEntity> getCheckinsByDate(String date);

    @Query("SELECT * FROM habit_checkins WHERE synced = 0")
    List<HabitCheckinEntity> getUnsyncedCheckins();
}
