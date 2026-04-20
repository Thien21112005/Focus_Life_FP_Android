package com.hcmute.edu.vn.focus_life.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class ProfileEntity {
    @PrimaryKey
    @NonNull
    public String uid = "";

    public String displayName;
    public String email;
    public String phone;
    public String dateOfBirth;
    public String gender;
    public float heightCm;
    public float weightKg;
    public String avatarUrl;
    public String primaryGoal;

    public long createdAt;
    public long updatedAt;
    public boolean synced;
}