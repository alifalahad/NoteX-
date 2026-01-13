package com.example.notex.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.notex.models.User;
import com.example.notex.models.Notebook;
import com.example.notex.models.Page;
import com.example.notex.models.Reminder;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private static final int DATABASE_VERSION = 4; // Updated for reminders table

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD_HASH = "password_hash";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_LAST_LOGIN = "last_login";

    // Notebooks table
    private static final String TABLE_NOTEBOOKS = "notebooks";
    private static final String COLUMN_NOTEBOOK_ID = "id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_IS_PINNED = "is_pinned";
    private static final String COLUMN_NOTEBOOK_CREATED_AT = "created_at";
    private static final String COLUMN_NOTEBOOK_UPDATED_AT = "updated_at";

    // Pages table
    private static final String TABLE_PAGES = "pages";
    private static final String COLUMN_PAGE_ID = "id";
    private static final String COLUMN_PAGE_NOTEBOOK_ID = "notebook_id";
    private static final String COLUMN_PAGE_TITLE = "title";
    private static final String COLUMN_PAGE_CONTENT = "content";
    private static final String COLUMN_PAGE_NUMBER = "page_number";
    private static final String COLUMN_PAGE_CREATED_AT = "created_at";
    private static final String COLUMN_PAGE_UPDATED_AT = "updated_at";

    // Reminders table
    private static final String TABLE_REMINDERS = "reminders";
    private static final String COLUMN_REMINDER_ID = "id";
    private static final String COLUMN_REMINDER_USER_ID = "user_id";
    private static final String COLUMN_REMINDER_NOTE_ID = "note_id";
    private static final String COLUMN_REMINDER_NOTEBOOK_ID = "notebook_id";
    private static final String COLUMN_REMINDER_TITLE = "title";
    private static final String COLUMN_REMINDER_DESCRIPTION = "description";
    private static final String COLUMN_REMINDER_TYPE = "type";
    private static final String COLUMN_REMINDER_TRIGGER_TYPE = "trigger_type";
    private static final String COLUMN_REMINDER_SCHEDULED_AT = "scheduled_at";
    private static final String COLUMN_REMINDER_LOCATION = "location";
    private static final String COLUMN_REMINDER_LATITUDE = "latitude";
    private static final String COLUMN_REMINDER_LONGITUDE = "longitude";
    private static final String COLUMN_REMINDER_RADIUS = "radius_meters";
    private static final String COLUMN_REMINDER_REPEAT_TYPE = "repeat_type";
    private static final String COLUMN_REMINDER_REPEAT_RULE = "repeat_rule";
    private static final String COLUMN_REMINDER_PRIORITY = "priority";
    private static final String COLUMN_REMINDER_IS_COMPLETED = "is_completed";
    private static final String COLUMN_REMINDER_IS_NOTIFIED = "is_notified";
    private static final String COLUMN_REMINDER_IS_ALL_DAY = "is_all_day";
    private static final String COLUMN_REMINDER_TIMEZONE = "timezone";
    private static final String COLUMN_REMINDER_CREATED_AT = "created_at";
    private static final String COLUMN_REMINDER_UPDATED_AT = "updated_at";

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

        // Create notebooks table
        String CREATE_NOTEBOOKS_TABLE = "CREATE TABLE " + TABLE_NOTEBOOKS + "("
                + COLUMN_NOTEBOOK_ID + " TEXT PRIMARY KEY,"
                + COLUMN_USER_ID + " TEXT NOT NULL,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_COLOR + " TEXT,"
                + COLUMN_IS_PINNED + " INTEGER DEFAULT 0,"
                + COLUMN_NOTEBOOK_CREATED_AT + " TEXT,"
                + COLUMN_NOTEBOOK_UPDATED_AT + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_NOTEBOOKS_TABLE);

        // Create pages table
        String CREATE_PAGES_TABLE = "CREATE TABLE " + TABLE_PAGES + "("
                + COLUMN_PAGE_ID + " TEXT PRIMARY KEY,"
                + COLUMN_PAGE_NOTEBOOK_ID + " TEXT NOT NULL,"
                + COLUMN_PAGE_TITLE + " TEXT NOT NULL,"
                + COLUMN_PAGE_CONTENT + " TEXT,"
                + COLUMN_PAGE_NUMBER + " INTEGER,"
                + COLUMN_PAGE_CREATED_AT + " TEXT,"
                + COLUMN_PAGE_UPDATED_AT + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_PAGE_NOTEBOOK_ID + ") REFERENCES " + TABLE_NOTEBOOKS + "("
                + COLUMN_NOTEBOOK_ID + ")"
                + ")";
        db.execSQL(CREATE_PAGES_TABLE);

        // Create reminders table
        String CREATE_REMINDERS_TABLE = "CREATE TABLE " + TABLE_REMINDERS + "("
                + COLUMN_REMINDER_ID + " TEXT PRIMARY KEY,"
                + COLUMN_REMINDER_USER_ID + " TEXT NOT NULL,"
                + COLUMN_REMINDER_NOTE_ID + " TEXT,"
                + COLUMN_REMINDER_NOTEBOOK_ID + " TEXT,"
                + COLUMN_REMINDER_TITLE + " TEXT NOT NULL,"
                + COLUMN_REMINDER_DESCRIPTION + " TEXT,"
                + COLUMN_REMINDER_TYPE + " TEXT NOT NULL,"
                + COLUMN_REMINDER_TRIGGER_TYPE + " TEXT NOT NULL,"
                + COLUMN_REMINDER_SCHEDULED_AT + " INTEGER,"
                + COLUMN_REMINDER_LOCATION + " TEXT,"
                + COLUMN_REMINDER_LATITUDE + " REAL,"
                + COLUMN_REMINDER_LONGITUDE + " REAL,"
                + COLUMN_REMINDER_RADIUS + " INTEGER,"
                + COLUMN_REMINDER_REPEAT_TYPE + " TEXT,"
                + COLUMN_REMINDER_REPEAT_RULE + " TEXT,"
                + COLUMN_REMINDER_PRIORITY + " INTEGER,"
                + COLUMN_REMINDER_IS_COMPLETED + " INTEGER DEFAULT 0,"
                + COLUMN_REMINDER_IS_NOTIFIED + " INTEGER DEFAULT 0,"
                + COLUMN_REMINDER_IS_ALL_DAY + " INTEGER DEFAULT 0,"
                + COLUMN_REMINDER_TIMEZONE + " TEXT,"
                + COLUMN_REMINDER_CREATED_AT + " INTEGER,"
                + COLUMN_REMINDER_UPDATED_AT + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_REMINDER_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_REMINDERS_TABLE);

        // Insert default users
        insertDefaultUsers(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Add reminders table for version 4
            String CREATE_REMINDERS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_REMINDERS + "("
                    + COLUMN_REMINDER_ID + " TEXT PRIMARY KEY,"
                    + COLUMN_REMINDER_USER_ID + " TEXT NOT NULL,"
                    + COLUMN_REMINDER_NOTE_ID + " TEXT,"
                    + COLUMN_REMINDER_NOTEBOOK_ID + " TEXT,"
                    + COLUMN_REMINDER_TITLE + " TEXT NOT NULL,"
                    + COLUMN_REMINDER_DESCRIPTION + " TEXT,"
                    + COLUMN_REMINDER_TYPE + " TEXT NOT NULL,"
                    + COLUMN_REMINDER_TRIGGER_TYPE + " TEXT NOT NULL,"
                    + COLUMN_REMINDER_SCHEDULED_AT + " INTEGER,"
                    + COLUMN_REMINDER_LOCATION + " TEXT,"
                    + COLUMN_REMINDER_LATITUDE + " REAL,"
                    + COLUMN_REMINDER_LONGITUDE + " REAL,"
                    + COLUMN_REMINDER_RADIUS + " INTEGER,"
                    + COLUMN_REMINDER_REPEAT_TYPE + " TEXT,"
                    + COLUMN_REMINDER_REPEAT_RULE + " TEXT,"
                    + COLUMN_REMINDER_PRIORITY + " INTEGER,"
                    + COLUMN_REMINDER_IS_COMPLETED + " INTEGER DEFAULT 0,"
                    + COLUMN_REMINDER_IS_NOTIFIED + " INTEGER DEFAULT 0,"
                    + COLUMN_REMINDER_IS_ALL_DAY + " INTEGER DEFAULT 0,"
                    + COLUMN_REMINDER_TIMEZONE + " TEXT,"
                    + COLUMN_REMINDER_CREATED_AT + " INTEGER,"
                    + COLUMN_REMINDER_UPDATED_AT + " INTEGER,"
                    + "FOREIGN KEY(" + COLUMN_REMINDER_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
                    + ")";
            db.execSQL(CREATE_REMINDERS_TABLE);
        }
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

    // ==================== Notebook Methods ====================

    /**
     * Add a new notebook
     */
    public boolean addNotebook(String userId, String title, String color) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOTEBOOK_ID, UUID.randomUUID().toString());
            values.put(COLUMN_USER_ID, userId);
            values.put(COLUMN_TITLE, title);
            values.put(COLUMN_COLOR, color);
            values.put(COLUMN_IS_PINNED, 0);
            values.put(COLUMN_NOTEBOOK_CREATED_AT, getCurrentTimestamp());
            values.put(COLUMN_NOTEBOOK_UPDATED_AT, getCurrentTimestamp());

            long result = db.insert(TABLE_NOTEBOOKS, null, values);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all notebooks for a specific user
     */
    public List<Notebook> getUserNotebooks(String userId) {
        List<Notebook> notebooks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NOTEBOOKS,
                    null,
                    COLUMN_USER_ID + "=?",
                    new String[] { userId },
                    null, null,
                    COLUMN_IS_PINNED + " DESC, " + COLUMN_NOTEBOOK_UPDATED_AT + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notebooks.add(cursorToNotebook(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return notebooks;
    }

    /**
     * Get notebook by ID
     */
    public Notebook getNotebookById(String notebookId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Notebook notebook = null;
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NOTEBOOKS,
                    null,
                    COLUMN_NOTEBOOK_ID + "=?",
                    new String[] { notebookId },
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                notebook = cursorToNotebook(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return notebook;
    }

    /**
     * Update notebook
     */
    public boolean updateNotebook(String notebookId, String title, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_COLOR, color);
        values.put(COLUMN_NOTEBOOK_UPDATED_AT, getCurrentTimestamp());

        int result = db.update(TABLE_NOTEBOOKS, values, COLUMN_NOTEBOOK_ID + "=?", new String[] { notebookId });
        return result > 0;
    }

    /**
     * Toggle notebook pinned status
     */
    public boolean toggleNotebookPin(String notebookId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Notebook notebook = getNotebookById(notebookId);

        if (notebook != null) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_PINNED, notebook.isPinned() ? 0 : 1);
            values.put(COLUMN_NOTEBOOK_UPDATED_AT, getCurrentTimestamp());

            int result = db.update(TABLE_NOTEBOOKS, values, COLUMN_NOTEBOOK_ID + "=?", new String[] { notebookId });
            return result > 0;
        }
        return false;
    }

    /**
     * Delete notebook
     */
    public boolean deleteNotebook(String notebookId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NOTEBOOKS, COLUMN_NOTEBOOK_ID + "=?", new String[] { notebookId });
        return result > 0;
    }

    /**
     * Get notebook count for a user
     */
    public int getNotebookCount(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_NOTEBOOKS + " WHERE " + COLUMN_USER_ID + "=?",
                    new String[] { userId });

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

    public int getRemindersCount(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_REMINDERS + " WHERE " + COLUMN_USER_ID + "=?",
                    new String[] { userId });

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
     * Convert cursor to Notebook object
     */
    private Notebook cursorToNotebook(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTEBOOK_ID));
        String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        String color = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR));
        int isPinnedInt = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PINNED));
        boolean isPinned = isPinnedInt == 1;

        Notebook notebook = new Notebook(id, userId, title, color, isPinned);
        // Set page count for display
        notebook.setPageCount(getPageCount(id));
        return notebook;
    }

    // ==================== Page Methods ====================

    /**
     * Add a new page to a notebook
     */
    public boolean addPage(String notebookId, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Get next page number
            int pageNumber = getPageCount(notebookId) + 1;

            ContentValues values = new ContentValues();
            values.put(COLUMN_PAGE_ID, UUID.randomUUID().toString());
            values.put(COLUMN_PAGE_NOTEBOOK_ID, notebookId);
            values.put(COLUMN_PAGE_TITLE, title);
            values.put(COLUMN_PAGE_CONTENT, content);
            values.put(COLUMN_PAGE_NUMBER, pageNumber);
            values.put(COLUMN_PAGE_CREATED_AT, getCurrentTimestamp());
            values.put(COLUMN_PAGE_UPDATED_AT, getCurrentTimestamp());

            long result = db.insert(TABLE_PAGES, null, values);

            // Update notebook's updated_at timestamp
            if (result != -1) {
                updateNotebookTimestamp(notebookId);
            }

            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all pages for a notebook
     */
    public List<Page> getNotebookPages(String notebookId) {
        List<Page> pages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_PAGES,
                    null,
                    COLUMN_PAGE_NOTEBOOK_ID + "=?",
                    new String[] { notebookId },
                    null, null,
                    COLUMN_PAGE_NUMBER + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    pages.add(cursorToPage(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return pages;
    }

    /**
     * Get page by ID
     */
    public Page getPageById(String pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Page page = null;
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_PAGES,
                    null,
                    COLUMN_PAGE_ID + "=?",
                    new String[] { pageId },
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                page = cursorToPage(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return page;
    }

    /**
     * Update page
     */
    public boolean updatePage(String pageId, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PAGE_TITLE, title);
        values.put(COLUMN_PAGE_CONTENT, content);
        values.put(COLUMN_PAGE_UPDATED_AT, getCurrentTimestamp());

        int result = db.update(TABLE_PAGES, values, COLUMN_PAGE_ID + "=?", new String[] { pageId });

        // Update notebook timestamp
        if (result > 0) {
            Page page = getPageById(pageId);
            if (page != null) {
                updateNotebookTimestamp(page.getNotebookId());
            }
        }

        return result > 0;
    }

    /**
     * Delete page
     */
    public boolean deletePage(String pageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PAGES, COLUMN_PAGE_ID + "=?", new String[] { pageId });
        return result > 0;
    }

    /**
     * Get page count for a notebook
     */
    public int getPageCount(String notebookId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        try {
                cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_PAGES + " WHERE " + COLUMN_PAGE_NOTEBOOK_ID + "=?",
                    new String[] { notebookId });

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // If this notebook uses the multi-page canvas editor, all pages are stored
        // inside a single DB page row as JSON. In that case, try to parse the real
        // page count from the stored JSON.
        if (count == 1) {
            Cursor contentCursor = null;
            try {
                contentCursor = db.query(
                        TABLE_PAGES,
                        new String[] { COLUMN_PAGE_CONTENT },
                        COLUMN_PAGE_NOTEBOOK_ID + "=?",
                        new String[] { notebookId },
                        null,
                        null,
                        COLUMN_PAGE_NUMBER + " ASC",
                        "1");

                if (contentCursor != null && contentCursor.moveToFirst()) {
                    String content = contentCursor.getString(0);
                    if (content != null && !content.isEmpty()) {
                        try {
                            JSONObject json = new JSONObject(content);
                            if (json.has("totalPages")) {
                                int total = json.optInt("totalPages", 1);
                                return Math.max(1, total);
                            }
                            if (json.has("pages")) {
                                JSONArray pages = json.optJSONArray("pages");
                                if (pages != null) {
                                    return Math.max(1, pages.length());
                                }
                            }
                        } catch (Exception ignored) {
                            // Not a multi-page JSON payload; fall back to row count
                        }
                    }
                }
            } finally {
                if (contentCursor != null) {
                    contentCursor.close();
                }
            }
        }

        return count;
    }

    /**
     * Update notebook timestamp
     */
    private void updateNotebookTimestamp(String notebookId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTEBOOK_UPDATED_AT, getCurrentTimestamp());
        db.update(TABLE_NOTEBOOKS, values, COLUMN_NOTEBOOK_ID + "=?", new String[] { notebookId });
    }

    /**
     * Convert cursor to Page object
     */
    private Page cursorToPage(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_ID));
        String notebookId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_NOTEBOOK_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_TITLE));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGE_CONTENT));
        int pageNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGE_NUMBER));

        return new Page(id, notebookId, title, content, pageNumber);
    }

    // ==================== REMINDER CRUD OPERATIONS ====================

    /**
     * Create a new reminder
     */
    public String createReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_REMINDER_ID, reminder.getId());
        values.put(COLUMN_REMINDER_USER_ID, reminder.getUserId());
        values.put(COLUMN_REMINDER_NOTE_ID, reminder.getNoteId());
        values.put(COLUMN_REMINDER_NOTEBOOK_ID, reminder.getNotebookId());
        values.put(COLUMN_REMINDER_TITLE, reminder.getTitle());
        values.put(COLUMN_REMINDER_DESCRIPTION, reminder.getDescription());
        values.put(COLUMN_REMINDER_TYPE, reminder.getType());
        values.put(COLUMN_REMINDER_TRIGGER_TYPE, reminder.getTriggerType());
        values.put(COLUMN_REMINDER_SCHEDULED_AT, reminder.getScheduledAt());
        values.put(COLUMN_REMINDER_LOCATION, reminder.getLocation());
        values.put(COLUMN_REMINDER_LATITUDE, reminder.getLatitude());
        values.put(COLUMN_REMINDER_LONGITUDE, reminder.getLongitude());
        values.put(COLUMN_REMINDER_RADIUS, reminder.getRadiusMeters());
        values.put(COLUMN_REMINDER_REPEAT_TYPE, reminder.getRepeatType());
        values.put(COLUMN_REMINDER_REPEAT_RULE, reminder.getRepeatRule());
        values.put(COLUMN_REMINDER_PRIORITY, reminder.getPriority());
        values.put(COLUMN_REMINDER_IS_COMPLETED, reminder.isCompleted() ? 1 : 0);
        values.put(COLUMN_REMINDER_IS_NOTIFIED, reminder.isNotified() ? 1 : 0);
        values.put(COLUMN_REMINDER_IS_ALL_DAY, reminder.isAllDay() ? 1 : 0);
        values.put(COLUMN_REMINDER_TIMEZONE, reminder.getTimezone());
        values.put(COLUMN_REMINDER_CREATED_AT, reminder.getCreatedAt());
        values.put(COLUMN_REMINDER_UPDATED_AT, reminder.getUpdatedAt());

        long result = db.insert(TABLE_REMINDERS, null, values);
        return result != -1 ? reminder.getId() : null;
    }

    /**
     * Get all reminders for a user
     */
    public List<Reminder> getAllReminders(String userId) {
        List<Reminder> reminders = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_REMINDERS,
                    null,
                    COLUMN_REMINDER_USER_ID + "=?",
                    new String[]{userId},
                    null,
                    null,
                    COLUMN_REMINDER_SCHEDULED_AT + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    reminders.add(cursorToReminder(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return reminders;
    }

    /**
     * Get upcoming reminders (not completed, scheduled in future)
     */
    public List<Reminder> getUpcomingReminders(String userId) {
        List<Reminder> reminders = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            long currentTime = System.currentTimeMillis();
            cursor = db.query(
                    TABLE_REMINDERS,
                    null,
                    COLUMN_REMINDER_USER_ID + "=? AND " + COLUMN_REMINDER_IS_COMPLETED + "=0 AND " +
                            COLUMN_REMINDER_SCHEDULED_AT + ">?",
                    new String[]{userId, String.valueOf(currentTime)},
                    null,
                    null,
                    COLUMN_REMINDER_SCHEDULED_AT + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    reminders.add(cursorToReminder(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return reminders;
    }

    /**
     * Get reminder by ID
     */
    public Reminder getReminderById(String reminderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Reminder reminder = null;

        try {
            cursor = db.query(
                    TABLE_REMINDERS,
                    null,
                    COLUMN_REMINDER_ID + "=?",
                    new String[]{reminderId},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                reminder = cursorToReminder(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return reminder;
    }

    /**
     * Update reminder
     */
    public boolean updateReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_REMINDER_TITLE, reminder.getTitle());
        values.put(COLUMN_REMINDER_DESCRIPTION, reminder.getDescription());
        values.put(COLUMN_REMINDER_TYPE, reminder.getType());
        values.put(COLUMN_REMINDER_TRIGGER_TYPE, reminder.getTriggerType());
        values.put(COLUMN_REMINDER_SCHEDULED_AT, reminder.getScheduledAt());
        values.put(COLUMN_REMINDER_LOCATION, reminder.getLocation());
        values.put(COLUMN_REMINDER_LATITUDE, reminder.getLatitude());
        values.put(COLUMN_REMINDER_LONGITUDE, reminder.getLongitude());
        values.put(COLUMN_REMINDER_RADIUS, reminder.getRadiusMeters());
        values.put(COLUMN_REMINDER_REPEAT_TYPE, reminder.getRepeatType());
        values.put(COLUMN_REMINDER_REPEAT_RULE, reminder.getRepeatRule());
        values.put(COLUMN_REMINDER_PRIORITY, reminder.getPriority());
        values.put(COLUMN_REMINDER_IS_COMPLETED, reminder.isCompleted() ? 1 : 0);
        values.put(COLUMN_REMINDER_IS_NOTIFIED, reminder.isNotified() ? 1 : 0);
        values.put(COLUMN_REMINDER_IS_ALL_DAY, reminder.isAllDay() ? 1 : 0);
        values.put(COLUMN_REMINDER_TIMEZONE, reminder.getTimezone());
        values.put(COLUMN_REMINDER_UPDATED_AT, System.currentTimeMillis());

        int result = db.update(TABLE_REMINDERS, values, COLUMN_REMINDER_ID + "=?",
                new String[]{reminder.getId()});
        return result > 0;
    }

    /**
     * Mark reminder as completed
     */
    public boolean markReminderCompleted(String reminderId, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_IS_COMPLETED, completed ? 1 : 0);
        values.put(COLUMN_REMINDER_UPDATED_AT, System.currentTimeMillis());

        int result = db.update(TABLE_REMINDERS, values, COLUMN_REMINDER_ID + "=?",
                new String[]{reminderId});
        return result > 0;
    }

    /**
     * Delete reminder
     */
    public boolean deleteReminder(String reminderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_REMINDERS, COLUMN_REMINDER_ID + "=?",
                new String[]{reminderId});
        return result > 0;
    }

    /**
     * Convert cursor to Reminder object
     */
    private Reminder cursorToReminder(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_ID));
        String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_USER_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TITLE));
        String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TYPE));

        Reminder reminder = new Reminder(id, userId, title, type);
        
        int noteIdIndex = cursor.getColumnIndex(COLUMN_REMINDER_NOTE_ID);
        if (noteIdIndex >= 0) reminder.setNoteId(cursor.getString(noteIdIndex));
        
        int notebookIdIndex = cursor.getColumnIndex(COLUMN_REMINDER_NOTEBOOK_ID);
        if (notebookIdIndex >= 0) reminder.setNotebookId(cursor.getString(notebookIdIndex));
        
        int descIndex = cursor.getColumnIndex(COLUMN_REMINDER_DESCRIPTION);
        if (descIndex >= 0) reminder.setDescription(cursor.getString(descIndex));
        
        reminder.setTriggerType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TRIGGER_TYPE)));
        reminder.setScheduledAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_SCHEDULED_AT)));
        
        int locationIndex = cursor.getColumnIndex(COLUMN_REMINDER_LOCATION);
        if (locationIndex >= 0) reminder.setLocation(cursor.getString(locationIndex));
        
        reminder.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_LATITUDE)));
        reminder.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_LONGITUDE)));
        reminder.setRadiusMeters(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_RADIUS)));
        
        int repeatTypeIndex = cursor.getColumnIndex(COLUMN_REMINDER_REPEAT_TYPE);
        if (repeatTypeIndex >= 0) reminder.setRepeatType(cursor.getString(repeatTypeIndex));
        
        int repeatRuleIndex = cursor.getColumnIndex(COLUMN_REMINDER_REPEAT_RULE);
        if (repeatRuleIndex >= 0) reminder.setRepeatRule(cursor.getString(repeatRuleIndex));
        
        reminder.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_PRIORITY)));
        reminder.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_IS_COMPLETED)) == 1);
        reminder.setNotified(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_IS_NOTIFIED)) == 1);
        reminder.setAllDay(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_IS_ALL_DAY)) == 1);
        
        int timezoneIndex = cursor.getColumnIndex(COLUMN_REMINDER_TIMEZONE);
        if (timezoneIndex >= 0) reminder.setTimezone(cursor.getString(timezoneIndex));
        
        reminder.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_CREATED_AT)));
        reminder.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_UPDATED_AT)));

        return reminder;
    }
}
