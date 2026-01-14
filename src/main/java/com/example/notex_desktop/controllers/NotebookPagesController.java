package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.Notebook;
import com.example.notex_desktop.models.Page;
import com.example.notex_desktop.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Notebook Pages screen
 */
public class NotebookPagesController implements Initializable {

    @FXML private Label notebookTitle;
    @FXML private Label pageCountLabel;
    @FXML private VBox pagesContainer;
    @FXML private VBox emptyState;

    private DatabaseHelper databaseHelper;
    private Notebook currentNotebook;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        databaseHelper = AuthManager.getInstance().getDatabaseHelper();
        currentNotebook = NoteXApp.getCurrentNotebook();

        if (currentNotebook == null) {
            handleBack();
            return;
        }

        setupUI();
        loadPages();
    }

    private void setupUI() {
        notebookTitle.setText(currentNotebook.getTitle());
    }

    private void loadPages() {
        List<Page> pages = databaseHelper.getNotebookPages(currentNotebook.getId());
        
        pageCountLabel.setText(pages.size() + " pages");
        pagesContainer.getChildren().clear();

        if (pages.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            pagesContainer.setVisible(false);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            pagesContainer.setVisible(true);

            for (Page page : pages) {
                pagesContainer.getChildren().add(createPageCard(page));
            }
        }
    }

    private HBox createPageCard(Page page) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.getStyleClass().add("page-card");

        // Page number
        Label numberLabel = new Label(String.valueOf(page.getPageNumber()));
        numberLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        numberLabel.getStyleClass().add("page-number");
        numberLabel.setMinWidth(50);
        numberLabel.setAlignment(Pos.CENTER);

        // Page info
        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(page.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.getStyleClass().add("page-title");

        // Preview of content
        String preview = page.getContent() != null ? 
            (page.getContent().length() > 100 ? 
                page.getContent().substring(0, 100) + "..." : 
                page.getContent()) : 
            "Empty page";
        Label previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("page-preview");
        previewLabel.setWrapText(true);

        // Last modified
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        Label dateLabel = new Label("Modified: " + 
            (page.getUpdatedAt() != null ? sdf.format(page.getUpdatedAt()) : "N/A"));
        dateLabel.getStyleClass().add("page-date");

        infoBox.getChildren().addAll(titleLabel, previewLabel, dateLabel);

        // Actions
        VBox actionsBox = new VBox(5);
        actionsBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setOnAction(e -> openPage(page));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger-small");
        deleteBtn.setOnAction(e -> deletePage(page));

        actionsBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(numberLabel, infoBox, actionsBox);

        // Double-click to open
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openPage(page);
            }
        });

        return card;
    }

    private void openPage(Page page) {
        NoteXApp.setCurrentPage(page);
        NoteXApp.setRoot("views/page_editor");
    }

    private void deletePage(Page page) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Page");
        alert.setHeaderText("Delete \"" + page.getTitle() + "\"?");
        alert.setContentText("This will permanently delete this page. This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            databaseHelper.deletePage(page.getId());
            loadPages();
        }
    }

    @FXML
    private void handleNewPage() {
        TextInputDialog dialog = new TextInputDialog("New Page");
        dialog.setTitle("Create New Page");
        dialog.setHeaderText("Create a new page");
        dialog.setContentText("Page title:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            if (!title.trim().isEmpty()) {
                databaseHelper.addPage(currentNotebook.getId(), title.trim(), "");
                loadPages();
            }
        });
    }

    @FXML
    private void handleBack() {
        NoteXApp.setCurrentNotebook(null);
        NoteXApp.setRoot("views/user_home");
    }
}
