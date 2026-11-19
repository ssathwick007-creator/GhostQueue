package com.ghostqueue.prediction;

/**
 * Encapsulates formal machine learning evaluation metrics calculated on a held-out test split.
 * Provides granular tracking of no-show recall, precision, specificity, F1-score, and ROC-AUC.
 */
public class EvaluationReport {

    private final String modelVersion;
    private final int totalTestSamples;
    private final int truePositives;   // Predicted No-Show, Actually No-Show
    private final int falsePositives;  // Predicted No-Show, Actually Attended
    private final int trueNegatives;   // Predicted Attended, Actually Attended
    private final int falseNegatives;  // Predicted Attended, Actually No-Show

    private final double accuracy;
    private final double precision;
    private final double recall;       // Critical: sensitivity for catching no-shows
    private final double specificity;
    private final double f1Score;
    private final double rocAuc;
    private final double classificationThreshold;

    public EvaluationReport(String modelVersion, int totalTestSamples,
                            int truePositives, int falsePositives, int trueNegatives, int falseNegatives,
                            double rocAuc, double classificationThreshold) {
        this.modelVersion = modelVersion;
        this.totalTestSamples = totalTestSamples;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.trueNegatives = trueNegatives;
        this.falseNegatives = falseNegatives;
        this.rocAuc = rocAuc;
        this.classificationThreshold = classificationThreshold;

        int total = truePositives + falsePositives + trueNegatives + falseNegatives;
        this.accuracy = total > 0 ? (double)(truePositives + trueNegatives) / total : 0.0;
        this.precision = (truePositives + falsePositives) > 0 ? (double) truePositives / (truePositives + falsePositives) : 0.0;
        this.recall = (truePositives + falseNegatives) > 0 ? (double) truePositives / (truePositives + falseNegatives) : 0.0;
        this.specificity = (trueNegatives + falsePositives) > 0 ? (double) trueNegatives / (trueNegatives + falsePositives) : 0.0;
        this.f1Score = (precision + recall) > 0 ? 2.0 * (precision * recall) / (precision + recall) : 0.0;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public int getTotalTestSamples() {
        return totalTestSamples;
    }

    public int getTruePositives() {
        return truePositives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public int getTrueNegatives() {
        return trueNegatives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getSpecificity() {
        return specificity;
    }

    public double getF1Score() {
        return f1Score;
    }

    public double getRocAuc() {
        return rocAuc;
    }

    public double getClassificationThreshold() {
        return classificationThreshold;
    }

    @Override
    public String toString() {
        return String.format(
                "EvaluationReport for %s (N=%d):\n" +
                "  Accuracy:    %.2f%%\n" +
                "  Precision:   %.2f%%\n" +
                "  Recall:      %.2f%% (No-Show Class)\n" +
                "  F1-Score:    %.4f\n" +
                "  ROC-AUC:     %.4f\n" +
                "  Confusion Matrix: TP=%d, FP=%d, TN=%d, FN=%d",
                modelVersion, totalTestSamples,
                accuracy * 100.0, precision * 100.0, recall * 100.0,
                f1Score, rocAuc,
                truePositives, falsePositives, trueNegatives, falseNegatives
        );
    }
}
