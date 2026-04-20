package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;

import java.util.List;

@Dao
public interface NutritionDao {
    @Insert
    long insert(NutritionEntryEntity entity);

    @Query("SELECT * FROM nutrition_entries WHERE entryDate = :date ORDER BY createdAt DESC")
    List<NutritionEntryEntity> getByDate(String date);

    @Query("SELECT * FROM nutrition_entries WHERE synced = 0")
    List<NutritionEntryEntity> getUnsynced();
}
