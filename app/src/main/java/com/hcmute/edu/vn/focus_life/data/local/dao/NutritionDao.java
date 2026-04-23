package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;

import java.util.List;

@Dao
public interface NutritionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(NutritionEntryEntity entity);

    @Query("SELECT * FROM nutrition_entries WHERE uid = :uid AND entryDate = :date AND deleted = 0 ORDER BY createdAt DESC")
    List<NutritionEntryEntity> getByDate(String uid, String date);

    @Query("SELECT * FROM nutrition_entries WHERE uid = :uid AND entryDate = :date ORDER BY updatedAt DESC")
    List<NutritionEntryEntity> getByDateIncludingDeleted(String uid, String date);

    @Query("SELECT * FROM nutrition_entries WHERE uid = :uid AND synced = 0 ORDER BY updatedAt ASC")
    List<NutritionEntryEntity> getUnsynced(String uid);

    @Query("SELECT * FROM nutrition_entries WHERE uid = :uid AND entryUuid = :entryUuid LIMIT 1")
    NutritionEntryEntity getByEntryUuid(String uid, String entryUuid);

    @Query("UPDATE nutrition_entries SET synced = :synced WHERE uid = :uid AND entryUuid = :entryUuid")
    void markSynced(String uid, String entryUuid, boolean synced);

    @Query("DELETE FROM nutrition_entries WHERE uid = :uid AND deleted = 1 AND synced = 1 AND updatedAt < :staleBefore")
    int pruneDeleted(String uid, long staleBefore);
}
