package com.ghostqueue.controller;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.Doctor;
import com.ghostqueue.service.AppointmentService;
import com.ghostqueue.service.DoctorService;
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<String> doctorFilter;
    @FXML private ComboBox<AppointmentStatus> statusFilter;
    @FXML private DatePicker dateFilter;
    @FXML private Label recordCountLabel;

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> colId;
    @FXML private TableColumn<Appointment, String> colPatientId;
    @FXML private TableColumn<Appointment, String> colPatientName;
    @FXML private TableColumn<Appointment, String> colDoctor;
    @FXML private TableColumn<Appointment, String> colDepartment;
    @FXML private TableColumn<Appointment, String> colDate;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, Appointment> colRisk;
    @FXML private TableColumn<Appointment, Appointment> colActions;

    private final AppointmentService appointmentService = ServiceRegistry.getInstance().getAppointmentService();
    private final DoctorService doctorService = ServiceRegistry.getInstance().getDoctorService();

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        applyFilters();
    }

    private void setupFilters() {
        departmentFilter.setItems(FXCollections.observableArrayList(Department.values()));
        departmentFilter.getSelectionModel().select(Department.ALL);
        departmentFilter.setOnAction(e -> applyFilters());

        populateDoctorFilter();
        doctorFilter.setOnAction(e -> applyFilters());

        statusFilter.setItems(FXCollections.observableArrayList(AppointmentStatus.values()));
        statusFilter.getSelectionModel().select(AppointmentStatus.ALL);
        statusFilter.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        dateFilter.setOnAction(e -> applyFilters());
    }

    private void populateDoctorFilter() {
        List<String> docOptions = new ArrayList<>();
        docOptions.add("All Doctors");
        for (Doctor doc : doctorService.getAllDoctors()) {
            docOptions.add(doc.getId() + " - " + doc.getFullName());
        }
        doctorFilter.setItems(FXCollections.observableArrayList(docOptions));
        doctorFilter.getSelectionModel().select(0);
    }

    private void setupTable() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colPatientId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientId()));
        colPatientName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientName()));
        colDoctor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDoctorName()));
        colDepartment.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDepartment().getDisplayName()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateFormatted()));
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimeFormatted()));

        // Status badge
        colStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
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

        // Risk badge
        colRisk.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colRisk.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Appointment apt, boolean empty) {
                super.updateItem(apt, empty);
                if (empty || apt == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UiHelper.createRiskBadge(apt.getRiskLevel(), apt.getRiskScorePercent()));
                }
            }
        });

        // Action buttons
        colActions.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnStatus = new Button("Status");
            private final Button btnCancel = new Button("Cancel");
            private final HBox container = new HBox(6, btnStatus, btnCancel);

            {
                btnStatus.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnCancel.getStyleClass().addAll("btn-secondary", "btn-sm");

                btnStatus.setOnAction(e -> {
                    Appointment apt = getItem();
                    if (apt != null) {
                        handleCycleStatus(apt);
                    }
                });

                btnCancel.setOnAction(e -> {
                    Appointment apt = getItem();
                    if (apt != null) {
                        handleCancelWithReason(apt);
                    }
                });
            }

            @Override
            protected void updateItem(Appointment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void handleCycleStatus(Appointment apt) {
        // Clinical status options
        ChoiceDialog<AppointmentStatus> dialog = new ChoiceDialog<>(
                apt.getStatus(),
                AppointmentStatus.BOOKED,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.NO_SHOW,
                AppointmentStatus.CANCELLED
        );
        dialog.setTitle("Update Appointment Status");
        dialog.setHeaderText("Update status for " + apt.getId() + " (" + apt.getPatientName() + ")");
        dialog.setContentText("Select new clinical status:");

        Optional<AppointmentStatus> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            if (newStatus != apt.getStatus()) {
                if (newStatus == AppointmentStatus.CANCELLED) {
                    handleCancelWithReason(apt);
                } else {
                    appointmentService.updateAppointmentStatus(apt.getId(), newStatus, null, "Status updated by operator to " + newStatus.getDisplayName());
                    applyFilters();
                    NotificationManager.showSuccess("Status Updated",
                            "Appointment " + apt.getId() + " status updated to " + newStatus.getDisplayName() + " in MySQL.");
                }
            }
        });
    }

    private void handleCancelWithReason(Appointment apt) {
        TextInputDialog dialog = new TextInputDialog("Patient requested reschedule");
        dialog.setTitle("Cancel Clinical Appointment");
        dialog.setHeaderText("Cancel appointment " + apt.getId() + " for " + apt.getPatientName());
        dialog.setContentText("Please provide the reason for cancellation:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            if (!reason.isBlank()) {
                appointmentService.cancelAppointment(apt.getId(), reason.trim());
                applyFilters();
                NotificationManager.showSuccess("Appointment Cancelled",
                        "Slot " + apt.getId() + " was cancelled with reason: \"" + reason.trim() + "\".\nNow available for Smart Reallocation.");
            }
        });
    }

    private void applyFilters() {
        String query = searchField.getText();
        Department dept = departmentFilter.getValue();
        AppointmentStatus status = statusFilter.getValue();
        LocalDate date = dateFilter.getValue();

        String selectedDocOption = doctorFilter.getValue();
        String doctorId = null;
        if (selectedDocOption != null && !selectedDocOption.equals("All Doctors")) {
            doctorId = selectedDocOption.split(" - ")[0].trim();
        }

        List<Appointment> filtered = appointmentService.filterAppointments(query, dept, doctorId, status, date);
        appointmentsTable.setItems(FXCollections.observableArrayList(filtered));
        recordCountLabel.setText("Showing " + filtered.size() + " of " + appointmentService.countTotal() + " appointments");
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        departmentFilter.getSelectionModel().select(Department.ALL);
        doctorFilter.getSelectionModel().select(0);
        statusFilter.getSelectionModel().select(AppointmentStatus.ALL);
        dateFilter.setValue(null);
        applyFilters();
    }

    public void setSearchQuery(String query) {
        if (searchField != null) {
            searchField.setText(query);
            applyFilters();
        }
    }

    @FXML
    public void handleOpenAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/add_appointment_dialog.fxml"));
            Parent root = loader.load();
            AddAppointmentDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Schedule New Appointment — GhostQueue");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                applyFilters();
                NotificationManager.showSuccess("Appointment Scheduled", "New clinical appointment registered and queued for prediction.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Dialog Error", "Unable to open schedule dialog: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenDoctorManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/doctor_management_dialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Medical Staff & Doctor Management — GhostQueue");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(true);
            dialogStage.showAndWait();

            // Refresh doctor filter and appointments table after doctor changes
            populateDoctorFilter();
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Dialog Error", "Unable to open doctor management: " + e.getMessage());
        }
    }
}
