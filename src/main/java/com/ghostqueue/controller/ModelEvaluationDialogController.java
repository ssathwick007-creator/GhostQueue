package com.ghostqueue.controller;

import com.ghostqueue.prediction.EvaluationReport;
import com.ghostqueue.prediction.FeatureVector;
import com.ghostqueue.prediction.LogisticRegressionModel;
import com.ghostqueue.service.PredictionService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Model Evaluation &amp; Cross-Validation Dialog.
 * Displays discrimination metrics (Recall, Precision, Accuracy, F1, ROC-AUC),
 * a 2x2 confusion matrix on the held-out test cohort, and the learned logistic regression weights (β).
 */
public class ModelEvaluationDialogController {

    @FXML private Label accuracyLabel;
    @FXML private Label recallLabel;
    @FXML private Label precisionLabel;
    @FXML private Label f1Label;
    @FXML private Label rocAucLabel;

    @FXML private Label tpCountLabel;
    @FXML private Label fpCountLabel;
    @FXML private Label fnCountLabel;
    @FXML private Label tnCountLabel;

    @FXML private TableView<WeightRow> weightsTable;
    @FXML private TableColumn<WeightRow, String> colWeightFeature;
    @FXML private TableColumn<WeightRow, String> colWeightBeta;
    @FXML private TableColumn<WeightRow, String> colWeightDirection;
    @FXML private TableColumn<WeightRow, String> colWeightInterpretation;

    private PredictionService predictionService;

    @FXML
    public void initialize() {
        colWeightFeature.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().feature()));
        colWeightBeta.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().betaFormatted()));
        colWeightDirection.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().direction()));
        colWeightInterpretation.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().interpretation()));

        // Default to global service registry if available
        this.predictionService = ServiceRegistry.getInstance().getPredictionService();
        if (predictionService != null) {
            refreshMetrics();
        }
    }

    public void setPredictionService(PredictionService predictionService) {
        this.predictionService = predictionService;
        refreshMetrics();
    }

    private void refreshMetrics() {
        if (predictionService == null) return;

        EvaluationReport report = predictionService.getEvaluationReport();
        if (report == null) {
            report = predictionService.retrainModelOnDemonstrationCohort();
        }

        // Metrics display
        accuracyLabel.setText(String.format("%.1f%%", report.getAccuracy() * 100.0));
        recallLabel.setText(String.format("%.1f%%", report.getRecall() * 100.0));
        precisionLabel.setText(String.format("%.1f%%", report.getPrecision() * 100.0));
        f1Label.setText(String.format("%.3f", report.getF1Score()));
        rocAucLabel.setText(String.format("%.3f", report.getRocAuc()));

        // Confusion Matrix
        tpCountLabel.setText("TP: " + report.getTruePositives());
        fpCountLabel.setText("FP: " + report.getFalsePositives());
        fnCountLabel.setText("FN: " + report.getFalseNegatives());
        tnCountLabel.setText("TN: " + report.getTrueNegatives());

        // Model Coefficients
        LogisticRegressionModel model = predictionService.getPredictionEngine().getActiveModel();
        if (model != null) {
            populateWeightsTable(model);
        }
    }

    private void populateWeightsTable(LogisticRegressionModel model) {
        double[] weights = model.getWeights();
        List<WeightRow> rows = new ArrayList<>();

        String[] interpretations = {
                "Extended lead times increase decay in schedule recall and double booking risk",
                "Strongest empirical predictor: habitual previous non-attendance correlates with recurrence",
                "Demonstrated visit compliance strongly mitigates overall no-show probability",
                "Prior administrative cancellations indicate schedule volatility",
                "Late afternoon appointments encounter higher work, transit, or fatigue conflicts",
                "Adjacency to weekend exhibits higher patient travel friction",
                "Elective or non-urgent clinic visits have higher attendance variance",
                "Appointments booked on short notice exhibit high patient urgency and attendance intent",
                "Bookings placed > 3 weeks in advance suffer significant forgetfulness"
        };

        for (int i = 0; i < Math.min(weights.length, FeatureVector.FEATURE_NAMES.length); i++) {
            double w = weights[i];
            String dir = w > 0.05 ? "Increases Risk (+)" : (w < -0.05 ? "Mitigates Risk (-)" : "Neutral");
            String sign = w > 0 ? "+" : "";
            String betaStr = String.format("%s%.3f", sign, w);
            String interp = i < interpretations.length ? interpretations[i] : "Empirical coefficient";
            rows.add(new WeightRow(FeatureVector.FEATURE_NAMES[i], betaStr, dir, interp));
        }

        weightsTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void handleRetrainModel() {
        if (predictionService != null) {
            EvaluationReport newReport = predictionService.retrainModelOnDemonstrationCohort();
            refreshMetrics();
            NotificationManager.showSuccess(
                    "Model Re-Trained Successfully",
                    "Logistic Regression model re-converged on held-out demonstration cohort.\n\n" +
                            "Test Accuracy: " + String.format("%.1f%%", newReport.getAccuracy() * 100.0) + "\n" +
                            "No-Show Recall: " + String.format("%.1f%%", newReport.getRecall() * 100.0) + "\n" +
                            "ROC-AUC: " + String.format("%.3f", newReport.getRocAuc()) + "\n\n" +
                            "Active patient appointment predictions have been re-calibrated."
            );
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) accuracyLabel.getScene().getWindow();
        stage.close();
    }

    public record WeightRow(String feature, String betaFormatted, String direction, String interpretation) {}
}
