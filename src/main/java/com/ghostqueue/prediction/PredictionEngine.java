package com.ghostqueue.prediction;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.Patient;
import com.ghostqueue.model.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Core intelligence engine orchestrating pre-appointment feature extraction,
 * logistic regression inference, rule-based fallback, and explainable AI factor derivation.
 */
public class PredictionEngine {

    private static final Logger LOGGER = Logger.getLogger(PredictionEngine.class.getName());

    public static final String METHOD_LOGISTIC_REGRESSION = "LOGISTIC_REGRESSION_V1";
    public static final String METHOD_PROVISIONAL_BASELINE = "PROVISIONAL_CLINICAL_BASELINE";

    private final PredictionFeatureExtractor featureExtractor;
    private final RiskClassifier riskClassifier;
    private final PredictionExplanation explanationEngine;
    private final ModelTrainer modelTrainer;
    private final ModelEvaluator modelEvaluator;
    private final SyntheticDemonstrationDataset demoDataset;

    private LogisticRegressionModel activeModel;
    private EvaluationReport latestEvaluationReport;

    public PredictionEngine() {
        this.featureExtractor = new PredictionFeatureExtractor();
        this.riskClassifier = new RiskClassifier();
        this.explanationEngine = new PredictionExplanation();
        this.modelTrainer = new ModelTrainer();
        this.modelEvaluator = new ModelEvaluator();
        this.demoDataset = new SyntheticDemonstrationDataset();

        // Initialize active model: trained on synthetic clinical demo cohort
        trainOnDemonstrationCohort();
    }

    /**
     * Trains the Logistic Regression model using the labeled synthetic demonstration cohort.
     */
    public synchronized EvaluationReport trainOnDemonstrationCohort() {
        LOGGER.info("Initiating Logistic Regression training on demonstration cohort...");
        this.activeModel = modelTrainer.train(demoDataset.getTrainSet(), "LOGISTIC_REGRESSION_V1.2");
        this.latestEvaluationReport = modelEvaluator.evaluate(activeModel, demoDataset.getTestSet());
        LOGGER.info("Demonstration training complete. " + latestEvaluationReport.toString());
        return latestEvaluationReport;
    }

    /**
     * Generates a complete, explainable prediction for an appointment.
     */
    public PredictionResult predict(Appointment appointment, Patient patient) {
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment cannot be null for prediction.");
        }

        // 1. Extract strictly pre-appointment features (no future leakage)
        FeatureVector features = featureExtractor.extractFeatures(appointment, patient);

        // 2. Perform Inference
        double probability;
        String method;
        String modelVersion;

        if (activeModel != null) {
            probability = activeModel.predictProbability(features);
            method = METHOD_LOGISTIC_REGRESSION;
            modelVersion = activeModel.getVersion();
        } else {
            // Transparent provisional baseline fallback
            HeuristicRiskScorer baseline = new HeuristicRiskScorer();
            probability = baseline.predictNoShowProbability(appointment, patient);
            method = METHOD_PROVISIONAL_BASELINE;
            modelVersion = "HEURISTIC_BASELINE_V1";
        }

        // 3. Classify Risk
        RiskLevel riskLevel = riskClassifier.classify(probability);

        // 4. Derive Explainable Contributing Factors
        List<FactorExplanation> factors = (activeModel != null) ?
                explanationEngine.explain(activeModel, features, appointment, patient) :
                List.of(new FactorExplanation("Provisional Clinical Heuristic", FactorExplanation.Direction.NEUTRAL, 0.0,
                        "Derived from empirical outpatient appointment baseline."));

        // 5. Actionable Recommendation
        String recommendedAction = explanationEngine.getRecommendedAction(riskLevel);

        // 6. Build Historical Summary
        String historySummary = "New patient without prior recorded clinic visits.";
        if (patient != null && patient.getTotalAppointments() > 0) {
            historySummary = String.format("%d total appointments (%d attended, %d missed, %.1f%% attendance adherence).",
                    patient.getTotalAppointments(), patient.getAttendedAppointments(),
                    patient.getNoShowCount(), patient.getAttendanceRate());
        }

        String predId = "PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new PredictionResult(
                predId,
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getPatientName(),
                appointment.getDoctorName(),
                appointment.getDepartment(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                probability,
                riskLevel,
                method,
                modelVersion,
                factors,
                recommendedAction,
                LocalDateTime.now(),
                appointment.getStatus().name(),
                "PENDING",
                historySummary
        );
    }

    public LogisticRegressionModel getActiveModel() {
        return activeModel;
    }

    public EvaluationReport getLatestEvaluationReport() {
        if (latestEvaluationReport == null && activeModel != null) {
            latestEvaluationReport = modelEvaluator.evaluate(activeModel, demoDataset.getTestSet());
        }
        return latestEvaluationReport;
    }

    public RiskClassifier getRiskClassifier() {
        return riskClassifier;
    }

    public SyntheticDemonstrationDataset getDemoDataset() {
        return demoDataset;
    }
}
