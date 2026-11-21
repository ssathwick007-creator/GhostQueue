package com.ghostqueue.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Domain model representing an administrative outreach or reminder communication for an appointment.
 */
public class AppointmentReminder {

    public enum ReminderType {
        ALL("All Channels"),
        SMS("SMS Text"),
        EMAIL("Email Notification"),
        PHONE_CALL("Direct Phone Call"),
        MANUAL_FOLLOWUP("Manual Administrative Outreach");

        private final String displayName;

        ReminderType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public static ReminderType fromString(String text) {
            if (text == null) return SMS;
            for (ReminderType t : values()) {
                if (t.name().equalsIgnoreCase(text) || t.displayName.equalsIgnoreCase(text)) {
                    return t;
                }
            }
            return SMS;
        }
    }

    public enum ReminderStatus {
        ALL("All Statuses", "badge-neutral"),
        PENDING("Pending Outreach", "badge-pending"),
        COMPLETED("Completed", "badge-confirmed"),
        FAILED("Delivery Failed", "badge-danger"),
        CANCELLED("Cancelled", "badge-neutral");

        private final String displayName;
        private final String styleClass;

        ReminderStatus(String displayName, String styleClass) {
            this.displayName = displayName;
            this.styleClass = styleClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getStyleClass() {
            return styleClass;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public static ReminderStatus fromString(String text) {
            if (text == null) return PENDING;
            for (ReminderStatus s : values()) {
                if (s.name().equalsIgnoreCase(text) || s.displayName.equalsIgnoreCase(text)) {
                    return s;
                }
            }
            return PENDING;
        }
    }

    private String reminderId;
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String patientPhone;
    private ReminderType reminderType;
    private ReminderStatus reminderStatus;
    private LocalDateTime scheduledFor;
    private LocalDateTime completedAt;
    private String messageContent;
    private String responseStatus; // CONFIRMED, RESCHEDULE_REQUESTED, CANCEL_REQUESTED, NO_RESPONSE
    private String operatorNotes;
    private LocalDateTime createdAt;

    public AppointmentReminder(String reminderId, String appointmentId, String patientId,
                               String patientName, String patientPhone,
                               ReminderType reminderType, ReminderStatus reminderStatus,
                               LocalDateTime scheduledFor, LocalDateTime completedAt,
                               String messageContent, String responseStatus,
                               String operatorNotes, LocalDateTime createdAt) {
        this.reminderId = reminderId;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName != null ? patientName : patientId;
        this.patientPhone = patientPhone != null ? patientPhone : "";
        this.reminderType = reminderType != null ? reminderType : ReminderType.SMS;
        this.reminderStatus = reminderStatus != null ? reminderStatus : ReminderStatus.PENDING;
        this.scheduledFor = scheduledFor != null ? scheduledFor : LocalDateTime.now();
        this.completedAt = completedAt;
        this.messageContent = messageContent != null ? messageContent : "";
        this.responseStatus = responseStatus != null ? responseStatus : "NO_RESPONSE";
        this.operatorNotes = operatorNotes != null ? operatorNotes : "";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getReminderId() {
        return reminderId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public ReminderStatus getReminderStatus() {
        return reminderStatus;
    }

    public void setReminderStatus(ReminderStatus reminderStatus) {
        this.reminderStatus = reminderStatus;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public String getScheduledForFormatted() {
        if (scheduledFor == null) return "Immediate";
        return scheduledFor.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getCompletedAtFormatted() {
        if (completedAt == null) return "—";
        return completedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getOperatorNotes() {
        return operatorNotes;
    }

    public void setOperatorNotes(String operatorNotes) {
        this.operatorNotes = operatorNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
