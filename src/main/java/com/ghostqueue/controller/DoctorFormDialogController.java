package com.ghostqueue.controller;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.Doctor;
import com.ghostqueue.service.DoctorService;
import com.ghostqueue.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class DoctorFormDialogController {

    @FXML private Label dialogTitleLabel;
    @FXML private Label dialogSubtitleLabel;
    @FXML private TextField doctorIdField;
    @FXML private TextField doctorNameField;
    @FXML private ComboBox<String> departmentCombo;
    @FXML private TextField specializationField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> availabilityCombo;
    @FXML private Label validationMessageLabel;
    @FXML private Button saveButton;

    private final DoctorService doctorService = ServiceRegistry.getInstance().getDoctorService();
    private boolean editMode = false;
    private boolean saved = false;

    @FXML
    public void initialize() {
        departmentCombo.setItems(FXCollections.observableArrayList(
                "Cardiology", "Neurology", "Orthopedics", "Oncology", "Pediatrics", "General Medicine", "Dermatology"
        ));
        departmentCombo.getSelectionModel().select("Cardiology");

        availabilityCombo.setItems(FXCollections.observableArrayList("AVAILABLE", "ON_LEAVE", "BUSY"));
        availabilityCombo.getSelectionModel().select("AVAILABLE");

        // Suggest next doctor ID
        int nextNum = doctorService.countTotal() + 1;
        doctorIdField.setText(String.format("DOC-%03d", nextNum));
    }

    public void setDoctorForEdit(Doctor doctor) {
        this.editMode = true;
        dialogTitleLabel.setText("Edit Doctor Profile");
        dialogSubtitleLabel.setText("Update clinical assignments and contact details for " + doctor.getId());
        saveButton.setText("Update Doctor");

        doctorIdField.setText(doctor.getId());
        doctorIdField.setDisable(true);
        doctorNameField.setText(doctor.getFullName());
        departmentCombo.setValue(doctor.getDepartment());
        specializationField.setText(doctor.getSpecialization());
        phoneField.setText(doctor.getPhone());
        availabilityCombo.setValue(doctor.getAvailabilityStatus());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        String id = doctorIdField.getText();
        String name = doctorNameField.getText();
        String dept = departmentCombo.getValue();
        String spec = specializationField.getText();
        String phone = phoneField.getText();
        String status = availabilityCombo.getValue();

        if (id == null || id.isBlank() || name == null || name.isBlank() ||
                dept == null || dept.isBlank() || spec == null || spec.isBlank() || phone == null || phone.isBlank()) {
            validationMessageLabel.setText("Please fill in all mandatory fields (*).");
            return;
        }

        Doctor doc = new Doctor(
                id.trim(),
                name.trim(),
                dept.trim(),
                spec.trim(),
                phone.trim(),
                status != null ? status : "AVAILABLE"
        );

        try {
            if (editMode) {
                doctorService.updateDoctor(doc);
            } else {
                doctorService.registerDoctor(doc);
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
        Stage stage = (Stage) doctorIdField.getScene().getWindow();
        stage.close();
    }
}
