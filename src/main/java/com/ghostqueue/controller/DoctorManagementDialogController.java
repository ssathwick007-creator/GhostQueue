package com.ghostqueue.controller;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.Doctor;
import com.ghostqueue.service.AppointmentService;
import com.ghostqueue.service.DoctorService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
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
import java.util.stream.Collectors;

public class DoctorManagementDialogController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> availabilityFilter;
    @FXML private Label doctorCountLabel;

    @FXML private TableView<Doctor> doctorsTable;
    @FXML private TableColumn<Doctor, String> colId;
    @FXML private TableColumn<Doctor, String> colName;
    @FXML private TableColumn<Doctor, String> colDept;
    @FXML private TableColumn<Doctor, String> colSpec;
    @FXML private TableColumn<Doctor, String> colPhone;
    @FXML private TableColumn<Doctor, String> colAvailability;

    // Detail Panel
    @FXML private Label detailDoctorName;
    @FXML private Label detailDoctorId;
    @FXML private Label detailDepartment;
    @FXML private Label detailSpecialization;
    @FXML private Label detailPhone;
    @FXML private Label detailStatusLabel;

    @FXML private TableView<Appointment> doctorAppointmentsTable;
    @FXML private TableColumn<Appointment, String> colApptId;
    @FXML private TableColumn<Appointment, String> colPatientName;
    @FXML private TableColumn<Appointment, String> colApptDate;
    @FXML private TableColumn<Appointment, String> colApptTime;

    private final DoctorService doctorService = ServiceRegistry.getInstance().getDoctorService();
    private final AppointmentService appointmentService = ServiceRegistry.getInstance().getAppointmentService();

    @FXML
    public void initialize() {
        setupFilters();
        setupDoctorsTable();
        setupAppointmentsTable();
        setupListeners();
        loadDoctors();
    }

    private void setupFilters() {
        departmentFilter.setItems(FXCollections.observableArrayList(
                "All Departments", "Cardiology", "Neurology", "Orthopedics", "Oncology", "Pediatrics", "General Medicine", "Dermatology"
        ));
        departmentFilter.getSelectionModel().select(0);
        departmentFilter.setOnAction(e -> applyFilters());

        availabilityFilter.setItems(FXCollections.observableArrayList(
                "All Statuses", "AVAILABLE", "ON_LEAVE", "BUSY"
        ));
        availabilityFilter.getSelectionModel().select(0);
        availabilityFilter.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void setupDoctorsTable() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        colDept.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDepartment()));
        colSpec.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecialization()));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colAvailability.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAvailabilityStatus()));

        colAvailability.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("badge");
                    if ("AVAILABLE".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-confirmed");
                    } else if ("ON_LEAVE".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-cancelled");
                    } else {
                        badge.getStyleClass().add("badge-neutral");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void setupAppointmentsTable() {
        colApptId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colPatientName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientName()));
        colApptDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateFormatted()));
        colApptTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimeFormatted()));
    }

    private void setupListeners() {
        doctorsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                displayDoctorDetails(selected);
            }
        });
    }

    private void displayDoctorDetails(Doctor doctor) {
        detailDoctorName.setText(doctor.getFullName());
        detailDoctorId.setText("Doctor ID: " + doctor.getId());
        detailDepartment.setText("Department: " + doctor.getDepartment());
        detailSpecialization.setText("Specialization: " + doctor.getSpecialization());
        detailPhone.setText("Phone: " + doctor.getPhone());
        detailStatusLabel.setText(doctor.getAvailabilityStatus());

        detailStatusLabel.getStyleClass().removeAll("badge-confirmed", "badge-cancelled", "badge-neutral");
        if ("AVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
            detailStatusLabel.getStyleClass().add("badge-confirmed");
        } else if ("ON_LEAVE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
            detailStatusLabel.getStyleClass().add("badge-cancelled");
        } else {
            detailStatusLabel.getStyleClass().add("badge-neutral");
        }

        List<Appointment> doctorAppts = appointmentService.getAppointmentsForDoctor(doctor.getId());
        doctorAppointmentsTable.setItems(FXCollections.observableArrayList(doctorAppts));
    }

    public void loadDoctors() {
        applyFilters();
        if (!doctorsTable.getItems().isEmpty()) {
            doctorsTable.getSelectionModel().select(0);
        }
    }

    private void applyFilters() {
        String query = searchField.getText();
        String dept = departmentFilter.getValue();
        String avail = availabilityFilter.getValue();

        List<Doctor> all = (query != null && !query.isBlank()) ? doctorService.searchDoctors(query) : doctorService.getAllDoctors();

        List<Doctor> filtered = all.stream().filter(doc -> {
            if (dept != null && !dept.equals("All Departments") && !dept.equalsIgnoreCase(doc.getDepartment())) {
                return false;
            }
            if (avail != null && !avail.equals("All Statuses") && !avail.equalsIgnoreCase(doc.getAvailabilityStatus())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        doctorsTable.setItems(FXCollections.observableArrayList(filtered));
        doctorCountLabel.setText("Showing " + filtered.size() + " of " + doctorService.countTotal() + " doctors");
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        departmentFilter.getSelectionModel().select(0);
        availabilityFilter.getSelectionModel().select(0);
        applyFilters();
    }

    @FXML
    private void handleOpenAddDoctor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/doctor_form_dialog.fxml"));
            Parent root = loader.load();
            DoctorFormDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Register New Medical Specialist — GhostQueue");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadDoctors();
                NotificationManager.showSuccess("Doctor Added", "New physician profile saved into MySQL database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Dialog Error", "Unable to open doctor registration form: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditSelectedDoctor() {
        Doctor selected = doctorsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.showWarning("Selection Required", "Please select a doctor to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/doctor_form_dialog.fxml"));
            Parent root = loader.load();
            DoctorFormDialogController controller = loader.getController();
            controller.setDoctorForEdit(selected);

            Stage stage = new Stage();
            stage.setTitle("Edit Doctor Profile — GhostQueue");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadDoctors();
                NotificationManager.showSuccess("Doctor Updated", "Doctor information updated in MySQL.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Dialog Error", "Unable to open edit dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleSetAvailable() {
        updateSelectedDoctorStatus("AVAILABLE");
    }

    @FXML
    private void handleSetBusy() {
        updateSelectedDoctorStatus("BUSY");
    }

    @FXML
    private void handleSetOnLeave() {
        updateSelectedDoctorStatus("ON_LEAVE");
    }

    private void updateSelectedDoctorStatus(String newStatus) {
        Doctor selected = doctorsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.showWarning("Selection Required", "Please select a physician from the roster.");
            return;
        }

        try {
            doctorService.setAvailabilityStatus(selected.getId(), newStatus);
            selected.setAvailabilityStatus(newStatus);
            displayDoctorDetails(selected);
            doctorsTable.refresh();
            NotificationManager.showSuccess("Availability Updated",
                    selected.getFullName() + " status is now " + newStatus + ".");
        } catch (Exception e) {
            NotificationManager.showWarning("Update Error", "Could not update availability: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }
}
