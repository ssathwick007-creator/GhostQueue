package com.ghostqueue.controller;

import com.ghostqueue.service.PredictionService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.service.SettingsService;
import com.ghostqueue.util.NotificationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the Hospital Facility Configuration & Administrative Settings Dashboard.
 * Persists operating hours, campus metadata, and predictive model risk thresholds in MySQL.
 */
public class SettingsController {

    @FXML private TextField hospitalNameField;
    @FXML private TextField campusField;
    @FXML private ComboBox<String> timezoneCombo;
    @FXML private TextField hoursField;

    @FXML private Slider highRiskSlider;
    @FXML private Label highRiskValueLabel;
    @FXML private Slider medRiskSlider;
    @FXML private Label medRiskValueLabel;
    @FXML private CheckBox autoStandbyCheck;

    @FXML private CheckBox requireConfirmationCheck;
    @FXML private CheckBox prioritizeUrgentCheck;
    @FXML private CheckBox smsPatientAlertCheck;

    private final SettingsService settingsService = ServiceRegistry.getInstance().getSettingsService();
    private final PredictionService predictionService = ServiceRegistry.getInstance().getPredictionService();

    @FXML
    public void initialize() {
        timezoneCombo.setItems(FXCollections.observableArrayList(
                "Eastern Time (US & Canada, UTC-05:00)",
                "Central Time (US & Canada, UTC-06:00)",
                "Pacific Time (US & Canada, UTC-08:00)",
                "UTC (Coordinated Universal Time)",
                "IST (Indian Standard Time, UTC+05:30)"
        ));

        highRiskSlider.valueProperty().addListener((obs, oldV, newV) -> {
            highRiskValueLabel.setText(String.format("%.0f%%", newV.doubleValue()));
        });

        medRiskSlider.valueProperty().addListener((obs, oldV, newV) -> {
            medRiskValueLabel.setText(String.format("%.0f%%", newV.doubleValue()));
        });

        loadSettingsFromDatabase();
    }

    private void loadSettingsFromDatabase() {
        settingsService.loadSettings();

        hospitalNameField.setText(settingsService.getString(SettingsService.KEY_HOSPITAL_NAME, "Metropolitan Academic Medical Center"));
        campusField.setText(settingsService.getString(SettingsService.KEY_HOSPITAL_CAMPUS, "Main Medical Pavilion (North Tower)"));
        hoursField.setText(settingsService.getString(SettingsService.KEY_HOSPITAL_HOURS, "08:00 AM - 17:30 PM (Mon - Sat)"));

        String savedTz = settingsService.getString(SettingsService.KEY_HOSPITAL_TIMEZONE, "Eastern Time (US & Canada, UTC-05:00)");
        if (timezoneCombo.getItems().contains(savedTz)) {
            timezoneCombo.getSelectionModel().select(savedTz);
        } else {
            timezoneCombo.getSelectionModel().select(0);
        }

        double highThresholdPercent = settingsService.getHighRiskThreshold() * 100.0;
        double medThresholdPercent = settingsService.getMediumRiskThreshold() * 100.0;

        highRiskSlider.setValue(highThresholdPercent);
        highRiskValueLabel.setText(String.format("%.0f%%", highThresholdPercent));

        medRiskSlider.setValue(medThresholdPercent);
        medRiskValueLabel.setText(String.format("%.0f%%", medThresholdPercent));

        autoStandbyCheck.setSelected(settingsService.getBoolean(SettingsService.KEY_REMINDERS_AUTO_HIGH_RISK, true));
        requireConfirmationCheck.setSelected(settingsService.getBoolean(SettingsService.KEY_REALLOC_STRICT_CONFIRM, true));
        prioritizeUrgentCheck.setSelected(settingsService.getInt(SettingsService.KEY_REALLOC_URGENT_BOOST, 20) > 0);
        smsPatientAlertCheck.setSelected(true);
    }

    @FXML
    private void handleSaveSettings() {
        String hospital = hospitalNameField.getText();
        String campus = campusField.getText();
        String hours = hoursField.getText();
        String timezone = timezoneCombo.getValue();

        if (hospital == null || hospital.isBlank()) {
            NotificationManager.showWarning("Validation Error", "Hospital name cannot be blank.");
            return;
        }

        double highRisk = highRiskSlider.getValue();
        double medRisk = medRiskSlider.getValue();

        if (medRisk >= highRisk) {
            NotificationManager.showWarning("Threshold Conflict",
                    "Medium-risk threshold (" + String.format("%.0f%%", medRisk) +
                            ") must be lower than High-risk threshold (" + String.format("%.0f%%", highRisk) + ").");
            return;
        }

        Map<String, String> map = new HashMap<>();
        map.put(SettingsService.KEY_HOSPITAL_NAME, hospital.trim());
        map.put(SettingsService.KEY_HOSPITAL_CAMPUS, campus != null ? campus.trim() : "");
        map.put(SettingsService.KEY_HOSPITAL_HOURS, hours != null ? hours.trim() : "");
        map.put(SettingsService.KEY_HOSPITAL_TIMEZONE, timezone != null ? timezone : "Eastern Time (US & Canada, UTC-05:00)");
        map.put(SettingsService.KEY_THRESHOLD_HIGH_RISK, String.format(java.util.Locale.US, "%.2f", highRisk / 100.0));
        map.put(SettingsService.KEY_THRESHOLD_MEDIUM_RISK, String.format(java.util.Locale.US, "%.2f", medRisk / 100.0));
        map.put(SettingsService.KEY_REALLOC_STRICT_CONFIRM, String.valueOf(requireConfirmationCheck.isSelected()));
        map.put(SettingsService.KEY_REALLOC_URGENT_BOOST, prioritizeUrgentCheck.isSelected() ? "20" : "0");
        map.put(SettingsService.KEY_REMINDERS_AUTO_HIGH_RISK, String.valueOf(autoStandbyCheck.isSelected()));

        settingsService.saveSettings(map);

        // Dynamically update model prediction engine thresholds without app restart
        predictionService.updateRiskThresholds(medRisk / 100.0, highRisk / 100.0);

        NotificationManager.showSuccess("Settings Saved",
                "Hospital facility configuration and predictive model thresholds saved to MySQL system_settings.\n\n" +
                        "Facility: " + hospital + " (" + campus + ")\n" +
                        "Classification Thresholds: Med ≥ " + String.format("%.0f%%", medRisk) + " | High ≥ " + String.format("%.0f%%", highRisk) + "\n" +
                        "Active appointments automatically reclassified.");
    }

    @FXML
    private void handleOpenDoctorManagement() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/dialogs/doctor_management_dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Medical Staff & Doctor Management — GhostQueue");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(root));
            dialogStage.setResizable(true);
            dialogStage.showAndWait();
        } catch (Exception e) {
            NotificationManager.showWarning("Error", "Could not open doctor management: " + e.getMessage());
        }
    }

    @FXML
    private void handleResetDefaults() {
        settingsService.resetToDefaults();
        loadSettingsFromDatabase();

        // Sync with prediction engine
        predictionService.updateRiskThresholds(0.30, 0.65);

        NotificationManager.showInfo("Defaults Restored",
                "Hospital settings and model thresholds reverted to enterprise baseline recommendations (30% Med, 65% High).");
    }
}
