package com.example.notex;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.databinding.ActivityAdminDashboardBinding;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AdminDashboardActivity - Main dashboard for administrators.
 * Provides user management, system settings, backups, and reports.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private AuthManager authManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance(this);
        currentUser = authManager.getCurrentUser();

        // Security check - only admins allowed
        if (currentUser == null || !currentUser.isAdmin()) {
            Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        setupToolbar();
        setupUI();
        setupClickListeners();
        loadSystemStats();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }
    }

    private void setupUI() {
        // Set admin name
        binding.tvAdminName.setText(String.format("Welcome, %s", currentUser.getUsername()));

        // Set last login
        if (currentUser.getLastLogin() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            String lastLogin = sdf.format(currentUser.getLastLogin());
            binding.tvLastLogin.setText(String.format("Last login: %s", lastLogin));
        }
    }

    private void setupClickListeners() {
        // Manage Users
        binding.cardManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, UserListActivity.class);
            startActivity(intent);
        });

        // System Settings
        binding.cardSystemSettings.setOnClickListener(v -> {
            Toast.makeText(this, "System Settings - Coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to system settings
            // Intent intent = new Intent(AdminDashboardActivity.this, SystemSettingsActivity.class);
            // startActivity(intent);
        });

        // Backup & Restore
        binding.cardBackupRestore.setOnClickListener(v -> {
            Toast.makeText(this, "Backup & Restore - Coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to backup management
            // Intent intent = new Intent(AdminDashboardActivity.this, BackupActivity.class);
            // startActivity(intent);
        });

        // View Reports
        binding.cardViewReports.setOnClickListener(v -> {
            Toast.makeText(this, "Reports - Coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to reports/analytics
            // Intent intent = new Intent(AdminDashboardActivity.this, ReportsActivity.class);
            // startActivity(intent);
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            handleLogout();
        });
    }

    private void loadSystemStats() {
        // Load actual statistics from database
        AuthManager authManager = AuthManager.getInstance(this);
        DatabaseHelper db = authManager.getDatabaseHelper();
        
        // Get total user count
        int totalUsers = db.getTotalUserCount();
        binding.tvTotalUsers.setText(String.valueOf(totalUsers));
        
        // Total notes - will be implemented when notes feature is added
        binding.tvTotalNotes.setText("0");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
            loadSystemStats();
            return true;
        } else if (id == R.id.action_notifications) {
            Toast.makeText(this, "Notifications - Coming soon!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            handleLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleLogout() {
        authManager.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to login screen
        // Instead, minimize the app
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this activity
        loadSystemStats();
    }
}
