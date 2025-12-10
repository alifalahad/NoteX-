package com.example.notex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.databinding.ActivityHomeBinding;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;

/**
 * HomeActivity - Main home screen with role-based UI.
 * Shows different content for USER vs ADMIN roles.
 */
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private AuthManager authManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance(this);
        currentUser = authManager.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        setupToolbar();
        loadRoleBasedContent();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);

        // Set toolbar title based on role
        if (currentUser.isAdmin()) {
            binding.toolbar.setTitle(R.string.welcome_admin);
        } else {
            binding.toolbar.setTitle(R.string.app_name);
        }

        // Handle menu item clicks
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                handleLogout();
                return true;
            }
            return false;
        });
    }

    private void loadRoleBasedContent() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View contentView;

        if (currentUser.isAdmin()) {
            // Load admin content
            contentView = inflater.inflate(R.layout.home_admin_content, binding.contentContainer, false);
            binding.fabNewNote.setVisibility(View.GONE);

            // Set admin name
            TextView tvAdminName = contentView.findViewById(R.id.tvAdminName);
            if (tvAdminName != null) {
                tvAdminName.setText(String.format("Welcome, %s", currentUser.getUsername()));
            }

            setupAdminActions(contentView);
        } else {
            // Load user content
            contentView = inflater.inflate(R.layout.home_user_content, binding.contentContainer, false);
            binding.fabNewNote.setVisibility(View.VISIBLE);

            // Set welcome message
            TextView tvWelcome = contentView.findViewById(R.id.tvWelcome);
            if (tvWelcome != null) {
                tvWelcome.setText(String.format(getString(R.string.welcome_user), currentUser.getUsername()));
            }
        }

        binding.contentContainer.removeAllViews();
        binding.contentContainer.addView(contentView);
    }

    private void setupClickListeners() {
        // FAB for new note (user only)
        binding.fabNewNote.setOnClickListener(v -> {
            Toast.makeText(this, "Create new note feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAdminActions(View contentView) {
        // Manage Users button
        View btnManageUsers = contentView.findViewById(R.id.btnManageUsers);
        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> {
                Toast.makeText(this, "Manage Users feature coming soon!", Toast.LENGTH_SHORT).show();
            });
        }

        // View Backups button
        View btnViewBackups = contentView.findViewById(R.id.btnViewBackups);
        if (btnViewBackups != null) {
            btnViewBackups.setOnClickListener(v -> {
                Toast.makeText(this, "View Backups feature coming soon!", Toast.LENGTH_SHORT).show();
            });
        }

        // System Settings button
        View btnSystemSettings = contentView.findViewById(R.id.btnSystemSettings);
        if (btnSystemSettings != null) {
            btnSystemSettings.setOnClickListener(v -> {
                Toast.makeText(this, "System Settings feature coming soon!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void handleLogout() {
        authManager.logout();
        Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to login screen
        moveTaskToBack(true);
    }
}
