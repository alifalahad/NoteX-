package com.example.notex.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.notex.database.DatabaseHelper;
import com.example.notex.models.User;

/**
 * AuthManager - Singleton class for managing user authentication and sessions.
 * Now uses DatabaseHelper for persistent storage.
 */
public class AuthManager {

    private static AuthManager instance;
    private static final String PREFS_NAME = "NoteXPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper;
    private User currentUser;

    private AuthManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        databaseHelper = DatabaseHelper.getInstance(context);
        loadUserFromPreferences();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
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
        // Normalize username (case-insensitive)
        String normalizedUsername = username.toLowerCase().trim();

        // Validate user against database
        User user = databaseHelper.validateUser(normalizedUsername, password, expectedRole);

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
        // Normalize username (case-insensitive)
        String normalizedUsername = username.toLowerCase().trim();

        // Check if username already exists
        if (databaseHelper.usernameExists(normalizedUsername)) {
            return false;
        }

        // Add user to database
        return databaseHelper.addUser(normalizedUsername, email, password, role);
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
        return currentUser != null && sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
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
     * Save user session to SharedPreferences.
     */
    private void saveUserToPreferences() {
        if (currentUser == null)
            return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, currentUser.getId());
        editor.putString(KEY_USERNAME, currentUser.getUsername());
        editor.putString(KEY_EMAIL, currentUser.getEmail());
        editor.putString(KEY_ROLE, currentUser.getRole().name());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Load user session from SharedPreferences.
     */
    private void loadUserFromPreferences() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn) {
            String id = sharedPreferences.getString(KEY_USER_ID, null);
            String username = sharedPreferences.getString(KEY_USERNAME, null);
            String email = sharedPreferences.getString(KEY_EMAIL, null);
            String roleStr = sharedPreferences.getString(KEY_ROLE, null);

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
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
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
