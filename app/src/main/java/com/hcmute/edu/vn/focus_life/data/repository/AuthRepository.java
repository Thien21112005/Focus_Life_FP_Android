package com.hcmute.edu.vn.focus_life.data.repository;

import android.app.Activity;
import android.content.Intent;

import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.core.common.Result;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.data.remote.auth.AuthRemoteDataSource;

public class AuthRepository {
    public interface RepositoryCallback {
        void onComplete(Result<FirebaseUser> result);
    }

    private final AuthRemoteDataSource remoteDataSource;
    private final SessionManager sessionManager;

    public AuthRepository(Activity activity) {
        remoteDataSource = new AuthRemoteDataSource(activity);
        sessionManager = FocusLifeApp.getInstance().getSessionManager();
    }

    public void register(String email, String password, RepositoryCallback callback) {
        remoteDataSource.register(email, password, new AuthRemoteDataSource.AuthCallback() {
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
                }
                callback.onComplete(Result.success(user));
            }

            @Override
            public void onError(Exception e) {
                callback.onComplete(Result.error(e));
            }
        });
    }

    public void logout() {
        remoteDataSource.logout();
        sessionManager.logout();
    }
}
