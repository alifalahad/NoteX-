package com.example.notex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.databinding.ActivityLoginBinding;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;

/**
 * LoginActivity - Handles user authentication with role selection.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance(this);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Login as User
        binding.btnLoginUser.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (validateInput(username, password)) {
                attemptLogin(username, password, User.UserRole.USER);
            }
        });

        // Login as Admin
        binding.btnLoginAdmin.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (validateInput(username, password)) {
                attemptLogin(username, password, User.UserRole.ADMIN);
            }
        });

        // Register link
        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateInput(String username, String password) {
        // Clear previous errors
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);

        if (username.isEmpty()) {
            binding.tilUsername.setError(getString(R.string.error_username_required));
            return false;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_password_required));
            return false;
        }

        return true;
    }

    private void attemptLogin(String username, String password, User.UserRole role) {
        boolean success = authManager.login(username, password, role);

        if (success) {
            // Login successful
            Toast.makeText(this, "Welcome, " + username + "!", Toast.LENGTH_SHORT).show();

            // Route to appropriate page based on role
            Intent intent;
            if (role == User.UserRole.ADMIN) {
                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(LoginActivity.this, UserHomeActivity.class);
            }
            startActivity(intent);
            finish();
        } else {
            // Login failed
            String errorMessage = getString(R.string.error_invalid_credentials);

            // Check if it's a role mismatch
            if (authManager.login(username, password,
                    role == User.UserRole.USER ? User.UserRole.ADMIN : User.UserRole.USER)) {
                authManager.logout(); // Logout the wrong role
                errorMessage = getString(R.string.error_wrong_role);
            }

            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }
}
