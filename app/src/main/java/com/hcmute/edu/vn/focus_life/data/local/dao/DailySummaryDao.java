package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.DailySummaryEntity;

import java.util.List;

@Dao
public interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DailySummaryEntity entity);

    @Query("SELECT * FROM activity_daily_summary WHERE date = :date LIMIT 1")
    DailySummaryEntity getByDate(String date);

    @Query("SELECT * FROM activity_daily_summary WHERE synced = 0")
    List<DailySummaryEntity> getUnsynced();
}
