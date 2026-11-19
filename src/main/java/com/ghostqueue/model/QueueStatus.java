package com.ghostqueue.model;

public enum QueueStatus {
    ALL("All Queue Statuses", "queue-status-all"),
    WAITING("Waiting", "queue-status-waiting"),
    OFFERED("Slot Offered", "queue-status-offered"),
    STANDBY("On Standby", "queue-status-standby"),
    REALLOCATED("Reallocated", "queue-status-reallocated");

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
        for (QueueStatus s : values()) {
            if (s.displayName.equalsIgnoreCase(text) || s.name().equalsIgnoreCase(text)) {
                return s;
            }
        }
        return WAITING;
    }
}
