package com.ghostqueue.model;

/**
 * Standard waiting queue lifecycle statuses.
 */
public enum QueueStatus {
    ALL("All Queue Statuses", "queue-status-all"),
    WAITING("Waiting", "queue-status-waiting"),
    OFFERED("Slot Offered", "queue-status-offered"),
    SCHEDULED("Scheduled", "queue-status-scheduled"),
    STANDBY("On Standby", "queue-status-standby"),
    REALLOCATED("Reallocated", "queue-status-reallocated"),
    REMOVED("Removed", "queue-status-removed"),
    EXPIRED("Expired", "queue-status-expired");

    private final String displayName;
    private final String styleClass;

    QueueStatus(String displayName, String styleClass) {
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

    public static QueueStatus fromString(String text) {
        if (text == null) return WAITING;
        for (QueueStatus s : values()) {
            if (s.displayName.equalsIgnoreCase(text) || 
                s.name().equalsIgnoreCase(text) ||
                s.name().replace("_", " ").equalsIgnoreCase(text)) {
                return s;
            }
        }
        return WAITING;
    }
}
