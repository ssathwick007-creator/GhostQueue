package com.ghostqueue.controller;

import com.ghostqueue.util.NotificationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    @FXML
    public void initialize() {
        timezoneCombo.setItems(FXCollections.observableArrayList(
                "Eastern Time (US & Canada, UTC-05:00)",
                "Central Time (US & Canada, UTC-06:00)",
                "Pacific Time (US & Canada, UTC-08:00)",
                "UTC (Coordinated Universal Time)",
                "IST (Indian Standard Time, UTC+05:30)"
        ));
        timezoneCombo.getSelectionModel().select(0);

        highRiskSlider.valueProperty().addListener((obs, oldV, newV) -> {
            highRiskValueLabel.setText(String.format("%.0f%%", newV.doubleValue()));
        });

        medRiskSlider.valueProperty().addListener((obs, oldV, newV) -> {
            medRiskValueLabel.setText(String.format("%.0f%%", newV.doubleValue()));
        });
    }

    @FXML
    private void handleSaveSettings() {
        String hospital = hospitalNameField.getText();
        String campus = campusField.getText();

        if (hospital == null || hospital.isBlank()) {
            NotificationManager.showWarning("Validation Error", "Hospital name cannot be blank.");
            return;
        }

        NotificationManager.showSuccess("Settings Saved",
                "Hospital facility configuration and predictive model thresholds updated successfully.\n\n" +
                        "Facility: " + hospital + " (" + campus + ")\n" +
                        "High-Risk Trigger: " + highRiskValueLabel.getText() + "\n" +
                        "Strict Operator Confirmation: " + (requireConfirmationCheck.isSelected() ? "Enabled (Active)" : "Disabled"));
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
        hospitalNameField.setText("Metropolitan Academic Medical Center");
        campusField.setText("Main Medical Pavilion (North Tower)");
        timezoneCombo.getSelectionModel().select(0);
        hoursField.setText("08:00 AM - 17:30 PM (Mon - Sat)");
        highRiskSlider.setValue(70);
        medRiskSlider.setValue(35);
        autoStandbyCheck.setSelected(true);
        requireConfirmationCheck.setSelected(true);
        prioritizeUrgentCheck.setSelected(true);
        smsPatientAlertCheck.setSelected(true);

        NotificationManager.showInfo("Defaults Restored", "All settings reverted to enterprise baseline recommendations.");
    }
}
