package com.hcmute.edu.vn.focus_life.domain.model;

public class UserProfile {
    public static final String PROVIDER_PASSWORD = "password";
    public static final String PROVIDER_GOOGLE = "google";

    public String uid;
    public String displayName;
    public String email;
    public String phone;
    public String dateOfBirth;
    public String gender;
    public float heightCm;
    public float weightKg;
    public String avatarUrl;
    public String primaryGoal;
    public String authProvider;
    public long createdAt;
    public long updatedAt;

    // Chỉ dùng tạm lúc đăng ký / đổi avatar, không lưu vào DB
    public String pendingAvatarUri;

    public UserProfile() {}

    public UserProfile(String uid,
                       String displayName,
                       String email,
                       String phone,
                       String dateOfBirth,
                       String gender,
                       float heightCm,
                       float weightKg,
                       String avatarUrl,
                       String primaryGoal,
                       String authProvider,
                       long createdAt,
                       long updatedAt) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.avatarUrl = avatarUrl;
        this.primaryGoal = primaryGoal;
        this.authProvider = authProvider;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
