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
import java.util.ResourceBundle;

public class MyNotebooksController implements Initializable {

    @FXML private VBox notebooksContainer;

    private AuthManager authManager;
    private DatabaseHelper databaseHelper;
    private User currentUser;

    private static final String[] NOTEBOOK_COLORS = {
        "#5dade2", "#58d68d", "#f7dc6f", "#ec7063",
        "#bb8fce", "#48c9b0", "#f0b27a", "#d7a6c5",
        "#85929e", "#5499c7", "#b8a893", "#85c1e9"
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authManager = AuthManager.getInstance();
        databaseHelper = authManager.getDatabaseHelper();
        currentUser = authManager.getCurrentUser();

        if (currentUser == null || currentUser.isAdmin()) {
            handleBack();
            return;
        }

        loadNotebooks();
    }

    private void loadNotebooks() {
        List<Notebook> notebooks = databaseHelper.getUserNotebooks(currentUser.getId());
        notebooksContainer.getChildren().clear();

        for (Notebook notebook : notebooks) {
            notebooksContainer.getChildren().add(createNotebookCard(notebook));
        }
    }

    private VBox createNotebookCard(Notebook notebook) {
        VBox card = new VBox(8);
        card.setPrefHeight(130);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("notebook-card");
        
        String color = notebook.getColor() != null ? notebook.getColor() : NOTEBOOK_COLORS[0];
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 16;");

        // Title
        Label titleLabel = new Label(notebook.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // Page count
        Label pageLabel = new Label(notebook.getPageCount() + " pages");
        pageLabel.setFont(Font.font(14));
        pageLabel.setStyle("-fx-text-fill: rgba(44, 62, 80, 0.7);");

        // Updated label
        Label updatedLabel = new Label("Updated recently");
        updatedLabel.setFont(Font.font(12));
        updatedLabel.setStyle("-fx-text-fill: rgba(44, 62, 80, 0.6);");

        // Checkmark icon (top right)
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font(24));
        checkmark.setStyle("-fx-text-fill: rgba(44, 62, 80, 0.4);");
        headerBox.getChildren().addAll(titleLabel, spacer, checkmark);

        card.getChildren().addAll(headerBox, pageLabel, updatedLabel);

        // Click to open notebook
        card.setOnMouseClicked(e -> openNotebook(notebook));

        return card;
    }

    private void openNotebook(Notebook notebook) {
        // Set current notebook and navigate to page editor
        NoteXApp.setCurrentNotebook(notebook);
        NoteXApp.setRoot("views/page_editor");
    }

    @FXML
    private void handleCreateNotebook() {
        NoteXApp.setRoot("views/create_notebook");
    }

    @FXML
    private void handleBack() {
        NoteXApp.setRoot("views/user_home");
    }
}
