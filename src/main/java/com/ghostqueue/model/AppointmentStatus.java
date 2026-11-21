package com.ghostqueue.model;

public enum AppointmentStatus {
    ALL("All Statuses", "badge-neutral"),
    BOOKED("Booked", "badge-pending"),
    CONFIRMED("Confirmed", "badge-confirmed"),
    PENDING_REALLOCATION("Pending Reallocation", "badge-pending"),
    REALLOCATED("Reallocated", "badge-confirmed"),
    COMPLETED("Completed", "badge-completed"),
    CANCELLED("Cancelled", "badge-danger"),
    NO_SHOW("No-Show", "badge-danger");

    private final String displayName;
    private final String styleClass;

    AppointmentStatus(String displayName, String styleClass) {
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

    public static AppointmentStatus fromString(String text) {
        if (text == null) return BOOKED;
        for (AppointmentStatus status : AppointmentStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || 
                status.displayName.equalsIgnoreCase(text) || 
                status.name().replace("_", " ").equalsIgnoreCase(text)) {
                return status;
            }
        }
        return BOOKED;
    }
}
