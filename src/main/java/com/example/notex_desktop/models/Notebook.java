package com.example.notex_desktop.models;

import java.util.Date;

/**
 * Notebook data model representing a notebook in NoteX Desktop.
 * A notebook contains multiple pages and belongs to a user.
 */
public class Notebook {

    private String id;
    private String userId;
    private String title;
    private String color;
    private boolean isPinned;
    private Date createdAt;
    private Date updatedAt;
    private int pageCount;

    /**
     * Constructor for creating a new notebook
     */
    public Notebook(String id, String userId, String title, String color, boolean isPinned) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.color = color;
        this.isPinned = isPinned;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.pageCount = 0;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    @Override
    public String toString() {
        return "Notebook{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", color='" + color + '\'' +
                ", isPinned=" + isPinned +
                ", pageCount=" + pageCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
