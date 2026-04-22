package com.hcmute.edu.vn.focus_life.ui.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
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

    private AuthViewModel viewModel;
    private AuthRepository repository;

    private MaterialButton tabLogin;
    private MaterialButton tabRegister;
    private MaterialButton btnPrimary;
    private MaterialButton btnGoogleSignIn;

    private TextView tvAuthTitle;
    private TextView tvAuthSubtitle;
    private TextView tvForgotPassword;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        repository = new AuthRepository(this);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        bindViews();
        setupDatePicker();
        setupGenderDropdown();

        tabLogin.setOnClickListener(v -> switchMode(MODE_LOGIN));
        tabRegister.setOnClickListener(v -> switchMode(MODE_REGISTER));

        btnPrimary.setOnClickListener(v -> {
            if (MODE_LOGIN.equals(currentMode)) {
                performLogin();
            } else {
                performRegister();
            }
        });

        btnGoogleSignIn.setOnClickListener(v -> googleLauncher.launch(repository.getGoogleSignInIntent()));

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

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finishAffinity();
            } else if (result.getError() != null) {
                showUserFriendlyError(result.getError().getMessage());
            }
        });
    }

    private void bindViews() {
        tabLogin = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = findViewById(R.id.tvAuthSubtitle);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

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

    private void switchMode(String mode) {
        currentMode = mode;
        clearAllErrors();

        if (MODE_LOGIN.equals(mode)) {
            tvAuthTitle.setText("Đăng nhập");
            tvAuthSubtitle.setText("Chào mừng bạn quay lại FocusLife");
            btnPrimary.setText("Đăng nhập");

            inputDisplayName.setVisibility(android.view.View.GONE);
            inputPhone.setVisibility(android.view.View.GONE);
            inputDob.setVisibility(android.view.View.GONE);
            inputGender.setVisibility(android.view.View.GONE);
            inputConfirmPassword.setVisibility(android.view.View.GONE);
            tvForgotPassword.setVisibility(android.view.View.VISIBLE);

            selectTab(tabLogin, tabRegister);
        } else {
            tvAuthTitle.setText("Đăng ký");
            tvAuthSubtitle.setText("Tạo tài khoản FocusLife nhanh gọn, avatar mặc định sẽ được dùng cho tài khoản thường.");
            btnPrimary.setText("Tạo tài khoản");

            inputDisplayName.setVisibility(android.view.View.VISIBLE);
            inputPhone.setVisibility(android.view.View.VISIBLE);
            inputDob.setVisibility(android.view.View.VISIBLE);
            inputGender.setVisibility(android.view.View.VISIBLE);
            inputConfirmPassword.setVisibility(android.view.View.VISIBLE);
            tvForgotPassword.setVisibility(android.view.View.GONE);

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

        UserProfile profile = new UserProfile();
        profile.displayName = displayName;
        profile.phone = phone;
        profile.dateOfBirth = dob;
        profile.gender = gender;
        profile.primaryGoal = new OnboardingPreferences(this).getPrimaryGoal();
        profile.authProvider = UserProfile.PROVIDER_PASSWORD;
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

        dialog.show();
    }

    private void setupGenderDropdown() {
        String[] options = new String[]{"Nam", "Nữ", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        edtGender.setAdapter(adapter);
        edtGender.setOnClickListener(v -> edtGender.showDropDown());
        edtGender.setOnItemClickListener((parent, view, position, id) -> edtGender.setError(null));
    }

    private void showUserFriendlyError(String message) {
        clearAllErrors();

        String safeMessage = message == null || message.trim().isEmpty()
                ? "Có lỗi xảy ra. Vui lòng thử lại."
                : message;

        String lower = safeMessage.toLowerCase(Locale.ROOT);

        if (lower.contains("mật khẩu")) {
            edtPassword.setError(safeMessage);
        } else if (lower.contains("email")) {
            edtEmail.setError(safeMessage);
        } else {
            Toast.makeText(this, safeMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void selectTab(MaterialButton selected, MaterialButton unselected) {
        selected.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        selected.setTextColor(ContextCompat.getColor(this, R.color.on_primary));

        unselected.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_container_low)));
        unselected.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
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

    private String valueOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }
}
