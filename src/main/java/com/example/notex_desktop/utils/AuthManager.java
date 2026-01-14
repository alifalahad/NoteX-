package com.example.notex_desktop.utils;

import com.example.notex_desktop.database.DatabaseHelper;
import com.example.notex_desktop.models.User;

import java.util.prefs.Preferences;

/**
 * AuthManager - Singleton class for managing user authentication and sessions.
 * Uses Java Preferences API for persistent storage on desktop.
 */
public class AuthManager {

    private static AuthManager instance;
    private static final String PREFS_NODE = "com/example/notex_desktop";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private Preferences preferences;
    private DatabaseHelper databaseHelper;
    private User currentUser;

    private AuthManager() {
        preferences = Preferences.userRoot().node(PREFS_NODE);
        databaseHelper = DatabaseHelper.getInstance();
        loadUserFromPreferences();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    /**
     * Attempt to login with username and password.
     *
     * @param username     Username or email
     * @param password     Password
     * @param expectedRole Expected role (USER or ADMIN) for validation
     * @return true if login successful, false otherwise
     */
    public boolean login(String username, String password, User.UserRole expectedRole) {
        // Trim username but preserve case (case-insensitive lookup handled in database)
        String trimmedUsername = username.trim();

        // Validate user against database
        User user = databaseHelper.validateUser(trimmedUsername, password, expectedRole);

        if (user != null) {
            currentUser = user;
            saveUserToPreferences();
            return true;
        }

        return false;
    }

    /**
     * Register a new user.
     *
     * @param username Username
     * @param email    Email
     * @param password Password
     * @param role     User role
     * @return true if registration successful
     */
    public boolean register(String username, String email, String password, User.UserRole role) {
        // Trim but preserve case for display
        String trimmedUsername = username.trim();

        // Check if username already exists (case-insensitive check)
        if (databaseHelper.usernameExists(trimmedUsername)) {
            return false;
        }

        // Add user to database with original case
        return databaseHelper.addUser(trimmedUsername, email, password, role);
    }

    /**
     * Logout current user.
     */
    public void logout() {
        currentUser = null;
        clearPreferences();
    }

    /**
     * Check if a user is currently logged in.
     *
     * @return true if logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null && preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get the current logged-in user.
     *
     * @return Current user or null if not logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Get database helper instance
     */
    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    /**
     * Save user session to Preferences.
     */
    private void saveUserToPreferences() {
        if (currentUser == null)
            return;

        preferences.put(KEY_USER_ID, currentUser.getId());
        preferences.put(KEY_USERNAME, currentUser.getUsername());
        preferences.put(KEY_EMAIL, currentUser.getEmail() != null ? currentUser.getEmail() : "");
        preferences.put(KEY_ROLE, currentUser.getRole().name());
        preferences.putBoolean(KEY_IS_LOGGED_IN, true);
    }

    /**
     * Load user session from Preferences.
     */
    private void loadUserFromPreferences() {
        boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn) {
            String id = preferences.get(KEY_USER_ID, null);
            String username = preferences.get(KEY_USERNAME, null);
            String email = preferences.get(KEY_EMAIL, null);
            String roleStr = preferences.get(KEY_ROLE, null);

            if (id != null && username != null && roleStr != null) {
                User.UserRole role = User.UserRole.valueOf(roleStr);
                currentUser = new User(id, username, email, "", role);
            }
        }
    }

    /**
     * Clear all preferences.
     */
    private void clearPreferences() {
        try {
            preferences.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validate email format.
     *
     * @param email Email to validate
     * @return true if valid
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Validate password strength.
     *
     * @param password Password to validate
     * @return true if password meets minimum requirements
     */
    public static boolean isValidPassword(String password) {
        // Minimum 6 characters for demo
        return password != null && password.length() >= 6;
    }
}
