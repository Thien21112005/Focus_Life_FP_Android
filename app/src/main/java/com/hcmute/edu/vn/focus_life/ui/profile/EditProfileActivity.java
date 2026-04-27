package com.hcmute.edu.vn.focus_life.ui.profile;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.exception.AppExceptionLogger;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.core.session.SettingsPreferences;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.data.repository.ProfileRepository;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;

import java.util.Calendar;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {
    private ProfileRepository profileRepository;
    private SettingsPreferences settingsPreferences;
    private UserProfile currentProfile;

    private Button btnSaveProfile;
    private EditText edtName;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtDob;
    private Spinner spinnerGender;
    private EditText edtHeight;
    private EditText edtWeight;
    private EditText edtGoal;
    private FrameLayout frameAvatarPicker;
    private ImageView imgEditAvatar;
    private TextView tvEditAvatarInitial;
    private TextView tvAvatarAction;
    private TextView tvAvatarHint;

    private String[] genderValues;

    private boolean nameLockedByGoogle;
    private boolean emailLockedByGoogle;
    private boolean avatarLockedByGoogle;
    private String lockedGoogleName = "";
    private String lockedGoogleEmail = "";
    private Uri selectedAvatarUri;

    private final ActivityResultLauncher<String> avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) return;
                selectedAvatarUri = uri;
                showAvatar(uri, safe(valueOf(edtName), "FocusLife"));
                tvAvatarAction.setText("Đã chọn ảnh mới");
                tvAvatarHint.setText("Ảnh sẽ được lưu khi bạn bấm Lưu thay đổi.");
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileRepository = new ProfileRepository(this);
        settingsPreferences = new SettingsPreferences(this);
        bindViews();
        setupGenderDropdown();
        setupActions();
        applyAccent();
        loadProfile();
    }

    private void bindViews() {
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtDob = findViewById(R.id.edtDob);
        spinnerGender = findViewById(R.id.spinnerGender);
        edtHeight = findViewById(R.id.edtHeight);
        edtWeight = findViewById(R.id.edtWeight);
        edtGoal = findViewById(R.id.edtGoal);
        frameAvatarPicker = findViewById(R.id.frameAvatarPicker);
        imgEditAvatar = findViewById(R.id.imgEditAvatar);
        tvEditAvatarInitial = findViewById(R.id.tvEditAvatarInitial);
        tvAvatarAction = findViewById(R.id.tvAvatarAction);
        tvAvatarHint = findViewById(R.id.tvAvatarHint);
    }

    private void setupGenderDropdown() {
        genderValues = new String[]{
                getString(R.string.gender_male),
                getString(R.string.gender_female),
                getString(R.string.gender_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        edtPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        edtDob.setOnClickListener(v -> showDatePicker());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        frameAvatarPicker.setOnClickListener(v -> chooseAvatar());
    }

    private void loadProfile() {
        profileRepository.getCurrentProfile(profile -> {
            currentProfile = profile == null ? new UserProfile() : profile;
            bindProfile(currentProfile);
        });
    }

    private void bindProfile(UserProfile profile) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean googleUser = isGoogleUser(firebaseUser, profile);
        String firebaseName = firebaseUser != null ? safeNullable(firebaseUser.getDisplayName()) : "";
        String firebaseEmail = firebaseUser != null ? safeNullable(firebaseUser.getEmail()) : "";
        String firebasePhoto = firebaseUser != null && firebaseUser.getPhotoUrl() != null
                ? firebaseUser.getPhotoUrl().toString()
                : "";

        nameLockedByGoogle = googleUser && !firebaseName.isEmpty();
        emailLockedByGoogle = googleUser && !firebaseEmail.isEmpty();
        avatarLockedByGoogle = googleUser && !firebasePhoto.isEmpty();
        lockedGoogleName = nameLockedByGoogle ? firebaseName : "";
        lockedGoogleEmail = emailLockedByGoogle ? firebaseEmail : "";

        String displayName = nameLockedByGoogle
                ? lockedGoogleName
                : safe(profile.displayName, new OnboardingPreferences(this).getDisplayName());
        String email = emailLockedByGoogle
                ? lockedGoogleEmail
                : safe(profile.email, firebaseEmail);

        edtName.setText(displayName);
        edtEmail.setText(email);
        applyGoogleFieldLock(edtName, nameLockedByGoogle);
        applyGoogleFieldLock(edtEmail, emailLockedByGoogle);

        String avatarUrl = avatarLockedByGoogle ? firebasePhoto : safe(profile.avatarUrl, Constants.DEFAULT_APP_AVATAR_URL);
        showAvatar(avatarUrl, displayName);
        bindAvatarLockState();

        edtPhone.setText(safe(profile.phone, ""));
        edtDob.setText(safe(profile.dateOfBirth, ""));
        setGenderSelection(safe(profile.gender, getString(R.string.gender_male)));
        edtHeight.setText(formatNumber(profile.heightCm));
        edtWeight.setText(formatNumber(profile.weightKg));
        edtGoal.setText(safe(profile.primaryGoal, new OnboardingPreferences(this).getPrimaryGoal()));
    }

    private void chooseAvatar() {
        if (avatarLockedByGoogle) {
            Toast.makeText(this, "Ảnh đại diện đang đồng bộ từ Google nên không thể đổi trong app.", Toast.LENGTH_LONG).show();
            return;
        }
        avatarPickerLauncher.launch("image/*");
    }

    private void bindAvatarLockState() {
        frameAvatarPicker.setEnabled(!avatarLockedByGoogle);
        frameAvatarPicker.setAlpha(1f);
        if (avatarLockedByGoogle) {
            tvAvatarAction.setText("Ảnh đại diện từ Google");
            tvAvatarHint.setText("Ảnh này được đồng bộ từ tài khoản Google và không thể chỉnh sửa trong FocusLife.");
        } else {
            tvAvatarAction.setText("Thay ảnh đại diện");
            tvAvatarHint.setText("Chạm vào ảnh để chọn ảnh mới từ thư viện.");
        }
    }

    private void showAvatar(Uri uri, String displayName) {
        imgEditAvatar.setVisibility(View.VISIBLE);
        tvEditAvatarInitial.setVisibility(View.GONE);
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(R.drawable.bg_circle_primary)
                .error(R.drawable.bg_circle_primary)
                .into(imgEditAvatar);
    }

    private void showAvatar(String avatarUrl, String displayName) {
        String initial = safe(displayName, "F").substring(0, 1).toUpperCase(Locale.getDefault());
        tvEditAvatarInitial.setText(initial);
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            imgEditAvatar.setVisibility(View.GONE);
            tvEditAvatarInitial.setVisibility(View.VISIBLE);
            return;
        }
        imgEditAvatar.setVisibility(View.VISIBLE);
        tvEditAvatarInitial.setVisibility(View.GONE);
        Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.bg_circle_primary)
                .error(R.drawable.bg_circle_primary)
                .into(imgEditAvatar);
    }

    private void saveProfile() {
        if (currentProfile == null) {
            currentProfile = new UserProfile();
        }

        String name = nameLockedByGoogle ? lockedGoogleName : valueOf(edtName);
        String email = emailLockedByGoogle ? lockedGoogleEmail : valueOf(edtEmail);
        String phone = valueOf(edtPhone);
        if (!emailLockedByGoogle && !email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (!phone.isEmpty() && phone.length() != 10) {
            edtPhone.setError(getString(R.string.error_phone_10_digits));
            return;
        }

        UserProfile edited = copyProfile(currentProfile);
        edited.displayName = name;
        edited.email = email;
        edited.phone = phone;
        edited.dateOfBirth = valueOf(edtDob);
        edited.gender = spinnerGender.getSelectedItem() == null ? "" : spinnerGender.getSelectedItem().toString();
        edited.primaryGoal = valueOf(edtGoal);
        if (selectedAvatarUri != null && !avatarLockedByGoogle) {
            edited.pendingAvatarUri = selectedAvatarUri.toString();
        }

        Float height = parseOptionalFloat(edtHeight, getString(R.string.profile_height));
        if (height == null) return;
        Float weight = parseOptionalFloat(edtWeight, getString(R.string.profile_weight));
        if (weight == null) return;
        edited.heightCm = height;
        edited.weightKg = weight;

        setSaving(true);
        profileRepository.updateProfile(edited, (success, message, updatedProfile) -> {
            setSaving(false);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (success) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void setSaving(boolean saving) {
        btnSaveProfile.setEnabled(!saving);
        btnSaveProfile.setAlpha(saving ? 0.65f : 1f);
        btnSaveProfile.setText(saving ? "Đang lưu..." : getString(R.string.save_changes));
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        String current = valueOf(edtDob);
        try {
            String[] parts = current.split("/");
            if (parts.length == 3) {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                calendar.set(Calendar.YEAR, Integer.parseInt(parts[2]));
            }
        } catch (Exception ignored) {
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> edtDob.setText(String.format(
                        Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void applyAccent() {
        btnSaveProfile.setBackgroundTintList(ColorStateList.valueOf(settingsPreferences.getAccentColor(this)));
    }

    private void applyGoogleFieldLock(EditText editText, boolean locked) {
        editText.setEnabled(!locked);
        editText.setFocusable(!locked);
        editText.setFocusableInTouchMode(!locked);
        editText.setCursorVisible(!locked);
        editText.setLongClickable(!locked);
        editText.setTextColor(getResources().getColor(R.color.on_surface));
        editText.setHintTextColor(getResources().getColor(R.color.on_surface_variant));
        editText.setAlpha(1f);
    }

    private boolean isGoogleUser(FirebaseUser user, UserProfile profile) {
        if (profile != null && UserProfile.PROVIDER_GOOGLE.equals(profile.authProvider)) {
            return true;
        }
        if (user != null && user.getProviderData() != null) {
            for (UserInfo info : user.getProviderData()) {
                if ("google.com".equals(info.getProviderId())) {
                    return true;
                }
            }
        }
        return false;
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

    private void setGenderSelection(String gender) {
        if (gender == null) return;
        for (int i = 0; i < genderValues.length; i++) {
            if (genderValues[i].equalsIgnoreCase(gender.trim())) {
                spinnerGender.setSelection(i);
                return;
            }
        }
        if (gender.toLowerCase(Locale.getDefault()).contains("nữ")
                || gender.toLowerCase(Locale.getDefault()).contains("female")) {
            spinnerGender.setSelection(1);
        } else if (gender.toLowerCase(Locale.getDefault()).contains("khác")
                || gender.toLowerCase(Locale.getDefault()).contains("other")) {
            spinnerGender.setSelection(2);
        } else {
            spinnerGender.setSelection(0);
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

    private String valueOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String formatNumber(float value) {
        if (value <= 0f) return "";
        if (value == (long) value) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String safeNullable(String value) {
        return value == null ? "" : value.trim();
    }
}
