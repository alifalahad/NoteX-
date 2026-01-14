package com.example.notex_desktop.models;

/**
 * Reminder model class representing reminders, events, and tasks
 */
public class Reminder {

    public enum ReminderType {
        REMINDER, EVENT, TASK
    }

    public enum TriggerType {
        TIME, LOCATION
    }

    public enum RepeatType {
        NONE, DAILY, WEEKLY, MONTHLY
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public enum Status {
        PENDING, COMPLETED, OVERDUE
    }

    private String id;
    private String userId;
    private String title;
    private String description;
    private ReminderType type;
    private TriggerType triggerType;
    private String triggerDate; // Format: yyyy-MM-dd HH:mm
    private String locationName;
    private int locationRadius; // in meters
    private RepeatType repeatType;
    private Priority priority;
    private String ringtone;
    private boolean isAllDay;
    private Status status;
    private String createdAt;
    private String updatedAt;

    public Reminder() {
        this.type = ReminderType.REMINDER;
        this.triggerType = TriggerType.TIME;
        this.repeatType = RepeatType.NONE;
        this.priority = Priority.LOW;
        this.status = Status.PENDING;
        this.locationRadius = 100;
        this.isAllDay = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ReminderType getType() { return type; }
    public void setType(ReminderType type) { this.type = type; }

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public String getTriggerDate() { return triggerDate; }
    public void setTriggerDate(String triggerDate) { this.triggerDate = triggerDate; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public int getLocationRadius() { return locationRadius; }
    public void setLocationRadius(int locationRadius) { this.locationRadius = locationRadius; }

    public RepeatType getRepeatType() { return repeatType; }
    public void setRepeatType(RepeatType repeatType) { this.repeatType = repeatType; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getRingtone() { return ringtone; }
    public void setRingtone(String ringtone) { this.ringtone = ringtone; }

    public boolean isAllDay() { return isAllDay; }
    public void setAllDay(boolean allDay) { isAllDay = allDay; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // Helper method to get priority color
    public String getPriorityColor() {
        switch (priority) {
            case HIGH: return "#F44336"; // Red
            case MEDIUM: return "#FF9800"; // Orange/Yellow
            case LOW: 
            default: return "#2196F3"; // Blue
        }
    }

    // Helper method to get status color
    public String getStatusColor() {
        switch (status) {
            case COMPLETED: return "#4CAF50"; // Green
            case OVERDUE: return "#9E9E9E"; // Gray
            case PENDING:
            default: return getPriorityColor();
        }
    }
}
