package com.hcmute.edu.vn.focus_life.data.repository;

import android.app.Activity;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.core.exception.FirebaseExceptionMapper;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.ProfileEntity;
import com.hcmute.edu.vn.focus_life.data.remote.firestore.UserRemoteDataSource;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;

import java.util.concurrent.Executors;

public class ProfileRepository {
    public interface ProfileCallback {
        void onLoaded(@Nullable UserProfile profile);
    }

    public interface UpdateCallback {
        void onComplete(boolean success, String message, @Nullable UserProfile updatedProfile);
    }

    private final Activity activity;
    private final AppDatabase database;
    private final SessionManager sessionManager;
    private final UserRemoteDataSource userRemoteDataSource;

    public ProfileRepository(Activity activity) {
        this.activity = activity;
        this.database = FocusLifeApp.getInstance().getDatabase();
        this.sessionManager = FocusLifeApp.getInstance().getSessionManager();
        this.userRemoteDataSource = new UserRemoteDataSource();
    }

    public void getCurrentProfile(ProfileCallback callback) {
        String uid = sessionManager.requireUid();
        Executors.newSingleThreadExecutor().execute(() -> {
            ProfileEntity entity = uid == null ? null : database.profileDao().getByUid(uid);
            UserProfile localProfile = AuthRepository.mapEntity(entity);
            if (localProfile != null) {
                enrichAvatar(localProfile);
                activity.runOnUiThread(() -> callback.onLoaded(localProfile));
                return;
            }

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            UserProfile fallback = new UserProfile();
            fallback.uid = firebaseUser != null ? firebaseUser.getUid() : uid;
            fallback.email = firebaseUser != null ? firebaseUser.getEmail() : null;
            fallback.displayName = firebaseUser != null && firebaseUser.getDisplayName() != null
                    ? firebaseUser.getDisplayName()
                    : new OnboardingPreferences(activity).getDisplayName();
            fallback.authProvider = firebaseUser != null && firebaseUser.getPhotoUrl() != null
                    ? UserProfile.PROVIDER_GOOGLE
                    : UserProfile.PROVIDER_PASSWORD;
            fallback.avatarUrl = firebaseUser != null && firebaseUser.getPhotoUrl() != null
                    ? firebaseUser.getPhotoUrl().toString()
                    : Constants.DEFAULT_APP_AVATAR_URL;
            activity.runOnUiThread(() -> callback.onLoaded(fallback));
        });
    }

    public void updateAvatar(UpdateCallback callback) {
        getCurrentProfile(profile -> callback.onComplete(false, FirebaseExceptionMapper.toUserMessage(new IllegalStateException("Đổi avatar đã được tắt cho bản hiện tại")), profile));
    }

    private void enrichAvatar(UserProfile profile) {
        if (UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider)) {
            if (profile.avatarUrl == null) {
                profile.avatarUrl = "";
            }
            return;
        }

        if (profile.avatarUrl == null || profile.avatarUrl.trim().isEmpty()) {
            profile.avatarUrl = Constants.DEFAULT_APP_AVATAR_URL;
        }
    }
}
