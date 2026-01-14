package com.example.notex_desktop;

import com.example.notex_desktop.models.Notebook;
import com.example.notex_desktop.models.Page;
import com.example.notex_desktop.utils.AuthManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * NoteX Desktop Application - Main Application Class
 * A smart note-taking application for desktop
 */
public class NoteXApp extends Application {

    private static Scene scene;
    private static Stage primaryStage;
    
    // State holders for navigation
    private static Notebook currentNotebook;
    private static Page currentPage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        
        // Always start with splash screen
        scene = new Scene(loadFXML("views/splash"), 1024, 768);
        
        stage.setTitle("NoteX++ Desktop");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void setRoot(String fxml) {
        try {
            scene.setRoot(loadFXML(fxml));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(NoteXApp.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // Notebook navigation helpers
    public static Notebook getCurrentNotebook() {
        return currentNotebook;
    }

    public static void setCurrentNotebook(Notebook notebook) {
        currentNotebook = notebook;
    }

    // Page navigation helpers
    public static Page getCurrentPage() {
        return currentPage;
    }

    public static void setCurrentPage(Page page) {
        currentPage = page;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
