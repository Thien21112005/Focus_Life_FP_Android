package com.hcmute.edu.vn.focus_life.data.remote.firestore;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;

import java.util.HashMap;
import java.util.Map;

public class UserRemoteDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void upsertUser(UserProfile profile) {
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", profile.displayName);
        data.put("email", profile.email);
        data.put("avatarUrl", profile.avatarUrl);
        data.put("primaryGoal", profile.primaryGoal);
        data.put("updatedAt", System.currentTimeMillis());
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(profile.uid)
                .set(data);
    }
}
