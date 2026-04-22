package com.hcmute.edu.vn.focus_life.data.repository;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.core.common.Result;
import com.hcmute.edu.vn.focus_life.core.exception.AppExceptionLogger;
import com.hcmute.edu.vn.focus_life.core.exception.FirebaseExceptionMapper;
import com.hcmute.edu.vn.focus_life.core.exception.UserFacingException;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
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
                if (user == null) {
                    callback.onComplete(Result.error(new UserFacingException("Không tạo được người dùng")));
                    return;
                }

                sessionManager.onLoginSuccess(user.getUid(), user.getEmail());

                profile.uid = user.getUid();
                profile.email = user.getEmail();
                profile.authProvider = UserProfile.PROVIDER_PASSWORD;
                profile.avatarUrl = Constants.DEFAULT_APP_AVATAR_URL;
                if (profile.createdAt == 0L) profile.createdAt = System.currentTimeMillis();
                profile.updatedAt = System.currentTimeMillis();

                if (profile.displayName != null && !profile.displayName.trim().isEmpty()) {
                    new OnboardingPreferences(activity).setDisplayName(profile.displayName);
                }

                saveProfile(profile);
                callback.onComplete(Result.success(user));
            }

            @Override
            public void onError(Exception e) {
                callback.onComplete(Result.error(toUserFacingException("register", e)));
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
                    hydrateProfileAfterLogin(user, callback);
                } else {
                    callback.onComplete(Result.success(null));
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onComplete(Result.error(toUserFacingException("login", e)));
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
                if (user == null) {
                    callback.onComplete(Result.error(new UserFacingException("Không lấy được tài khoản Google")));
                    return;
                }

                sessionManager.onLoginSuccess(user.getUid(), user.getEmail());

                UserProfile profile = new UserProfile();
                profile.uid = user.getUid();
                profile.email = user.getEmail();
                profile.displayName = user.getDisplayName();
                profile.phone = user.getPhoneNumber();
                profile.avatarUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
                profile.primaryGoal = new OnboardingPreferences(activity).getPrimaryGoal();
                profile.authProvider = UserProfile.PROVIDER_GOOGLE;
                profile.createdAt = System.currentTimeMillis();
                profile.updatedAt = System.currentTimeMillis();

                if (profile.displayName != null && !profile.displayName.trim().isEmpty()) {
                    new OnboardingPreferences(activity).setDisplayName(profile.displayName);
                }

                saveProfile(profile);
                callback.onComplete(Result.success(user));
            }

            @Override
            public void onError(Exception e) {
                callback.onComplete(Result.error(toUserFacingException("google_sign_in", e)));
            }
        });
    }

    private void hydrateProfileAfterLogin(FirebaseUser user, RepositoryCallback callback) {
        userRemoteDataSource.fetchUser(user.getUid(), remoteProfile -> {
            UserProfile profileToSave;
            String provider = guessProvider(user);

            if (remoteProfile != null) {
                profileToSave = remoteProfile;
                if (profileToSave.uid == null || profileToSave.uid.trim().isEmpty()) {
                    profileToSave.uid = user.getUid();
                }
                if (profileToSave.email == null || profileToSave.email.trim().isEmpty()) {
                    profileToSave.email = user.getEmail();
                }
                if (profileToSave.displayName == null || profileToSave.displayName.trim().isEmpty()) {
                    profileToSave.displayName = resolveDisplayName(user, provider);
                }
                if (profileToSave.updatedAt == 0L) {
                    profileToSave.updatedAt = System.currentTimeMillis();
                }
                if (profileToSave.createdAt == 0L) {
                    profileToSave.createdAt = System.currentTimeMillis();
                }
            } else {
                profileToSave = new UserProfile();
                profileToSave.uid = user.getUid();
                profileToSave.email = user.getEmail();
                profileToSave.displayName = resolveDisplayName(user, provider);
                profileToSave.phone = user.getPhoneNumber();
                profileToSave.primaryGoal = new OnboardingPreferences(activity).getPrimaryGoal();
                profileToSave.createdAt = System.currentTimeMillis();
                profileToSave.updatedAt = System.currentTimeMillis();
                profileToSave.authProvider = provider;
            }

            profileToSave.authProvider = provider;
            if (UserProfile.PROVIDER_GOOGLE.equals(provider)) {
                profileToSave.avatarUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
            } else if (profileToSave.avatarUrl == null || profileToSave.avatarUrl.trim().isEmpty()) {
                profileToSave.avatarUrl = Constants.DEFAULT_APP_AVATAR_URL;
            }

            if (profileToSave.displayName != null && !profileToSave.displayName.trim().isEmpty()) {
                new OnboardingPreferences(activity).setDisplayName(profileToSave.displayName);
            }

            saveProfile(profileToSave);
            callback.onComplete(Result.success(user));
        });
    }

    private UserFacingException toUserFacingException(String scope, Exception e) {
        AppExceptionLogger.log(scope, e);
        return new UserFacingException(FirebaseExceptionMapper.toUserMessage(e), e);
    }

    private String resolveDisplayName(FirebaseUser user, String provider) {
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }

        String onboardingName = new OnboardingPreferences(activity).getDisplayName();
        if (onboardingName != null && !onboardingName.trim().isEmpty()) {
            return onboardingName;
        }

        if (user != null && user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf('@'));
        }

        return UserProfile.PROVIDER_GOOGLE.equals(provider) ? "Google User" : "FocusLife User";
    }

    private String guessProvider(FirebaseUser user) {
        if (user != null && user.getProviderData() != null) {
            for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
                if ("google.com".equals(info.getProviderId())) {
                    return UserProfile.PROVIDER_GOOGLE;
                }
            }
        }
        return UserProfile.PROVIDER_PASSWORD;
    }

    private void saveProfile(UserProfile profile) {
        Executors.newSingleThreadExecutor().execute(() -> {
            userRemoteDataSource.upsertUser(profile);
            database.profileDao().upsert(toEntity(profile));
        });
    }

    private ProfileEntity toEntity(UserProfile profile) {
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
        entity.authProvider = profile.authProvider;
        entity.createdAt = profile.createdAt;
        entity.updatedAt = profile.updatedAt;
        entity.synced = true;
        return entity;
    }

    public static UserProfile mapEntity(@Nullable ProfileEntity entity) {
        if (entity == null) return null;

        UserProfile profile = new UserProfile();
        profile.uid = entity.uid;
        profile.displayName = entity.displayName;
        profile.email = entity.email;
        profile.phone = entity.phone;
        profile.dateOfBirth = entity.dateOfBirth;
        profile.gender = entity.gender;
        profile.heightCm = entity.heightCm;
        profile.weightKg = entity.weightKg;
        profile.avatarUrl = entity.avatarUrl;
        profile.primaryGoal = entity.primaryGoal;
        profile.authProvider = entity.authProvider;
        profile.createdAt = entity.createdAt;
        profile.updatedAt = entity.updatedAt;
        return profile;
    }

    public void logout() {
        remoteDataSource.logout();
        sessionManager.logout();
    }
}
