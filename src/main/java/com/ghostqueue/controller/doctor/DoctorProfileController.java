package com.ghostqueue.controller.doctor;

import com.ghostqueue.model.Doctor;
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

public class DoctorProfileController {

    @FXML private TextField doctorIdField;
    @FXML private TextField fullNameField;
    @FXML private TextField departmentField;
    @FXML private TextField specializationField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Button saveButton;

    private Doctor currentDoctor;

    @FXML
    public void initialize() {
        loadData();
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        String doctorId = session.getDoctorId() != null ? session.getDoctorId() : "DOC-101";

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getDoctorRepo().findById(doctorId);
        }).thenAccept(opt -> Platform.runLater(() -> {
            if (opt.isPresent()) {
                currentDoctor = opt.get();
                doctorIdField.setText(currentDoctor.getId());
                fullNameField.setText(currentDoctor.getFullName());
                departmentField.setText(currentDoctor.getDepartment());
                specializationField.setText(currentDoctor.getSpecialization());
                emailField.setText(session.getEmail());
                phoneField.setText(currentDoctor.getPhone());
            } else {
                doctorIdField.setText(doctorId);
                fullNameField.setText(session.getFullName());
                emailField.setText(session.getEmail());
            }
        }));
    }

    @FXML
    private void handleSavePhone() {
        if (currentDoctor == null) return;

        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        if (phone.isBlank()) {
            NotificationManager.showWarning("Missing Field", "Contact phone cannot be blank.");
            return;
        }

        saveButton.setDisable(true);
        currentDoctor.setPhone(phone);

        CompletableFuture.runAsync(() -> {
            ServiceRegistry.getDoctorRepo().update(currentDoctor);
            Platform.runLater(() -> {
                saveButton.setDisable(false);
                NotificationManager.showSuccess("Profile Updated", "Your contact details have been updated in Supabase.");
            });
        });
    }
}
