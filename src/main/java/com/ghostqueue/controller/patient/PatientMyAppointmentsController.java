package com.ghostqueue.controller.patient;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.service.NotificationService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PatientMyAppointmentsController {

    @FXML private TextField searchField;
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> colId;
    @FXML private TableColumn<Appointment, String> colDoctor;
    @FXML private TableColumn<Appointment, String> colDept;
    @FXML private TableColumn<Appointment, LocalDate> colDate;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, String> colReason;
    @FXML private TableColumn<Appointment, Void> colActions;

    private final ObservableList<Appointment> tableData = FXCollections.observableArrayList();
    private final List<Appointment> masterData = new ArrayList<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colDept.setCellValueFactory(cellData -> {
            String deptName = cellData.getValue().getDepartment() != null
                    ? cellData.getValue().getDepartment().getDisplayName() : "General Consultation";
            return new javafx.beans.property.SimpleStringProperty(deptName);
        });
        colDate.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        colTime.setCellValueFactory(cellData -> {
            String timeStr = cellData.getValue().getAppointmentTime() != null
                    ? cellData.getValue().getAppointmentTime().toString() : "";
            return new javafx.beans.property.SimpleStringProperty(timeStr);
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reasonForVisit"));

        setupActionsColumn();

        appointmentsTable.setItems(tableData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        loadData();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel");

            {
                cancelBtn.setStyle("-fx-background-color: rgba(201, 95, 95, 0.15); -fx-text-fill: #C95F5F; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 5px 12px; -fx-background-radius: 4px; -fx-cursor: hand;");
                cancelBtn.setOnAction(event -> {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    handleCancel(appt);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    if (appt.getStatus() == AppointmentStatus.BOOKED || appt.getStatus() == AppointmentStatus.CONFIRMED
                            || appt.getStatus() == AppointmentStatus.PENDING_REALLOCATION) {
                        setGraphic(cancelBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getAppointmentRepo().findAll();
        }).thenAccept(list -> Platform.runLater(() -> {
            masterData.clear();
            for (Appointment a : list) {
                if (a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.PENDING_REALLOCATION) {
                    masterData.add(a);
                }
            }
            applyFilter(searchField.getText());
        }));
    }

    private void applyFilter(String query) {
        tableData.clear();
        if (query == null || query.isBlank()) {
            tableData.addAll(masterData);
            return;
        }

        String q = query.trim().toLowerCase();
        for (Appointment a : masterData) {
            boolean matchDoc = a.getDoctorName() != null && a.getDoctorName().toLowerCase().contains(q);
            boolean matchReason = a.getReasonForVisit() != null && a.getReasonForVisit().toLowerCase().contains(q);
            boolean matchDept = a.getDepartment() != null && a.getDepartment().getDisplayName().toLowerCase().contains(q);
            if (matchDoc || matchReason || matchDept) {
                tableData.add(a);
            }
        }
    }

    private void handleCancel(Appointment appt) {
        if (appt == null) return;

        TextInputDialog dialog = new TextInputDialog("Patient requested schedule change");
        dialog.setTitle("Cancel Appointment");
        dialog.setHeaderText("Cancel Appointment with " + appt.getDoctorName());
        dialog.setContentText("Reason for cancellation:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().isBlank()) {
            String reason = result.get().trim();
            CompletableFuture.runAsync(() -> {
                ServiceRegistry.getAppointmentRepo().cancelAppointment(appt.getId(), reason);
                NotificationService.getInstance().notifyAppointmentCancelled(appt, reason);
            }).thenRun(() -> Platform.runLater(() -> {
                NotificationManager.showSuccess("Appointment Cancelled", "Your booking has been cancelled.");
                loadData();
            }));
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
