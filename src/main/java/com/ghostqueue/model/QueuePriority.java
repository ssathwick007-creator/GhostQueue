package com.ghostqueue.model;

public enum QueuePriority {
    ALL("All Priorities", "priority-all", "#77838D"),
    URGENT("Urgent", "priority-urgent", "#C95F5F"),
    HIGH("High", "priority-high", "#D99A42"),
    STANDARD("Standard", "priority-standard", "#168C83");

    private final String label;
    private final String styleClass;
    private final String colorHex;

    QueuePriority(String label, String styleClass, String colorHex) {
        this.label = label;
        this.styleClass = styleClass;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public String getColorHex() {
        return colorHex;
    }

    @Override
    public String toString() {
        return label;
    }

    public static QueuePriority fromString(String text) {
        for (QueuePriority p : values()) {
            if (p.label.equalsIgnoreCase(text) || p.name().equalsIgnoreCase(text)) {
                return p;
            }
        }
        return STANDARD;
    }
}
