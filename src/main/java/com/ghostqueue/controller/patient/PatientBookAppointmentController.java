package com.ghostqueue.controller.patient;

import com.ghostqueue.model.*;
import com.ghostqueue.service.NotificationService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PatientBookAppointmentController {

    @FXML private ComboBox<Department> departmentCombo;
    @FXML private ComboBox<Doctor> doctorCombo;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private Label doctorDetailsLabel;
    @FXML private Label selectedSlotLabel;
    @FXML private FlowPane slotsFlowPane;
    @FXML private TextArea reasonArea;
    @FXML private Button confirmBookingBtn;

    private LocalTime selectedTime;
    private final List<Doctor> allDoctors = new ArrayList<>();
    private final List<String> standardSlots = List.of(
            "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "14:00", "14:30", "15:00", "15:30", "16:00"
    );

    @FXML
    public void initialize() {
        // Setup departments
        List<Department> depts = new ArrayList<>();
        for (Department d : Department.values()) {
            if (d != Department.ALL) depts.add(d);
        }
        departmentCombo.setItems(FXCollections.observableArrayList(depts));
        departmentCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Department object) {
                return object != null ? object.getDisplayName() : "";
            }
            @Override
            public Department fromString(String string) { return null; }
        });

        // Setup doctor converter
        doctorCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Doctor doctor) {
                return doctor != null ? doctor.getFullName() + " (" + doctor.getSpecialization() + ")" : "";
            }
            @Override
            public Doctor fromString(String string) { return null; }
        });

        // Setup date picker
        appointmentDatePicker.setValue(LocalDate.now().plusDays(1));
        appointmentDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Listeners
        departmentCombo.setOnAction(e -> filterDoctorsByDepartment());
        doctorCombo.setOnAction(e -> {
            updateDoctorDetails();
            refreshAvailableSlots();
        });
        appointmentDatePicker.setOnAction(e -> refreshAvailableSlots());

        loadDoctors();
    }

    private void loadDoctors() {
        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getDoctorRepo().findAll();
        }).thenAccept(docs -> Platform.runLater(() -> {
            allDoctors.clear();
            allDoctors.addAll(docs);
            if (!departmentCombo.getItems().isEmpty()) {
                departmentCombo.setValue(departmentCombo.getItems().get(0));
                filterDoctorsByDepartment();
            }
        }));
    }

    private void filterDoctorsByDepartment() {
        Department selDept = departmentCombo.getValue();
        if (selDept == null) return;

        List<Doctor> filtered = new ArrayList<>();
        for (Doctor doc : allDoctors) {
            if (doc.getDepartment() != null && doc.getDepartment().equalsIgnoreCase(selDept.name())
                    || doc.getSpecialization() != null && doc.getSpecialization().toLowerCase().contains(selDept.getDisplayName().toLowerCase())) {
                filtered.add(doc);
            }
        }

        if (filtered.isEmpty()) {
            filtered.addAll(allDoctors);
        }

        doctorCombo.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            doctorCombo.setValue(filtered.get(0));
            updateDoctorDetails();
        } else {
            doctorDetailsLabel.setText("No specialists registered in this department.");
        }
        refreshAvailableSlots();
    }

    private void updateDoctorDetails() {
        Doctor doc = doctorCombo.getValue();
        if (doc != null) {
            doctorDetailsLabel.setText(doc.getFullName() + " • " + doc.getSpecialization() +
                    " • Status: " + (doc.isAvailable() ? "Available" : "On Duty"));
        }
    }

    private void refreshAvailableSlots() {
        slotsFlowPane.getChildren().clear();
        selectedTime = null;
        selectedSlotLabel.setText("No slot selected");

        Doctor doc = doctorCombo.getValue();
        LocalDate date = appointmentDatePicker.getValue();
        if (doc == null || date == null) return;

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getAppointmentRepo().findByDoctor(doc.getId());
        }).thenAccept(appts -> Platform.runLater(() -> {
            Set<String> occupiedTimes = new HashSet<>();
            for (Appointment a : appts) {
                if (date.equals(a.getAppointmentDate()) && a.getStatus() != AppointmentStatus.CANCELLED) {
                    if (a.getAppointmentTime() != null) {
                        occupiedTimes.add(a.getAppointmentTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    }
                }
            }

            for (String slotStr : standardSlots) {
                Button slotBtn = new Button(slotStr);
                boolean isOccupied = occupiedTimes.contains(slotStr);

                if (isOccupied) {
                    slotBtn.setText(slotStr + " (Booked)");
                    slotBtn.setDisable(true);
                    slotBtn.setStyle("-fx-background-color: #F0F4F8; -fx-text-fill: #A0B2C6; -fx-font-size: 11.5px; -fx-padding: 8px 12px; -fx-background-radius: 6px;");
                } else {
                    slotBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #142A3A; -fx-border-color: #D1DDE8; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8px 14px; -fx-cursor: hand;");
                    slotBtn.setOnAction(e -> selectSlot(slotBtn, slotStr));
                }
                slotsFlowPane.getChildren().add(slotBtn);
            }
        }));
    }

    private void selectSlot(Button btn, String slotStr) {
        selectedTime = LocalTime.parse(slotStr);
        selectedSlotLabel.setText(slotStr + " (Selected)");

        // Reset styling on all slot buttons
        for (var node : slotsFlowPane.getChildren()) {
            if (node instanceof Button b && !b.isDisabled()) {
                b.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #142A3A; -fx-border-color: #D1DDE8; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-font-weight: 500; -fx-padding: 8px 14px; -fx-cursor: hand;");
            }
        }
        btn.setStyle("-fx-background-color: #168C83; -fx-text-fill: #FFFFFF; -fx-border-color: #168C83; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8px 14px; -fx-cursor: hand;");
    }

    @FXML
    private void handleConfirmBooking() {
        Department dept = departmentCombo.getValue();
        Doctor doc = doctorCombo.getValue();
        LocalDate date = appointmentDatePicker.getValue();
        String reason = reasonArea.getText() != null ? reasonArea.getText().trim() : "";

        if (dept == null) {
            NotificationManager.showWarning("Incomplete Form", "Please select a clinical department.");
            return;
        }
        if (doc == null) {
            NotificationManager.showWarning("Incomplete Form", "Please select an attending physician.");
            return;
        }
        if (date == null) {
            NotificationManager.showWarning("Incomplete Form", "Please select a consultation date.");
            return;
        }
        if (selectedTime == null) {
            NotificationManager.showWarning("Incomplete Form", "Please select an available appointment time slot.");
            return;
        }
        if (reason.isBlank()) {
            NotificationManager.showWarning("Incomplete Form", "Please provide a reason or chief complaint for your visit.");
            return;
        }

        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        String patientId = session != null && session.getPatientId() != null ? session.getPatientId() : "PT-1001";
        String patientName = session != null ? session.getFullName() : "Patient " + patientId;

        confirmBookingBtn.setDisable(true);

        CompletableFuture.runAsync(() -> {
            String apptId = "APT-" + (1000 + (int)(Math.random() * 9000));
            Appointment appt = new Appointment(
                    apptId, patientId, doc.getId(), date, selectedTime,
                    AppointmentStatus.BOOKED, LocalDate.now(), null
            );
            appt.setPatientName(patientName);
            appt.setDoctorName(doc.getFullName());
            appt.setDepartment(dept);
            appt.setReasonForVisit(reason);

            ServiceRegistry.getAppointmentRepo().save(appt);

            AppointmentHistory history = new AppointmentHistory(
                    UUID.randomUUID().toString(), apptId, null,
                    AppointmentStatus.BOOKED, "Initial patient online reservation"
            );
            ServiceRegistry.getHistoryRepo().save(history);

            NotificationService.getInstance().notifyAppointmentBooked(appt);

            Platform.runLater(() -> {
                confirmBookingBtn.setDisable(false);
                NotificationManager.showSuccess("Appointment Booked Successfully",
                        "Your consultation with " + doc.getFullName() + " on " + date + " at " + selectedTime + " is confirmed.");
                handleReset();
            });
        });
    }

    @FXML
    private void handleReset() {
        reasonArea.clear();
        selectedTime = null;
        selectedSlotLabel.setText("No slot selected");
        refreshAvailableSlots();
    }
}
