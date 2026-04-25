package com.hcmute.edu.vn.focus_life.data.repository;

import android.app.Activity;

import androidx.annotation.Nullable;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.exception.AppExceptionLogger;
import com.hcmute.edu.vn.focus_life.core.exception.FirebaseExceptionMapper;
import com.hcmute.edu.vn.focus_life.core.exception.ProfileSettingsException;
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

    public interface SimpleCallback {
        void onComplete(boolean success, String message);
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
                hydrateProvider(localProfile);
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
            fallback.authProvider = guessProvider(firebaseUser);
            fallback.avatarUrl = firebaseUser != null && firebaseUser.getPhotoUrl() != null
                    ? firebaseUser.getPhotoUrl().toString()
                    : Constants.DEFAULT_APP_AVATAR_URL;
            fallback.primaryGoal = new OnboardingPreferences(activity).getPrimaryGoal();
            fallback.createdAt = System.currentTimeMillis();
            fallback.updatedAt = System.currentTimeMillis();
            enrichAvatar(fallback);
            activity.runOnUiThread(() -> callback.onLoaded(fallback));
        });
    }

    public void updateProfile(UserProfile editedProfile, UpdateCallback callback) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            callback.onComplete(false, activity.getString(R.string.error_login_required), null);
            return;
        }

        String provider = guessProvider(firebaseUser);
        boolean googleUser = UserProfile.PROVIDER_GOOGLE.equals(provider);
        String googleName = safe(firebaseUser.getDisplayName(), "").trim();
        String googleEmail = safe(firebaseUser.getEmail(), "").trim();

        if (googleUser && !googleName.isEmpty()) {
            editedProfile.displayName = googleName;
        }
        if (googleUser && !googleEmail.isEmpty()) {
            editedProfile.email = googleEmail;
        }

        try {
            validateProfile(editedProfile);
        } catch (ProfileSettingsException e) {
            callback.onComplete(false, e.getUserMessage(), editedProfile);
            return;
        }

        String requestedEmail = safe(editedProfile.email, firebaseUser.getEmail()).trim();
        String currentEmail = safe(firebaseUser.getEmail(), "").trim();
        if (requestedEmail.isEmpty()) {
            callback.onComplete(false, activity.getString(R.string.error_invalid_email), editedProfile);
            return;
        }

        editedProfile.uid = firebaseUser.getUid();
        editedProfile.email = requestedEmail;
        editedProfile.authProvider = provider;
        editedProfile.avatarUrl = googleUser && firebaseUser.getPhotoUrl() != null
                ? firebaseUser.getPhotoUrl().toString()
                : safe(editedProfile.avatarUrl, Constants.DEFAULT_APP_AVATAR_URL);
        if (editedProfile.createdAt == 0L) editedProfile.createdAt = System.currentTimeMillis();
        editedProfile.updatedAt = System.currentTimeMillis();

        if (!googleUser) {
            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                    .setDisplayName(editedProfile.displayName)
                    .build();
            firebaseUser.updateProfile(request).addOnFailureListener(error ->
                    AppExceptionLogger.log("profile_update_auth_display_name", toException(error)));
        }

        Runnable persist = () -> saveProfileLocallyAndRemotely(editedProfile, callback);
        if (!googleUser && !requestedEmail.equalsIgnoreCase(currentEmail)) {
            firebaseUser.updateEmail(requestedEmail)
                    .addOnSuccessListener(unused -> persist.run())
                    .addOnFailureListener(error -> {
                        AppExceptionLogger.log("profile_update_email", toException(error));
                        callback.onComplete(false, FirebaseExceptionMapper.toUserMessage(toException(error)), editedProfile);
                    });
        } else {
            persist.run();
        }
    }

    private void saveProfileLocallyAndRemotely(UserProfile editedProfile, UpdateCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                database.profileDao().upsert(toEntity(editedProfile, false));
                new OnboardingPreferences(activity).setDisplayName(editedProfile.displayName);
                userRemoteDataSource.upsertUser(editedProfile, (success, error) -> {
                    if (success) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            database.profileDao().upsert(toEntity(editedProfile, true));
                            activity.runOnUiThread(() -> callback.onComplete(
                                    true,
                                    activity.getString(R.string.profile_update_success),
                                    editedProfile));
                        });
                    } else {
                        AppExceptionLogger.log("profile_update_remote", toException(error));
                        activity.runOnUiThread(() -> callback.onComplete(
                                true,
                                activity.getString(R.string.profile_update_saved_offline),
                                editedProfile));
                    }
                });
            } catch (Exception e) {
                AppExceptionLogger.log("profile_update_local", e);
                activity.runOnUiThread(() -> callback.onComplete(false, FirebaseExceptionMapper.toUserMessage(e), editedProfile));
            }
        });
    }

    public void changePassword(String oldPassword,
                               String newPassword,
                               String confirmPassword,
                               SimpleCallback callback) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || firebaseUser.getEmail() == null) {
            callback.onComplete(false, activity.getString(R.string.error_login_required));
            return;
        }

        if (isGoogleUser(firebaseUser, null)) {
            callback.onComplete(false, activity.getString(R.string.password_google_hidden));
            return;
        }

        try {
            validatePasswordChange(oldPassword, newPassword, confirmPassword);
        } catch (ProfileSettingsException e) {
            callback.onComplete(false, e.getUserMessage());
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), oldPassword);
        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(authResult -> firebaseUser.updatePassword(newPassword)
                        .addOnSuccessListener(unused -> callback.onComplete(true, activity.getString(R.string.password_update_success)))
                        .addOnFailureListener(error -> {
                            AppExceptionLogger.log("password_update", toException(error));
                            callback.onComplete(false, FirebaseExceptionMapper.toUserMessage(toException(error)));
                        }))
                .addOnFailureListener(error -> {
                    AppExceptionLogger.log("password_reauth", toException(error));
                    callback.onComplete(false, FirebaseExceptionMapper.toUserMessage(toException(error)));
                });
    }

    public void deleteAccount(@Nullable String currentPassword, SimpleCallback callback) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            callback.onComplete(false, activity.getString(R.string.error_login_required));
            return;
        }

        if (!isGoogleUser(firebaseUser, null) && currentPassword != null && !currentPassword.trim().isEmpty()) {
            AuthCredential credential = EmailAuthProvider.getCredential(safe(firebaseUser.getEmail(), ""), currentPassword);
            firebaseUser.reauthenticate(credential)
                    .addOnSuccessListener(result -> performDeleteAccount(firebaseUser, callback))
                    .addOnFailureListener(error -> {
                        AppExceptionLogger.log("delete_account_reauth", toException(error));
                        callback.onComplete(false, FirebaseExceptionMapper.toUserMessage(toException(error)));
                    });
        } else {
            performDeleteAccount(firebaseUser, callback);
        }
    }

    public void updateAvatar(UpdateCallback callback) {
        getCurrentProfile(profile -> callback.onComplete(false, FirebaseExceptionMapper.toUserMessage(new IllegalStateException("Đổi avatar đã được tắt cho bản hiện tại")), profile));
    }

    private void performDeleteAccount(FirebaseUser firebaseUser, SimpleCallback callback) {
        String uid = firebaseUser.getUid();
        userRemoteDataSource.deleteUser(uid, (cloudSuccess, cloudError) -> {
            if (!cloudSuccess && cloudError != null) {
                AppExceptionLogger.log("delete_account_remote", toException(cloudError));
            }

            firebaseUser.delete()
                    .addOnSuccessListener(unused -> Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            database.profileDao().deleteByUid(uid);
                            sessionManager.logout();
                            FirebaseAuth.getInstance().signOut();
                            activity.runOnUiThread(() -> callback.onComplete(true, activity.getString(R.string.delete_account_success)));
                        } catch (Exception e) {
                            AppExceptionLogger.log("delete_account_local", e);
                            sessionManager.logout();
                            FirebaseAuth.getInstance().signOut();
                            activity.runOnUiThread(() -> callback.onComplete(true, activity.getString(R.string.delete_account_success)));
                        }
                    }))
                    .addOnFailureListener(error -> {
                        AppExceptionLogger.log("delete_account_auth", toException(error));
                        callback.onComplete(false, activity.getString(R.string.delete_account_recent_login_required));
                    });
        });
    }

    private void validateProfile(UserProfile profile) throws ProfileSettingsException {
        if (profile == null) {
            throw new ProfileSettingsException(activity.getString(R.string.error_profile_empty));
        }
        if (profile.displayName == null || profile.displayName.trim().isEmpty()) {
            throw new ProfileSettingsException(activity.getString(R.string.error_name_required));
        }
        profile.displayName = profile.displayName.trim();
        profile.phone = safe(profile.phone, "").trim();
        if (!profile.phone.isEmpty() && profile.phone.length() != 10) {
            throw new ProfileSettingsException(activity.getString(R.string.error_phone_10_digits));
        }
        profile.dateOfBirth = safe(profile.dateOfBirth, "").trim();
        profile.gender = safe(profile.gender, "").trim();
        profile.primaryGoal = safe(profile.primaryGoal, new OnboardingPreferences(activity).getPrimaryGoal()).trim();
        if (profile.heightCm < 0f || profile.weightKg < 0f) {
            throw new ProfileSettingsException(activity.getString(R.string.error_invalid_health_numbers));
        }
    }

    private void validatePasswordChange(String oldPassword,
                                        String newPassword,
                                        String confirmPassword) throws ProfileSettingsException {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new ProfileSettingsException(activity.getString(R.string.error_old_password_required));
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ProfileSettingsException(activity.getString(R.string.error_new_password_length));
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ProfileSettingsException(activity.getString(R.string.error_password_confirm_mismatch));
        }
        if (oldPassword.equals(newPassword)) {
            throw new ProfileSettingsException(activity.getString(R.string.error_password_same));
        }
    }

    private void hydrateProvider(UserProfile profile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String provider = guessProvider(user);
        if (profile.authProvider == null || profile.authProvider.trim().isEmpty()
                || UserProfile.PROVIDER_GOOGLE.equals(provider)) {
            profile.authProvider = provider;
        }
        if (profile.email == null || profile.email.trim().isEmpty()) {
            profile.email = user != null ? user.getEmail() : null;
        }
    }

    private void enrichAvatar(UserProfile profile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider)) {
            profile.avatarUrl = user != null && user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : safe(profile.avatarUrl, "");
            return;
        }

        if (profile.avatarUrl == null || profile.avatarUrl.trim().isEmpty()) {
            profile.avatarUrl = Constants.DEFAULT_APP_AVATAR_URL;
        }
    }

    private boolean isGoogleUser(FirebaseUser user, @Nullable UserProfile profile) {
        if (profile != null && UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider)) {
            return true;
        }
        return UserProfile.PROVIDER_GOOGLE.equals(guessProvider(user));
    }

    private String guessProvider(@Nullable FirebaseUser user) {
        if (user != null && user.getProviderData() != null) {
            for (UserInfo info : user.getProviderData()) {
                if ("google.com".equals(info.getProviderId())) {
                    return UserProfile.PROVIDER_GOOGLE;
                }
            }
        }
        return UserProfile.PROVIDER_PASSWORD;
    }

    private ProfileEntity toEntity(UserProfile profile, boolean synced) {
        ProfileEntity entity = new ProfileEntity();
        entity.uid = safe(profile.uid, "");
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
        entity.synced = synced;
        return entity;
    }

    private Exception toException(Exception exception) {
        return exception == null ? new Exception(activity.getString(R.string.error_unknown)) : exception;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
