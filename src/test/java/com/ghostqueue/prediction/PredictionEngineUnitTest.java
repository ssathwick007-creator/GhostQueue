package com.ghostqueue.prediction;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.Patient;
import com.ghostqueue.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Granular Unit Tests for the Phase 3 Intelligent No-Show Prediction Engine.
 * Verifies mathematical inference, feature extraction integrity, gradient descent convergence,
 * confusion matrix metrics, and explainable AI factor ranking.
 */
public class PredictionEngineUnitTest {

    private PredictionEngine engine;
    private PredictionFeatureExtractor extractor;

    @BeforeEach
    void setUp() {
        engine = new PredictionEngine();
        extractor = new PredictionFeatureExtractor();
    }

    @Test
    @DisplayName("Verify Feature Extraction adheres to strict pre-appointment constraints (No Leakage)")
    void testFeatureExtractionNoLeakage() {
        Patient compliantPatient = new Patient(
                "PT-TEST-01", "Compliant Test Patient", 45, "Female",
                "+1 555-001-1122", "test@hospital.org", "123 Elm St", LocalDateTime.now()
        );
        compliantPatient.setTotalAppointments(10);
        compliantPatient.setAttendedAppointments(9);
        compliantPatient.setNoShowCount(1);

        LocalDate apptDate = LocalDate.now().plusDays(14);
        LocalDate bookingDate = LocalDate.now();

        Appointment appointment = new Appointment(
                "APT-TEST-01", "PT-TEST-01", "Compliant Test Patient", 45, "+1 555-001-1122",
                "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY,
                apptDate, LocalTime.of(10, 30), AppointmentStatus.CONFIRMED,
                "Routine checkup", 14, 0.0, List.of()
        );
        appointment.setBookingDate(bookingDate);

        FeatureVector fv = extractor.extractFeatures(appointment, compliantPatient);
        assertNotNull(fv);
        assertEquals(9, fv.size());

        // Feature 0: Lead days normalized (14 days / 30.0 = ~0.467)
        assertTrue(fv.getValue(0) > 0.4 && fv.getValue(0) < 0.5);

        // Feature 1: Prior No-Shows (1 / 5.0 = 0.2)
        assertEquals(0.2, fv.getValue(1), 0.001);

        // Feature 2: Historical attendance rate (9 / (9+1) = 0.9)
        assertEquals(0.9, fv.getValue(2), 0.001);

        // Feature 7: Short notice (<3 days) should be 0.0
        assertEquals(0.0, fv.getValue(7), 0.001);

        // Feature 8: Extended advance (>21 days) should be 0.0
        assertEquals(0.0, fv.getValue(8), 0.001);
    }

    @Test
    @DisplayName("Verify Logistic Regression Sigmoid and Inference Math")
    void testLogisticRegressionMath() {
        LogisticRegressionModel model = new LogisticRegressionModel();

        // 1. All zero features -> probability equals sigmoid(bias)
        FeatureVector zeroFeatures = new FeatureVector(new double[9]);
        double expectedZeroProb = 1.0 / (1.0 + Math.exp(-model.getBias()));
        double actualZeroProb = model.predictProbability(zeroFeatures);
        assertEquals(expectedZeroProb, actualZeroProb, 0.005);

        // 2. High risk archetype: many prior no-shows, long lead time, low attendance rate
        double[] highRiskVals = new double[]{
                1.5,  // ~45 lead days
                1.0,  // 5+ prior no-shows
                0.2,  // 20% attendance rate
                0.8,  // 4 prior cancellations
                0.9,  // 17:00 pm
                1.0,  // Friday
                1.0,  // Dermatology
                0.0,  // Not short notice
                1.0   // Extended advance
        };
        FeatureVector highRiskVector = new FeatureVector(highRiskVals);
        double highRiskProb = model.predictProbability(highRiskVector);
        assertTrue(highRiskProb >= 0.65, "High risk vector must yield high no-show probability >= 65%");

        // 3. Low risk archetype: short notice, 100% past attendance, morning slot
        double[] lowRiskVals = new double[]{
                0.03, // 1 lead day
                0.0,  // 0 prior no-shows
                1.0,  // 100% attendance rate
                0.0,  // 0 cancellations
                0.1,  // 09:00 am
                0.0,  // Wednesday
                0.0,  // General medicine
                1.0,  // Short notice
                0.0   // Not extended
        };
        FeatureVector lowRiskVector = new FeatureVector(lowRiskVals);
        double lowRiskProb = model.predictProbability(lowRiskVector);
        assertTrue(lowRiskProb < 0.30, "Low risk vector must yield low no-show probability < 30%");
    }

    @Test
    @DisplayName("Verify Model Training, Gradient Descent Convergence, and Cross-Validation Evaluation")
    void testModelTrainingAndEvaluation() {
        SyntheticDemonstrationDataset dataset = new SyntheticDemonstrationDataset();
        List<FeatureVector> trainSet = dataset.getTrainSet();
        List<FeatureVector> testSet = dataset.getTestSet();

        assertEquals(96, trainSet.size(), "80% of 120 samples should be training set");
        assertEquals(24, testSet.size(), "20% of 120 samples should be test set");

        // Train model with batch gradient descent
        ModelTrainer trainer = new ModelTrainer(0.08, 600, 0.005);
        LogisticRegressionModel trainedModel = trainer.train(trainSet, "TEST_MODEL_V1");

        assertNotNull(trainedModel);
        assertTrue(trainedModel.getTrainingLoss() < 0.65, "Training loss should converge below 0.65");

        // Evaluate model on held-out test split
        ModelEvaluator evaluator = new ModelEvaluator(0.50);
        EvaluationReport report = evaluator.evaluate(trainedModel, testSet);

        assertNotNull(report);
        assertEquals(24, report.getTotalTestSamples());
        int sumMatrix = report.getTruePositives() + report.getFalsePositives() +
                report.getTrueNegatives() + report.getFalseNegatives();
        assertEquals(24, sumMatrix, "Confusion matrix elements must sum to total test samples");

        assertTrue(report.getAccuracy() >= 0.70, "Accuracy on demonstration dataset should be >= 70%");
        assertTrue(report.getRecall() >= 0.70, "Recall for no-show class must be >= 70%");
        assertTrue(report.getRocAuc() >= 0.75, "ROC-AUC discrimination should be >= 0.75");
    }

    @Test
    @DisplayName("Verify Explainable AI (XAI) Factor Ranking and Administrative Action Formulation")
    void testExplainableAIFactors() {
        Patient chronicPatient = new Patient(
                "PT-RISK-01", "Chronic Misser", 50, "Male",
                "+1 555-444-5555", "chronic@hospital.org", "321 Oak St", LocalDateTime.now()
        );
        chronicPatient.setTotalAppointments(6);
        chronicPatient.setAttendedAppointments(2);
        chronicPatient.setNoShowCount(4);

        Appointment longLeadAppt = new Appointment(
                "APT-RISK-01", "PT-RISK-01", "Chronic Misser", 50, "+1 555-444-5555",
                "DOC-105", "Dr. Emily Taylor", Department.DERMATOLOGY,
                LocalDate.now().plusDays(28), LocalTime.of(16, 30), AppointmentStatus.CONFIRMED,
                "Consult", 28, 0.0, List.of()
        );
        longLeadAppt.setBookingDate(LocalDate.now());

        PredictionResult result = engine.predict(longLeadAppt, chronicPatient);
        assertNotNull(result);

        // Risk should be classified as HIGH
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertTrue(result.getNoShowProbability() >= 0.65);

        // Contributing factors should be ranked
        List<FactorExplanation> factors = result.getContributingFactors();
        assertFalse(factors.isEmpty());

        // Top factor should have impact and point to risk increase
        FactorExplanation topFactor = factors.get(0);
        assertTrue(topFactor.getImpactScore() != 0.0);
        assertEquals(FactorExplanation.Direction.INCREASES_RISK, topFactor.getDirection());

        // Administrative action protocol recommendation
        assertNotNull(result.getSuggestedAction());
        assertFalse(result.getSuggestedAction().isBlank());
        assertTrue(result.getSuggestedAction().contains("outreach") ||
                   result.getSuggestedAction().contains("confirmation") ||
                   result.getSuggestedAction().contains("standby") ||
                   result.getSuggestedAction().contains("phone"));
    }

    @Test
    @DisplayName("Verify Risk Classifier Boundary Thresholds")
    void testRiskClassifierBoundaries() {
        assertEquals(RiskLevel.LOW, RiskClassifier.classifyDefault(0.05));
        assertEquals(RiskLevel.LOW, RiskClassifier.classifyDefault(0.299));
        assertEquals(RiskLevel.MEDIUM, RiskClassifier.classifyDefault(0.30));
        assertEquals(RiskLevel.MEDIUM, RiskClassifier.classifyDefault(0.649));
        assertEquals(RiskLevel.HIGH, RiskClassifier.classifyDefault(0.65));
        assertEquals(RiskLevel.HIGH, RiskClassifier.classifyDefault(0.95));
    }
}
