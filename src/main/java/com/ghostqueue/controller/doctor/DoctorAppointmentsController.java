package com.ghostqueue.controller.doctor;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.RiskLevel;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DoctorAppointmentsController {

    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;

    @FXML private TableView<Appointment> consultationsTable;
    @FXML private TableColumn<Appointment, String> colId;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, LocalDate> colDate;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, RiskLevel> colRisk;
    @FXML private TableColumn<Appointment, String> colReason;
    @FXML private TableColumn<Appointment, Void> colActions;

    private final ObservableList<Appointment> tableData = FXCollections.observableArrayList();
    private final List<Appointment> masterData = new ArrayList<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        colTime.setCellValueFactory(cellData -> {
            var t = cellData.getValue().getAppointmentTime();
            return new javafx.beans.property.SimpleStringProperty(t != null ? t.toString() : "TBD");
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colRisk.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reasonForVisit"));

        setupActionsColumn();

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "All Consultations", "Active / Booked", "COMPLETED", "NO_SHOW", "CANCELLED"
        ));
        statusFilterCombo.setValue("All Consultations");
        statusFilterCombo.setOnAction(e -> applyFilter());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        consultationsTable.setItems(tableData);

        loadData();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button checkInBtn = new Button("Arrived");
            private final Button completeBtn = new Button("Complete");
            private final Button noShowBtn = new Button("No-Show");
            private final HBox container = new HBox(6, checkInBtn, completeBtn, noShowBtn);

            {
                checkInBtn.setStyle("-fx-background-color: #E6F5F3; -fx-text-fill: #168C83; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4px 8px; -fx-background-radius: 4px; -fx-cursor: hand;");
                completeBtn.setStyle("-fx-background-color: #168C83; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4px 8px; -fx-background-radius: 4px; -fx-cursor: hand;");
                noShowBtn.setStyle("-fx-background-color: rgba(201, 95, 95, 0.15); -fx-text-fill: #C95F5F; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4px 8px; -fx-background-radius: 4px; -fx-cursor: hand;");

                checkInBtn.setOnAction(e -> {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    handleCheckIn(appt);
                });
                completeBtn.setOnAction(e -> {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    handleComplete(appt);
                });
                noShowBtn.setOnAction(e -> {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    handleNoShow(appt);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    if (appt.getStatus() == AppointmentStatus.COMPLETED) {
                        setGraphic(new Label("✓ Finished"));
                    } else if (appt.getStatus() == AppointmentStatus.CANCELLED) {
                        setGraphic(new Label("Cancelled"));
                    } else if (appt.getStatus() == AppointmentStatus.NO_SHOW) {
                        setGraphic(new Label("⚠ Missed"));
                    } else {
                        checkInBtn.setVisible(appt.getStatus() != AppointmentStatus.CONFIRMED);
                        setGraphic(container);
                    }
                }
            }
        });
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        String doctorId = session.getDoctorId() != null ? session.getDoctorId() : "DOC-101";

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getAppointmentRepo().findByDoctor(doctorId);
        }).thenAccept(list -> Platform.runLater(() -> {
            masterData.clear();
            masterData.addAll(list);
            applyFilter();
        }));
    }

    private void handleCheckIn(Appointment appt) {
        if (appt == null) return;
        CompletableFuture.runAsync(() -> {
            ServiceRegistry.getAppointmentRepo().updateStatus(appt.getId(), AppointmentStatus.CONFIRMED);
            ServiceRegistry.getHistoryRepo().save(new AppointmentHistory(
                    UUID.randomUUID().toString(), appt.getId(), appt.getStatus(),
                    AppointmentStatus.CONFIRMED, "Patient confirmed arrived in clinic"
            ));
        }).thenRun(() -> Platform.runLater(() -> {
            NotificationManager.showSuccess("Patient Checked In", appt.getPatientName() + " is marked present.");
            loadData();
        }));
    }

    private void handleComplete(Appointment appt) {
        if (appt == null) return;

        TextInputDialog dialog = new TextInputDialog("Consultation finished. Normal clinical outcome.");
        dialog.setTitle("Complete Visit");
        dialog.setHeaderText("Clinical Outcome Record for " + appt.getPatientName());
        dialog.setContentText("Enter diagnosis notes:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            CompletableFuture.runAsync(() -> {
                ServiceRegistry.getAppointmentRepo().updateStatus(appt.getId(), AppointmentStatus.COMPLETED);
                ServiceRegistry.getHistoryRepo().save(new AppointmentHistory(
                        UUID.randomUUID().toString(), appt.getId(), appt.getStatus(),
                        AppointmentStatus.COMPLETED, result.get().trim()
                ));
            }).thenRun(() -> Platform.runLater(() -> {
                NotificationManager.showSuccess("Consultation Completed", "Encounter finalized for " + appt.getPatientName());
                loadData();
            }));
        }
    }

    private void handleNoShow(Appointment appt) {
        if (appt == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mark Patient No-Show");
        alert.setHeaderText("Mark " + appt.getPatientName() + " as No-Show?");
        alert.setContentText("This will update the appointment status to NO_SHOW and trigger dynamic waiting list triage for slot reallocation.");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            CompletableFuture.runAsync(() -> {
                ServiceRegistry.getAppointmentRepo().updateStatus(appt.getId(), AppointmentStatus.NO_SHOW);
                ServiceRegistry.getHistoryRepo().save(new AppointmentHistory(
                        UUID.randomUUID().toString(), appt.getId(), appt.getStatus(),
                        AppointmentStatus.NO_SHOW, "Patient failed to appear for scheduled appointment"
                ));
                NotificationService.getInstance().notifyAppointmentCancelled(appt, "No-Show detected by attending physician");
            }).thenRun(() -> Platform.runLater(() -> {
                NotificationManager.showWarning("No-Show Logged", appt.getPatientName() + " recorded as absent. Slot ready for reallocation.");
                loadData();
            }));
        }
    }

    private void applyFilter() {
        tableData.clear();
        String filter = statusFilterCombo.getValue();
        String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        for (Appointment a : masterData) {
            boolean matchStatus = switch (filter) {
                case "Active / Booked" -> a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED;
                case "COMPLETED" -> a.getStatus() == AppointmentStatus.COMPLETED;
                case "NO_SHOW" -> a.getStatus() == AppointmentStatus.NO_SHOW;
                case "CANCELLED" -> a.getStatus() == AppointmentStatus.CANCELLED;
                default -> true;
            };

            if (!matchStatus) continue;

            if (!q.isBlank()) {
                boolean matchPat = a.getPatientName() != null && a.getPatientName().toLowerCase().contains(q);
                boolean matchReason = a.getReasonForVisit() != null && a.getReasonForVisit().toLowerCase().contains(q);
                if (!matchPat && !matchReason) continue;
            }

            tableData.add(a);
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
