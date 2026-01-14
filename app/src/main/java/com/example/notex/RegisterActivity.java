package com.example.notex;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.databinding.ActivityRegisterBinding;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;

/**
 * RegisterActivity - Handles new user registration.
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance(this);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Register button
        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        // Login link
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Clear previous errors
        binding.tilUsername.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        // Validate inputs
        if (username.isEmpty()) {
            binding.tilUsername.setError(getString(R.string.error_username_required));
            return;
        }

        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.error_email_required));
            return;
        }

        if (!AuthManager.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_password_required));
            return;
        }

        if (!AuthManager.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_weak));
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        // Get selected role
        User.UserRole role = binding.btnRoleUser.isChecked() ? User.UserRole.USER : User.UserRole.ADMIN;

        // Attempt registration
        boolean success = authManager.register(username, email, password, role);

        if (success) {
            Toast.makeText(this, R.string.registration_success, Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, R.string.error_username_exists, Toast.LENGTH_LONG).show();
        }
    }
}
