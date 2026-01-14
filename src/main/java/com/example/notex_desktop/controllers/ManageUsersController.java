package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.User;
import com.example.notex_desktop.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Manage Users screen
 */
public class ManageUsersController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private VBox userListContainer;

    private AuthManager authManager;
    private DatabaseHelper databaseHelper;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authManager = AuthManager.getInstance();
        databaseHelper = authManager.getDatabaseHelper();
        currentUser = authManager.getCurrentUser();

        // Security check
        if (currentUser == null || !currentUser.isAdmin()) {
            showAccessDenied();
            return;
        }

        loadStats();
        loadUsers();
    }

    private void loadStats() {
        int totalUsers = databaseHelper.getTotalUserCount();
        int adminCount = databaseHelper.getUserCountByRole(User.UserRole.ADMIN);

        totalUsersLabel.setText(String.valueOf(totalUsers));
        adminCountLabel.setText(String.valueOf(adminCount));
    }

    private void loadUsers() {
        userListContainer.getChildren().clear();
        List<User> users = databaseHelper.getAllUsers();

        for (User user : users) {
            userListContainer.getChildren().add(createUserCard(user));
        }
    }

    private HBox createUserCard(User user) {
        HBox card = new HBox();
        card.setSpacing(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("manage-users-user-card");
        card.setPadding(new Insets(16));

        // Icon
        Label icon = new Label(user.isAdmin() ? "🛡" : "👤");
        icon.setFont(Font.font(32));
        icon.getStyleClass().add(user.isAdmin() ? "manage-users-icon-admin" : "manage-users-icon-user");

        // User Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label username = new Label(user.getUsername());
        username.setFont(Font.font("System Bold", 16));
        username.getStyleClass().add("manage-users-username");

        Label email = new Label(user.getEmail() != null ? user.getEmail() : "");
        email.setFont(Font.font(14));
        email.getStyleClass().add("manage-users-email");

        Label id = new Label("ID: " + user.getId());
        id.setFont(Font.font(12));
        id.getStyleClass().add("manage-users-id");

        Label password = new Label("Pass: hashed_" + user.getPasswordHash().substring(0, Math.min(8, user.getPasswordHash().length())) + "...");
        password.setFont(Font.font(12));
        password.getStyleClass().add("manage-users-password");

        info.getChildren().addAll(username, email, id, password);

        // Role Badge
        Label badge = new Label(user.getRole().name());
        badge.setFont(Font.font("System Bold", 14));
        badge.setPadding(new Insets(8, 16, 8, 16));
        badge.getStyleClass().add(user.isAdmin() ? "manage-users-badge-admin" : "manage-users-badge-user");

        // Delete Button
        Button deleteBtn = new Button("🗑");
        deleteBtn.setFont(Font.font(20));
        deleteBtn.getStyleClass().add("manage-users-delete-btn");
        deleteBtn.setOnAction(e -> handleDeleteUser(user));

        card.getChildren().addAll(icon, info, badge, deleteBtn);
        return card;
    }

    private void handleDeleteUser(User user) {
        // Prevent deleting yourself
        if (user.getId().equals(currentUser.getId())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Delete");
            alert.setHeaderText("Cannot delete your own account");
            alert.setContentText("You cannot delete your own account while logged in.");
            alert.showAndWait();
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete User");
        confirmAlert.setHeaderText("Delete " + user.getUsername() + "?");
        confirmAlert.setContentText("This will permanently delete this user. This action cannot be undone.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = databaseHelper.deleteUser(user.getId());
                if (success) {
                    loadStats();
                    loadUsers();
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        NoteXApp.setRoot("views/admin_dashboard");
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
