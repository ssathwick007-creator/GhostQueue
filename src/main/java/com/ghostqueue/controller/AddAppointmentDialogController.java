package com.ghostqueue.controller;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.Doctor;
import com.ghostqueue.model.Patient;
import com.ghostqueue.service.AppointmentService;
import com.ghostqueue.service.DoctorService;
import com.ghostqueue.service.PatientService;
import com.ghostqueue.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class AddAppointmentDialogController {

    @FXML private ComboBox<Patient> patientSelectCombo;
    @FXML private TextField patientNameField;
    @FXML private TextField patientIdField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<Department> departmentCombo;
    @FXML private ComboBox<Doctor> doctorCombo;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    @FXML private TextField reasonField;
    @FXML private Label validationMessageLabel;

    private final PatientService patientService = ServiceRegistry.getInstance().getPatientService();
    private final DoctorService doctorService = ServiceRegistry.getInstance().getDoctorService();
    private final AppointmentService appointmentService = ServiceRegistry.getInstance().getAppointmentService();
    private boolean saved = false;

    @FXML
    public void initialize() {
        setupPatientCombo();
        setupDepartmentCombo();
        setupDoctorCombo();
        setupTimeCombo();

        datePicker.setValue(LocalDate.now());
    }

    private void setupPatientCombo() {
        List<Patient> patients = patientService.getAllPatients();
        patientSelectCombo.setItems(FXCollections.observableArrayList(patients));
        patientSelectCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Patient p) {
                return p == null ? "" : p.getFullName() + " (" + p.getId() + ")";
            }

            @Override
            public Patient fromString(String string) {
                return null;
            }
        });

        patientSelectCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                patientNameField.setText(selected.getFullName());
                patientIdField.setText(selected.getId());
                phoneField.setText(selected.getPhone());
                validationMessageLabel.setText("");
            }
        });
    }

    private void setupDepartmentCombo() {
        departmentCombo.setItems(FXCollections.observableArrayList(
                Department.CARDIOLOGY, Department.NEUROLOGY, Department.ORTHOPEDICS,
                Department.ONCOLOGY, Department.PEDIATRICS, Department.GENERAL_MEDICINE, Department.DERMATOLOGY
        ));
        departmentCombo.getSelectionModel().select(Department.CARDIOLOGY);

        departmentCombo.setOnAction(e -> updateDoctorOptions());
    }

    private void setupDoctorCombo() {
        doctorCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Doctor d) {
                return d == null ? "" : d.getFullName() + " (" + d.getDepartment() + " - " + d.getAvailabilityStatus() + ")";
            }

            @Override
            public Doctor fromString(String string) {
                return null;
            }
        });
        updateDoctorOptions();
    }

    private void updateDoctorOptions() {
        Department dept = departmentCombo.getValue();
        List<Doctor> allDoctors = doctorService.getAllDoctors();

        List<Doctor> filtered = (dept != null && dept != Department.ALL) ?
                allDoctors.stream().filter(d -> d.getDepartment().equalsIgnoreCase(dept.getDisplayName())).collect(Collectors.toList()) :
                allDoctors;

        if (filtered.isEmpty()) {
            filtered = allDoctors;
        }

        doctorCombo.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            doctorCombo.getSelectionModel().select(0);
        }
    }

    private void setupTimeCombo() {
        timeCombo.setItems(FXCollections.observableArrayList(
                "08:30 AM", "09:15 AM", "10:00 AM", "10:45 AM", "11:30 AM",
                "01:15 PM", "02:00 PM", "02:45 PM", "03:30 PM", "04:15 PM"
        ));
        timeCombo.getSelectionModel().select("09:15 AM");
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        validationMessageLabel.setText("");

        String name = patientNameField.getText();
        String id = patientIdField.getText();
        String phone = phoneField.getText();
        Department dept = departmentCombo.getValue();
        Doctor doc = doctorCombo.getValue();
        LocalDate date = datePicker.getValue();
        String timeStr = timeCombo.getValue();
        String reason = reasonField.getText();

        if (name == null || name.isBlank() || id == null || id.isBlank() || doc == null || date == null || timeStr == null) {
            validationMessageLabel.setText("Please fill in all mandatory fields (*).");
            return;
        }

        LocalTime time = parseTime(timeStr);
        String appointmentId = "APT-" + (830 + (int)(Math.random() * 800));

        // Ensure patient exists in repository or auto-register new patient
        if (!patientService.getPatientById(id.trim()).isPresent()) {
            try {
                Patient newPatient = new Patient(
                        id.trim(), name.trim(), 40, "Other", phone != null ? phone.trim() : "555-0100",
                        null, null, LocalDateTime.now()
                );
                patientService.registerPatient(newPatient);
            } catch (Exception ignored) {
                // Ignore if duplicate phone or exists
            }
        }

        Appointment apt = new Appointment(
                appointmentId,
                id.trim(),
                name.trim(),
                45,
                phone != null ? phone.trim() : "",
                doc.getId(),
                doc.getFullName(),
                dept != null ? dept : Department.GENERAL_MEDICINE,
                date,
                time,
                AppointmentStatus.CONFIRMED,
                (reason != null && !reason.isBlank()) ? reason.trim() : "Routine Clinical Evaluation",
                7,
                0.20,
                List.of("Confirmed patient registration")
        );

        try {
            appointmentService.createAppointment(apt);
            saved = true;
            closeStage();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Clearly communicate booking failure / conflict / past date to user
            validationMessageLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            validationMessageLabel.setText("System error scheduling appointment: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        saved = false;
        closeStage();
    }

    private LocalTime parseTime(String timeStr) {
        try {
            String[] parts = timeStr.split(" ");
            String[] hm = parts[0].split(":");
            int hour = Integer.parseInt(hm[0]);
            int minute = Integer.parseInt(hm[1]);
            if (parts[1].equalsIgnoreCase("PM") && hour < 12) hour += 12;
            if (parts[1].equalsIgnoreCase("AM") && hour == 12) hour = 0;
            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            return LocalTime.of(9, 15);
        }
    }

    private void closeStage() {
        Stage stage = (Stage) patientNameField.getScene().getWindow();
        stage.close();
    }
}
