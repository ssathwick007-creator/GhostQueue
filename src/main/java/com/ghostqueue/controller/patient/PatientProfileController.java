package com.ghostqueue.controller.patient;

import com.ghostqueue.model.Patient;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PatientProfileController {

    @FXML private TextField patientIdField;
    @FXML private TextField fullNameField;
    @FXML private TextField dobField;
    @FXML private TextField genderField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private Button saveButton;

    private Patient currentPatient;

    @FXML
    public void initialize() {
        loadData();
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        String patientId = session.getPatientId() != null ? session.getPatientId() : "PT-1001";

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getPatientRepo().findById(patientId);
        }).thenAccept(opt -> Platform.runLater(() -> {
            if (opt.isPresent()) {
                currentPatient = opt.get();
                patientIdField.setText(currentPatient.getId());
                fullNameField.setText(currentPatient.getFullName());
                dobField.setText(currentPatient.getDateOfBirth() != null
                        ? currentPatient.getDateOfBirth().toString() + " (" + currentPatient.getAge() + " yrs)"
                        : currentPatient.getAge() + " years old");
                genderField.setText(currentPatient.getGender() != null ? currentPatient.getGender() : "Not Specified");
                emailField.setText(currentPatient.getEmail());
                phoneField.setText(currentPatient.getPhone());
                addressField.setText(currentPatient.getAddress());
            } else {
                patientIdField.setText(patientId);
                fullNameField.setText(session.getFullName());
                emailField.setText(session.getEmail());
            }
        }));
    }

    @FXML
    private void handleSaveProfile() {
        if (currentPatient == null) return;

        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        String address = addressField.getText() != null ? addressField.getText().trim() : "";

        if (phone.isBlank()) {
            NotificationManager.showWarning("Missing Information", "Phone number cannot be blank.");
            return;
        }

        saveButton.setDisable(true);
        currentPatient.setPhone(phone);
        currentPatient.setAddress(address);

        CompletableFuture.runAsync(() -> {
            ServiceRegistry.getPatientRepo().update(currentPatient);
            Platform.runLater(() -> {
                saveButton.setDisable(false);
                NotificationManager.showSuccess("Profile Updated", "Your contact details have been successfully updated in Supabase.");
            });
        });
    }
}
