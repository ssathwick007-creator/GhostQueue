package com.ghostqueue.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppointmentHistory {
    private int historyId;
    private String appointmentId;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime changedAt;
    private String remarks;

    public AppointmentHistory(int historyId, String appointmentId, String previousStatus, String newStatus, LocalDateTime changedAt, String remarks) {
        this.historyId = historyId;
        this.appointmentId = appointmentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
        this.remarks = remarks != null ? remarks : "";
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPreviousStatus() {
        return previousStatus != null ? previousStatus : "—";
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getRemarks() {
        return remarks != null ? remarks : "";
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getChangedAtFormatted() {
        if (changedAt == null) return "";
        return changedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"));
    }
}
