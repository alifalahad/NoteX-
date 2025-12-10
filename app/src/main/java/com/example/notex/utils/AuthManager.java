package com.example.notex.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.notex.models.User;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthManager - Singleton class for managing user authentication and sessions.
 * Provides placeholder authentication logic with hardcoded demo users.
 * Will be replaced with SQLite database integration in future phases.
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
    private User currentUser;

    // Placeholder demo users (hardcoded for now)
    private static final Map<String, DemoUser> DEMO_USERS = new HashMap<>();

    static {
        DEMO_USERS.put("demo_user",
                new DemoUser("1", "demo_user", "user@notex.com", "password123", User.UserRole.USER));
        DEMO_USERS.put("admin", new DemoUser("2", "admin", "admin@notex.com", "admin123", User.UserRole.ADMIN));
        DEMO_USERS.put("john", new DemoUser("3", "john", "john@notex.com", "john123", User.UserRole.USER));
    }

    private static class DemoUser {
        String id;
        String username;
        String email;
        String password; // Plain text for demo only
        User.UserRole role;

        DemoUser(String id, String username, String email, String password, User.UserRole role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.password = password;
            this.role = role;
        }
    }

    private AuthManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
        // Check against demo users
        DemoUser demoUser = DEMO_USERS.get(username);

        if (demoUser != null && demoUser.password.equals(password)) {
            // Verify role matches
            if (demoUser.role != expectedRole) {
                return false; // Wrong role selected
            }

            // Create user object
            currentUser = new User(
                    demoUser.id,
                    demoUser.username,
                    demoUser.email,
                    hashPassword(password), // In real app, use bcrypt
                    demoUser.role);

            // Save to SharedPreferences
            saveUserToPreferences();
            return true;
        }

        return false;
    }

    /**
     * Register a new user (placeholder implementation).
     * 
     * @param username Username
     * @param email    Email
     * @param password Password
     * @param role     User role
     * @return true if registration successful
     */
    public boolean register(String username, String email, String password, User.UserRole role) {
        // Check if username already exists
        if (DEMO_USERS.containsKey(username)) {
            return false;
        }

        // In real implementation, this would save to database
        // For now, just add to in-memory map
        String newId = String.valueOf(DEMO_USERS.size() + 1);
        DEMO_USERS.put(username, new DemoUser(newId, username, email, password, role));

        return true;
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
     * Simple password hashing (placeholder - use bcrypt in production).
     * 
     * @param password Plain text password
     * @return Hashed password
     */
    private String hashPassword(String password) {
        // In production, use BCrypt or PBKDF2
        return "hashed_" + password;
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
