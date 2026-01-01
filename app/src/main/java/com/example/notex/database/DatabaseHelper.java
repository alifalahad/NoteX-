package com.example.notex.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.notex.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * DatabaseHelper - SQLite database helper for NoteX++
 * Manages users table and CRUD operations
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notex.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD_HASH = "password_hash";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_LAST_LOGIN = "last_login";

    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " TEXT PRIMARY KEY,"
                + COLUMN_USERNAME + " TEXT UNIQUE NOT NULL,"
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_PASSWORD_HASH + " TEXT NOT NULL,"
                + COLUMN_ROLE + " TEXT NOT NULL,"
                + COLUMN_CREATED_AT + " TEXT,"
                + COLUMN_LAST_LOGIN + " TEXT"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Insert default users
        insertDefaultUsers(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private void insertDefaultUsers(SQLiteDatabase db) {
        // Default user
        insertUser(db, "demo_user", "user@notex.com", "password123", User.UserRole.USER);

        // Default admin
        insertUser(db, "admin", "admin@notex.com", "admin123", User.UserRole.ADMIN);

        // Additional user
        insertUser(db, "john", "john@notex.com", "john123", User.UserRole.USER);
    }

    private void insertUser(SQLiteDatabase db, String username, String email, String password, User.UserRole role) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, UUID.randomUUID().toString());
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD_HASH, hashPassword(password));
        values.put(COLUMN_ROLE, role.name());
        values.put(COLUMN_CREATED_AT, getCurrentTimestamp());
        values.put(COLUMN_LAST_LOGIN, getCurrentTimestamp());

        db.insert(TABLE_USERS, null, values);
    }

    /**
     * Add a new user to the database
     */
    public boolean addUser(String username, String email, String password, User.UserRole role) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, UUID.randomUUID().toString());
            values.put(COLUMN_USERNAME, username);
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_PASSWORD_HASH, hashPassword(password));
            values.put(COLUMN_ROLE, role.name());
            values.put(COLUMN_CREATED_AT, getCurrentTimestamp());
            values.put(COLUMN_LAST_LOGIN, getCurrentTimestamp());

            long result = db.insert(TABLE_USERS, null, values);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_USERS,
                    null,
                    COLUMN_USERNAME + "=?",
                    new String[] { username },
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return user;
    }

    /**
     * Validate user credentials
     */
    public User validateUser(String username, String password, User.UserRole expectedRole) {
        User user = getUserByUsername(username);

        if (user != null) {
            // Check password
            if (user.getPasswordHash().equals(hashPassword(password))) {
                // Check role
                if (user.getRole() == expectedRole) {
                    // Update last login
                    updateLastLogin(user.getId());
                    return user;
                }
            }
        }

        return null;
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_USERS,
                    null, null, null, null, null,
                    COLUMN_CREATED_AT + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    users.add(cursorToUser(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return users;
    }

    /**
     * Get user count by role
     */
    public int getUserCountByRole(User.UserRole role) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_USERS + " WHERE " + COLUMN_ROLE + "=?",
                    new String[] { role.name() });

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }

    /**
     * Update last login timestamp
     */
    private void updateLastLogin(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_LOGIN, getCurrentTimestamp());

        db.update(TABLE_USERS, values, COLUMN_ID + "=?", new String[] { userId });
    }

    /**
     * Delete user by ID
     */
    public boolean deleteUser(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_USERS, COLUMN_ID + "=?", new String[] { userId });
        return result > 0;
    }

    /**
     * Update user role
     */
    public boolean updateUserRole(String userId, User.UserRole newRole) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ROLE, newRole.name());
        
        int result = db.update(TABLE_USERS, values, COLUMN_ID + "=?", new String[] { userId });
        return result > 0;
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        return getUserByUsername(username) != null;
    }

    /**
     * Convert cursor to User object
     */
    private User cursorToUser(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
        String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
        String passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
        String roleStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));

        User.UserRole role = User.UserRole.valueOf(roleStr);

        User user = new User(id, username, email, passwordHash, role);

        // Set timestamps if available
        int createdAtIndex = cursor.getColumnIndex(COLUMN_CREATED_AT);
        if (createdAtIndex != -1) {
            String createdAt = cursor.getString(createdAtIndex);
            // You can parse and set the date if needed
        }

        return user;
    }

    /**
     * Simple password hashing (use BCrypt in production)
     */
    private String hashPassword(String password) {
        // For demo purposes, using simple hash
        // In production, use BCrypt or PBKDF2
        return "hashed_" + password;
    }

    /**
     * Get current timestamp as string
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Get total user count
     */
    public int getTotalUserCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }
}
