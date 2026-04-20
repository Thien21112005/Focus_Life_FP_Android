package com.hcmute.edu.vn.focus_life.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.utils.PermissionManager;

import java.util.Map;

public class PermissionSlideFragment extends Fragment {
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_onboarding_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button btnAllowAll = view.findViewById(R.id.btnAllowAll);
        Button btnSkip = view.findViewById(R.id.btnSkip);

        btnAllowAll.setOnClickListener(v -> PermissionManager.requestOnboardingPermissions(permissionLauncher));
        btnSkip.setOnClickListener(v -> Toast.makeText(requireContext(), "Ban co the cap quyen sau trong cai dat", Toast.LENGTH_SHORT).show());
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        boolean allGranted = true;
        for (Boolean granted : result.values()) {
            if (!Boolean.TRUE.equals(granted)) {
                allGranted = false;
                break;
            }
        }
        Toast.makeText(requireContext(), allGranted ? "Da cap quyen" : "Con thieu mot so quyen", Toast.LENGTH_SHORT).show();
    }
}
