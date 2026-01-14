# NoteX++ Project Documentation - Lab Presentation

**Date**: December 23, 2025  
**Project**: NoteX++ - Smart Note-Taking Application  
**Platform**: Android (Java)

---

## 1. PROJECT OVERVIEW

NoteX++ is a local-first, role-based smart note-taking application for Android that provides:
- Secure user authentication with role management (USER and ADMIN)
- Separate interfaces for regular users and administrators
- User management capabilities for admins
- Persistent local storage using SQLite database
- Session management with auto-login

**Current Development Status**: Phase 1 Complete - Authentication & User Management

---

## 2. TECHNOLOGY STACK

### Programming Language
- **Java** - Core application logic

### UI Framework
- **Android SDK** - Native Android development
- **Material Design Components** - Modern UI elements
- **View Binding** - Type-safe view access

### Database
- **SQLite** - Local relational database
- **SQLiteOpenHelper** - Database management

### Security
- **Password Hashing** - Simple hash function (can be upgraded to BCrypt)
- **Session Management** - SharedPreferences for persistent sessions

### Build System
- **Gradle** - Build automation
- **Android Studio** - IDE

---

## 3. PROJECT ARCHITECTURE

### Layer Structure

```
┌─────────────────────────────────┐
│      Presentation Layer         │
│  (Activities, Layouts, Adapters)│
├─────────────────────────────────┤
│      Business Logic Layer       │
│    (AuthManager, Models)        │
├─────────────────────────────────┤
│      Data Layer                 │
│    (DatabaseHelper, SQLite)     │
└─────────────────────────────────┘
```

### Package Structure
```
com.example.notex/
├── activities/          # UI screens
│   ├── SplashActivity
│   ├── LoginActivity
│   ├── RegisterActivity
│   ├── UserHomeActivity
│   ├── AdminDashboardActivity
│   └── UserListActivity
├── models/             # Data models
│   └── User
├── utils/              # Utilities
│   └── AuthManager
├── database/           # Database layer
│   └── DatabaseHelper
└── adapters/           # RecyclerView adapters
    └── UserAdapter
```

---

## 4. DATABASE DESIGN

### Users Table Schema

```sql
CREATE TABLE users (
    id              TEXT PRIMARY KEY,
    username        TEXT UNIQUE NOT NULL,
    email           TEXT,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL,
    created_at      TEXT,
    last_login      TEXT
)
```

### Sample Data
- **Admin User**: username="admin", password="admin123"
- **Demo User**: username="demo_user", password="password123"
- **Test User**: username="john", password="john123"

### Database Operations
- ✅ Create (Register new user)
- ✅ Read (Login validation, Get all users)
- ✅ Update (Change user role)
- ✅ Delete (Remove user)

---

## 5. KEY FEATURES IMPLEMENTED

### A. Authentication System

**LoginActivity.java**
- Dual role login (User/Admin buttons)
- Username and password validation
- Role-based routing after login
- Password verification with hashing
- Error handling for invalid credentials

```java
Key Methods:
- attemptLogin(username, password, role)
- validateInput(username, password)
```

**RegisterActivity.java**
- New user registration
- Email validation
- Password confirmation
- Duplicate username check
- Auto-login after registration

**AuthManager.java** (Singleton Pattern)
- Centralized authentication management
- Session persistence using SharedPreferences
- Login/logout functionality
- Current user state management

```java
Key Methods:
- login(username, password, role) → boolean
- logout()
- isLoggedIn() → boolean
- getCurrentUser() → User
```

### B. User Management (Admin Feature)

**UserListActivity.java**
- Display all users in RecyclerView
- Real-time statistics (Total Users, Admin Count)
- Click on user to open action menu
- Role-based action buttons

**Actions Available:**
1. **For Regular Users:**
   - Promote to Admin
   - Delete User

2. **For Admin Users:**
   - Demote to User

**UserAdapter.java**
- Custom RecyclerView adapter
- Display user details (username, email, ID, password)
- Visual role badges (USER/ADMIN)
- Different icons for users vs admins

### C. Role-Based Access Control

**Two Separate Home Screens:**

1. **UserHomeActivity.java** (Regular Users)
   - Welcome message
   - Statistics: Notebooks, Notes, Tags (placeholder)
   - Quick Actions: My Notebooks, Search, Settings
   - FAB for creating new notes

2. **AdminDashboardActivity.java** (Administrators)
   - System overview statistics
   - Admin actions:
     - Manage Users
     - System Settings
     - Backup & Restore
     - View Reports

**Security Checks:**
- Each activity validates user role on launch
- Redirects unauthorized users to login
- Back button disabled to prevent navigation to login

### D. Session Management

**SplashActivity.java**
- App entry point
- Checks login status
- Auto-routes to appropriate screen:
  - Not logged in → LoginActivity
  - User logged in → UserHomeActivity
  - Admin logged in → AdminDashboardActivity

**Session Persistence:**
- Uses SharedPreferences
- Stores: user_id, username, email, role, is_logged_in
- Survives app restarts

---

## 6. CODE WALKTHROUGH - KEY COMPONENTS

### 6.1 DatabaseHelper.java

**Purpose**: Manages all database operations

**Key Features:**
- Singleton pattern (one instance app-wide)
- Version management (DATABASE_VERSION = 1)
- Default users creation on first run

**Important Methods:**

```java
// User Authentication
validateUser(username, password, role) → User

// User CRUD
addUser(username, email, password, role) → boolean
getAllUsers() → List<User>
deleteUser(userId) → boolean
updateUserRole(userId, newRole) → boolean

// Statistics
getTotalUserCount() → int
getUserCountByRole(role) → int

// Utilities
hashPassword(password) → String
usernameExists(username) → boolean
```

### 6.2 User Model (User.java)

**Data Structure:**
```java
public class User {
    private String id;           // UUID
    private String username;
    private String email;
    private String passwordHash;
    private UserRole role;       // Enum: USER or ADMIN
    private Date createdAt;
    private Date lastLogin;
}
```

**Role Enum:**
```java
public enum UserRole {
    USER,
    ADMIN
}
```

### 6.3 Login Flow

```
User launches app
    ↓
SplashActivity
    ↓
Check isLoggedIn()?
    ├─ Yes → Check role
    │         ├─ ADMIN → AdminDashboardActivity
    │         └─ USER → UserHomeActivity
    └─ No → LoginActivity
              ↓
         Enter credentials + Select role
              ↓
         AuthManager.login()
              ↓
         DatabaseHelper.validateUser()
              ↓
         Success? → Save session + Route to home
         Failure? → Show error message
```

### 6.4 User Management Flow (Admin)

```
AdminDashboardActivity
    ↓
Click "Manage Users"
    ↓
UserListActivity
    ↓
RecyclerView displays all users
    ↓
Click on a user
    ↓
Dialog shows:
- Username, Email, Role, ID, Password
- Action buttons (Promote/Demote/Delete)
    ↓
Select action → Confirm → Database update → Refresh list
```

---

## 7. UI/UX DESIGN DECISIONS

### Color Scheme

**User Interface:**
- Primary: Teal tones (#00695C, #00897B)
- Background: White
- Text: Dark gray to black (#333333 - #000000)

**Admin Interface:**
- Primary: Indigo tones (#283593, #3949AB)
- Background: Light gray (#F5F5F5)
- Accent: Blue (#1976D2)

### Material Design Elements
- Cards with rounded corners (8-12dp radius)
- Elevation for depth (2-4dp)
- Chips for role badges
- FAB for primary actions
- Icons for visual guidance

### Typography
- Headings: Bold, 18-24sp
- Body: Regular, 14-16sp
- Captions: 12sp, monospace for IDs/passwords

---

## 8. SECURITY FEATURES

1. **Password Hashing**: Passwords are never stored in plain text
2. **Role Validation**: Each screen validates user permissions
3. **Session Security**: Logout clears all session data
4. **Admin Protection**: Admin users cannot be deleted
5. **Confirmation Dialogs**: Destructive actions require confirmation

---

## 9. CURRENT LIMITATIONS & FUTURE WORK

### Current Limitations
- Password hashing is basic (should upgrade to BCrypt/PBKDF2)
- No password reset functionality
- No email verification
- Notes/Notebooks features not yet implemented

### Next Phase Features
1. Notebook creation and management
2. Canvas for handwriting/drawing
3. OCR integration
4. Speech-to-text
5. AI summarization
6. Export to PDF/PNG
7. Cloud sync (optional)

---

## 10. TESTING & BUILD

### Build Commands
```bash
./gradlew assembleDebug    # Build debug APK
./gradlew clean            # Clean build
```

### Test Accounts
```
Admin:
- Username: admin
- Password: admin123

User:
- Username: demo_user
- Password: password123
```

### Manual Testing Checklist
✅ User registration works
✅ User login works
✅ Admin login works
✅ Session persists after app restart
✅ User home shows correct interface
✅ Admin dashboard shows correct interface
✅ Manage users displays all users
✅ Delete user works
✅ Promote/Demote user works
✅ Statistics update in real-time
✅ Logout clears session

---

## 11. CODE STATISTICS

- **Total Activities**: 6
- **Total Layouts**: 9
- **Database Tables**: 1 (Users)
- **Lines of Code**: ~2000+
- **Build Status**: ✅ SUCCESS

---

## 12. DEMONSTRATION SCRIPT

**Step 1: Launch App**
- Shows splash screen
- Auto-routes to login (first time)

**Step 2: Register New User**
- Click "Register here"
- Fill form: username, email, password
- Creates account and logs in

**Step 3: User Interface**
- Shows UserHomeActivity
- Statistics (placeholders)
- Quick actions

**Step 4: Logout & Admin Login**
- Logout from menu
- Login as admin (username: admin, password: admin123)
- Shows AdminDashboardActivity

**Step 5: User Management**
- Click "Manage Users"
- Shows list of all users with details
- Click on a user
- Show Promote/Demote/Delete actions
- Delete a user → Confirm → List updates

**Step 6: Session Persistence**
- Close app
- Reopen app
- Automatically logs in to admin dashboard

---

## 13. KEY LEARNING OUTCOMES

1. **Android Activity Lifecycle**: Managing state across activities
2. **SQLite Database**: CRUD operations with local storage
3. **RecyclerView**: Efficient list display with custom adapters
4. **Material Design**: Modern UI components and guidelines
5. **Authentication**: User login/logout with session management
6. **RBAC**: Role-based access control implementation
7. **Singleton Pattern**: Centralized management (AuthManager, DatabaseHelper)
8. **View Binding**: Type-safe view access

---

## 14. PROJECT STRUCTURE SUMMARY

```
NoteX/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/notex/
│   │   │   ├── SplashActivity.java
│   │   │   ├── LoginActivity.java
│   │   │   ├── RegisterActivity.java
│   │   │   ├── UserHomeActivity.java
│   │   │   ├── AdminDashboardActivity.java
│   │   │   ├── UserListActivity.java
│   │   │   ├── UserAdapter.java
│   │   │   ├── models/User.java
│   │   │   ├── utils/AuthManager.java
│   │   │   └── database/DatabaseHelper.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_splash.xml
│   │   │   │   ├── activity_login.xml
│   │   │   │   ├── activity_register.xml
│   │   │   │   ├── activity_user_home.xml
│   │   │   │   ├── activity_admin_dashboard.xml
│   │   │   │   ├── activity_user_list.xml
│   │   │   │   └── item_user.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   └── strings.xml
│   │   │   └── menu/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
└── build.gradle.kts
```

---

## 15. CONCLUSION

This project successfully implements the **foundation phase** of NoteX++:
- ✅ Complete authentication system
- ✅ Role-based access control
- ✅ User management for admins
- ✅ Clean, modern UI
- ✅ Persistent local storage
- ✅ Session management

The architecture is modular and extensible, ready for the next phases of development including note-taking features, OCR, and AI integration.

---

**End of Documentation**

*This project demonstrates proficiency in Android development, database management, user authentication, and Material Design principles.*
