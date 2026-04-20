package com.hcmute.edu.vn.focus_life.domain.model;

public class UserProfile {
    public String uid;
    public String displayName;
    public String email;
    public String avatarUrl;
    public String primaryGoal;

    public UserProfile() {}

    public UserProfile(String uid, String displayName, String email, String avatarUrl, String primaryGoal) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.primaryGoal = primaryGoal;
    }
}
