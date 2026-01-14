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
 * Controller for the Registration screen
 */
public class RegisterController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginLink;
    @FXML private ToggleButton userRoleButton;
    @FXML private ToggleButton adminRoleButton;

    private AuthManager authManager;
    private ToggleGroup roleToggleGroup;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authManager = AuthManager.getInstance();
        hideError();
        
        // Setup toggle group for role selection
        roleToggleGroup = new ToggleGroup();
        userRoleButton.setToggleGroup(roleToggleGroup);
        adminRoleButton.setToggleGroup(roleToggleGroup);
        userRoleButton.setSelected(true);
        
        // Ensure at least one is always selected
        roleToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
            }
        });
        
        // Add enter key handler
        confirmPasswordField.setOnAction(e -> handleRegister());
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
    private void toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        if (isConfirmPasswordVisible) {
            confirmPasswordTextField.setText(confirmPasswordField.getText());
            confirmPasswordTextField.setVisible(true);
            confirmPasswordField.setVisible(false);
        } else {
            confirmPasswordField.setText(confirmPasswordTextField.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordTextField.setVisible(false);
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.isVisible() ? passwordField.getText().trim() : passwordTextField.getText().trim();
        String confirmPassword = confirmPasswordField.isVisible() ? confirmPasswordField.getText().trim() : confirmPasswordTextField.getText().trim();

        // Validate inputs
        if (username.isEmpty()) {
            showError("Please enter a username");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }

        if (!email.isEmpty() && !AuthManager.isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter a password");
            return;
        }

        if (!AuthManager.isValidPassword(password)) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        // Get selected role
        User.UserRole selectedRole = userRoleButton.isSelected() ? User.UserRole.USER : User.UserRole.ADMIN;

        // Attempt registration
        boolean success = authManager.register(username, email, password, selectedRole);

        if (success) {
            // Auto-login after registration
            authManager.login(username, password, selectedRole);
            
            // Navigate based on role
            if (selectedRole == User.UserRole.ADMIN) {
                NoteXApp.setRoot("views/admin_dashboard");
            } else {
                NoteXApp.setRoot("views/user_home");
            }
        } else {
            showError("Username already exists. Please choose another.");
        }
    }

    @FXML
    private void handleLogin() {
        NoteXApp.setRoot("views/login");
    }

    @FXML
    private void handleBackToLogin() {
        NoteXApp.setRoot("views/login");
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
