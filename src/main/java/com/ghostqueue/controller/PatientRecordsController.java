package com.ghostqueue.controller;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Patient;
import com.ghostqueue.service.AppointmentService;
import com.ghostqueue.service.PatientService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
import com.ghostqueue.util.UiHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class PatientRecordsController {

    @FXML private TextField searchField;
    @FXML private Label patientCountLabel;

    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> colId;
    @FXML private TableColumn<Patient, String> colName;
    @FXML private TableColumn<Patient, String> colDemographics;
    @FXML private TableColumn<Patient, String> colPhone;
    @FXML private TableColumn<Patient, String> colPhysician;
    @FXML private TableColumn<Patient, String> colApptStats;
    @FXML private TableColumn<Patient, String> colAttendanceRate;

    // Detail Panel
    @FXML private Label detailNameLabel;
    @FXML private Label detailIdLabel;
    @FXML private Label detailPhoneLabel;
    @FXML private Label detailAddressLabel;
    @FXML private Label detailPhysicianLabel;
    @FXML private Label detailNotesLabel;

    @FXML private Button btnEditPatient;
    @FXML private Button btnDeletePatient;

    @FXML private TableView<Appointment> patientHistoryTable;
    @FXML private TableColumn<Appointment, String> colHistId;
    @FXML private TableColumn<Appointment, String> colHistDate;
    @FXML private TableColumn<Appointment, String> colHistDoctor;
    @FXML private TableColumn<Appointment, AppointmentStatus> colHistStatus;

    private final PatientService patientService = ServiceRegistry.getInstance().getPatientService();
    private final AppointmentService appointmentService = ServiceRegistry.getInstance().getAppointmentService();

    @FXML
    public void initialize() {
        setupPatientTable();
        setupHistoryTable();
        setupListeners();
        loadPatients();
    }

    private void setupPatientTable() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        colDemographics.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAge() + " yrs / " + data.getValue().getGender()
        ));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colPhysician.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrimaryPhysician()));
        colApptStats.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAttendedAppointments() + " of " + data.getValue().getTotalAppointments() + " attended"
        ));
        colAttendanceRate.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.1f%%", data.getValue().getAttendanceRate())
        ));
    }

    private void setupHistoryTable() {
        colHistId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colHistDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateFormatted()));
        colHistDoctor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDoctorName()));

        colHistStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        colHistStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AppointmentStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UiHelper.createStatusBadge(status));
                }
            }
        });
    }

    private void setupListeners() {
        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selectedPatient) -> {
            if (selectedPatient != null) {
                displayPatientDetails(selectedPatient);
                btnEditPatient.setDisable(false);
                btnDeletePatient.setDisable(false);
            } else {
                btnEditPatient.setDisable(true);
                btnDeletePatient.setDisable(true);
            }
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            List<Patient> results = patientService.searchPatients(newV);
            patientTable.setItems(FXCollections.observableArrayList(results));
            patientCountLabel.setText("Showing " + results.size() + " patients");
        });
    }

    private void displayPatientDetails(Patient p) {
        detailNameLabel.setText(p.getFullName());
        detailIdLabel.setText("MRN / ID: " + p.getId() + " • Age: " + p.getAge() + " (" + p.getGender() + ")");
        detailPhoneLabel.setText("Contact: " + p.getPhone() + (p.getEmail() != null && !p.getEmail().isBlank() ? " • " + p.getEmail() : ""));
        detailAddressLabel.setText("Address: " + (p.getAddress() != null && !p.getAddress().isBlank() ? p.getAddress() : "Not Specified"));
        detailPhysicianLabel.setText("Primary Physician: " + p.getPrimaryPhysician());
        detailNotesLabel.setText(p.getMedicalNotes() + "\nLast clinic visit on " + p.getLastVisitDate());

        List<Appointment> history = appointmentService.getAppointmentsForPatient(p.getId());
        patientHistoryTable.setItems(FXCollections.observableArrayList(history));
    }

    public void loadPatients() {
        List<Patient> patients = patientService.getAllPatients();
        patientTable.setItems(FXCollections.observableArrayList(patients));
        patientCountLabel.setText("Total: " + patients.size() + " registered patients");
        if (!patients.isEmpty()) {
            patientTable.getSelectionModel().select(0);
        } else {
            btnEditPatient.setDisable(true);
            btnDeletePatient.setDisable(true);
        }
    }

    @FXML
    private void handleRegisterPatient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/patient_form_dialog.fxml"));
            Parent root = loader.load();
            PatientFormDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Register New Patient — GhostQueue");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadPatients();
                NotificationManager.showSuccess("Patient Registered", "Patient was successfully registered into the MySQL database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Dialog Error", "Unable to open patient registration form: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditPatient() {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.showWarning("Selection Required", "Please select a patient to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/patient_form_dialog.fxml"));
            Parent root = loader.load();
            PatientFormDialogController controller = loader.getController();
            controller.setPatientForEdit(selected);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Patient Profile — GhostQueue");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadPatients();
                NotificationManager.showSuccess("Patient Updated", "Patient information updated successfully in MySQL.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Dialog Error", "Unable to open edit dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeletePatient() {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.showWarning("Selection Required", "Please select a patient to delete.");
            return;
        }

        // Check referential integrity / clinical audit constraint
        if (!patientService.canDeletePatient(selected.getId())) {
            NotificationManager.showWarning(
                    "Action Prohibited — Active Records Exist",
                    "Cannot delete patient " + selected.getFullName() + " (" + selected.getId() + ").\n\n" +
                            "This patient has associated clinical appointments or queue history. " +
                            "Hospital data retention policies require maintaining clinical audit trails."
            );
            return;
        }

        boolean confirmed = NotificationManager.showConfirmation(
                "Delete Patient Record",
                "Permanently delete patient " + selected.getFullName() + " (" + selected.getId() + ")?",
                "This action cannot be undone. Are you sure you wish to proceed?"
        );

        if (confirmed) {
            try {
                patientService.deletePatient(selected.getId());
                loadPatients();
                NotificationManager.showSuccess("Patient Deleted", "Patient record " + selected.getId() + " was deleted from the database.");
            } catch (Exception e) {
                NotificationManager.showWarning("Delete Error", "Could not delete patient: " + e.getMessage());
            }
        }
    }
}
