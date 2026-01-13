package com.example.notex.models;

import java.util.Date;

/**
 * Reminder data model for events, tasks, and time/location-based reminders.
 * Supports time-based, location-based, and recurring reminders.
 */
public class Reminder {

    // Reminder types
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_TASK = "task";
    public static final String TYPE_REMINDER = "reminder";

    // Trigger types
    public static final String TRIGGER_TIME = "time";
    public static final String TRIGGER_LOCATION = "location";

    // Priority levels
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    // Repeat types
    public static final String REPEAT_NONE = "none";
    public static final String REPEAT_DAILY = "daily";
    public static final String REPEAT_WEEKLY = "weekly";
    public static final String REPEAT_MONTHLY = "monthly";
    public static final String REPEAT_CUSTOM = "custom";

    private String id;
    private String userId;
    private String noteId; // Optional: link to a note
    private String notebookId; // Optional: link to a notebook
    private String title;
    private String description;
    private String type; // event, task, reminder
    private String triggerType; // time, location
    private long scheduledAt; // Epoch milliseconds for time-based
    private String location; // Location name/address for location-based
    private double latitude; // For location-based
    private double longitude; // For location-based
    private int radiusMeters; // Geofence radius for location-based
    private String repeatType; // none, daily, weekly, monthly, custom
    private String repeatRule; // iCal RRULE or custom rule
    private int priority; // 0=low, 1=medium, 2=high
    private boolean isCompleted;
    private boolean isNotified;
    private boolean isAllDay;
    private String timezone;
    private long createdAt;
    private long updatedAt;

    /**
     * Constructor for new reminder
     */
    public Reminder(String id, String userId, String title, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.type = type;
        this.triggerType = TRIGGER_TIME;
        this.repeatType = REPEAT_NONE;
        this.priority = PRIORITY_MEDIUM;
        this.isCompleted = false;
        this.isNotified = false;
        this.isAllDay = false;
        this.radiusMeters = 100; // Default 100 meters
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public long getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(long scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(int radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getRepeatRule() {
        return repeatRule;
    }

    public void setRepeatRule(String repeatRule) {
        this.repeatRule = repeatRule;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified(boolean notified) {
        isNotified = notified;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public void setAllDay(boolean allDay) {
        isAllDay = allDay;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Get formatted date for display
     */
    public Date getScheduledDate() {
        return new Date(scheduledAt);
    }

    /**
     * Check if reminder is time-based
     */
    public boolean isTimeBased() {
        return TRIGGER_TIME.equals(triggerType);
    }

    /**
     * Check if reminder is location-based
     */
    public boolean isLocationBased() {
        return TRIGGER_LOCATION.equals(triggerType);
    }

    /**
     * Check if reminder is recurring
     */
    public boolean isRecurring() {
        return !REPEAT_NONE.equals(repeatType);
    }
}
