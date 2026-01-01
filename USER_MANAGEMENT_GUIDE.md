# User Management Guide - NoteX++ Admin Panel

## Overview
Admins can now perform comprehensive user management actions through the "Manage Users" section in the Admin Dashboard.

## How to Access User Management

1. **Login as Admin**
   - Use your admin credentials on the login screen
   - Click "Login as Admin" button

2. **Navigate to User Management**
   - From Admin Dashboard, click on "Manage Users" card
   - You'll see a list of all registered users

## Available Actions

### 1. View User List
- Shows all users with their:
  - Username
  - Email
  - Role (USER or ADMIN badge)
  - User ID (shortened for display)
- Statistics displayed at the top:
  - **Total Users**: Count of all users in the system
  - **Admins**: Count of admin users

### 2. User Actions Menu
Click on any user in the list to open the action menu.

#### For Regular Users (USER role):
- **View Details** - Shows full user information
- **Promote to Admin** - Elevate user to admin privileges
- **Delete User** - Permanently remove user from system
- **Cancel** - Close the menu

#### For Admin Users (ADMIN role):
- **View Details** - Shows full user information
- **Demote to User** - Remove admin privileges (convert to regular user)
- **Cancel** - Close the menu

### 3. Delete User
**Steps:**
1. Click on a regular user (not admin)
2. Select "Delete User" from the action menu
3. Confirm deletion in the dialog
4. User is permanently removed from the database

**Important Notes:**
- Admin users cannot be deleted (protection against accidental deletion)
- Deletion is permanent and cannot be undone
- User data including login credentials will be removed
- The user list refreshes automatically after deletion

### 4. Promote User to Admin
**Steps:**
1. Click on a regular user
2. Select "Promote to Admin"
3. Confirm the promotion
4. User role is updated to ADMIN

**What this does:**
- Grants full admin privileges
- User can now access Admin Dashboard
- User can manage other users
- Statistics update automatically

### 5. Demote Admin to User
**Steps:**
1. Click on an admin user
2. Select "Demote to User"
3. Confirm the demotion
4. User role is changed to USER

**What this does:**
- Removes admin privileges
- User can only access regular User Home page
- User cannot manage other users
- Statistics update automatically

## User List Features

### Visual Indicators
- **Admin Badge**: Purple/blue chip showing "ADMIN" role
- **User Badge**: Teal chip showing "USER" role
- **Admin Icon**: Shield icon for administrators
- **User Icon**: Person icon for regular users

### Statistics
The stats card shows real-time counts:
- Updates automatically after any user action
- Shows total users including both roles
- Shows admin count separately

### Empty State
If no users exist in the database:
- Shows a friendly "No users found" message
- Suggests creating users

## Security & Best Practices

### Protection Features
1. **Admin Protection**: Admin users cannot be deleted (only demoted)
2. **Confirmation Dialogs**: All destructive actions require confirmation
3. **Role Validation**: Actions are validated based on current role
4. **Auto-refresh**: List updates after every action to prevent stale data

### Recommendations
1. **Keep at least one admin**: Ensure you don't demote all admins
2. **Verify before deletion**: Double-check username before deleting
3. **Use role changes carefully**: Only promote trusted users to admin
4. **Regular audits**: Periodically review user list and roles

## Technical Details

### Database Operations
- **Delete**: Removes user record permanently from SQLite database
- **Role Update**: Modifies the role field in the users table
- **Real-time sync**: All operations immediately reflect in the database

### Error Handling
- Success/failure messages appear as Toast notifications
- Failed operations show error message
- Database errors are caught and reported

## Future Enhancements (Coming Soon)
- Edit user details (email, username)
- Password reset functionality
- User activity logs
- Bulk user operations
- Export user list to CSV
- User search and filtering

## Testing Checklist

✅ Build successful (tested and verified)
✅ Delete user functionality working
✅ Promote to admin functionality working
✅ Demote to user functionality working
✅ Statistics update after actions
✅ Confirmation dialogs appear
✅ Success/error messages display
✅ List refreshes automatically
✅ Admin protection works (cannot delete admins)

---

**Version**: 1.0  
**Last Updated**: December 23, 2025  
**Tested**: Yes ✓
