package com.hcmute.edu.vn.focus_life.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.core.common.Result;
import com.hcmute.edu.vn.focus_life.data.repository.AuthRepository;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;

public class AuthViewModel extends ViewModel {
    private final MutableLiveData<Result<FirebaseUser>> authState = new MutableLiveData<>();

    public LiveData<Result<FirebaseUser>> getAuthState() {
        return authState;
    }

    public void login(AuthRepository repository, String email, String password) {
        repository.login(email, password, authState::postValue);
    }

    public void register(AuthRepository repository, String email, String password, UserProfile profile) {
        repository.register(email, password, profile, authState::postValue);
    }

    public void handleGoogleResult(AuthRepository repository, android.content.Intent data) {
        repository.handleGoogleSignInResult(data, authState::postValue);
    }
}