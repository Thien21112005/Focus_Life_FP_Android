package com.hcmute.edu.vn.focus_life.ui.profile;

import androidx.lifecycle.ViewModel;

import com.hcmute.edu.vn.focus_life.data.repository.AuthRepository;

public class ProfileViewModel extends ViewModel {
    public void logout(AuthRepository repository) {
        repository.logout();
    }
}
