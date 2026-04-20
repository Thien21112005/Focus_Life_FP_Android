package com.hcmute.edu.vn.focus_life.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.data.repository.AuthRepository;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;

public class RegisterActivity extends AppCompatActivity {
    private AuthViewModel viewModel;
    private AuthRepository repository;

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

        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPassword = findViewById(R.id.edtPassword);
        MaterialButton btnRegister = findViewById(R.id.btnLogin);
        View btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextView tvTabLogin = findViewById(R.id.tvTabLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnRegister.setText("Đăng ký");
        tvForgotPassword.setVisibility(View.GONE);
        tvTabLogin.setOnClickListener(v -> finish());
        btnGoogleSignIn.setOnClickListener(v -> googleLauncher.launch(repository.getGoogleSignInIntent()));

        btnRegister.setOnClickListener(v ->
                viewModel.register(repository,
                        edtEmail.getText().toString().trim(),
                        edtPassword.getText().toString().trim()));

        viewModel.getAuthState().observe(this, result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                FirebaseUser user = result.getData();
                Toast.makeText(this, "Tạo tài khoản thành công: " + (user != null ? user.getEmail() : ""), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finishAffinity();
            } else if (result.getError() != null) {
                Toast.makeText(this, result.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
