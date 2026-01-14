package com.example.notex.models;

import java.util.Date;

/**
 * Page data model representing a page/note within a notebook
 */
public class Page {

    private String id;
    private String notebookId;
    private String title;
    private String content;
    private int pageNumber;
    private Date createdAt;
    private Date updatedAt;

    public Page(String id, String notebookId, String title, String content, int pageNumber) {
        this.id = id;
        this.notebookId = notebookId;
        this.title = title;
        this.content = content;
        this.pageNumber = pageNumber;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
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

    @Override
    public String toString() {
        return "Page{" +
                "id='" + id + '\'' +
                ", notebookId='" + notebookId + '\'' +
                ", title='" + title + '\'' +
                ", pageNumber=" + pageNumber +
                ", createdAt=" + createdAt +
                '}';
    }
}
