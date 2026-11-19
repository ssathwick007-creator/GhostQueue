package com.ghostqueue.prediction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Provides a dedicated, clearly segregated synthetic demonstration dataset
 * containing realistic clinical scheduling and historical attendance archetypes.
 * Used exclusively for model training, cross-validation, and demonstration mode.
 */
public class SyntheticDemonstrationDataset {

    public static final String DATASET_LABEL = "SYNTHETIC_DEMO_COHORT_V1";

    private final List<FeatureVector> trainSet = new ArrayList<>();
    private final List<FeatureVector> testSet = new ArrayList<>();

    public SyntheticDemonstrationDataset() {
        generateCohort();
    }

    private void generateCohort() {
        Random rand = new Random(42); // Deterministic seed for exact reproducibility

        List<FeatureVector> allSamples = new ArrayList<>();

        // Archetype 1: Highly Compliant, Short Lead Time (Attendance expected ~95%)
        for (int i = 0; i < 40; i++) {
            double leadDays = 1 + rand.nextInt(4); // 1 - 4 days
            double noShows = 0;
            double attRate = 0.90 + (rand.nextDouble() * 0.10);
            double cancellations = rand.nextInt(2) == 0 ? 0 : 1;
            double hour = 9 + rand.nextInt(6);
            double monFri = rand.nextDouble() < 0.3 ? 1.0 : 0.0;
            double highFrictionDept = rand.nextDouble() < 0.2 ? 1.0 : 0.0;
            double shortNotice = 1.0;
            double longLead = 0.0;

            // Outcome: 95% Attended (0.0)
            double outcome = rand.nextDouble() < 0.05 ? 1.0 : 0.0;

            double[] f = new double[]{
                    leadDays / 30.0,
                    noShows / 5.0,
                    attRate,
                    cancellations / 5.0,
                    (hour - 8.0) / 10.0,
                    monFri,
                    highFrictionDept,
                    shortNotice,
                    longLead
            };
            allSamples.add(new FeatureVector(f, outcome));
        }

        // Archetype 2: Chronic Missed Visits & Extended Lead Time (No-Show expected ~80%)
        for (int i = 0; i < 40; i++) {
            double leadDays = 22 + rand.nextInt(25); // 22 - 46 days
            double noShows = 2 + rand.nextInt(4);   // 2 - 5 no shows
            double attRate = 0.25 + (rand.nextDouble() * 0.35); // 25% - 60% attendance
            double cancellations = 1 + rand.nextInt(3);
            double hour = 14 + rand.nextInt(4); // Afternoon
            double monFri = rand.nextDouble() < 0.6 ? 1.0 : 0.0;
            double highFrictionDept = rand.nextDouble() < 0.5 ? 1.0 : 0.0;
            double shortNotice = 0.0;
            double longLead = 1.0;

            // Outcome: 80% No-Show (1.0)
            double outcome = rand.nextDouble() < 0.80 ? 1.0 : 0.0;

            double[] f = new double[]{
                    leadDays / 30.0,
                    noShows / 5.0,
                    attRate,
                    cancellations / 5.0,
                    (hour - 8.0) / 10.0,
                    monFri,
                    highFrictionDept,
                    shortNotice,
                    longLead
            };
            allSamples.add(new FeatureVector(f, outcome));
        }

        // Archetype 3: Moderate Baseline Patients with Varying Scheduling Windows (~30% no-show)
        for (int i = 0; i < 40; i++) {
            double leadDays = 5 + rand.nextInt(15);
            double noShows = rand.nextInt(2) == 0 ? 0 : 1;
            double attRate = 0.70 + (rand.nextDouble() * 0.20);
            double cancellations = rand.nextInt(2);
            double hour = 10 + rand.nextInt(6);
            double monFri = rand.nextDouble() < 0.4 ? 1.0 : 0.0;
            double highFrictionDept = rand.nextDouble() < 0.4 ? 1.0 : 0.0;
            double shortNotice = 0.0;
            double longLead = 0.0;

            // Outcome: 30% No-Show (1.0)
            double outcome = rand.nextDouble() < 0.30 ? 1.0 : 0.0;

            double[] f = new double[]{
                    leadDays / 30.0,
                    noShows / 5.0,
                    attRate,
                    cancellations / 5.0,
                    (hour - 8.0) / 10.0,
                    monFri,
                    highFrictionDept,
                    shortNotice,
                    longLead
            };
            allSamples.add(new FeatureVector(f, outcome));
        }

        // Shuffle deterministically
        Collections.shuffle(allSamples, new Random(101));

        // 80% Train, 20% Test split
        int trainCount = (int) (allSamples.size() * 0.80);
        for (int i = 0; i < allSamples.size(); i++) {
            if (i < trainCount) {
                trainSet.add(allSamples.get(i));
            } else {
                testSet.add(allSamples.get(i));
            }
        }
    }

    public List<FeatureVector> getTrainSet() {
        return Collections.unmodifiableList(trainSet);
    }

    public List<FeatureVector> getTestSet() {
        return Collections.unmodifiableList(testSet);
    }

    public List<FeatureVector> getAllSamples() {
        List<FeatureVector> combined = new ArrayList<>(trainSet);
        combined.addAll(testSet);
        return Collections.unmodifiableList(combined);
    }
}
