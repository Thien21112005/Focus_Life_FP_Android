package com.hcmute.edu.vn.focus_life.data.repository;

import android.app.Activity;
import android.content.Intent;

import com.google.firebase.auth.FirebaseUser;
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

    public AuthRepository(Activity activity) {
        this.activity = activity;
        this.remoteDataSource = new AuthRemoteDataSource(activity);
        this.sessionManager = FocusLifeApp.getInstance().getSessionManager();
        this.database = FocusLifeApp.getInstance().getDatabase();
        this.userRemoteDataSource = new UserRemoteDataSource();
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

                    if (profile.displayName != null && !profile.displayName.trim().isEmpty()) {
                        new OnboardingPreferences(activity).setDisplayName(profile.displayName);
                    }

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

                    if (profile.displayName != null && !profile.displayName.trim().isEmpty()) {
                        new OnboardingPreferences(activity).setDisplayName(profile.displayName);
                    }

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

    private void saveProfile(UserProfile profile) {
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