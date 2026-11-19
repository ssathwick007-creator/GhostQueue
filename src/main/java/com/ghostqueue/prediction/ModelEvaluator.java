package com.ghostqueue.prediction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Validates model generalization performance strictly on held-out test datasets.
 * Computes confusion matrix, precision, recall, specificity, F1, and numerical ROC-AUC.
 */
public class ModelEvaluator {

    private double decisionThreshold = 0.50;

    public ModelEvaluator() {}

    public ModelEvaluator(double decisionThreshold) {
        this.decisionThreshold = decisionThreshold;
    }

    public EvaluationReport evaluate(LogisticRegressionModel model, List<FeatureVector> testData) {
        if (testData == null || testData.isEmpty()) {
            return new EvaluationReport(model.getVersion(), 0, 0, 0, 0, 0, 0.5, decisionThreshold);
        }

        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;

        List<ScoredSample> scoredList = new ArrayList<>();

        for (FeatureVector sample : testData) {
            if (!sample.isLabeled()) continue;

            double prob = model.predictProbability(sample);
            double actual = sample.getLabel(); // 1.0 = NO_SHOW, 0.0 = ATTENDED

            scoredList.add(new ScoredSample(prob, actual));

            boolean predictedNoShow = (prob >= decisionThreshold);
            boolean actualNoShow = (actual >= 0.5);

            if (predictedNoShow && actualNoShow) {
                tp++;
            } else if (predictedNoShow && !actualNoShow) {
                fp++;
            } else if (!predictedNoShow && !actualNoShow) {
                tn++;
            } else {
                fn++;
            }
        }

        double rocAuc = calculateRocAuc(scoredList);

        return new EvaluationReport(
                model.getVersion(),
                scoredList.size(),
                tp, fp, tn, fn,
                rocAuc,
                decisionThreshold
        );
    }

    /**
     * Calculates ROC-AUC (Area Under the Curve) using the trapezoidal rule over sorted predicted probabilities.
     */
    private double calculateRocAuc(List<ScoredSample> list) {
        if (list.size() < 2) return 0.5;

        // Count positives (1.0) and negatives (0.0)
        long positives = list.stream().filter(s -> s.actual >= 0.5).count();
        long negatives = list.size() - positives;

        if (positives == 0 || negatives == 0) {
            return 0.5; // Undefined if only one class exists
        }

        // Sort descending by predicted probability
        list.sort(Comparator.comparingDouble((ScoredSample s) -> s.probability).reversed());

        double auc = 0.0;
        long tp = 0;
        long fp = 0;
        long prevTp = 0;
        long prevFp = 0;

        for (ScoredSample sample : list) {
            if (sample.actual >= 0.5) {
                tp++;
            } else {
                fp++;
                // Trapezoidal area segment
                auc += (tp + prevTp) * (fp - prevFp) / 2.0;
                prevFp = fp;
                prevTp = tp;
            }
        }

        // Final trapezoid if ending on negative
        if (fp > prevFp) {
            auc += (tp + prevTp) * (fp - prevFp) / 2.0;
        }

        return auc / ((double) positives * negatives);
    }

    private static class ScoredSample {
        final double probability;
        final double actual;

        ScoredSample(double probability, double actual) {
            this.probability = probability;
            this.actual = actual;
        }
    }
}
