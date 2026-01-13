package com.example.notex;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notex.databinding.ActivityUserHomeBinding;
import com.example.notex.models.User;
import com.example.notex.utils.AuthManager;
import com.example.notex.activities.RemindersActivity;

/**
 * UserHomeActivity - Main home screen for regular users.
 * Displays notebooks, notes, and quick actions.
 */
public class UserHomeActivity extends AppCompatActivity {

    private ActivityUserHomeBinding binding;
    private AuthManager authManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance(this);
        currentUser = authManager.getCurrentUser();

        // Security check
        if (currentUser == null || currentUser.isAdmin()) {
            redirectToLogin();
            return;
        }

        setupToolbar();
        setupUI();
        setupClickListeners();
        loadUserData();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }
    }

    private void setupUI() {
        // Set username
        binding.tvUsername.setText(currentUser.getUsername());
    }

    private void setupClickListeners() {
        // FAB - Create New Note
        binding.fabNewNote.setOnClickListener(v -> {
            Toast.makeText(this, "Create new note - Coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to canvas/note creation activity
            // Intent intent = new Intent(UserHomeActivity.this, CreateNoteActivity.class);
            // startActivity(intent);
        });

        // My Notebooks
        binding.cardMyNotebooks.setOnClickListener(v -> {
            Intent intent = new Intent(UserHomeActivity.this, NotebooksActivity.class);
            startActivity(intent);
        });

        // Search Notes
        binding.cardSearchNotes.setOnClickListener(v -> {
            Toast.makeText(this, "Search Notes - Coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to search activity
            // Intent intent = new Intent(UserHomeActivity.this, SearchActivity.class);
            // startActivity(intent);
        });

        // Scan Documents
        binding.cardScanDocuments.setOnClickListener(v -> {
            Intent intent = new Intent(UserHomeActivity.this, ScanDocumentsActivity.class);
            startActivity(intent);
        });

        // Reminders & Events
        binding.cardReminders.setOnClickListener(v -> {
            Intent intent = new Intent(UserHomeActivity.this, RemindersActivity.class);
            startActivity(intent);
        });

        // Settings
        binding.cardSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings - Coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to settings
            // Intent intent = new Intent(UserHomeActivity.this, SettingsActivity.class);
            // startActivity(intent);
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            handleLogout();
        });
    }

    private void loadUserData() {
        // Load user statistics from database
        com.example.notex.database.DatabaseHelper db = com.example.notex.database.DatabaseHelper.getInstance(this);
        int notebookCount = db.getNotebookCount(currentUser.getId());

        // Count documents in scanned_documents folder
        int documentsCount = 0;
        try {
            java.io.File docsDir = new java.io.File(getExternalFilesDir(null), "scanned_documents");
            if (docsDir.exists() && docsDir.isDirectory()) {
                java.io.File[] files = docsDir.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isFile() && (file.getName().endsWith(".pdf") || file.getName().endsWith(".txt"))) {
                            documentsCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.tvNotebookCount.setText(String.valueOf(notebookCount));
        binding.tvNotesCount.setText(String.valueOf(documentsCount));
        binding.tvTagsCount.setText("0");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_notifications) {
            Toast.makeText(this, "Notifications - Coming soon!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_sync) {
            Toast.makeText(this, "Sync - Coming soon!", Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(UserHomeActivity.this, LoginActivity.class);
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
        // Refresh data when returning to this activity
        loadUserData();
    }
}
