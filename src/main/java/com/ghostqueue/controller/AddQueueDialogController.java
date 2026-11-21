package com.ghostqueue.controller;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.service.QueueService;
import com.ghostqueue.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class AddQueueDialogController {

    @FXML private TextField nameField;
    @FXML private TextField idField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<QueuePriority> priorityCombo;
    @FXML private ComboBox<Department> departmentCombo;
    @FXML private TextField preferredDoctorField;
    @FXML private ComboBox<String> timeSlotCombo;
    @FXML private TextField clinicalNoteField;
    @FXML private Label validationMsg;
    @FXML private Button btnSubmit;

    private final QueueService queueService = ServiceRegistry.getInstance().getQueueService();
    private QueueEntry editEntry;
    private boolean saved = false;

    @FXML
    public void initialize() {
        priorityCombo.setItems(FXCollections.observableArrayList(
                QueuePriority.URGENT, QueuePriority.HIGH, QueuePriority.STANDARD
        ));
        priorityCombo.getSelectionModel().select(QueuePriority.HIGH);

        departmentCombo.setItems(FXCollections.observableArrayList(
                Department.CARDIOLOGY, Department.NEUROLOGY, Department.ORTHOPEDICS,
                Department.ONCOLOGY, Department.PEDIATRICS, Department.GENERAL_MEDICINE, Department.DERMATOLOGY
        ));
        departmentCombo.getSelectionModel().select(Department.CARDIOLOGY);

        timeSlotCombo.setItems(FXCollections.observableArrayList(
                "Morning (08:00 - 12:00)", "Afternoon (13:00 - 17:00)", "Anytime (Immediate Standby)"
        ));
        timeSlotCombo.getSelectionModel().select(0);
    }

    public void setEditEntry(QueueEntry entry) {
        this.editEntry = entry;
        if (entry != null) {
            nameField.setText(entry.getPatientName());
            nameField.setDisable(true); // Don't change patient link
            idField.setText(entry.getPatientId());
            idField.setDisable(true);
            phoneField.setText(entry.getPatientPhone());
            phoneField.setDisable(true);
            priorityCombo.getSelectionModel().select(entry.getPriority());
            departmentCombo.getSelectionModel().select(entry.getRequestedDepartment());
            preferredDoctorField.setText(entry.getPreferredDoctor());
            timeSlotCombo.getSelectionModel().select(entry.getPreferredTimeSlot());
            clinicalNoteField.setText(entry.getClinicalNote());
            if (btnSubmit != null) {
                btnSubmit.setText("Update Queue Entry");
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText();
        String id = idField.getText();
        String phone = phoneField.getText();
        QueuePriority priority = priorityCombo.getValue();
        Department dept = departmentCombo.getValue();
        String doc = preferredDoctorField.getText();
        String time = timeSlotCombo.getValue();
        String note = clinicalNoteField.getText();

        if (name == null || name.isBlank() || id == null || id.isBlank()) {
            validationMsg.setText("Patient name and ID are required.");
            return;
        }

        if (editEntry != null) {
            editEntry.setPriority(priority);
            editEntry.setRequestedDepartment(dept);
            editEntry.setPreferredDoctor((doc != null && !doc.isBlank()) ? doc.trim() : "Any Available Specialist");
            editEntry.setPreferredTimeSlot(time);
            editEntry.setClinicalNote((note != null && !note.isBlank()) ? note.trim() : "Standard waiting triage");
            queueService.updateQueueEntry(editEntry);
            saved = true;
            closeStage();
            return;
        }

        String queueId = "WQ-" + (310 + (int)(Math.random() * 200));
        QueueEntry entry = new QueueEntry(
                queueId, id.trim(), name.trim(), phone != null ? phone.trim() : "",
                priority, dept, (doc != null && !doc.isBlank()) ? doc.trim() : "Any Available Specialist",
                time, LocalDateTime.now(), QueueStatus.WAITING,
                (note != null && !note.isBlank()) ? note.trim() : "Standard waiting queue triage"
        );

        queueService.addToQueue(entry);
        saved = true;
        closeStage();
    }

    @FXML
    private void handleCancel() {
        saved = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
