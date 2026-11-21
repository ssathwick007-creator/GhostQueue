package com.ghostqueue.model;

public enum RiskLevel {
    ALL("All Risk Levels", "risk-badge-all", "#77838D"),
    LOW("Low Risk", "risk-badge-low", "#438B70"),
    MEDIUM("Medium Risk", "risk-badge-med", "#D99A42"),
    HIGH("High Risk", "risk-badge-high", "#C95F5F");

    private final String label;
    private final String styleClass;
    private final String colorHex;

    RiskLevel(String label, String styleClass, String colorHex) {
        this.label = label;
        this.styleClass = styleClass;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayName() {
        return label;
    }

    public static RiskLevel fromString(String text) {
        if (text == null || text.isBlank()) return ALL;
        for (RiskLevel rl : values()) {
            if (rl.name().equalsIgnoreCase(text) || rl.label.equalsIgnoreCase(text)) {
                return rl;
            }
        }
        return LOW;
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

    public static RiskLevel fromScore(double probability) {
        if (probability >= 0.70) {
            return HIGH;
        } else if (probability >= 0.35) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}
