package com.hcmute.edu.vn.focus_life.data.remote.firestore;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;

import java.util.HashMap;
import java.util.Map;

public class UserRemoteDataSource {
    public interface WriteCallback {
        void onComplete(boolean success, @Nullable Exception error);
    }

    public interface UserCallback {
        void onLoaded(@Nullable UserProfile profile);
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void upsertUser(UserProfile profile) {
        upsertUser(profile, null);
    }

    public void upsertUser(UserProfile profile, @Nullable WriteCallback callback) {
        if (profile == null || profile.uid == null || profile.uid.trim().isEmpty()) {
            if (callback != null) callback.onComplete(false, new IllegalArgumentException("Missing user id"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("displayName", profile.displayName);
        data.put("email", profile.email);
        data.put("phone", profile.phone);
        data.put("dateOfBirth", profile.dateOfBirth);
        data.put("gender", profile.gender);
        data.put("heightCm", profile.heightCm);
        data.put("weightKg", profile.weightKg);
        data.put("avatarUrl", profile.avatarUrl);
        data.put("primaryGoal", profile.primaryGoal);
        data.put("authProvider", profile.authProvider);
        data.put("createdAt", profile.createdAt);
        data.put("updatedAt", profile.updatedAt);

        firestore.collection(Constants.FIRESTORE_USERS)
                .document(profile.uid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onComplete(false, error);
                });
    }

    public void deleteUser(String uid, @Nullable WriteCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) callback.onComplete(false, new IllegalArgumentException("Missing user id"));
            return;
        }

        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onComplete(false, error);
                });
    }

    public void fetchUser(String uid, UserCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onLoaded(null);
            return;
        }

        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> callback.onLoaded(mapSnapshot(snapshot)))
                .addOnFailureListener(error -> callback.onLoaded(null));
    }

    @Nullable
    private UserProfile mapSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        UserProfile profile = new UserProfile();
        profile.uid = snapshot.getId();
        profile.displayName = snapshot.getString("displayName");
        profile.email = snapshot.getString("email");
        profile.phone = snapshot.getString("phone");
        profile.dateOfBirth = snapshot.getString("dateOfBirth");
        profile.gender = snapshot.getString("gender");
        profile.avatarUrl = snapshot.getString("avatarUrl");
        profile.primaryGoal = snapshot.getString("primaryGoal");
        profile.authProvider = snapshot.getString("authProvider");

        Double height = snapshot.getDouble("heightCm");
        Double weight = snapshot.getDouble("weightKg");
        Long createdAt = snapshot.getLong("createdAt");
        Long updatedAt = snapshot.getLong("updatedAt");

        profile.heightCm = height == null ? 0f : height.floatValue();
        profile.weightKg = weight == null ? 0f : weight.floatValue();
        profile.createdAt = createdAt == null ? 0L : createdAt;
        profile.updatedAt = updatedAt == null ? 0L : updatedAt;

        return profile;
    }
}
