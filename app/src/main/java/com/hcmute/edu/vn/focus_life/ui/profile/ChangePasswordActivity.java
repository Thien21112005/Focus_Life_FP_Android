package com.hcmute.edu.vn.focus_life.ui.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.SettingsPreferences;
import com.hcmute.edu.vn.focus_life.data.repository.ProfileRepository;

public class ChangePasswordActivity extends AppCompatActivity {
    private ProfileRepository profileRepository;
    private SettingsPreferences settingsPreferences;
    private LinearLayout layoutGoogleMessage;
    private LinearLayout layoutPasswordForm;
    private Button btnUpdatePassword;
    private EditText edtOldPassword;
    private EditText edtNewPassword;
    private EditText edtConfirmPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        profileRepository = new ProfileRepository(this);
        settingsPreferences = new SettingsPreferences(this);

        bindViews();
        setupActions();
        applyAccent();
        renderUi();
    }

    private void bindViews() {
        layoutGoogleMessage = findViewById(R.id.layoutGoogleMessage);
        layoutPasswordForm = findViewById(R.id.layoutPasswordForm);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnUpdatePassword.setOnClickListener(v -> profileRepository.changePassword(
                valueOf(edtOldPassword),
                valueOf(edtNewPassword),
                valueOf(edtConfirmPassword),
                (success, message) -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    if (success) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }
        ));
    }

    private void renderUi() {
        boolean googleUser = isGoogleUser();
        layoutGoogleMessage.setVisibility(googleUser ? View.VISIBLE : View.GONE);
        layoutPasswordForm.setVisibility(googleUser ? View.GONE : View.VISIBLE);
        btnUpdatePassword.setVisibility(googleUser ? View.GONE : View.VISIBLE);
    }

    private boolean isGoogleUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getProviderData() == null) return false;
        for (UserInfo info : user.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    private void applyAccent() {
        btnUpdatePassword.setBackgroundTintList(ColorStateList.valueOf(settingsPreferences.getAccentColor(this)));
    }

    private String valueOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
