package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.Notebook;
import com.example.notex_desktop.models.User;
import com.example.notex_desktop.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the User Home screen
 */
public class UserHomeController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label notebookCountLabel;
    @FXML private Label pageCountLabel;
    @FXML private Label reminderCountLabel;

    private AuthManager authManager;
    private DatabaseHelper databaseHelper;
    private User currentUser;

    // Notebook colors
    private static final String[] NOTEBOOK_COLORS = {
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
        "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authManager = AuthManager.getInstance();
        databaseHelper = authManager.getDatabaseHelper();
        currentUser = authManager.getCurrentUser();

        // Security check
        if (currentUser == null || currentUser.isAdmin()) {
            handleLogout();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        welcomeLabel.setText(currentUser.getUsername());
        loadStats();
    }
    
    private void loadStats() {
        List<Notebook> notebooks = databaseHelper.getUserNotebooks(currentUser.getId());
        int notebookCount = notebooks.size();
        
        notebookCountLabel.setText(String.valueOf(notebookCount));
        
        // Count scanned documents (PDF and TXT files)
        int documentCount = countDocuments();
        pageCountLabel.setText(String.valueOf(documentCount));
        
        // Get reminder count
        int reminderCount = databaseHelper.getUserReminderCount(currentUser.getId());
        reminderCountLabel.setText(String.valueOf(reminderCount));
    }
    
    private int countDocuments() {
        try {
            String userHome = System.getProperty("user.home");
            java.io.File documentsDir = new java.io.File(userHome, ".notex_desktop/documents");
            
            if (!documentsDir.exists() || !documentsDir.isDirectory()) {
                return 0;
            }
            
            java.io.File[] files = documentsDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".pdf") || name.toLowerCase().endsWith(".txt")
            );
            
            return files != null ? files.length : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @FXML
    private void handleNewNotebook() {
        // Navigate to My Notebooks view
        NoteXApp.setRoot("views/my_notebooks");
    }

    @FXML
    private void handleScanDocuments() {
        // Navigate to Document Scanner view
        NoteXApp.setRoot("views/document_scanner");
    }

    @FXML
    private void handleReminders() {
        // Navigate to Reminders & Events view
        NoteXApp.setRoot("views/reminders");
    }

    @FXML
    private void handleLogout() {
        authManager.logout();
        NoteXApp.setRoot("views/login");
    }
}
