package com.hcmute.edu.vn.focus_life.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        OnboardingPreferences onboardingPreferences = new OnboardingPreferences(requireContext());

        TextView tvBrand = view.findViewById(R.id.tvHomeBrand);
        TextView tvGreeting = view.findViewById(R.id.tvHomeGreeting);
        EditText etTaskName = view.findViewById(R.id.etTaskName);
        MaterialButton btnStartFocus = view.findViewById(R.id.btnStartFocus);

        String displayName = onboardingPreferences.getDisplayName();
        if (tvBrand != null) {
            tvBrand.setText("FocusLife");
        }
        if (tvGreeting != null) {
            tvGreeting.setText("Chào buổi sáng, " + displayName + " 👋");
        }
        if (etTaskName != null && etTaskName.getText().toString().trim().isEmpty()) {
            etTaskName.setText("Deep Work cho " + displayName);
        }

        btnStartFocus.setOnClickListener(v -> {
            String task = etTaskName.getText().toString().trim();
            if (task.isEmpty()) task = "Phiên Focus";
            Toast.makeText(requireContext(), "Bắt đầu: " + task, Toast.LENGTH_SHORT).show();
        });
    }
}
