package com.example.notex_desktop;

/**
 * Launcher class to start the JavaFX application.
 * This is needed because JavaFX requires a separate launcher class
 * when running from a JAR file without the JavaFX modules in the module path.
 */
public class Launcher {
    public static void main(String[] args) {
        NoteXApp.main(args);
    }
}
