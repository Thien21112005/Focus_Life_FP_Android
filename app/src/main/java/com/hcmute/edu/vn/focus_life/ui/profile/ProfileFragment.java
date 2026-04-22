package com.hcmute.edu.vn.focus_life.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.data.repository.AuthRepository;
import com.hcmute.edu.vn.focus_life.data.repository.ProfileRepository;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;
import com.hcmute.edu.vn.focus_life.ui.auth.LoginActivity;
import com.hcmute.edu.vn.focus_life.ui.focus.PomodoroPreferences;
import com.hcmute.edu.vn.focus_life.ui.focus.PomodoroSettingsActivity;

public class ProfileFragment extends Fragment {
    private ProfileRepository profileRepository;
    private ImageView imgProfileAvatar;
    private TextView tvProfileInitial;
    private TextView tvTopBarInitial;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvPomodoroSettingSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileRepository = new ProfileRepository(requireActivity());

        imgProfileAvatar = view.findViewById(R.id.imgProfileAvatar);
        tvProfileInitial = view.findViewById(R.id.tvProfileInitial);
        tvTopBarInitial = view.findViewById(R.id.tvTopBarInitial);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvPomodoroSettingSummary = view.findViewById(R.id.tvPomodoroSettingSummary);
        TextView btnLogout = view.findViewById(R.id.btnLogout);
        View rowPomodoroSettings = view.findViewById(R.id.rowPomodoroSettings);

        rowPomodoroSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), PomodoroSettingsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            new AuthRepository(requireActivity()).logout();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finishAffinity();
        });

        loadProfile();
        bindPomodoroSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        bindPomodoroSummary();
    }

    private void loadProfile() {
        if (profileRepository == null) return;

        profileRepository.getCurrentProfile(profile -> {
            if (profile != null) bindProfile(profile);
        });
    }

    private void bindProfile(UserProfile profile) {
        String displayName = safe(profile.displayName, new OnboardingPreferences(requireContext()).getDisplayName());
        String email = safe(profile.email, currentEmail());
        String avatarUrl = safe(profile.avatarUrl,
                UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider) ? "" : Constants.DEFAULT_APP_AVATAR_URL);

        tvProfileName.setText(displayName);
        tvProfileEmail.setText(email);

        String initial = displayName.isEmpty() ? "F" : displayName.substring(0, 1).toUpperCase();
        tvProfileInitial.setText(initial);
        tvTopBarInitial.setText(initial);

        if (!avatarUrl.isEmpty()) {
            imgProfileAvatar.setVisibility(View.VISIBLE);
            tvProfileInitial.setVisibility(View.GONE);
            Glide.with(this).load(avatarUrl).circleCrop().into(imgProfileAvatar);
        } else {
            imgProfileAvatar.setVisibility(View.GONE);
            tvProfileInitial.setVisibility(View.VISIBLE);
        }
    }

    private void bindPomodoroSummary() {
        PomodoroPreferences.Config config = new PomodoroPreferences(requireContext()).getConfig();
        String summary = config.focusMinutes + "m focus · "
                + config.shortBreakMinutes + "m nghỉ ngắn · "
                + config.longBreakMinutes + "m nghỉ dài"
                + (config.autoDnd ? " · Auto DND" : "");
        tvPomodoroSettingSummary.setText(summary);
    }

    private String currentEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && user.getEmail() != null ? user.getEmail() : "focuslife.user@example.com";
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}