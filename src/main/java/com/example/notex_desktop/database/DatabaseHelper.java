package com.example.notex_desktop.database;

import com.example.notex_desktop.models.User;
import com.example.notex_desktop.models.Notebook;
import com.example.notex_desktop.models.Page;
import com.example.notex_desktop.models.Reminder;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * DatabaseHelper - SQLite database helper for NoteX Desktop
 * Manages users, notebooks, and pages tables with CRUD operations
 */
public class DatabaseHelper {

    private static final String DATABASE_NAME = "notex_desktop.db";
    private static DatabaseHelper instance;
    private Connection connection;

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

    private DatabaseHelper() {
        initializeDatabase();
    }

    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            // Get user home directory for database file
            String userHome = System.getProperty("user.home");
            String dbPath = userHome + "/.notex_desktop/" + DATABASE_NAME;
            
            // Create directory if it doesn't exist
            java.io.File dbDir = new java.io.File(userHome + "/.notex_desktop");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Create users table
            String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + "("
                    + COLUMN_ID + " TEXT PRIMARY KEY,"
                    + COLUMN_USERNAME + " TEXT UNIQUE NOT NULL,"
                    + COLUMN_EMAIL + " TEXT,"
                    + COLUMN_PASSWORD_HASH + " TEXT NOT NULL,"
                    + COLUMN_ROLE + " TEXT NOT NULL,"
                    + COLUMN_CREATED_AT + " TEXT,"
                    + COLUMN_LAST_LOGIN + " TEXT"
                    + ")";
            stmt.execute(CREATE_USERS_TABLE);

            // Create notebooks table
            String CREATE_NOTEBOOKS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTEBOOKS + "("
                    + COLUMN_NOTEBOOK_ID + " TEXT PRIMARY KEY,"
                    + COLUMN_USER_ID + " TEXT NOT NULL,"
                    + COLUMN_TITLE + " TEXT NOT NULL,"
                    + COLUMN_COLOR + " TEXT,"
                    + COLUMN_IS_PINNED + " INTEGER DEFAULT 0,"
                    + COLUMN_NOTEBOOK_CREATED_AT + " TEXT,"
                    + COLUMN_NOTEBOOK_UPDATED_AT + " TEXT,"
                    + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
                    + ")";
            stmt.execute(CREATE_NOTEBOOKS_TABLE);

            // Create pages table
            String CREATE_PAGES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PAGES + "("
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
            stmt.execute(CREATE_PAGES_TABLE);

            // Create reminders table
            String CREATE_REMINDERS_TABLE = "CREATE TABLE IF NOT EXISTS reminders ("
                    + "id TEXT PRIMARY KEY,"
                    + "user_id TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "description TEXT,"
                    + "type TEXT NOT NULL,"
                    + "trigger_type TEXT NOT NULL,"
                    + "trigger_date TEXT,"
                    + "location_name TEXT,"
                    + "location_radius INTEGER DEFAULT 100,"
                    + "repeat_type TEXT DEFAULT 'NONE',"
                    + "priority TEXT DEFAULT 'LOW',"
                    + "ringtone TEXT,"
                    + "is_all_day INTEGER DEFAULT 0,"
                    + "status TEXT DEFAULT 'PENDING',"
                    + "created_at TEXT,"
                    + "updated_at TEXT,"
                    + "FOREIGN KEY(user_id) REFERENCES users(id)"
                    + ")";
            stmt.execute(CREATE_REMINDERS_TABLE);

            // Insert default users if table is empty
            insertDefaultUsersIfEmpty();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertDefaultUsersIfEmpty() {
        try {
            String countQuery = "SELECT COUNT(*) FROM " + TABLE_USERS;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(countQuery)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertDefaultUsers();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertDefaultUsers() {
        insertUser("demo_user", "user@notex.com", "password123", User.UserRole.USER);
        insertUser("admin", "admin@notex.com", "admin123", User.UserRole.ADMIN);
        insertUser("john", "john@notex.com", "john123", User.UserRole.USER);
    }

    private void insertUser(String username, String email, String password, User.UserRole role) {
        String sql = "INSERT INTO " + TABLE_USERS + " (" + COLUMN_ID + ", " + COLUMN_USERNAME + ", "
                + COLUMN_EMAIL + ", " + COLUMN_PASSWORD_HASH + ", " + COLUMN_ROLE + ", "
                + COLUMN_CREATED_AT + ", " + COLUMN_LAST_LOGIN + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, username);
            pstmt.setString(3, email);
            pstmt.setString(4, hashPassword(password));
            pstmt.setString(5, role.name());
            pstmt.setString(6, getCurrentTimestamp());
            pstmt.setString(7, getCurrentTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a new user to the database
     */
    public boolean addUser(String username, String email, String password, User.UserRole role) {
        String sql = "INSERT INTO " + TABLE_USERS + " (" + COLUMN_ID + ", " + COLUMN_USERNAME + ", "
                + COLUMN_EMAIL + ", " + COLUMN_PASSWORD_HASH + ", " + COLUMN_ROLE + ", "
                + COLUMN_CREATED_AT + ", " + COLUMN_LAST_LOGIN + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, username);
            pstmt.setString(3, email);
            pstmt.setString(4, hashPassword(password));
            pstmt.setString(5, role.name());
            pstmt.setString(6, getCurrentTimestamp());
            pstmt.setString(7, getCurrentTimestamp());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get user by username (case-insensitive)
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM " + TABLE_USERS + " WHERE LOWER(" + COLUMN_USERNAME + ") = LOWER(?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Validate user credentials
     */
    public User validateUser(String username, String password, User.UserRole expectedRole) {
        User user = getUserByUsername(username);

        if (user != null) {
            if (user.getPasswordHash().equals(hashPassword(password))) {
                if (user.getRole() == expectedRole) {
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
        String sql = "SELECT * FROM " + TABLE_USERS + " ORDER BY " + COLUMN_CREATED_AT + " DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(resultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Get user count by role
     */
    public int getUserCountByRole(User.UserRole role) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_USERS + " WHERE " + COLUMN_ROLE + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, role.name());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Update last login timestamp
     */
    private void updateLastLogin(String userId) {
        String sql = "UPDATE " + TABLE_USERS + " SET " + COLUMN_LAST_LOGIN + " = ? WHERE " + COLUMN_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, getCurrentTimestamp());
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete user by ID
     */
    public boolean deleteUser(String userId) {
        String sql = "DELETE FROM " + TABLE_USERS + " WHERE " + COLUMN_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user role
     */
    public boolean updateUserRole(String userId, User.UserRole newRole) {
        String sql = "UPDATE " + TABLE_USERS + " SET " + COLUMN_ROLE + " = ? WHERE " + COLUMN_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newRole.name());
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        return getUserByUsername(username) != null;
    }

    /**
     * Convert ResultSet to User object
     */
    private User resultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString(COLUMN_ID);
        String username = rs.getString(COLUMN_USERNAME);
        String email = rs.getString(COLUMN_EMAIL);
        String passwordHash = rs.getString(COLUMN_PASSWORD_HASH);
        String roleStr = rs.getString(COLUMN_ROLE);
        User.UserRole role = User.UserRole.valueOf(roleStr);

        return new User(id, username, email, passwordHash, role);
    }

    /**
     * Get total user count
     */
    public int getTotalUserCount() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_USERS;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ==================== Notebook Methods ====================

    /**
     * Add a new notebook
     */
    public boolean addNotebook(String userId, String title, String color) {
        String sql = "INSERT INTO " + TABLE_NOTEBOOKS + " (" + COLUMN_NOTEBOOK_ID + ", " + COLUMN_USER_ID + ", "
                + COLUMN_TITLE + ", " + COLUMN_COLOR + ", " + COLUMN_IS_PINNED + ", "
                + COLUMN_NOTEBOOK_CREATED_AT + ", " + COLUMN_NOTEBOOK_UPDATED_AT + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, userId);
            pstmt.setString(3, title);
            pstmt.setString(4, color);
            pstmt.setInt(5, 0);
            pstmt.setString(6, getCurrentTimestamp());
            pstmt.setString(7, getCurrentTimestamp());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all notebooks for a specific user
     */
    public List<Notebook> getUserNotebooks(String userId) {
        List<Notebook> notebooks = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTEBOOKS + " WHERE " + COLUMN_USER_ID + " = ? ORDER BY "
                + COLUMN_IS_PINNED + " DESC, " + COLUMN_NOTEBOOK_UPDATED_AT + " DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notebooks.add(resultSetToNotebook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notebooks;
    }

    /**
     * Get notebook by ID
     */
    public Notebook getNotebookById(String notebookId) {
        String sql = "SELECT * FROM " + TABLE_NOTEBOOKS + " WHERE " + COLUMN_NOTEBOOK_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, notebookId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return resultSetToNotebook(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update notebook
     */
    public boolean updateNotebook(String notebookId, String title, String color) {
        String sql = "UPDATE " + TABLE_NOTEBOOKS + " SET " + COLUMN_TITLE + " = ?, " + COLUMN_COLOR + " = ?, "
                + COLUMN_NOTEBOOK_UPDATED_AT + " = ? WHERE " + COLUMN_NOTEBOOK_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, color);
            pstmt.setString(3, getCurrentTimestamp());
            pstmt.setString(4, notebookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Toggle notebook pinned status
     */
    public boolean toggleNotebookPin(String notebookId) {
        Notebook notebook = getNotebookById(notebookId);
        if (notebook != null) {
            String sql = "UPDATE " + TABLE_NOTEBOOKS + " SET " + COLUMN_IS_PINNED + " = ?, "
                    + COLUMN_NOTEBOOK_UPDATED_AT + " = ? WHERE " + COLUMN_NOTEBOOK_ID + " = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, notebook.isPinned() ? 0 : 1);
                pstmt.setString(2, getCurrentTimestamp());
                pstmt.setString(3, notebookId);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Delete notebook and its pages
     */
    public boolean deleteNotebook(String notebookId) {
        // First delete all pages in the notebook
        String deletePagesSql = "DELETE FROM " + TABLE_PAGES + " WHERE " + COLUMN_PAGE_NOTEBOOK_ID + " = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deletePagesSql)) {
            pstmt.setString(1, notebookId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Then delete the notebook
        String sql = "DELETE FROM " + TABLE_NOTEBOOKS + " WHERE " + COLUMN_NOTEBOOK_ID + " = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, notebookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get notebook count for a user
     */
    public int getNotebookCount(String userId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NOTEBOOKS + " WHERE " + COLUMN_USER_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Convert ResultSet to Notebook object
     */
    private Notebook resultSetToNotebook(ResultSet rs) throws SQLException {
        String id = rs.getString(COLUMN_NOTEBOOK_ID);
        String userId = rs.getString(COLUMN_USER_ID);
        String title = rs.getString(COLUMN_TITLE);
        String color = rs.getString(COLUMN_COLOR);
        boolean isPinned = rs.getInt(COLUMN_IS_PINNED) == 1;

        Notebook notebook = new Notebook(id, userId, title, color, isPinned);
        notebook.setPageCount(getPageCount(id));
        return notebook;
    }

    /**
     * Update notebook timestamp
     */
    private void updateNotebookTimestamp(String notebookId) {
        String sql = "UPDATE " + TABLE_NOTEBOOKS + " SET " + COLUMN_NOTEBOOK_UPDATED_AT + " = ? WHERE " + COLUMN_NOTEBOOK_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, getCurrentTimestamp());
            pstmt.setString(2, notebookId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== Page Methods ====================

    /**
     * Add a new page to a notebook
     */
    public boolean addPage(String notebookId, String title, String content) {
        int pageNumber = getPageCount(notebookId) + 1;

        String sql = "INSERT INTO " + TABLE_PAGES + " (" + COLUMN_PAGE_ID + ", " + COLUMN_PAGE_NOTEBOOK_ID + ", "
                + COLUMN_PAGE_TITLE + ", " + COLUMN_PAGE_CONTENT + ", " + COLUMN_PAGE_NUMBER + ", "
                + COLUMN_PAGE_CREATED_AT + ", " + COLUMN_PAGE_UPDATED_AT + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, notebookId);
            pstmt.setString(3, title);
            pstmt.setString(4, content);
            pstmt.setInt(5, pageNumber);
            pstmt.setString(6, getCurrentTimestamp());
            pstmt.setString(7, getCurrentTimestamp());
            
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                updateNotebookTimestamp(notebookId);
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all pages for a notebook
     */
    public List<Page> getNotebookPages(String notebookId) {
        List<Page> pages = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_PAGES + " WHERE " + COLUMN_PAGE_NOTEBOOK_ID + " = ? ORDER BY "
                + COLUMN_PAGE_NUMBER + " ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, notebookId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                pages.add(resultSetToPage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pages;
    }

    /**
     * Get page by ID
     */
    public Page getPageById(String pageId) {
        String sql = "SELECT * FROM " + TABLE_PAGES + " WHERE " + COLUMN_PAGE_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pageId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return resultSetToPage(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update page
     */
    public boolean updatePage(String pageId, String title, String content) {
        String sql = "UPDATE " + TABLE_PAGES + " SET " + COLUMN_PAGE_TITLE + " = ?, " + COLUMN_PAGE_CONTENT + " = ?, "
                + COLUMN_PAGE_UPDATED_AT + " = ? WHERE " + COLUMN_PAGE_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setString(3, getCurrentTimestamp());
            pstmt.setString(4, pageId);
            
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                Page page = getPageById(pageId);
                if (page != null) {
                    updateNotebookTimestamp(page.getNotebookId());
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete page
     */
    public boolean deletePage(String pageId) {
        String sql = "DELETE FROM " + TABLE_PAGES + " WHERE " + COLUMN_PAGE_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get page count for a notebook
     */
    public int getPageCount(String notebookId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_PAGES + " WHERE " + COLUMN_PAGE_NOTEBOOK_ID + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, notebookId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Convert ResultSet to Page object
     */
    private Page resultSetToPage(ResultSet rs) throws SQLException {
        String id = rs.getString(COLUMN_PAGE_ID);
        String notebookId = rs.getString(COLUMN_PAGE_NOTEBOOK_ID);
        String title = rs.getString(COLUMN_PAGE_TITLE);
        String content = rs.getString(COLUMN_PAGE_CONTENT);
        int pageNumber = rs.getInt(COLUMN_PAGE_NUMBER);

        return new Page(id, notebookId, title, content, pageNumber);
    }

    /**
     * Simple password hashing (use BCrypt in production)
     */
    private String hashPassword(String password) {
        return "hashed_" + password;
    }

    /**
     * Get current timestamp as string
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ==================== REMINDER OPERATIONS ====================

    /**
     * Add a new reminder
     */
    public boolean addReminder(Reminder reminder) {
        String sql = "INSERT INTO reminders (id, user_id, title, description, type, trigger_type, "
                + "trigger_date, location_name, location_radius, repeat_type, priority, ringtone, "
                + "is_all_day, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String id = UUID.randomUUID().toString();
            reminder.setId(id);
            
            pstmt.setString(1, id);
            pstmt.setString(2, reminder.getUserId());
            pstmt.setString(3, reminder.getTitle());
            pstmt.setString(4, reminder.getDescription());
            pstmt.setString(5, reminder.getType().name());
            pstmt.setString(6, reminder.getTriggerType().name());
            pstmt.setString(7, reminder.getTriggerDate());
            pstmt.setString(8, reminder.getLocationName());
            pstmt.setInt(9, reminder.getLocationRadius());
            pstmt.setString(10, reminder.getRepeatType().name());
            pstmt.setString(11, reminder.getPriority().name());
            pstmt.setString(12, reminder.getRingtone());
            pstmt.setInt(13, reminder.isAllDay() ? 1 : 0);
            pstmt.setString(14, reminder.getStatus().name());
            pstmt.setString(15, getCurrentTimestamp());
            pstmt.setString(16, getCurrentTimestamp());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing reminder
     */
    public boolean updateReminder(Reminder reminder) {
        String sql = "UPDATE reminders SET title = ?, description = ?, type = ?, trigger_type = ?, "
                + "trigger_date = ?, location_name = ?, location_radius = ?, repeat_type = ?, "
                + "priority = ?, ringtone = ?, is_all_day = ?, status = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reminder.getTitle());
            pstmt.setString(2, reminder.getDescription());
            pstmt.setString(3, reminder.getType().name());
            pstmt.setString(4, reminder.getTriggerType().name());
            pstmt.setString(5, reminder.getTriggerDate());
            pstmt.setString(6, reminder.getLocationName());
            pstmt.setInt(7, reminder.getLocationRadius());
            pstmt.setString(8, reminder.getRepeatType().name());
            pstmt.setString(9, reminder.getPriority().name());
            pstmt.setString(10, reminder.getRingtone());
            pstmt.setInt(11, reminder.isAllDay() ? 1 : 0);
            pstmt.setString(12, reminder.getStatus().name());
            pstmt.setString(13, getCurrentTimestamp());
            pstmt.setString(14, reminder.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a reminder
     */
    public boolean deleteReminder(String reminderId) {
        String sql = "DELETE FROM reminders WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reminderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get reminder by ID
     */
    public Reminder getReminderById(String reminderId) {
        String sql = "SELECT * FROM reminders WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reminderId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToReminder(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get all reminders for a user
     */
    public List<Reminder> getUserReminders(String userId) {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT * FROM reminders WHERE user_id = ? ORDER BY trigger_date ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reminders.add(resultSetToReminder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reminders;
    }

    /**
     * Get reminders by type for a user
     */
    public List<Reminder> getUserRemindersByType(String userId, Reminder.ReminderType type) {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT * FROM reminders WHERE user_id = ? AND type = ? ORDER BY trigger_date ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, type.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reminders.add(resultSetToReminder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reminders;
    }

    /**
     * Get reminders for a specific date
     */
    public List<Reminder> getUserRemindersByDate(String userId, String date) {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT * FROM reminders WHERE user_id = ? AND trigger_date LIKE ? ORDER BY trigger_date ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, date + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reminders.add(resultSetToReminder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reminders;
    }

    /**
     * Mark reminder as completed
     */
    public boolean markReminderCompleted(String reminderId) {
        String sql = "UPDATE reminders SET status = 'COMPLETED', updated_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, getCurrentTimestamp());
            pstmt.setString(2, reminderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get reminder count for a user
     */
    public int getUserReminderCount(String userId) {
        String sql = "SELECT COUNT(*) FROM reminders WHERE user_id = ? AND status = 'PENDING'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Convert ResultSet to Reminder object
     */
    private Reminder resultSetToReminder(ResultSet rs) throws SQLException {
        Reminder reminder = new Reminder();
        reminder.setId(rs.getString("id"));
        reminder.setUserId(rs.getString("user_id"));
        reminder.setTitle(rs.getString("title"));
        reminder.setDescription(rs.getString("description"));
        reminder.setType(Reminder.ReminderType.valueOf(rs.getString("type")));
        reminder.setTriggerType(Reminder.TriggerType.valueOf(rs.getString("trigger_type")));
        reminder.setTriggerDate(rs.getString("trigger_date"));
        reminder.setLocationName(rs.getString("location_name"));
        reminder.setLocationRadius(rs.getInt("location_radius"));
        reminder.setRepeatType(Reminder.RepeatType.valueOf(rs.getString("repeat_type")));
        reminder.setPriority(Reminder.Priority.valueOf(rs.getString("priority")));
        reminder.setRingtone(rs.getString("ringtone"));
        reminder.setAllDay(rs.getInt("is_all_day") == 1);
        reminder.setStatus(Reminder.Status.valueOf(rs.getString("status")));
        reminder.setCreatedAt(rs.getString("created_at"));
        reminder.setUpdatedAt(rs.getString("updated_at"));
        return reminder;
    }

    /**
     * Close the database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
