package com.ghostqueue.controller;

import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.prediction.FactorExplanation;
import com.ghostqueue.prediction.PredictionResult;
import com.ghostqueue.util.NotificationManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PredictionExplanationDialogController {

    @FXML private Label patientNameLabel;
    @FXML private Label appointmentInfoLabel;
    @FXML private Label riskBadgeLabel;
    @FXML private Label probabilityValueLabel;
    @FXML private Label algorithmLabel;
    @FXML private Label historySummaryLabel;
    @FXML private VBox factorsContainer;
    @FXML private Label suggestedActionLabel;
    @FXML private Label metaPredictionId;
    @FXML private Label metaModelVersion;
    @FXML private Label metaTimestamp;
    @FXML private Button btnDispatchReminder;

    private PredictionResult predictionResult;

    public void setPredictionResult(PredictionResult result) {
        this.predictionResult = result;

        patientNameLabel.setText(result.getPatientName() + " (" + result.getPatientId() + ")");
        appointmentInfoLabel.setText(String.format("Appointment %s • %s (%s) • %s at %s",
                result.getAppointmentId(),
                result.getDoctorName(),
                result.getDepartment() != null ? result.getDepartment().getDisplayName() : "General",
                result.getAppointmentDate(),
                result.getTimeFormatted()));

        // Probability and Risk
        probabilityValueLabel.setText(String.format("%.1f%%", result.getNoShowProbability() * 100.0));

        RiskLevel level = result.getRiskLevel();
        riskBadgeLabel.setText(level.getLabel().toUpperCase());
        riskBadgeLabel.getStyleClass().removeAll("risk-badge-low", "risk-badge-medium", "risk-badge-high");
        if (level == RiskLevel.HIGH) {
            riskBadgeLabel.getStyleClass().add("risk-badge-high");
            probabilityValueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #C95F5F;");
        } else if (level == RiskLevel.MEDIUM) {
            riskBadgeLabel.getStyleClass().add("risk-badge-medium");
            probabilityValueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #D99A42;");
        } else {
            riskBadgeLabel.getStyleClass().add("risk-badge-low");
            probabilityValueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #438B70;");
        }

        algorithmLabel.setText(result.getPredictionMethod() + " (" + result.getModelVersion() + ")");
        historySummaryLabel.setText(result.getHistoricalAttendanceSummary());
        suggestedActionLabel.setText(result.getSuggestedAction());

        metaPredictionId.setText(result.getPredictionId());
        metaModelVersion.setText(result.getModelVersion());
        metaTimestamp.setText(result.getPredictedAtFormatted());

        // Populate Ranked Factor Cards
        factorsContainer.getChildren().clear();
        for (FactorExplanation factor : result.getContributingFactors()) {
            factorsContainer.getChildren().add(createFactorCard(factor));
        }
    }

    private VBox createFactorCard(FactorExplanation factor) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10px 14px; -fx-background-radius: 6px; -fx-border-color: #E4E9E8; -fx-border-radius: 6px;");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label();
        badge.getStyleClass().add("badge");

        if (factor.getDirection() == FactorExplanation.Direction.INCREASES_RISK) {
            badge.setText("▲ Increases Risk");
            badge.getStyleClass().add("badge-cancelled"); // Red tint
        } else if (factor.getDirection() == FactorExplanation.Direction.MITIGATES_RISK) {
            badge.setText("▼ Mitigates Risk");
            badge.getStyleClass().add("badge-confirmed"); // Green tint
        } else {
            badge.setText("• Baseline");
            badge.getStyleClass().add("badge-neutral");
        }

        Label title = new Label(factor.getFeatureName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #172A3A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(badge, title, spacer);

        Label desc = new Label(factor.getDescription());
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #77838D;");
        desc.setWrapText(true);

        card.getChildren().addAll(topRow, desc);
        return card;
    }

    @FXML
    private void handleDispatchReminder() {
        if (predictionResult != null) {
            NotificationManager.showSuccess(
                    "Intervention Alert Queued",
                    "Simulated administrative outreach dispatched for " + predictionResult.getPatientName() +
                            " (Appt: " + predictionResult.getAppointmentId() + ").\n\n" +
                            "Action: " + predictionResult.getSuggestedAction() + "\n" +
                            "Notification protocol logged in hospital operational registry."
            );
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) patientNameLabel.getScene().getWindow();
        stage.close();
    }
}
