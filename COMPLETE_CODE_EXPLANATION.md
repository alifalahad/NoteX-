# NoteX++ - Complete Code Explanation

This document explains every major code file in the NoteX++ project in detail.

---

## TABLE OF CONTENTS

1. [Database Layer](#1-database-layer)
2. [Models](#2-models)
3. [Utilities](#3-utilities)
4. [Activities](#4-activities)
5. [Adapters](#5-adapters)
6. [Layouts](#6-layouts)
7. [Application Flow](#7-application-flow)

---

## 1. DATABASE LAYER

### DatabaseHelper.java

**Purpose**: Manages all database operations for the application.

**Key Concepts**:
- **Singleton Pattern**: Only one instance exists throughout the app
- **SQLiteOpenHelper**: Android's built-in class for database management
- **CRUD Operations**: Create, Read, Update, Delete

```java
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notex.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table and column names
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    // ... more columns
```

**Explanation**:
- `DATABASE_NAME`: The SQLite file name stored on device
- `DATABASE_VERSION`: If you change schema, increment this to trigger `onUpgrade()`
- Constants define table/column names to avoid typos

#### onCreate() Method

```java
@Override
public void onCreate(SQLiteDatabase db) {
    String CREATE_USERS_TABLE = "CREATE TABLE users ("
            + "id TEXT PRIMARY KEY,"
            + "username TEXT UNIQUE NOT NULL,"
            + "email TEXT,"
            + "password_hash TEXT NOT NULL,"
            + "role TEXT NOT NULL,"
            + "created_at TEXT,"
            + "last_login TEXT"
            + ")";
    db.execSQL(CREATE_USERS_TABLE);
    insertDefaultUsers(db);
}
```

**What it does**:
1. Called **first time** the database is created
2. Creates the `users` table with SQL
3. Inserts default users (admin, demo_user, john)

**SQL Breakdown**:
- `TEXT PRIMARY KEY`: Unique identifier (UUID)
- `UNIQUE NOT NULL`: Username must be unique and can't be empty
- `TEXT`: Stores strings
- `execSQL()`: Executes raw SQL commands

#### insertDefaultUsers() Method

```java
private void insertDefaultUsers(SQLiteDatabase db) {
    insertUser(db, "demo_user", "user@notex.com", "password123", User.UserRole.USER);
    insertUser(db, "admin", "admin@notex.com", "admin123", User.UserRole.ADMIN);
    insertUser(db, "john", "john@notex.com", "john123", User.UserRole.USER);
}
```

**What it does**:
- Creates 3 test accounts when app first runs
- 1 admin, 2 regular users
- Passwords are hashed before storing

#### addUser() Method

```java
public boolean addUser(String username, String email, String password, User.UserRole role) {
    SQLiteDatabase db = this.getWritableDatabase();
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
}
```

**Explanation**:
- `ContentValues`: Android's way to insert data (like a HashMap)
- `UUID.randomUUID()`: Generates unique ID like "a1b2c3d4-..."
- `hashPassword()`: Converts plain password to hash
- `role.name()`: Converts enum to string ("USER" or "ADMIN")
- `insert()`: Returns -1 if failed, otherwise row ID
- `result != -1`: Returns true if successful

#### validateUser() Method

```java
public User validateUser(String username, String password, User.UserRole expectedRole) {
    SQLiteDatabase db = this.getReadableDatabase();
    String hashedPassword = hashPassword(password);
    
    Cursor cursor = db.query(
        TABLE_USERS,
        null, // all columns
        COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD_HASH + "=? AND " + COLUMN_ROLE + "=?",
        new String[]{username, hashedPassword, expectedRole.name()},
        null, null, null
    );
    
    User user = null;
    if (cursor != null && cursor.moveToFirst()) {
        user = cursorToUser(cursor);
        updateLastLogin(user.getId());
    }
    
    if (cursor != null) cursor.close();
    return user;
}
```

**What it does**:
1. Hashes the entered password
2. Queries database for matching username, password hash, AND role
3. If found, converts database row to User object
4. Updates last login timestamp
5. Returns User object or null if not found

**Cursor Explained**:
- `Cursor`: Like a pointer to database results
- `moveToFirst()`: Moves to first result (returns false if no results)
- `cursorToUser()`: Extracts data from cursor into User object
- `close()`: Must close cursor to free memory

#### getAllUsers() Method

```java
public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    SQLiteDatabase db = this.getReadableDatabase();
    
    Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null, null);
    
    if (cursor != null && cursor.moveToFirst()) {
        do {
            users.add(cursorToUser(cursor));
        } while (cursor.moveToNext());
        cursor.close();
    }
    
    return users;
}
```

**What it does**:
1. Creates empty list
2. Queries ALL users (no WHERE clause)
3. Loop through each result with `do...while`
4. `moveToNext()`: Moves cursor to next row, returns false when done
5. Returns list of all users

#### deleteUser() Method

```java
public boolean deleteUser(String userId) {
    SQLiteDatabase db = this.getWritableDatabase();
    int result = db.delete(TABLE_USERS, COLUMN_ID + "=?", new String[]{userId});
    return result > 0;
}
```

**What it does**:
- Deletes user by ID
- `delete()`: Returns number of rows deleted
- `result > 0`: True if at least one row deleted

#### updateUserRole() Method

```java
public boolean updateUserRole(String userId, User.UserRole newRole) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(COLUMN_ROLE, newRole.name());
    
    int result = db.update(TABLE_USERS, values, COLUMN_ID + "=?", new String[]{userId});
    return result > 0;
}
```

**What it does**:
- Changes user's role (USER ↔ ADMIN)
- Only updates the role column
- Returns true if successful

#### hashPassword() Method

```java
private String hashPassword(String password) {
    return String.valueOf(password.hashCode());
}
```

**Current Implementation**:
- Uses Java's built-in `hashCode()`
- **NOT secure** for production (just for demo)
- **Should use**: BCrypt, PBKDF2, or Argon2

**Why hash passwords?**:
- If database is stolen, passwords aren't visible
- Can't reverse a hash to get original password
- Same password always produces same hash

---

## 2. MODELS

### User.java

**Purpose**: Represents a user in the system (data structure).

```java
public class User {
    public enum UserRole {
        USER,
        ADMIN
    }
    
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private UserRole role;
    private Date createdAt;
    private Date lastLogin;
```

**Explanation**:
- **Enum**: `UserRole` can only be USER or ADMIN (type-safe)
- **Private fields**: Can't be accessed directly from outside
- **Must use getters/setters**: Encapsulation principle

#### Constructor

```java
public User(String id, String username, String email, String passwordHash, UserRole role) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.createdAt = new Date();
    this.lastLogin = new Date();
}
```

**What it does**:
- Creates a new User object
- `this.id`: "this" refers to the current object's field
- `new Date()`: Sets to current date/time

#### isAdmin() Method

```java
public boolean isAdmin() {
    return role == UserRole.ADMIN;
}
```

**Convenience method**:
- Instead of: `if (user.getRole() == User.UserRole.ADMIN)`
- Use: `if (user.isAdmin())`
- Cleaner, more readable code

---

## 3. UTILITIES

### AuthManager.java

**Purpose**: Centralized authentication and session management.

**Singleton Pattern**:
```java
private static AuthManager instance;

public static synchronized AuthManager getInstance(Context context) {
    if (instance == null) {
        instance = new AuthManager(context.getApplicationContext());
    }
    return instance;
}
```

**Why Singleton?**:
- Only ONE AuthManager exists in entire app
- All activities share the same instance
- Maintains consistent state across app

#### SharedPreferences

```java
private SharedPreferences sharedPreferences;

private AuthManager(Context context) {
    sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    databaseHelper = DatabaseHelper.getInstance(context);
    loadUserFromPreferences();
}
```

**What is SharedPreferences?**:
- Stores simple key-value pairs on device
- Like a permanent HashMap
- Survives app restarts
- Stores: strings, ints, booleans, etc.

**What we store**:
- user_id
- username
- email
- role
- is_logged_in

#### login() Method

```java
public boolean login(String username, String password, User.UserRole expectedRole) {
    String normalizedUsername = username.toLowerCase().trim();
    
    User user = databaseHelper.validateUser(normalizedUsername, password, expectedRole);
    
    if (user != null) {
        currentUser = user;
        saveUserToPreferences();
        return true;
    }
    
    return false;
}
```

**Process**:
1. Normalize username (lowercase, remove spaces)
2. Validate against database
3. If valid, store as `currentUser`
4. Save to SharedPreferences for persistence
5. Return true/false for success

#### saveUserToPreferences()

```java
private void saveUserToPreferences() {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(KEY_USER_ID, currentUser.getId());
    editor.putString(KEY_USERNAME, currentUser.getUsername());
    editor.putString(KEY_EMAIL, currentUser.getEmail());
    editor.putString(KEY_ROLE, currentUser.getRole().name());
    editor.putBoolean(KEY_IS_LOGGED_IN, true);
    editor.apply();
}
```

**What it does**:
- `edit()`: Enter edit mode
- `putString()`: Save string value
- `putBoolean()`: Save true/false
- `apply()`: Commit changes asynchronously

#### loadUserFromPreferences()

```java
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
```

**What it does**:
- Called when app starts
- Checks if user was logged in
- Reconstructs User object from saved data
- If successful, user stays logged in

#### logout()

```java
public void logout() {
    currentUser = null;
    clearPreferences();
}

private void clearPreferences() {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.clear();
    editor.apply();
}
```

**What it does**:
- Sets currentUser to null
- Clears ALL SharedPreferences data
- Next app start will go to login screen

---

## 4. ACTIVITIES

### 4.1 SplashActivity.java

**Purpose**: Entry point of the app, decides where to route user.

```java
public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 2000; // 2 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthManager authManager = AuthManager.getInstance(this);
            
            Intent intent;
            if (authManager.isLoggedIn()) {
                if (authManager.getCurrentUser().isAdmin()) {
                    intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, UserHomeActivity.class);
                }
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
```

**Flow**:
1. Show splash screen for 2 seconds
2. Check if user is logged in
3. Route to appropriate screen:
   - Not logged in → LoginActivity
   - Logged in as Admin → AdminDashboardActivity
   - Logged in as User → UserHomeActivity
4. `finish()`: Closes splash so user can't go back to it

**Handler Explained**:
- `Handler`: Schedules code to run later
- `postDelayed()`: Runs after specified milliseconds
- `Looper.getMainLooper()`: Runs on UI thread (safe for UI operations)

---

### 4.2 LoginActivity.java

**Purpose**: Handles user authentication with role selection.

#### View Binding

```java
private ActivityLoginBinding binding;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityLoginBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    
    authManager = AuthManager.getInstance(this);
    setupClickListeners();
}
```

**View Binding Benefits**:
- `binding.etUsername`: Type-safe access to views
- No `findViewById()` needed
- Compile-time checking (catches errors before running)
- `binding.getRoot()`: Returns the root layout view

#### setupClickListeners()

```java
private void setupClickListeners() {
    binding.btnLoginUser.setOnClickListener(v -> {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        if (validateInput(username, password)) {
            attemptLogin(username, password, User.UserRole.USER);
        }
    });
    
    binding.btnLoginAdmin.setOnClickListener(v -> {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        if (validateInput(username, password)) {
            attemptLogin(username, password, User.UserRole.ADMIN);
        }
    });
}
```

**Lambda Expression**: `v -> { ... }`
- Shorter way to write click listeners
- `v`: The view that was clicked
- Same as: `new View.OnClickListener() { public void onClick(View v) { ... } }`

**Process**:
1. Get username and password from input fields
2. `trim()`: Remove leading/trailing spaces
3. Validate inputs aren't empty
4. Attempt login with appropriate role

#### attemptLogin()

```java
private void attemptLogin(String username, String password, User.UserRole role) {
    boolean success = authManager.login(username, password, role);
    
    if (success) {
        Toast.makeText(this, "Welcome, " + username + "!", Toast.LENGTH_SHORT).show();
        
        Intent intent;
        if (role == User.UserRole.ADMIN) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, UserHomeActivity.class);
        }
        startActivity(intent);
        finish();
    } else {
        String errorMessage = getString(R.string.error_invalid_credentials);
        
        if (authManager.login(username, password,
                role == User.UserRole.USER ? User.UserRole.ADMIN : User.UserRole.USER)) {
            authManager.logout();
            errorMessage = getString(R.string.error_wrong_role);
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
}
```

**Logic**:
1. Try to login with given credentials and role
2. **If successful**:
   - Show welcome message
   - Route to appropriate home screen
   - `finish()`: Close login screen
3. **If failed**:
   - Check if credentials work with OTHER role
   - If yes: Show "wrong role" error
   - If no: Show "invalid credentials" error

**Why check other role?**:
- User might exist but clicked wrong button
- Better error message for user experience

---

### 4.3 RegisterActivity.java

**Purpose**: Creates new user accounts.

```java
private void attemptRegister() {
    String username = binding.etUsername.getText().toString().trim();
    String email = binding.etEmail.getText().toString().trim();
    String password = binding.etPassword.getText().toString().trim();
    String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
    
    if (!validateInput(username, email, password, confirmPassword)) {
        return;
    }
    
    boolean success = authManager.register(username, email, password, User.UserRole.USER);
    
    if (success) {
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
        authManager.login(username, password, User.UserRole.USER);
        
        Intent intent = new Intent(RegisterActivity.this, UserHomeActivity.class);
        startActivity(intent);
        finish();
    } else {
        Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
    }
}
```

**Process**:
1. Get all input values
2. Validate (username not empty, passwords match, etc.)
3. Try to register with database
4. If successful:
   - Auto-login the new user
   - Go to UserHomeActivity
5. If failed:
   - Show "username exists" error

---

### 4.4 UserHomeActivity.java

**Purpose**: Home screen for regular users.

#### Security Check

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityUserHomeBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    
    authManager = AuthManager.getInstance(this);
    currentUser = authManager.getCurrentUser();
    
    if (currentUser == null || currentUser.isAdmin()) {
        redirectToLogin();
        return;
    }
    
    setupToolbar();
    setupUI();
    setupClickListeners();
    loadUserData();
}
```

**Security Logic**:
- If no user logged in (`currentUser == null`): Redirect to login
- If user is admin (`isAdmin()`): Redirect to login
- Only regular users can access this screen

#### setupToolbar()

```java
private void setupToolbar() {
    setSupportActionBar(binding.toolbar);
    if (getSupportActionBar() != null) {
        getSupportActionBar().setTitle(getString(R.string.app_name));
    }
}
```

**What it does**:
- Sets up the toolbar at top of screen
- `setSupportActionBar()`: Makes it act as action bar
- Sets title to app name

#### FAB Click Listener

```java
binding.fabNewNote.setOnClickListener(v -> {
    Toast.makeText(this, "Create new note - Coming soon!", Toast.LENGTH_SHORT).show();
});
```

**FAB**: Floating Action Button
- Round button that floats over content
- For primary action (creating note)
- Shows placeholder message for now

#### onBackPressed()

```java
@Override
public void onBackPressed() {
    moveTaskToBack(true);
}
```

**Why override?**:
- Default: Goes back to previous activity (login screen)
- We want: Minimize app instead
- `moveTaskToBack(true)`: Sends app to background

---

### 4.5 AdminDashboardActivity.java

**Purpose**: Home screen for administrators.

#### Security Check

```java
if (currentUser == null || !currentUser.isAdmin()) {
    Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
    redirectToLogin();
    return;
}
```

**Opposite of UserHomeActivity**:
- Only admins allowed here
- Regular users are denied access

#### loadSystemStats()

```java
private void loadSystemStats() {
    AuthManager authManager = AuthManager.getInstance(this);
    DatabaseHelper db = authManager.getDatabaseHelper();
    
    int totalUsers = db.getTotalUserCount();
    binding.tvTotalUsers.setText(String.valueOf(totalUsers));
    
    binding.tvTotalNotes.setText("0");
}
```

**What it does**:
1. Get database helper instance
2. Query total user count
3. Display on screen
4. Notes count is 0 (feature not implemented yet)

**String.valueOf()**:
- TextView needs String, not int
- Converts number to text

#### onCreateOptionsMenu()

```java
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_admin_dashboard, menu);
    return true;
}
```

**What it does**:
- Loads menu from XML file
- Creates action items in toolbar (refresh, notifications, logout)

#### onOptionsItemSelected()

```java
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    
    if (id == R.id.action_refresh) {
        loadSystemStats();
        return true;
    } else if (id == R.id.action_logout) {
        handleLogout();
        return true;
    }
    
    return super.onOptionsItemSelected(item);
}
```

**What it does**:
- Handles menu item clicks
- `getItemId()`: Gets the menu item ID
- Performs appropriate action
- `return true`: Event was handled

---

### 4.6 UserListActivity.java

**Purpose**: Displays all users for admin management.

#### Implements Interface

```java
public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener
```

**Interface**: Contract that class must implement certain methods
- Must implement `onUserClick(User user)`
- Allows adapter to callback to this activity

#### setupRecyclerView()

```java
private void setupRecyclerView() {
    userAdapter = new UserAdapter(this);
    binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
    binding.recyclerViewUsers.setAdapter(userAdapter);
}
```

**RecyclerView Components**:
1. **Adapter**: Provides data to RecyclerView
2. **LayoutManager**: Decides how to display items (vertical list, grid, etc.)
3. `this`: Pass activity as click listener

#### loadUsers()

```java
private void loadUsers() {
    List<User> users = databaseHelper.getAllUsers();
    
    if (users.isEmpty()) {
        binding.recyclerViewUsers.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
    } else {
        binding.recyclerViewUsers.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        userAdapter.setUsers(users);
    }
    
    int totalUsers = databaseHelper.getTotalUserCount();
    int adminCount = databaseHelper.getUserCountByRole(User.UserRole.ADMIN);
    
    binding.tvTotalUsers.setText(String.valueOf(totalUsers));
    binding.tvAdminCount.setText(String.valueOf(adminCount));
}
```

**Process**:
1. Get all users from database
2. If empty: Show "no users" message
3. If not empty: Display in RecyclerView
4. Update statistics at top

**View Visibility**:
- `View.GONE`: Invisible and takes no space
- `View.VISIBLE`: Visible
- `View.INVISIBLE`: Invisible but takes space

#### onUserClick() - Interface Implementation

```java
@Override
public void onUserClick(User user) {
    showUserDetailsDialog(user);
}
```

**Called when**: User clicks on any user in the list
**Does**: Shows dialog with actions

#### showUserDetailsDialog()

```java
private void showUserDetailsDialog(User user) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Manage User: " + user.getUsername());
    
    String message = "Username: " + user.getUsername() + "\n" +
            "Email: " + (user.getEmail() != null ? user.getEmail() : "N/A") + "\n" +
            "Role: " + user.getRole().name() + "\n" +
            "ID: " + user.getId();
    
    builder.setMessage(message);
    
    if (user.isAdmin()) {
        builder.setPositiveButton("Demote to User", (dialog, which) -> {
            confirmRoleChange(user, User.UserRole.USER);
        });
        builder.setNeutralButton("Close", null);
    } else {
        builder.setPositiveButton("Promote to Admin", (dialog, which) -> {
            confirmRoleChange(user, User.UserRole.ADMIN);
        });
        builder.setNegativeButton("Delete User", (dialog, which) -> {
            confirmDeleteUser(user);
        });
        builder.setNeutralButton("Close", null);
    }
    
    builder.show();
}
```

**AlertDialog Builder Pattern**:
- `setTitle()`: Dialog title
- `setMessage()`: Dialog content
- `setPositiveButton()`: Green/confirm button
- `setNegativeButton()`: Red/destructive button
- `setNeutralButton()`: Gray/cancel button
- `show()`: Display the dialog

**Conditional Buttons**:
- Admin: Only shows "Demote" option
- User: Shows "Promote" and "Delete" options

#### confirmDeleteUser()

```java
private void confirmDeleteUser(User user) {
    new AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete " + user.getUsername() + "?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                boolean success = databaseHelper.deleteUser(user.getId());
                if (success) {
                    Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                    loadUsers();
                } else {
                    Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
}
```

**Two-step Process**:
1. First dialog: Shows user details and actions
2. Second dialog: Confirms destructive action
3. If confirmed: Delete from database and refresh list

---

## 5. ADAPTERS

### UserAdapter.java

**Purpose**: Converts User objects into views for RecyclerView.

#### Interface Definition

```java
public interface OnUserClickListener {
    void onUserClick(User user);
}
```

**Callback Pattern**:
- Activity implements this interface
- Adapter calls this method when user clicks
- Separates concerns (adapter handles display, activity handles logic)

#### Constructor

```java
public UserAdapter(OnUserClickListener listener) {
    this.users = new ArrayList<>();
    this.listener = listener;
}
```

**What it does**:
- Stores reference to activity (for callbacks)
- Initializes empty user list

#### ViewHolder Pattern

```java
class UserViewHolder extends RecyclerView.ViewHolder {
    private ImageView ivUserIcon;
    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvUserId;
    private TextView tvPassword;
    private Chip chipRole;
    
    public UserViewHolder(@NonNull View itemView) {
        super(itemView);
        ivUserIcon = itemView.findViewById(R.id.ivUserIcon);
        tvUsername = itemView.findViewById(R.id.tvUsername);
        tvEmail = itemView.findViewById(R.id.tvEmail);
        tvUserId = itemView.findViewById(R.id.tvUserId);
        tvPassword = itemView.findViewById(R.id.tvPassword);
        chipRole = itemView.findViewById(R.id.chipRole);
    }
}
```

**ViewHolder Purpose**:
- Holds references to views in one list item
- Called once per item (not every scroll)
- Performance optimization (avoids repeated findViewById)

#### bind() Method

```java
public void bind(User user) {
    tvUsername.setText(user.getUsername());
    tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
    tvUserId.setText("ID: " + user.getId().substring(0, Math.min(8, user.getId().length())));
    
    chipRole.setText(user.getRole().name());
    if (user.isAdmin()) {
        chipRole.setChipBackgroundColorResource(R.color.secondary);
        ivUserIcon.setImageResource(R.drawable.ic_admin);
    } else {
        chipRole.setChipBackgroundColorResource(R.color.primary);
        ivUserIcon.setImageResource(R.drawable.ic_user);
    }
    
    itemView.setOnClickListener(v -> {
        if (listener != null) {
            listener.onUserClick(user);
        }
    });
}
```

**What it does**:
1. Sets text values from user object
2. Truncates ID to 8 characters for display
3. Sets role chip color:
   - Admin: Secondary color (pink)
   - User: Primary color (blue)
4. Sets appropriate icon
5. Sets click listener to callback to activity

**Ternary Operator**: `condition ? valueIfTrue : valueIfFalse`
- `user.getEmail() != null ? user.getEmail() : "No email"`
- If email exists, show it; otherwise show "No email"

#### RecyclerView Methods

```java
@Override
public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user, parent, false);
    return new UserViewHolder(view);
}

@Override
public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
    User user = users.get(position);
    holder.bind(user);
}

@Override
public int getItemCount() {
    return users.size();
}
```

**RecyclerView Lifecycle**:
1. `getItemCount()`: "How many items?"
2. `onCreateViewHolder()`: "Create a view holder" (only when needed)
3. `onBindViewHolder()`: "Fill this holder with data at position X"

**Efficiency**:
- ViewHolders are reused as you scroll
- Only creates as many as visible on screen + buffer
- Just rebinds data to existing views

---

## 6. LAYOUTS

### 6.1 activity_user_home.xml

**Structure**:
```xml
CoordinatorLayout (root)
├── AppBarLayout
│   └── Toolbar
├── NestedScrollView (main content)
│   └── LinearLayout
│       ├── MaterialCardView (Welcome)
│       ├── MaterialCardView (Stats)
│       └── MaterialCardView items (Actions)
└── FloatingActionButton
```

**Key Attributes**:
- `layout_width="match_parent"`: Fill parent width
- `layout_height="wrap_content"`: Height fits content
- `layout_weight="1"`: Takes remaining space
- `android:padding="16dp"`: 16dp padding all sides
- `app:cardCornerRadius="12dp"`: Rounded corners

**Material Components**:
- `MaterialCardView`: Elevated card with shadow
- `FloatingActionButton`: Round floating button
- `Toolbar`: App bar at top

---

### 6.2 item_user.xml

**Layout for each user in the list**:

```xml
MaterialCardView
└── LinearLayout (horizontal)
    ├── ImageView (user icon)
    ├── LinearLayout (vertical - user info)
    │   ├── TextView (username)
    │   ├── TextView (email)
    │   ├── TextView (user ID)
    │   └── TextView (password)
    └── Chip (role badge)
```

**Layout Weights**:
```xml
<LinearLayout
    android:layout_width="0dp"
    android:layout_weight="1">
```
- `layout_width="0dp"`: Let weight determine width
- `layout_weight="1"`: Takes all available space
- Other views have `wrap_content`: Take only what they need

**Result**: User info expands, icon and chip stay fixed size

---

## 7. APPLICATION FLOW

### 7.1 First Launch Flow

```
App Starts
    ↓
SplashActivity.onCreate()
    ↓
AuthManager.getInstance()
    ↓
Load from SharedPreferences
    ↓
No saved session
    ↓
isLoggedIn() = false
    ↓
Route to LoginActivity
    ↓
User enters credentials + clicks role
    ↓
validateInput() - check not empty
    ↓
attemptLogin()
    ↓
AuthManager.login()
    ↓
DatabaseHelper.validateUser()
    ↓
Query: SELECT * WHERE username=? AND password_hash=? AND role=?
    ↓
Found! → Return User object
    ↓
AuthManager sets currentUser
    ↓
saveUserToPreferences()
    ↓
SharedPreferences stores session
    ↓
Route to UserHomeActivity or AdminDashboardActivity
    ↓
finish() LoginActivity
```

### 7.2 Subsequent Launch Flow

```
App Starts
    ↓
SplashActivity.onCreate()
    ↓
AuthManager.getInstance()
    ↓
Load from SharedPreferences
    ↓
Found saved session!
    ↓
Reconstruct User object
    ↓
isLoggedIn() = true
    ↓
getCurrentUser().isAdmin()?
    ├─ Yes → AdminDashboardActivity
    └─ No → UserHomeActivity
```

### 7.3 Admin Managing Users Flow

```
AdminDashboardActivity
    ↓
Click "Manage Users"
    ↓
Intent to UserListActivity
    ↓
onCreate()
    ↓
setupRecyclerView()
    ↓
loadUsers()
    ↓
DatabaseHelper.getAllUsers()
    ↓
Query: SELECT * FROM users
    ↓
Returns List<User>
    ↓
userAdapter.setUsers(users)
    ↓
notifyDataSetChanged()
    ↓
RecyclerView displays list
    ↓
User clicks on a user
    ↓
Adapter calls listener.onUserClick(user)
    ↓
Activity receives callback
    ↓
showUserDetailsDialog(user)
    ↓
Shows AlertDialog with actions
    ↓
User clicks "Delete User"
    ↓
confirmDeleteUser(user)
    ↓
Shows confirmation dialog
    ↓
User confirms
    ↓
DatabaseHelper.deleteUser(userId)
    ↓
Execute: DELETE FROM users WHERE id=?
    ↓
Returns true if successful
    ↓
Toast: "User deleted successfully"
    ↓
loadUsers() - refresh list
    ↓
RecyclerView updates
```

---

## 8. KEY PROGRAMMING CONCEPTS USED

### 8.1 Object-Oriented Programming

**Encapsulation**:
```java
private String username;

public String getUsername() {
    return username;
}

public void setUsername(String username) {
    this.username = username;
}
```
- Private fields, public methods
- Control access to data
- Can add validation in setters

**Inheritance**:
```java
public class UserListActivity extends AppCompatActivity
```
- `extends`: Inherits from AppCompatActivity
- Gets all methods from parent class
- Can override methods

**Interfaces**:
```java
public interface OnUserClickListener {
    void onUserClick(User user);
}
```
- Contract that must be implemented
- Enables callbacks
- Loose coupling between classes

**Enums**:
```java
public enum UserRole {
    USER,
    ADMIN
}
```
- Type-safe constants
- Can't use invalid values
- Prevents typos

### 8.2 Design Patterns

**Singleton**:
- One instance per app
- Global access point
- Used in: AuthManager, DatabaseHelper

**ViewHolder**:
- Caches view references
- Improves performance
- Used in: RecyclerView adapters

**Builder**:
- Step-by-step object construction
- Used in: AlertDialog.Builder

**Observer** (kind of):
- RecyclerView adapter notifies when data changes
- `notifyDataSetChanged()`

### 8.3 Android Concepts

**Activity Lifecycle**:
```java
onCreate() → onStart() → onResume() → [RUNNING]
                                           ↓
onPause() → onStop() → onDestroy()
```

**Intent**:
```java
Intent intent = new Intent(SourceActivity.this, TargetActivity.class);
startActivity(intent);
```
- Message to Android system
- "Start this activity"
- Can pass data between activities

**View Binding**:
```java
binding = ActivityLoginBinding.inflate(getLayoutInflater());
setContentView(binding.getRoot());
binding.btnLogin.setOnClickListener(...);
```
- Type-safe view access
- Generated at compile time
- Avoids findViewById()

**RecyclerView**:
- Efficiently displays lists
- Reuses views
- Better than ListView

---

## 9. COMMON ANDROID METHODS EXPLAINED

**onCreate()**:
- Called when activity is created
- Initialize UI here
- Called only once per instance

**onResume()**:
- Called when activity becomes visible
- Refresh data here
- Called every time user returns

**finish()**:
- Closes the activity
- Removes from back stack
- Can't go back to it

**startActivity()**:
- Launches new activity
- Adds to back stack
- Can go back with back button

**Toast.makeText()**:
- Shows popup message
- LENGTH_SHORT: 2 seconds
- LENGTH_LONG: 3.5 seconds

**findViewById()**:
- Finds view by ID
- Returns View, must cast
- Deprecated with View Binding

---

## 10. SQL OPERATIONS MAPPING

**INSERT**:
```java
db.insert(table, null, values);
// SQL: INSERT INTO users VALUES (...)
```

**SELECT**:
```java
db.query(table, columns, where, whereArgs, null, null, null);
// SQL: SELECT * FROM users WHERE username=?
```

**UPDATE**:
```java
db.update(table, values, where, whereArgs);
// SQL: UPDATE users SET role=? WHERE id=?
```

**DELETE**:
```java
db.delete(table, where, whereArgs);
// SQL: DELETE FROM users WHERE id=?
```

**Raw Query**:
```java
db.rawQuery("SELECT COUNT(*) FROM users", null);
// Direct SQL execution
```

---

## SUMMARY

This NoteX++ project demonstrates:

✅ **Database Management**: SQLite CRUD operations
✅ **Authentication**: Login/logout with session persistence
✅ **Role-Based Access**: User vs Admin separation
✅ **Material Design**: Modern Android UI
✅ **RecyclerView**: Efficient list display
✅ **Callbacks**: Activity-Adapter communication
✅ **Security**: Password hashing, role validation
✅ **Navigation**: Activity flow with Intents
✅ **View Binding**: Type-safe view access
✅ **Singleton Pattern**: Centralized management

**Total Lines of Code**: ~2500+
**Activities**: 6
**Database Tables**: 1
**User Roles**: 2

The code is well-structured, follows Android best practices, and provides a solid foundation for the full note-taking app features to be built on top of it.
