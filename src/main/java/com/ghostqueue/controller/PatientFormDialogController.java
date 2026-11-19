package com.ghostqueue.controller;

import com.ghostqueue.model.Patient;
import com.ghostqueue.service.PatientService;
import com.ghostqueue.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class PatientFormDialogController {

    @FXML private Label dialogTitleLabel;
    @FXML private Label dialogSubtitleLabel;
    @FXML private TextField patientIdField;
    @FXML private TextField fullNameField;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private Label validationMessageLabel;
    @FXML private Button saveButton;

    private final PatientService patientService = ServiceRegistry.getInstance().getPatientService();
    private boolean editMode = false;
    private boolean saved = false;

    @FXML
    public void initialize() {
        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderCombo.getSelectionModel().select("Female");

        // Generate default ID for new patients
        int rand = 100 + (int)(Math.random() * 900);
        patientIdField.setText("PAT-" + rand);
    }

    public void setPatientForEdit(Patient patient) {
        this.editMode = true;
        dialogTitleLabel.setText("Edit Patient Profile");
        dialogSubtitleLabel.setText("Update demographic and contact records for MRN " + patient.getId());
        saveButton.setText("Update Patient");

        patientIdField.setText(patient.getId());
        patientIdField.setDisable(true);
        fullNameField.setText(patient.getFullName());
        ageField.setText(String.valueOf(patient.getAge()));
        genderCombo.setValue(patient.getGender());
        phoneField.setText(patient.getPhone());
        emailField.setText(patient.getEmail() != null ? patient.getEmail() : "");
        addressField.setText(patient.getAddress() != null ? patient.getAddress() : "");
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        String id = patientIdField.getText();
        String name = fullNameField.getText();
        String ageStr = ageField.getText();
        String gender = genderCombo.getValue();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String address = addressField.getText();

        if (id == null || id.isBlank() || name == null || name.isBlank() || ageStr == null || ageStr.isBlank() || phone == null || phone.isBlank()) {
            validationMessageLabel.setText("Please fill in all mandatory fields (*).");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr.trim());
            if (age < 0 || age > 125) {
                validationMessageLabel.setText("Patient age must be between 0 and 125.");
                return;
            }
        } catch (NumberFormatException e) {
            validationMessageLabel.setText("Age must be a valid whole number.");
            return;
        }

        Patient patient = new Patient(
                id.trim(),
                name.trim(),
                age,
                gender != null ? gender : "Other",
                phone.trim(),
                (email != null && !email.isBlank()) ? email.trim() : null,
                (address != null && !address.isBlank()) ? address.trim() : null,
                LocalDateTime.now()
        );

        try {
            if (editMode) {
                patientService.updatePatient(patient);
            } else {
                patientService.registerPatient(patient);
            }
            saved = true;
            closeStage();
        } catch (IllegalArgumentException ex) {
            validationMessageLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            validationMessageLabel.setText("Database error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        saved = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) patientIdField.getScene().getWindow();
        stage.close();
    }
}
