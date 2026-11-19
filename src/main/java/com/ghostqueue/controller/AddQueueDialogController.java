package com.ghostqueue.controller;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.service.QueueService;
import com.ghostqueue.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
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

    private final QueueService queueService = ServiceRegistry.getInstance().getQueueService();
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
