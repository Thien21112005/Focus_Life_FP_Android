package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.WaterEntryEntity;

import java.util.List;

@Dao
public interface WaterDao {
    @Insert
    long insert(WaterEntryEntity entity);

    @Query("SELECT * FROM water_entries WHERE entryDate = :date ORDER BY createdAt DESC")
    List<WaterEntryEntity> getByDate(String date);

    @Query("SELECT COALESCE(SUM(amountMl),0) FROM water_entries WHERE entryDate = :date")
    int getTotalByDate(String date);

    @Query("SELECT * FROM water_entries WHERE synced = 0")
    List<WaterEntryEntity> getUnsynced();
}
