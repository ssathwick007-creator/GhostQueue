package com.ghostqueue.controller.doctor;

import com.ghostqueue.model.Doctor;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DoctorAvailabilityController {

    @FXML private RadioButton radioAvailable;
    @FXML private RadioButton radioBusy;
    @FXML private RadioButton radioLeave;
    private ToggleGroup statusGroup;

    @FXML private CheckBox checkMon;
    @FXML private CheckBox checkTue;
    @FXML private CheckBox checkWed;
    @FXML private CheckBox checkThu;
    @FXML private CheckBox checkFri;
    @FXML private CheckBox checkSat;

    @FXML private ComboBox<String> startTimeCombo;
    @FXML private ComboBox<String> endTimeCombo;
    @FXML private ComboBox<String> slotDurationCombo;
    @FXML private Button saveBtn;

    private DoctorLayoutController layoutController;
    private Doctor currentDoctor;

    public void setLayoutController(DoctorLayoutController layoutController) {
        this.layoutController = layoutController;
    }

    @FXML
    public void initialize() {
        statusGroup = new ToggleGroup();
        radioAvailable.setToggleGroup(statusGroup);
        radioBusy.setToggleGroup(statusGroup);
        radioLeave.setToggleGroup(statusGroup);

        radioAvailable.setSelected(true);

        startTimeCombo.setItems(FXCollections.observableArrayList("08:00 AM", "08:30 AM", "09:00 AM", "09:30 AM", "10:00 AM"));
        startTimeCombo.setValue("09:00 AM");

        endTimeCombo.setItems(FXCollections.observableArrayList("04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM"));
        endTimeCombo.setValue("05:00 PM");

        slotDurationCombo.setItems(FXCollections.observableArrayList("20 minutes", "30 minutes (Standard)", "45 minutes", "60 minutes"));
        slotDurationCombo.setValue("30 minutes (Standard)");

        loadData();
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        String docId = session.getDoctorId() != null ? session.getDoctorId() : "DOC-101";

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getDoctorRepo().findById(docId);
        }).thenAccept(opt -> Platform.runLater(() -> {
            if (opt.isPresent()) {
                currentDoctor = opt.get();
                if (currentDoctor.isAvailable()) {
                    radioAvailable.setSelected(true);
                } else {
                    radioBusy.setSelected(true);
                }
            }
        }));
    }

    @FXML
    private void handleSaveAvailability() {
        boolean isAvail = radioAvailable.isSelected();
        String statusText = isAvail ? "Available for Consultations" : (radioBusy.isSelected() ? "Busy / In Clinic" : "On Leave");

        if (currentDoctor != null) {
            currentDoctor.setAvailable(isAvail);
            saveBtn.setDisable(true);

            CompletableFuture.runAsync(() -> {
                ServiceRegistry.getDoctorRepo().update(currentDoctor);
            }).thenRun(() -> Platform.runLater(() -> {
                saveBtn.setDisable(false);
                if (layoutController != null) {
                    layoutController.setStatusPill(statusText);
                }
                NotificationManager.showSuccess("Availability Saved",
                        "Your practice status is now: " + statusText);
            }));
        } else {
            if (layoutController != null) {
                layoutController.setStatusPill(statusText);
            }
            NotificationManager.showSuccess("Availability Saved", "Your practice status is now: " + statusText);
        }
    }
}
