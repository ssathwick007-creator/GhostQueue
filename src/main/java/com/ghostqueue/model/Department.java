package com.ghostqueue.model;

public enum Department {
    ALL("All Departments"),
    CARDIOLOGY("Cardiology"),
    NEUROLOGY("Neurology"),
    ORTHOPEDICS("Orthopedics"),
    ONCOLOGY("Oncology"),
    PEDIATRICS("Pediatrics"),
    GENERAL_MEDICINE("General Medicine"),
    DERMATOLOGY("Dermatology");

    private final String displayName;

    Department(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Department fromString(String text) {
        for (Department dept : Department.values()) {
            if (dept.displayName.equalsIgnoreCase(text) || dept.name().equalsIgnoreCase(text)) {
                return dept;
            }
        }
        return GENERAL_MEDICINE;
    }

    public static Department fromDisplayName(String text) {
        return fromString(text);
    }
}
