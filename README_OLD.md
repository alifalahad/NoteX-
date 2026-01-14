# NoteX++ Desktop

A smart note-taking desktop application built with JavaFX and SQLite, mirroring the NoteX Android app.

## Features

- **User Authentication**: Login, registration, and session management
- **Role-Based Access**: USER and ADMIN roles with different dashboards
- **Notebooks**: Create, edit, delete, and pin notebooks with customizable colors
- **Pages**: Create and edit multi-page notes within notebooks
- **Admin Dashboard**: User management, statistics, and system overview
- **Persistent Storage**: SQLite database stored in `~/.notex_desktop/`
- **Modern UI**: Clean, intuitive interface with CSS styling

## Requirements

- **Java 21 or 22** (OpenJDK, Eclipse Temurin, or Oracle JDK)
- **Gradle** (included via wrapper)

## Quick Start

### Option 1: Run Script (macOS)
```bash
./run.sh
```

### Option 2: Gradle Command
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 22)
./gradlew run
```

### Option 3: IntelliJ IDEA
1. Open the project in IntelliJ IDEA
2. Set Project SDK to Java 21/22
3. Run the `Launcher` class

## Default Users

| Username   | Password    | Role  |
|------------|-------------|-------|
| demo_user  | password123 | USER  |
| admin      | admin123    | ADMIN |
| john       | john123     | USER  |

## Project Structure

```
NoteX_Desktop/
├── src/main/java/com/example/notex_desktop/
│   ├── NoteXApp.java              # Main JavaFX Application
│   ├── Launcher.java              # Application entry point
│   ├── controllers/               # FXML Controllers
│   │   ├── LoginController.java
│   │   ├── RegisterController.java
│   │   ├── UserHomeController.java
│   │   ├── AdminDashboardController.java
│   │   ├── NotebookPagesController.java
│   │   └── PageEditorController.java
│   ├── database/
│   │   └── DatabaseHelper.java    # SQLite database operations
│   ├── models/
│   │   ├── User.java
│   │   ├── Notebook.java
│   │   └── Page.java
│   └── utils/
│       └── AuthManager.java       # Authentication management
├── src/main/resources/com/example/notex_desktop/
│   ├── views/                     # FXML view files
│   │   ├── login.fxml
│   │   ├── register.fxml
│   │   ├── user_home.fxml
│   │   ├── admin_dashboard.fxml
│   │   ├── notebook_pages.fxml
│   │   └── page_editor.fxml
│   └── styles/
│       └── styles.css             # Application styling
├── build.gradle                   # Gradle build configuration
└── run.sh                         # Quick run script
```

## Building

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 22)
./gradlew clean build
```

The built JAR will be in `build/libs/`.

## Database Location

The SQLite database is stored at:
```
~/.notex_desktop/notex_desktop.db
```

## Features Comparison with Android Version

| Feature | Android | Desktop |
|---------|---------|---------|
| User Login/Register | ✅ | ✅ |
| User/Admin Roles | ✅ | ✅ |
| Notebooks | ✅ | ✅ |
| Pages | ✅ | ✅ |
| Pin Notebooks | ✅ | ✅ |
| Color Themes | ✅ | ✅ |
| SQLite Database | ✅ | ✅ |
| Admin Dashboard | ✅ | ✅ |
| User Management | ✅ | ✅ |
| Canvas Drawing | ✅ | ❌ |
| Document Scanning | ✅ | ❌ |
| Reminders | ✅ | ❌ |

## Technologies Used

- **Java 21/22** - Programming language
- **JavaFX 21** - UI framework
- **SQLite** - Embedded database
- **Gradle** - Build tool
- **ControlsFX** - Enhanced JavaFX controls

## License

© 2026 NoteX Team
