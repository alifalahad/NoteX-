package com.example.notex;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.databinding.ActivitySplashBinding;
import com.example.notex.utils.AuthManager;

/**
 * SplashActivity - Entry point of the app.
 * Shows logo for 2 seconds and checks if user is logged in.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Delay and check login status
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthManager authManager = AuthManager.getInstance(this);

            Intent intent;
            if (authManager.isLoggedIn()) {
                // User is logged in, route to appropriate home screen based on role
                if (authManager.getCurrentUser() != null && authManager.getCurrentUser().isAdmin()) {
                    intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, UserHomeActivity.class);
                }
            } else {
                // User not logged in, go to login
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
