package com.hcmute.edu.vn.focus_life.data.repository;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.core.common.Result;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.ProfileEntity;
import com.hcmute.edu.vn.focus_life.data.remote.auth.AuthRemoteDataSource;
import com.hcmute.edu.vn.focus_life.data.remote.firestore.UserRemoteDataSource;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;

import java.util.concurrent.Executors;

public class AuthRepository {
    public interface RepositoryCallback {
        void onComplete(Result<FirebaseUser> result);
    }

    private final AuthRemoteDataSource remoteDataSource;
    private final SessionManager sessionManager;
    private final AppDatabase database;
    private final UserRemoteDataSource userRemoteDataSource;
    private final Activity activity;
    private final FirebaseStorage storage;

    public AuthRepository(Activity activity) {
        this.activity = activity;
        this.remoteDataSource = new AuthRemoteDataSource(activity);
        this.sessionManager = FocusLifeApp.getInstance().getSessionManager();
        this.database = FocusLifeApp.getInstance().getDatabase();
        this.userRemoteDataSource = new UserRemoteDataSource();
        this.storage = FirebaseStorage.getInstance();
    }

    public void register(String email, String password, UserProfile profile, RepositoryCallback callback) {
        remoteDataSource.register(email, password, new AuthRemoteDataSource.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.AuthResult result) {
                FirebaseUser user = result.getUser();
                if (user != null) {
                    sessionManager.onLoginSuccess(user.getUid(), user.getEmail());

                    profile.uid = user.getUid();
                    profile.email = user.getEmail();
                    if (profile.createdAt == 0L) profile.createdAt = System.currentTimeMillis();
                    profile.updatedAt = System.currentTimeMillis();

                    applyPendingProfile(profile, user);
                    saveProfile(profile);
                }
                callback.onComplete(Result.success(user));
            }

            @Override
            public void onError(Exception e) {
                callback.onComplete(Result.error(e));
            }
        });
    }

    public void login(String email, String password, RepositoryCallback callback) {
        remoteDataSource.login(email, password, new AuthRemoteDataSource.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.AuthResult result) {
                FirebaseUser user = result.getUser();
                if (user != null) {
                    sessionManager.onLoginSuccess(user.getUid(), user.getEmail());
                    syncExistingProfile(user);
                }
                callback.onComplete(Result.success(user));
            }

            @Override
            public void onError(Exception e) {
                callback.onComplete(Result.error(e));
            }
        });
    }

    public Intent getGoogleSignInIntent() {
        return remoteDataSource.getGoogleSignInIntent();
    }

    public void handleGoogleSignInResult(Intent data, RepositoryCallback callback) {
        remoteDataSource.handleGoogleSignInResult(data, new AuthRemoteDataSource.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.AuthResult result) {
                FirebaseUser user = result.getUser();
                if (user != null) {
                    sessionManager.onLoginSuccess(user.getUid(), user.getEmail());

                    UserProfile profile = new UserProfile();
                    profile.uid = user.getUid();
                    profile.email = user.getEmail();
                    profile.displayName = user.getDisplayName();
                    profile.phone = user.getPhoneNumber();
                    profile.avatarUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
                    profile.primaryGoal = new OnboardingPreferences(activity).getPrimaryGoal();
                    profile.createdAt = System.currentTimeMillis();
                    profile.updatedAt = System.currentTimeMillis();

                    applyPendingProfile(profile, user);
                    saveProfile(profile);
                }
                callback.onComplete(Result.success(user));
            }

            @Override
            public void onError(Exception e) {
                callback.onComplete(Result.error(e));
            }
        });
    }

    private void syncExistingProfile(FirebaseUser user) {
        userRemoteDataSource.getUser(user.getUid(), new UserRemoteDataSource.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                UserProfile merged = profile != null ? profile : createProfileFromFirebaseUser(user);
                applyPendingProfile(merged, user);
                saveProfile(merged);
            }

            @Override
            public void onError(Exception exception) {
                UserProfile fallback = createProfileFromFirebaseUser(user);
                applyPendingProfile(fallback, user);
                saveProfile(fallback);
            }
        });
    }

    private UserProfile createProfileFromFirebaseUser(FirebaseUser user) {
        UserProfile profile = new UserProfile();
        profile.uid = user.getUid();
        profile.email = user.getEmail();
        profile.displayName = user.getDisplayName();
        profile.phone = user.getPhoneNumber();
        profile.avatarUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
        profile.primaryGoal = new OnboardingPreferences(activity).getPrimaryGoal();
        profile.createdAt = System.currentTimeMillis();
        profile.updatedAt = System.currentTimeMillis();
        return profile;
    }

    private void applyPendingProfile(UserProfile profile, FirebaseUser user) {
        OnboardingPreferences preferences = new OnboardingPreferences(activity);

        String pendingName = preferences.getDisplayName();
        if (pendingName != null && !pendingName.trim().isEmpty()) {
            profile.displayName = pendingName.trim();
            preferences.setDisplayName(profile.displayName);
        } else if ((profile.displayName == null || profile.displayName.trim().isEmpty())
                && user != null && user.getDisplayName() != null) {
            profile.displayName = user.getDisplayName();
            preferences.setDisplayName(profile.displayName);
        }

        String pendingGoal = preferences.getPrimaryGoal();
        if (pendingGoal != null && !pendingGoal.trim().isEmpty()) {
            profile.primaryGoal = pendingGoal;
        }

        String avatarSource = preferences.getBestAvatarSource();
        if (avatarSource != null && !avatarSource.trim().isEmpty()) {
            profile.avatarUrl = avatarSource;
        }

        if (profile.avatarUrl != null && profile.avatarUrl.startsWith("http")) {
            preferences.setAvatarUrl(profile.avatarUrl);
        }
        profile.updatedAt = System.currentTimeMillis();
    }

    private void saveProfile(UserProfile profile) {
        if (profile.avatarUrl != null && profile.avatarUrl.startsWith("content://")) {
            uploadAvatarThenSave(profile);
            return;
        }
        persistProfile(profile);
    }

    private void uploadAvatarThenSave(UserProfile profile) {
        StorageReference avatarRef = storage.getReference()
                .child("avatars")
                .child(profile.uid + "_" + System.currentTimeMillis() + ".jpg");

        avatarRef.putFile(Uri.parse(profile.avatarUrl))
                .addOnSuccessListener(taskSnapshot -> avatarRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            profile.avatarUrl = downloadUri.toString();
                            persistProfile(profile);
                        })
                        .addOnFailureListener(e -> persistProfile(profile)))
                .addOnFailureListener(e -> persistProfile(profile));
    }

    private void persistProfile(UserProfile profile) {
        OnboardingPreferences preferences = new OnboardingPreferences(activity);
        if (profile.displayName != null && !profile.displayName.trim().isEmpty()) {
            preferences.setDisplayName(profile.displayName);
        }
        if (profile.avatarUrl != null && !profile.avatarUrl.trim().isEmpty()) {
            if (profile.avatarUrl.startsWith("http")) {
                preferences.setAvatarUrl(profile.avatarUrl);
                preferences.clearPendingAvatarUri();
            } else {
                preferences.setPendingAvatarUri(profile.avatarUrl);
            }
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            userRemoteDataSource.upsertUser(profile);

            ProfileEntity entity = new ProfileEntity();
            entity.uid = profile.uid != null ? profile.uid : "";
            entity.displayName = profile.displayName;
            entity.email = profile.email;
            entity.phone = profile.phone;
            entity.dateOfBirth = profile.dateOfBirth;
            entity.gender = profile.gender;
            entity.heightCm = profile.heightCm;
            entity.weightKg = profile.weightKg;
            entity.avatarUrl = profile.avatarUrl;
            entity.primaryGoal = profile.primaryGoal;
            entity.createdAt = profile.createdAt;
            entity.updatedAt = profile.updatedAt;
            entity.synced = true;

            database.profileDao().upsert(entity);
        });
    }

    public void logout() {
        remoteDataSource.logout();
        sessionManager.logout();
    }
}
