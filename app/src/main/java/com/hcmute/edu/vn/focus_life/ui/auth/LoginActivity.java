package com.hcmute.edu.vn.focus_life.ui.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.data.repository.AuthRepository;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;

import java.util.Calendar;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "extra_mode";
    private static final String MODE_LOGIN = "login";
    private static final String MODE_REGISTER = "register";

    private String currentMode = MODE_LOGIN;
    private String selectedAvatarSource = "";

    private AuthViewModel viewModel;
    private AuthRepository repository;

    private MaterialButton tabLogin;
    private MaterialButton tabRegister;
    private MaterialButton btnPrimary;
    private MaterialButton btnGoogleSignIn;
    private MaterialButton btnPickAvatar;

    private TextView tvAuthTitle;
    private TextView tvAuthSubtitle;
    private TextView tvForgotPassword;

    private View avatarSection;
    private ShapeableImageView ivAuthAvatar;
    private TextInputLayout inputDisplayName;
    private TextInputLayout inputPhone;
    private TextInputLayout inputDob;
    private TextInputLayout inputGender;
    private TextInputLayout inputConfirmPassword;

    private TextInputEditText edtDisplayName;
    private TextInputEditText edtPhone;
    private TextInputEditText edtDob;
    private MaterialAutoCompleteTextView edtGender;
    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;
    private TextInputEditText edtConfirmPassword;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    viewModel.handleGoogleResult(repository, result.getData());
                }
            });

    private final ActivityResultLauncher<String[]> avatarPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                selectedAvatarSource = uri.toString();
                loadAvatarPreview(selectedAvatarSource);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        repository = new AuthRepository(this);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        bindViews();
        seedOnboardingData();
        setupDatePicker();
        setupGenderDropdown();

        tabLogin.setOnClickListener(v -> switchMode(MODE_LOGIN));
        tabRegister.setOnClickListener(v -> switchMode(MODE_REGISTER));
        btnPickAvatar.setOnClickListener(v -> avatarPickerLauncher.launch(new String[]{"image/*"}));

        btnPrimary.setOnClickListener(v -> {
            if (MODE_LOGIN.equals(currentMode)) {
                performLogin();
            } else {
                performRegister();
            }
        });

        btnGoogleSignIn.setOnClickListener(v ->
                googleLauncher.launch(repository.getGoogleSignInIntent()));

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Chức năng quên mật khẩu sẽ làm tiếp sau", Toast.LENGTH_SHORT).show());

        String requestedMode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_REGISTER.equals(requestedMode)) {
            switchMode(MODE_REGISTER);
        } else {
            switchMode(MODE_LOGIN);
        }

        viewModel.getAuthState().observe(this, result -> {
            if (result == null) return;

            if (result.isSuccess()) {
                FirebaseUser user = result.getData();
                Toast.makeText(
                        this,
                        MODE_LOGIN.equals(currentMode)
                                ? "Xin chào " + (user != null ? user.getEmail() : "")
                                : "Tạo tài khoản thành công: " + (user != null ? user.getEmail() : ""),
                        Toast.LENGTH_SHORT
                ).show();

                startActivity(new Intent(this, MainActivity.class));
                finishAffinity();
            } else if (result.getError() != null) {
                Toast.makeText(this, result.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindViews() {
        tabLogin = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnPickAvatar = findViewById(R.id.btnPickAuthAvatar);

        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = findViewById(R.id.tvAuthSubtitle);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        avatarSection = findViewById(R.id.authAvatarSection);
        ivAuthAvatar = findViewById(R.id.ivAuthAvatar);
        inputDisplayName = findViewById(R.id.inputDisplayName);
        inputPhone = findViewById(R.id.inputPhone);
        inputDob = findViewById(R.id.inputDob);
        inputGender = findViewById(R.id.inputGender);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);

        edtDisplayName = findViewById(R.id.edtDisplayName);
        edtPhone = findViewById(R.id.edtPhone);
        edtDob = findViewById(R.id.edtDob);
        edtGender = findViewById(R.id.edtGender);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
    }

    private void seedOnboardingData() {
        OnboardingPreferences preferences = new OnboardingPreferences(this);
        edtDisplayName.setText(preferences.getDisplayName());
        selectedAvatarSource = preferences.getBestAvatarSource();
        loadAvatarPreview(selectedAvatarSource);
    }

    private void switchMode(String mode) {
        currentMode = mode;
        clearAllErrors();

        if (MODE_LOGIN.equals(mode)) {
            tvAuthTitle.setText("Đăng nhập");
            tvAuthSubtitle.setText("Chào mừng bạn quay lại FocusLife");
            btnPrimary.setText("Đăng nhập");

            avatarSection.setVisibility(View.GONE);
            inputDisplayName.setVisibility(View.GONE);
            inputPhone.setVisibility(View.GONE);
            inputDob.setVisibility(View.GONE);
            inputGender.setVisibility(View.GONE);
            inputConfirmPassword.setVisibility(View.GONE);
            tvForgotPassword.setVisibility(View.VISIBLE);

            selectTab(tabLogin, tabRegister);
        } else {
            tvAuthTitle.setText("Đăng ký");
            tvAuthSubtitle.setText("Hoàn thiện tên và ảnh đại diện để lưu thẳng lên hồ sơ của bạn");
            btnPrimary.setText("Tạo tài khoản");

            avatarSection.setVisibility(View.VISIBLE);
            inputDisplayName.setVisibility(View.VISIBLE);
            inputPhone.setVisibility(View.VISIBLE);
            inputDob.setVisibility(View.VISIBLE);
            inputGender.setVisibility(View.VISIBLE);
            inputConfirmPassword.setVisibility(View.VISIBLE);
            tvForgotPassword.setVisibility(View.GONE);

            selectTab(tabRegister, tabLogin);
        }
    }

    private void performLogin() {
        String email = valueOf(edtEmail);
        String password = valueOf(edtPassword);

        if (email.isEmpty()) {
            edtEmail.setError("Nhập email");
            return;
        }

        if (password.isEmpty()) {
            edtPassword.setError("Nhập mật khẩu");
            return;
        }

        viewModel.login(repository, email, password);
    }

    private void performRegister() {
        String displayName = valueOf(edtDisplayName);
        String phone = valueOf(edtPhone);
        String dob = valueOf(edtDob);
        String gender = valueOf(edtGender);
        String email = valueOf(edtEmail);
        String password = valueOf(edtPassword);
        String confirmPassword = valueOf(edtConfirmPassword);

        if (displayName.isEmpty()) {
            edtDisplayName.setError("Nhập họ tên");
            return;
        }

        if (phone.isEmpty()) {
            edtPhone.setError("Nhập số điện thoại");
            return;
        }

        if (dob.isEmpty()) {
            edtDob.setError("Chọn ngày sinh");
            return;
        }

        if (gender.isEmpty()) {
            edtGender.setError("Chọn giới tính");
            return;
        }

        if (email.isEmpty()) {
            edtEmail.setError("Nhập email");
            return;
        }

        if (password.isEmpty()) {
            edtPassword.setError("Nhập mật khẩu");
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        OnboardingPreferences preferences = new OnboardingPreferences(this);
        preferences.setDisplayName(displayName);
        preferences.setPendingAvatarUri(selectedAvatarSource);

        UserProfile profile = new UserProfile();
        profile.displayName = displayName;
        profile.phone = phone;
        profile.dateOfBirth = dob;
        profile.gender = gender;
        profile.avatarUrl = selectedAvatarSource;
        profile.primaryGoal = preferences.getPrimaryGoal();
        profile.createdAt = System.currentTimeMillis();
        profile.updatedAt = System.currentTimeMillis();

        viewModel.register(repository, email, password, profile);
    }

    private void setupDatePicker() {
        edtDob.setFocusable(false);
        edtDob.setClickable(true);
        edtDob.setCursorVisible(false);
        edtDob.setKeyListener(null);

        edtDob.setOnClickListener(v -> showDatePicker());

        inputDob.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        inputDob.setEndIconDrawable(android.R.drawable.ic_menu_my_calendar);
        inputDob.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR) - 18;
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String current = valueOf(edtDob);
        if (!current.isEmpty() && current.matches("\\d{2}/\\d{2}/\\d{4}")) {
            try {
                String[] parts = current.split("/");
                day = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1;
                year = Integer.parseInt(parts[2]);
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formatted = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                    );
                    edtDob.setText(formatted);
                    edtDob.setError(null);
                },
                year,
                month,
                day
        );

        Calendar maxDate = Calendar.getInstance();
        dialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        Calendar minDate = Calendar.getInstance();
        minDate.set(1950, Calendar.JANUARY, 1);
        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        dialog.show();
    }

    private void setupGenderDropdown() {
        String[] genderOptions = new String[]{"Nam", "Nữ", "Khác"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                genderOptions
        );

        edtGender.setAdapter(adapter);
        edtGender.setOnClickListener(v -> edtGender.showDropDown());
        edtGender.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) edtGender.showDropDown();
        });
    }

    private void clearAllErrors() {
        edtDisplayName.setError(null);
        edtPhone.setError(null);
        edtDob.setError(null);
        edtGender.setError(null);
        edtEmail.setError(null);
        edtPassword.setError(null);
        edtConfirmPassword.setError(null);
    }

    private void selectTab(MaterialButton selected, MaterialButton unselected) {
        selected.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.primary)));
        selected.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        unselected.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.surface_container_low)));
        unselected.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
    }

    private String valueOf(TextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString().trim();
    }

    private void loadAvatarPreview(String source) {
        if (source == null || source.trim().isEmpty()) {
            ivAuthAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            return;
        }
        Glide.with(this)
                .load(Uri.parse(source))
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(ivAuthAvatar);
    }
}
