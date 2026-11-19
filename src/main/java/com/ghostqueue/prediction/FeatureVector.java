package com.ghostqueue.prediction;

import java.util.Arrays;

/**
 * Encapsulates the numerical feature vector extracted from pre-appointment information.
 * Used as input for mathematical machine learning models (Logistic Regression).
 */
public class FeatureVector {

    public static final String[] FEATURE_NAMES = {
            "Lead Days (Booking Interval)",
            "Prior No-Show Count",
            "Historical Attendance Rate",
            "Prior Cancellation Count",
            "Appointment Time (Hour)",
            "Weekend Proximity (Mon/Fri)",
            "Specialty Sensitivity Index",
            "Short-Notice Booking (<3 days)",
            "Extended Advance Booking (>21 days)"
    };

    private final double[] values;
    private final Double label; // 1.0 for NO_SHOW, 0.0 for ATTENDED/COMPLETED, or null if inference-only

    public FeatureVector(double[] values, Double label) {
        this.values = values;
        this.label = label;
    }

    public FeatureVector(double[] values) {
        this(values, null);
    }

    public double[] getValues() {
        return values;
    }

    public double getValue(int index) {
        return values[index];
    }

    public int size() {
        return values.length;
    }

    public Double getLabel() {
        return label;
    }

    public boolean isLabeled() {
        return label != null;
    }

    @Override
    public String toString() {
        return "FeatureVector{" +
                "values=" + Arrays.toString(values) +
                ", label=" + label +
                '}';
    }
}
