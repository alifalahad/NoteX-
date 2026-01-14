package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.User;
import com.example.notex_desktop.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

/**
 * Controller for the Admin Dashboard screen
 */
public class AdminDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalNotesLabel;

    private AuthManager authManager;
    private DatabaseHelper databaseHelper;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authManager = AuthManager.getInstance();
        databaseHelper = authManager.getDatabaseHelper();
        currentUser = authManager.getCurrentUser();

        // Security check - only admins allowed
        if (currentUser == null || !currentUser.isAdmin()) {
            showAccessDenied();
            return;
        }

        setupUI();
        loadStats();
    }

    private void setupUI() {
        welcomeLabel.setText("Welcome, " + currentUser.getUsername());
        
        // Set last login time
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
        String lastLogin = currentUser.getCreatedAt() != null ? 
            sdf.format(currentUser.getCreatedAt()) : "N/A";
        lastLoginLabel.setText("Last login: " + lastLogin);
    }

    private void loadStats() {
        int totalUsers = databaseHelper.getTotalUserCount();
        int totalNotes = 0; // TODO: Add notebook count aggregation

        totalUsersLabel.setText(String.valueOf(totalUsers));
        totalNotesLabel.setText(String.valueOf(totalNotes));
    }

    @FXML
    private void handleManageUsers() {
        NoteXApp.setRoot("views/manage_users");
    }

    @FXML
    private void handleRefresh() {
        loadStats();
    }

    @FXML
    private void handleLogout() {
        authManager.logout();
        NoteXApp.setRoot("views/login");
    }

    private void showAccessDenied() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access Denied");
        alert.setHeaderText(null);
        alert.setContentText("You don't have permission to access this page.");
        alert.showAndWait();
        NoteXApp.setRoot("views/login");
    }
}
