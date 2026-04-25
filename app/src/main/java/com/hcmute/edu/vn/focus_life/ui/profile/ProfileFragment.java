package com.hcmute.edu.vn.focus_life.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.exception.AppExceptionLogger;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.session.SettingsPreferences;
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
import java.util.Locale;

public class ProfileFragment extends Fragment {
    private ProfileRepository profileRepository;
    private SettingsPreferences settingsPreferences;

    private ImageView imgProfileAvatar;
    private TextView tvProfileInitial;
    private TextView tvTopBarInitial;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProviderBadge;
    private TextView tvProfileInfoSummary;
    private TextView tvPomodoroSettingSummary;
    private TextView tvCategorySettingSummary;
    private TextView tvThemeSummary;
    private TextView tvLanguageSummary;
    private SwitchCompat switchDarkMode;
    private View rowEditProfile;
    private View rowPassword;
    private View rowDarkMode;
    private View rowTheme;
    private View rowLanguage;
    private View rowAboutFocusLife;
    private View rowDeleteAccount;

    private UserProfile currentProfile;
    private boolean syncingSwitch;

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
        settingsPreferences = new SettingsPreferences(requireContext());

        bindViews(view);
        setupActions();
        bindAppearance();
        loadProfile();
        bindPomodoroSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        bindPomodoroSummary();
        bindAppearance();
    }

    private void bindViews(View view) {
        imgProfileAvatar = view.findViewById(R.id.imgProfileAvatar);
        tvProfileInitial = view.findViewById(R.id.tvProfileInitial);
        tvTopBarInitial = view.findViewById(R.id.tvTopBarInitial);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProviderBadge = view.findViewById(R.id.tvProviderBadge);
        tvProfileInfoSummary = view.findViewById(R.id.tvProfileInfoSummary);
        tvPomodoroSettingSummary = view.findViewById(R.id.tvPomodoroSettingSummary);
        tvCategorySettingSummary = view.findViewById(R.id.tvCategorySettingSummary);
        TextView btnLogout = view.findViewById(R.id.btnLogout);
        View rowPomodoroSettings = view.findViewById(R.id.rowPomodoroSettings);
        View rowCategorySettings = view.findViewById(R.id.rowCategorySettings);
        tvThemeSummary = view.findViewById(R.id.tvThemeSummary);
        tvLanguageSummary = view.findViewById(R.id.tvLanguageSummary);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        rowEditProfile = view.findViewById(R.id.rowEditProfile);
        rowPassword = view.findViewById(R.id.rowPassword);
        rowDarkMode = view.findViewById(R.id.rowDarkMode);
        rowTheme = view.findViewById(R.id.rowTheme);
        rowLanguage = view.findViewById(R.id.rowLanguage);
        rowAboutFocusLife = view.findViewById(R.id.rowAboutFocusLife);
        rowDeleteAccount = view.findViewById(R.id.rowDeleteAccount);
    }

    private void setupActions() {
        View rowPomodoroSettings = requireView().findViewById(R.id.rowPomodoroSettings);
        TextView btnLogout = requireView().findViewById(R.id.btnLogout);

        rowEditProfile.setOnClickListener(v -> startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        rowPassword.setOnClickListener(v -> startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));
        rowPomodoroSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), PomodoroSettingsActivity.class)));
        rowCategorySettings.setOnClickListener(v -> showCategoryManagerDialog());
        rowTheme.setOnClickListener(v -> showThemeDialog());
        rowLanguage.setOnClickListener(v -> showLanguageDialog());
        rowAboutFocusLife.setOnClickListener(v -> startActivity(new Intent(requireContext(), AboutFocusLifeActivity.class)));
        rowDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        rowDarkMode.setOnClickListener(v -> switchDarkMode.setChecked(!switchDarkMode.isChecked()));

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (syncingSwitch) return;
            settingsPreferences.setDarkModeEnabled(isChecked);
        });

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
            currentProfile = profile;
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
        tvProviderBadge.setText(UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider)
                ? getString(R.string.provider_google)
                : getString(R.string.provider_password));
        tvProfileInfoSummary.setText(buildProfileSummary(profile));

        boolean googleUser = UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider);
        rowPassword.setVisibility(googleUser ? View.GONE : View.VISIBLE);
        rowEditProfile.setAlpha(googleUser ? 0.72f : 1f);

        String initial = displayName.isEmpty() ? "F" : displayName.substring(0, 1).toUpperCase(Locale.getDefault());
        tvProfileInitial.setText(initial);
        tvTopBarInitial.setText(initial);
        applyThemeAccent();

        if (!avatarUrl.isEmpty()) {
            imgProfileAvatar.setVisibility(View.VISIBLE);
            tvProfileInitial.setVisibility(View.GONE);
            Glide.with(this).load(avatarUrl).circleCrop().into(imgProfileAvatar);
        } else {
            imgProfileAvatar.setVisibility(View.GONE);
            tvProfileInitial.setVisibility(View.VISIBLE);
        }
    }

    private String buildProfileSummary(UserProfile profile) {
        if (UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider)) {
            return getString(R.string.profile_google_basic_info);
        }

        String goal = safe(profile.primaryGoal, new OnboardingPreferences(requireContext()).getPrimaryGoal());
        String phone = safe(profile.phone, getString(R.string.profile_no_phone));
        return getString(R.string.profile_summary_format, phone, goal);
    }

    private void bindPomodoroSummary() {
        if (!isAdded() || tvPomodoroSettingSummary == null) return;
        PomodoroPreferences.Config config = new PomodoroPreferences(requireContext()).getConfig();
        String summary = getString(
                R.string.pomodoro_summary_format,
                config.focusMinutes,
                config.shortBreakMinutes,
                config.longBreakMinutes
        );
        if (config.autoDnd) {
            summary += getString(R.string.pomodoro_summary_dnd_suffix);
        }
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
    private void bindAppearance() {
        if (settingsPreferences == null || switchDarkMode == null) return;
        syncingSwitch = true;
        switchDarkMode.setChecked(settingsPreferences.isDarkModeEnabled());
        syncingSwitch = false;
        tvThemeSummary.setText(settingsPreferences.getThemeDisplayName(requireContext()));
        tvLanguageSummary.setText(settingsPreferences.getLanguageDisplayName(requireContext()));
        applyThemeAccent();
    }

    private void showEditProfileDialog() {
        UserProfile source = currentProfile == null ? new UserProfile() : currentProfile;
        UserProfile edited = copyProfile(source);

        LinearLayout form = createDialogForm();
        EditText edtName = addField(form, getString(R.string.profile_name), safe(edited.displayName, ""), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        EditText edtPhone = addField(form, getString(R.string.profile_phone), safe(edited.phone, ""), InputType.TYPE_CLASS_PHONE);
        EditText edtDob = addField(form, getString(R.string.profile_dob), safe(edited.dateOfBirth, ""), InputType.TYPE_CLASS_TEXT);
        EditText edtGender = addField(form, getString(R.string.profile_gender), safe(edited.gender, ""), InputType.TYPE_CLASS_TEXT);
        EditText edtHeight = addField(form, getString(R.string.profile_height), formatNumber(edited.heightCm), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText edtWeight = addField(form, getString(R.string.profile_weight), formatNumber(edited.weightKg), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText edtGoal = addField(form, getString(R.string.profile_primary_goal), safe(edited.primaryGoal, new OnboardingPreferences(requireContext()).getPrimaryGoal()), InputType.TYPE_CLASS_TEXT);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_profile_title)
                .setView(wrapInScroll(form))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            edited.displayName = valueOf(edtName);
            edited.phone = valueOf(edtPhone);
            edited.dateOfBirth = valueOf(edtDob);
            edited.gender = valueOf(edtGender);
            edited.primaryGoal = valueOf(edtGoal);

            Float height = parseOptionalFloat(edtHeight, getString(R.string.profile_height));
            if (height == null) return;
            Float weight = parseOptionalFloat(edtWeight, getString(R.string.profile_weight));
            if (weight == null) return;
            edited.heightCm = height;
            edited.weightKg = weight;

            profileRepository.updateProfile(edited, (success, message, updatedProfile) -> {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                if (success) {
                    currentProfile = updatedProfile;
                    if (updatedProfile != null) bindProfile(updatedProfile);
                    dialog.dismiss();
                }
            });
        }));

        dialog.show();
    }

    private void showPasswordDialog() {
        LinearLayout form = createDialogForm();
        EditText edtOld = addField(form, getString(R.string.old_password), "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText edtNew = addField(form, getString(R.string.new_password), "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText edtConfirm = addField(form, getString(R.string.confirm_new_password), "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_password_title)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.update, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                profileRepository.changePassword(valueOf(edtOld), valueOf(edtNew), valueOf(edtConfirm), (success, message) -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    if (success) dialog.dismiss();
                })));

        dialog.show();
    }

    private void showThemeDialog() {
        String[] keys = new String[]{
                SettingsPreferences.THEME_INDIGO,
                SettingsPreferences.THEME_EMERALD,
                SettingsPreferences.THEME_OCEAN,
                SettingsPreferences.THEME_ROSE,
                SettingsPreferences.THEME_AMBER
        };
        String[] labels = new String[]{
                getString(R.string.theme_indigo),
                getString(R.string.theme_emerald),
                getString(R.string.theme_ocean),
                getString(R.string.theme_rose),
                getString(R.string.theme_amber)
        };

        int checked = 0;
        String currentKey = settingsPreferences.getThemeKey();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(currentKey)) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.theme_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    settingsPreferences.setThemeKey(keys[which]);
                    bindAppearance();
                    Toast.makeText(requireContext(), getString(R.string.theme_updated), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showLanguageDialog() {
        String[] keys = new String[]{SettingsPreferences.LANGUAGE_VI, SettingsPreferences.LANGUAGE_EN};
        String[] labels = new String[]{getString(R.string.language_vietnamese), getString(R.string.language_english)};
        int checked = SettingsPreferences.LANGUAGE_EN.equals(settingsPreferences.getLanguage()) ? 1 : 0;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.language_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    settingsPreferences.setLanguage(keys[which]);
                    Toast.makeText(requireContext(), getString(R.string.language_updated), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteAccountDialog() {
        boolean google = isGoogleProfile();
        LinearLayout form = createDialogForm();
        TextView warning = new TextView(requireContext());
        warning.setText(getString(R.string.delete_account_warning_message));
        warning.setTextColor(requireContext().getColor(R.color.on_surface_variant));
        warning.setLineSpacing(2f, 1.05f);
        form.addView(warning);

        EditText edtPassword = null;
        if (!google) {
            edtPassword = addField(form, getString(R.string.current_password), "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        EditText finalPasswordField = edtPassword;
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_account_title)
                .setView(form)
                .setNegativeButton(R.string.keep_account, null)
                .setPositiveButton(R.string.delete_account_action, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(requireContext().getColor(R.color.error));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = finalPasswordField == null ? null : valueOf(finalPasswordField);
                if (!google && (password == null || password.isEmpty())) {
                    finalPasswordField.setError(getString(R.string.error_old_password_required));
                    return;
                }
                profileRepository.deleteAccount(password, (success, message) -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    if (success) {
                        dialog.dismiss();
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        requireActivity().finishAffinity();
                    }
                });
            });
        });

        dialog.show();
    }

    private void showAboutFocusLifeDialog() {
        LinearLayout content = createDialogForm();
        content.setPadding(dp(22), dp(12), dp(22), dp(8));

        TextView body = new TextView(requireContext());
        body.setText(getString(R.string.about_focuslife_body));
        body.setTextColor(requireContext().getColor(R.color.on_surface_variant));
        body.setTextSize(15f);
        body.setLineSpacing(4f, 1.08f);
        content.addView(body);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.about_focuslife_dialog_title)
                .setView(wrapInScroll(content))
                .setPositiveButton(R.string.close, null)
                .create();
        dialog.show();
    }

    private LinearLayout createDialogForm() {
        LinearLayout form = new LinearLayout(requireContext());
        int padding = dp(20);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(padding, dp(10), padding, dp(4));
        return form;
    }

    private ScrollView wrapInScroll(View child) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(child);
        return scrollView;
    }

    private EditText addField(LinearLayout parent, String hint, String value, int inputType) {
        EditText editText = new EditText(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, dp(8));
        editText.setLayoutParams(params);
        editText.setHint(hint);
        editText.setText(value);
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        parent.addView(editText);
        return editText;
    }

    private void applyThemeAccent() {
        if (!isAdded() || settingsPreferences == null) return;
        int accent = settingsPreferences.getAccentColor(requireContext());
        int accentSoft = settingsPreferences.getAccentContainerColor(requireContext());

        tvProfileInitial.setBackground(circle(accent));
        tvTopBarInitial.setBackground(circle(accent));
        tvProviderBadge.setBackground(roundRect(accentSoft, dp(999)));
        tvProviderBadge.setTextColor(accent);

        if (switchDarkMode != null) {
            switchDarkMode.setThumbTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.WHITE, requireContext().getColor(R.color.surface_container_lowest)}
            ));
            switchDarkMode.setTrackTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{accent, requireContext().getColor(R.color.surface_container_highest)}
            ));
        }
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private Float parseOptionalFloat(EditText editText, String label) {
        String raw = valueOf(editText);
        if (raw.isEmpty()) return 0f;
        try {
            float value = Float.parseFloat(raw);
            if (value < 0f) throw new NumberFormatException("negative");
            return value;
        } catch (Exception e) {
            AppExceptionLogger.log("profile_parse_float", e instanceof Exception ? (Exception) e : new Exception(e));
            editText.setError(getString(R.string.error_invalid_number_format, label));
            return null;
        }
    }

    private UserProfile copyProfile(UserProfile source) {
        UserProfile profile = new UserProfile();
        profile.uid = source.uid;
        profile.displayName = source.displayName;
        profile.email = source.email;
        profile.phone = source.phone;
        profile.dateOfBirth = source.dateOfBirth;
        profile.gender = source.gender;
        profile.heightCm = source.heightCm;
        profile.weightKg = source.weightKg;
        profile.avatarUrl = source.avatarUrl;
        profile.primaryGoal = source.primaryGoal;
        profile.authProvider = source.authProvider;
        profile.createdAt = source.createdAt;
        profile.updatedAt = source.updatedAt;
        return profile;
    }

    private boolean isGoogleProfile() {
        return currentProfile != null && UserProfile.PROVIDER_GOOGLE.equals(currentProfile.authProvider);
    }

    private String formatNumber(float value) {
        if (value <= 0f) return "";
        if (value == (long) value) {
            return String.format(Locale.getDefault(), "%d", (long) value);
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private String currentEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && user.getEmail() != null ? user.getEmail() : "focuslife.user@example.com";
    }

    private String valueOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
