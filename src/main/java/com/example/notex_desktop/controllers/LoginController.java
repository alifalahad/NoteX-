package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.models.User;
import com.example.notex_desktop.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Login screen
 */
public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Label errorLabel;
    @FXML private Button loginUserButton;
    @FXML private Button loginAdminButton;
    @FXML private Hyperlink registerLink;

    private AuthManager authManager;
    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authManager = AuthManager.getInstance();
        
        // Clear any previous error
        hideError();
        
        // Add enter key handler
        passwordField.setOnAction(e -> handleUserLogin());
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
        }
    }

    @FXML
    private void handleUserLogin() {
        attemptLogin(User.UserRole.USER);
    }

    @FXML
    private void handleAdminLogin() {
        attemptLogin(User.UserRole.ADMIN);
    }

    private void attemptLogin(User.UserRole role) {
        String username = usernameField.getText().trim();
        String password = passwordField.isVisible() ? passwordField.getText().trim() : passwordTextField.getText().trim();

        // Validate inputs
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        // Attempt login
        boolean success = authManager.login(username, password, role);

        if (success) {
            hideError();
            
            // Navigate to appropriate screen
            if (role == User.UserRole.ADMIN) {
                NoteXApp.setRoot("views/admin_dashboard");
            } else {
                NoteXApp.setRoot("views/user_home");
            }
        } else {
            // Check if credentials work with other role
            User.UserRole otherRole = (role == User.UserRole.USER) ? User.UserRole.ADMIN : User.UserRole.USER;
            if (authManager.login(username, password, otherRole)) {
                authManager.logout();
                showError("Invalid role. Please use the correct login button.");
            } else {
                showError("Invalid username or password");
            }
        }
    }

    @FXML
    private void handleRegister() {
        NoteXApp.setRoot("views/register");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
