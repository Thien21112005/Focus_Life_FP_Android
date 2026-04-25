package com.hcmute.edu.vn.focus_life.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hcmute.edu.vn.focus_life.data.local.entity.ProfileEntity;

import java.util.List;

@Dao
public interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProfileEntity entity);

    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    ProfileEntity getByUid(String uid);

    @Query("SELECT * FROM profiles WHERE synced = 0")
    List<ProfileEntity> getUnsynced();

    @Query("DELETE FROM profiles WHERE uid = :uid")
    void deleteByUid(String uid);
}