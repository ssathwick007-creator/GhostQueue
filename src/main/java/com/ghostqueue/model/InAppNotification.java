package com.ghostqueue.model;

import java.time.LocalDateTime;

public class InAppNotification {
    private final long id;
    private final String userId;
    private final String title;
    private final String message;
    private final String type;
    private boolean isRead;
    private final LocalDateTime createdAt;

    public InAppNotification(long id, String userId, String title, String message, String type, boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public long getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
