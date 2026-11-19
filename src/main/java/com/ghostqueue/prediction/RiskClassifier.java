package com.ghostqueue.prediction;

import com.ghostqueue.model.RiskLevel;

/**
 * Centrally manages clinical no-show risk classification based on configurable probability thresholds.
 */
public class RiskClassifier {

    public static final double DEFAULT_LOW_THRESHOLD = 0.30;
    public static final double DEFAULT_HIGH_THRESHOLD = 0.65;

    private double lowThreshold;
    private double highThreshold;

    public RiskClassifier() {
        this(DEFAULT_LOW_THRESHOLD, DEFAULT_HIGH_THRESHOLD);
    }

    public RiskClassifier(double lowThreshold, double highThreshold) {
        if (lowThreshold <= 0.0 || lowThreshold >= highThreshold || highThreshold >= 1.0) {
            throw new IllegalArgumentException("Invalid risk thresholds. Expected 0 < low < high < 1.");
        }
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
    }

    /**
     * Maps calculated probability to clinical RiskLevel:
     * - Probability < lowThreshold -> LOW
     * - lowThreshold <= Probability < highThreshold -> MEDIUM
     * - Probability >= highThreshold -> HIGH
     */
    public RiskLevel classify(double probability) {
        if (probability < lowThreshold) {
            return RiskLevel.LOW;
        } else if (probability < highThreshold) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }

    public double getLowThreshold() {
        return lowThreshold;
    }

    public void setLowThreshold(double lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public double getHighThreshold() {
        return highThreshold;
    }

    public void setHighThreshold(double highThreshold) {
        this.highThreshold = highThreshold;
    }

    public String getHexColor(RiskLevel level) {
        return switch (level) {
            case LOW -> "#438B70";    // Medical Green
            case MEDIUM -> "#D99A42"; // Clinical Amber
            case HIGH -> "#C95F5F";   // Critical Red
            default -> "#77838D";
        };
    }
}
