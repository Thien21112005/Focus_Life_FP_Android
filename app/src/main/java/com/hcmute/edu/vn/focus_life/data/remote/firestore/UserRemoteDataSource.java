package com.hcmute.edu.vn.focus_life.data.remote.firestore;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;

import java.util.HashMap;
import java.util.Map;

public class UserRemoteDataSource {
    public interface UserProfileCallback {
        void onSuccess(UserProfile profile);
        void onError(Exception exception);
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void upsertUser(UserProfile profile) {
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
        data.put("createdAt", profile.createdAt);
        data.put("updatedAt", profile.updatedAt);

        firestore.collection(Constants.FIRESTORE_USERS)
                .document(profile.uid)
                .set(data);
    }

    public void getUser(String uid, UserProfileCallback callback) {
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(mapSnapshotToProfile(uid, snapshot)))
                .addOnFailureListener(callback::onError);
    }

    private UserProfile mapSnapshotToProfile(String uid, DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        UserProfile profile = new UserProfile();
        profile.uid = uid;
        profile.displayName = snapshot.getString("displayName");
        profile.email = snapshot.getString("email");
        profile.phone = snapshot.getString("phone");
        profile.dateOfBirth = snapshot.getString("dateOfBirth");
        profile.gender = snapshot.getString("gender");
        profile.avatarUrl = snapshot.getString("avatarUrl");
        profile.primaryGoal = snapshot.getString("primaryGoal");

        Number height = snapshot.getDouble("heightCm");
        if (height != null) profile.heightCm = height.floatValue();
        Number weight = snapshot.getDouble("weightKg");
        if (weight != null) profile.weightKg = weight.floatValue();
        Number createdAt = snapshot.getLong("createdAt");
        if (createdAt != null) profile.createdAt = createdAt.longValue();
        Number updatedAt = snapshot.getLong("updatedAt");
        if (updatedAt != null) profile.updatedAt = updatedAt.longValue();

        return profile;
    }
}
