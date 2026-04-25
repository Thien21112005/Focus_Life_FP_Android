package com.hcmute.edu.vn.focus_life.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.hcmute.edu.vn.focus_life.ui.focus.FocusCategoryManager;
import com.hcmute.edu.vn.focus_life.ui.focus.PomodoroPreferences;
import com.hcmute.edu.vn.focus_life.ui.focus.PomodoroSettingsActivity;

import java.util.List;

public class ProfileFragment extends Fragment {
    private ProfileRepository profileRepository;
    private FocusCategoryManager categoryManager;
    private ImageView imgProfileAvatar;
    private TextView tvProfileInitial;
    private TextView tvTopBarInitial;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvPomodoroSettingSummary;
    private TextView tvCategorySettingSummary;

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
        categoryManager = new FocusCategoryManager(requireContext());

        imgProfileAvatar = view.findViewById(R.id.imgProfileAvatar);
        tvProfileInitial = view.findViewById(R.id.tvProfileInitial);
        tvTopBarInitial = view.findViewById(R.id.tvTopBarInitial);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvPomodoroSettingSummary = view.findViewById(R.id.tvPomodoroSettingSummary);
        tvCategorySettingSummary = view.findViewById(R.id.tvCategorySettingSummary);
        TextView btnLogout = view.findViewById(R.id.btnLogout);
        View rowPomodoroSettings = view.findViewById(R.id.rowPomodoroSettings);
        View rowCategorySettings = view.findViewById(R.id.rowCategorySettings);

        rowPomodoroSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), PomodoroSettingsActivity.class)));
        rowCategorySettings.setOnClickListener(v -> showCategoryManagerDialog());

        btnLogout.setOnClickListener(v -> {
            new AuthRepository(requireActivity()).logout();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finishAffinity();
        });

        loadProfile();
        bindPomodoroSummary();
        bindCategorySummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        bindPomodoroSummary();
        bindCategorySummary();
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

    private void bindCategorySummary() {
        if (categoryManager == null || tvCategorySettingSummary == null) return;
        List<String> categories = categoryManager.getCategories();
        StringBuilder builder = new StringBuilder();
        int visibleCount = Math.min(3, categories.size());
        for (int i = 0; i < visibleCount; i++) {
            if (i > 0) builder.append(", ");
            builder.append(categories.get(i));
        }
        if (categories.size() > visibleCount) {
            builder.append(" +").append(categories.size() - visibleCount).append(" mục khác");
        }
        tvCategorySettingSummary.setText(builder.toString());
    }

    private void showCategoryManagerDialog() {
        List<String> categories = categoryManager.getCategories();
        String[] items = categories.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Các Category")
                .setItems(items, (dialog, which) -> showCategoryActionDialog(categories.get(which)))
                .setPositiveButton("+ Thêm", (dialog, which) -> showCategoryInputDialog(null))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showCategoryActionDialog(@NonNull String category) {
        String[] actions = {"Đổi tên", "Xóa"};
        new AlertDialog.Builder(requireContext())
                .setTitle(category)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showCategoryInputDialog(category);
                    } else {
                        showDeleteCategoryDialog(category);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCategoryInputDialog(@Nullable String oldCategory) {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setHint("Ví dụ: Android, Reading, Fitness");
        if (oldCategory != null) {
            input.setText(oldCategory);
            input.setSelection(oldCategory.length());
        }

        String title = oldCategory == null ? "Thêm category" : "Đổi tên category";
        String positive = oldCategory == null ? "Thêm" : "Lưu";

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(positive, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (TextUtils.isEmpty(value)) {
                input.setError("Nhập tên category");
                return;
            }

            if (oldCategory == null) {
                categoryManager.addCategory(value);
                Toast.makeText(requireContext(), "Đã thêm category", Toast.LENGTH_SHORT).show();
            } else {
                categoryManager.renameCategory(oldCategory, value);
                Toast.makeText(requireContext(), "Đã đổi tên category", Toast.LENGTH_SHORT).show();
            }
            bindCategorySummary();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void showDeleteCategoryDialog(@NonNull String category) {
        if (categoryManager.getCategories().size() <= 1) {
            Toast.makeText(requireContext(), "Cần giữ lại ít nhất 1 category", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa category?")
                .setMessage("Category \"" + category + "\" sẽ không còn xuất hiện trong dropdown tạo task mới.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    categoryManager.deleteCategory(category);
                    bindCategorySummary();
                    Toast.makeText(requireContext(), "Đã xóa category", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String currentEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && user.getEmail() != null ? user.getEmail() : "focuslife.user@example.com";
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
