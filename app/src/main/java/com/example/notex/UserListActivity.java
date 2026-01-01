package com.example.notex;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.databinding.ActivityUserListBinding;
import com.example.notex.models.User;

import java.util.List;

/**
 * UserListActivity - Admin screen to view all users from database
 */
public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    private ActivityUserListBinding binding;
    private DatabaseHelper databaseHelper;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = DatabaseHelper.getInstance(this);

        setupToolbar();
        setupRecyclerView();
        loadUsers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(this);
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewUsers.setAdapter(userAdapter);
    }

    private void loadUsers() {
        List<User> users = databaseHelper.getAllUsers();

        if (users.isEmpty()) {
            binding.recyclerViewUsers.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerViewUsers.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
            userAdapter.setUsers(users);
        }

        // Update stats
        int totalUsers = databaseHelper.getTotalUserCount();
        int adminCount = databaseHelper.getUserCountByRole(User.UserRole.ADMIN);

        binding.tvTotalUsers.setText(String.valueOf(totalUsers));
        binding.tvAdminCount.setText(String.valueOf(adminCount));
    }

    @Override
    public void onUserClick(User user) {
        showUserDetailsDialog(user);
    }

    private void showUserDetailsDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage User: " + user.getUsername());

        String message = "Username: " + user.getUsername() + "\n" +
                "Email: " + (user.getEmail() != null ? user.getEmail() : "N/A") + "\n" +
                "Role: " + user.getRole().name() + "\n" +
                "ID: " + user.getId() + "\n\n" +
                "Password Hash:\n" + user.getPasswordHash();

        builder.setMessage(message);

        // Show different options based on user role
        if (user.isAdmin()) {
            // For admin users: Demote option
            builder.setPositiveButton("Demote to User", (dialog, which) -> {
                confirmRoleChange(user, User.UserRole.USER);
            });
            builder.setNeutralButton("Close", null);
        } else {
            // For regular users: Promote and Delete options
            builder.setPositiveButton("Promote to Admin", (dialog, which) -> {
                confirmRoleChange(user, User.UserRole.ADMIN);
            });
            builder.setNegativeButton("Delete User", (dialog, which) -> {
                confirmDeleteUser(user);
            });
            builder.setNeutralButton("Close", null);
        }

        builder.show();
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getUsername() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = databaseHelper.deleteUser(user.getId());
                    if (success) {
                        Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                        loadUsers(); // Refresh list
                    } else {
                        Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRoleChange(User user, User.UserRole newRole) {
        String action = newRole == User.UserRole.ADMIN ? "Promote to Admin" : "Demote to User";
        String message = "Are you sure you want to " + action.toLowerCase() + " for " + user.getUsername() + "?";

        new AlertDialog.Builder(this)
                .setTitle(action)
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    boolean success = databaseHelper.updateUserRole(user.getId(), newRole);
                    if (success) {
                        Toast.makeText(this, "Role updated successfully", Toast.LENGTH_SHORT).show();
                        loadUsers(); // Refresh list
                    } else {
                        Toast.makeText(this, "Failed to update role", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
