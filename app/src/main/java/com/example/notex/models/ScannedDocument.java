package com.example.notex.models;

public class ScannedDocument {
    private long id;
    private String name;
    private String filePath;
    private String fileType; // "PDF", "TXT", "IMAGE"
    private int pageCount;
    private long fileSize; // in bytes
    private long createdAt;
    private long modifiedAt;
    
    public ScannedDocument() {
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
    }
    
    public ScannedDocument(long id, String name, String filePath, String fileType, 
                          int pageCount, long fileSize, long createdAt, long modifiedAt) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.fileType = fileType;
        this.pageCount = pageCount;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }
    
    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name;
        this.modifiedAt = System.currentTimeMillis();
    }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(long modifiedAt) { this.modifiedAt = modifiedAt; }
    
    // Helper methods
    public String getFileSizeFormatted() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(createdAt));
    }
    
    public String getDocumentInfo() {
        StringBuilder info = new StringBuilder();
        info.append(fileType);
        if (pageCount > 0) {
            info.append(" • ").append(pageCount).append(" page");
            if (pageCount > 1) info.append("s");
        }
        info.append(" • ").append(getFileSizeFormatted());
        return info.toString();
    }
}
