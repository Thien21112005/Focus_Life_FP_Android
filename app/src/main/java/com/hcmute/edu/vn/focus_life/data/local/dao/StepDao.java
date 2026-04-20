package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.StepRecordEntity;

import java.util.List;

@Dao
public interface StepDao {
    @Insert
    long insert(StepRecordEntity entity);

    @Query("SELECT * FROM step_records WHERE date = :date ORDER BY timestamp DESC")
    List<StepRecordEntity> getByDate(String date);

    @Query("SELECT * FROM step_records WHERE synced = 0")
    List<StepRecordEntity> getUnsynced();

    @Query("UPDATE step_records SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
}
