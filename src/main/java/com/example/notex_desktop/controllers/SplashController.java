package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.utils.AuthManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Splash Screen
 */
public class SplashController implements Initializable {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Wait 2 seconds then navigate to appropriate screen
        PauseTransition delay = new PauseTransition(Duration.millis(SPLASH_DELAY));
        delay.setOnFinished(event -> navigateToNextScreen());
        delay.play();
    }

    private void navigateToNextScreen() {
        try {
            AuthManager authManager = AuthManager.getInstance();

            if (authManager.isLoggedIn()) {
                if (authManager.getCurrentUser() != null && authManager.getCurrentUser().isAdmin()) {
                    NoteXApp.setRoot("views/admin_dashboard");
                } else {
                    NoteXApp.setRoot("views/user_home");
                }
            } else {
                NoteXApp.setRoot("views/login");
            }
        } catch (Exception e) {
            System.err.println("Error navigating from splash: " + e.getMessage());
            e.printStackTrace();
            // Fallback to login on error
            try {
                NoteXApp.setRoot("views/login");
            } catch (Exception ex) {
                System.err.println("Critical error: Cannot load login screen");
                ex.printStackTrace();
            }
        }
    }
}
