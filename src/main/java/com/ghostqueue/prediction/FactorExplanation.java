package com.ghostqueue.prediction;

import java.io.Serializable;

/**
 * Represents an individual explainable factor contributing to a no-show risk estimate.
 */
public class FactorExplanation implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Direction {
        INCREASES_RISK,
        MITIGATES_RISK,
        NEUTRAL
    }

    private final String featureName;
    private final Direction direction;
    private final double impactScore;
    private final String description;

    public FactorExplanation(String featureName, Direction direction, double impactScore, String description) {
        this.featureName = featureName;
        this.direction = direction;
        this.impactScore = impactScore;
        this.description = description;
    }

    public String getFeatureName() {
        return featureName;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getImpactScore() {
        return impactScore;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRiskIncreasing() {
        return direction == Direction.INCREASES_RISK;
    }

    @Override
    public String toString() {
        String arrow = (direction == Direction.INCREASES_RISK) ? "▲ (+)" : "▼ (-)";
        return arrow + " " + featureName + ": " + description;
    }
}
