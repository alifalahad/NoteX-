package com.example.notex_desktop.controllers;

import com.example.notex_desktop.NoteXApp;
import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.User;
import com.example.notex_desktop.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateNotebookController implements Initializable {

    @FXML private TextField titleField;
    @FXML private GridPane colorGrid;

    private AuthManager authManager;
    private DatabaseHelper databaseHelper;
    private User currentUser;
    private String selectedColor;

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

        selectedColor = NOTEBOOK_COLORS[0]; // Default to first color
        setupColorPicker();
    }

    private void setupColorPicker() {
        int row = 0;
        int col = 0;

        for (String color : NOTEBOOK_COLORS) {
            StackPane colorButton = createColorButton(color);
            colorGrid.add(colorButton, col, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }

    private StackPane createColorButton(String color) {
        StackPane button = new StackPane();
        button.setPrefSize(70, 70);
        button.setAlignment(Pos.CENTER);
        button.setStyle("-fx-cursor: hand;");

        // Color circle
        Circle circle = new Circle(35);
        circle.setFill(Color.web(color));
        circle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");

        // Selection indicator (black border)
        Circle selectionRing = new Circle(35);
        selectionRing.setFill(Color.TRANSPARENT);
        selectionRing.setStroke(Color.BLACK);
        selectionRing.setStrokeWidth(3);
        selectionRing.setVisible(color.equals(selectedColor));

        button.getChildren().addAll(circle, selectionRing);

        // Click handler
        button.setOnMouseClicked(e -> {
            selectedColor = color;
            updateColorSelection();
        });

        return button;
    }

    private void updateColorSelection() {
        // Update all color buttons to show/hide selection
        colorGrid.getChildren().forEach(node -> {
            if (node instanceof StackPane) {
                StackPane button = (StackPane) node;
                if (button.getChildren().size() >= 2 && button.getChildren().get(1) instanceof Circle) {
                    Circle selectionRing = (Circle) button.getChildren().get(1);
                    Circle colorCircle = (Circle) button.getChildren().get(0);
                    String buttonColor = colorCircle.getFill().toString();
                    
                    // Convert Color to hex string for comparison
                    String hexColor = String.format("#%02x%02x%02x",
                        (int) (((Color) colorCircle.getFill()).getRed() * 255),
                        (int) (((Color) colorCircle.getFill()).getGreen() * 255),
                        (int) (((Color) colorCircle.getFill()).getBlue() * 255));
                    
                    selectionRing.setVisible(hexColor.equalsIgnoreCase(selectedColor));
                }
            }
        });
    }

    @FXML
    private void handleCreate() {
        String title = titleField.getText().trim();

        if (title.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Title Required");
            alert.setContentText("Please enter a notebook title.");
            alert.showAndWait();
            return;
        }

        // Create notebook in database
        boolean success = databaseHelper.addNotebook(currentUser.getId(), title, selectedColor);

        if (success) {
            // Navigate back to My Notebooks
            NoteXApp.setRoot("views/my_notebooks");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to Create Notebook");
            alert.setContentText("An error occurred while creating the notebook. Please try again.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleBack() {
        NoteXApp.setRoot("views/my_notebooks");
    }
}
