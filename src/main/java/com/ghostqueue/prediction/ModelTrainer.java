package com.ghostqueue.prediction;

import java.util.List;
import java.util.logging.Logger;

/**
 * Trains a Logistic Regression model on a labeled dataset using Batch Gradient Descent
 * with L2 (Ridge) regularization to prevent overfitting on clinical appointment features.
 */
public class ModelTrainer {

    private static final Logger LOGGER = Logger.getLogger(ModelTrainer.class.getName());

    private double learningRate = 0.08;
    private int epochs = 600;
    private double l2Regularization = 0.005;

    public ModelTrainer() {}

    public ModelTrainer(double learningRate, int epochs, double l2Regularization) {
        this.learningRate = learningRate;
        this.epochs = epochs;
        this.l2Regularization = l2Regularization;
    }

    /**
     * Trains the model using labeled samples (y = 1.0 for no-show, y = 0.0 for attended).
     */
    public LogisticRegressionModel train(List<FeatureVector> trainingData, String modelVersion) {
        if (trainingData == null || trainingData.isEmpty()) {
            throw new IllegalArgumentException("Training dataset cannot be empty.");
        }

        int m = trainingData.size();
        int n = trainingData.get(0).size();

        double[] weights = new double[n];
        double bias = -0.5; // Initial slightly negative bias reflecting clinical base rate

        double finalLoss = 0.0;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] weightGradients = new double[n];
            double biasGradient = 0.0;
            double epochLoss = 0.0;

            for (FeatureVector sample : trainingData) {
                if (!sample.isLabeled()) continue;

                double y = sample.getLabel();
                double[] x = sample.getValues();

                // Compute z
                double z = bias;
                for (int i = 0; i < n; i++) {
                    z += weights[i] * x[i];
                }

                // Sigmoid
                double p = 1.0 / (1.0 + Math.exp(-z));
                p = Math.max(1e-7, Math.min(1.0 - 1e-7, p)); // numerical stability

                double error = p - y;

                for (int i = 0; i < n; i++) {
                    weightGradients[i] += error * x[i];
                }
                biasGradient += error;

                // Log-loss accumulation
                epochLoss += -(y * Math.log(p) + (1.0 - y) * Math.log(1.0 - p));
            }

            // Apply regularization and update
            for (int i = 0; i < n; i++) {
                double regTerm = (l2Regularization / m) * weights[i];
                double grad = (weightGradients[i] / m) + regTerm;
                weights[i] -= learningRate * grad;
            }
            bias -= learningRate * (biasGradient / m);

            finalLoss = (epochLoss / m);
        }

        LOGGER.info(String.format("Trained %s over %d epochs on %d samples. Final Log-Loss: %.4f",
                modelVersion, epochs, m, finalLoss));

        return new LogisticRegressionModel(modelVersion, weights, bias, finalLoss, epochs);
    }

    public double getLearningRate() {
        return learningRate;
    }

    public int getEpochs() {
        return epochs;
    }

    public double getL2Regularization() {
        return l2Regularization;
    }
}
