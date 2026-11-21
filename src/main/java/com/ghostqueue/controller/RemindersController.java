package com.ghostqueue.controller;

import com.ghostqueue.model.AppointmentReminder;
import com.ghostqueue.service.ReminderService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Controller for the Clinical Reminders and Outreach Management Dashboard.
 * Coordinates multi-channel reminders, patient response tracking, and automated high-risk outreach.
 */
public class RemindersController {

    @FXML private Label totalRemindersLabel;
    @FXML private Label pendingRemindersLabel;
    @FXML private Label completedRemindersLabel;
    @FXML private Label confirmationRateLabel;
    @FXML private Label recordCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<AppointmentReminder.ReminderType> typeFilter;
    @FXML private ComboBox<AppointmentReminder.ReminderStatus> statusFilter;

    @FXML private TableView<AppointmentReminder> remindersTable;
    @FXML private TableColumn<AppointmentReminder, String> colReminderId;
    @FXML private TableColumn<AppointmentReminder, String> colApptId;
    @FXML private TableColumn<AppointmentReminder, String> colPatient;
    @FXML private TableColumn<AppointmentReminder, String> colType;
    @FXML private TableColumn<AppointmentReminder, String> colScheduled;
    @FXML private TableColumn<AppointmentReminder, AppointmentReminder.ReminderStatus> colStatus;
    @FXML private TableColumn<AppointmentReminder, String> colMessage;
    @FXML private TableColumn<AppointmentReminder, String> colResponse;
    @FXML private TableColumn<AppointmentReminder, AppointmentReminder> colActions;

    private final ReminderService reminderService = ServiceRegistry.getInstance().getReminderService();

    @FXML
    public void initialize() {
        setupMetrics();
        setupFilters();
        setupTable();
        applyFilters();
    }

    public void loadData() {
        setupMetrics();
        applyFilters();
    }

    private void setupMetrics() {
        int total = reminderService.countTotal();
        int pending = reminderService.countPending();
        int completed = reminderService.countCompleted();

        totalRemindersLabel.setText(String.valueOf(total));
        pendingRemindersLabel.setText(String.valueOf(pending));
        completedRemindersLabel.setText(String.valueOf(completed));

        double rate = (total > 0) ? ((double) completed / total) * 100.0 : 0.0;
        confirmationRateLabel.setText(String.format("%.1f%%", rate));
    }

    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList(AppointmentReminder.ReminderType.values()));
        typeFilter.getSelectionModel().select(AppointmentReminder.ReminderType.ALL);
        typeFilter.setOnAction(e -> applyFilters());

        statusFilter.setItems(FXCollections.observableArrayList(AppointmentReminder.ReminderStatus.values()));
        statusFilter.getSelectionModel().select(AppointmentReminder.ReminderStatus.ALL);
        statusFilter.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void setupTable() {
        colReminderId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReminderId()));
        colApptId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAppointmentId()));
        colPatient.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPatientName() + " (" + data.getValue().getPatientPhone() + ")"));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReminderType().getDisplayName()));
        colScheduled.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScheduledForFormatted()));
        colMessage.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessageContent()));

        // Response column with badge
        colResponse.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getResponseStatus()));
        colResponse.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String response, boolean empty) {
                super.updateItem(response, empty);
                if (empty || response == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(response);
                    if ("CONFIRMED".equalsIgnoreCase(response)) {
                        badge.getStyleClass().addAll("badge", "badge-confirmed");
                    } else if (response.contains("REQUESTED")) {
                        badge.getStyleClass().addAll("badge", "badge-pending");
                    } else {
                        badge.getStyleClass().addAll("badge", "badge-neutral");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Status badge column
        colStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getReminderStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AppointmentReminder.ReminderStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status.getDisplayName());
                    badge.getStyleClass().addAll("badge", status.getStyleClass());
                    setGraphic(badge);
                }
            }
        });

        // Actions column
        colActions.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnConfirm = new Button("Confirm");
            private final Button btnReschedule = new Button("Reschedule");
            private final Button btnCancel = new Button("Cancel");
            private final HBox container = new HBox(4, btnConfirm, btnReschedule, btnCancel);

            {
                btnConfirm.getStyleClass().addAll("btn-primary", "btn-sm");
                btnReschedule.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnCancel.getStyleClass().addAll("btn-secondary", "btn-sm");

                btnConfirm.setOnAction(e -> {
                    AppointmentReminder item = getItem();
                    if (item != null) {
                        reminderService.markCompleted(item.getReminderId(), "CONFIRMED", "Patient verbally confirmed attendance");
                        setupMetrics();
                        applyFilters();
                        NotificationManager.showSuccess("Response Recorded",
                                "Outreach marked as COMPLETED. Patient " + item.getPatientName() + " confirmed attendance.");
                    }
                });

                btnReschedule.setOnAction(e -> {
                    AppointmentReminder item = getItem();
                    if (item != null) {
                        reminderService.markCompleted(item.getReminderId(), "RESCHEDULE_REQUESTED", "Patient requested date adjustment");
                        setupMetrics();
                        applyFilters();
                        NotificationManager.showInfo("Reschedule Logged",
                                "Patient requested appointment reschedule. Notification logged for reception.");
                    }
                });

                btnCancel.setOnAction(e -> {
                    AppointmentReminder item = getItem();
                    if (item != null) {
                        reminderService.cancelReminder(item.getReminderId());
                        setupMetrics();
                        applyFilters();
                        NotificationManager.showInfo("Reminder Cancelled", "Reminder task cancelled.");
                    }
                });
            }

            @Override
            protected void updateItem(AppointmentReminder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else if (item.getReminderStatus() == AppointmentReminder.ReminderStatus.COMPLETED) {
                    setGraphic(new Label("✓ " + item.getCompletedAtFormatted()));
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void applyFilters() {
        String query = searchField.getText();
        AppointmentReminder.ReminderType type = typeFilter.getValue();
        AppointmentReminder.ReminderStatus status = statusFilter.getValue();

        List<AppointmentReminder> filtered = reminderService.filterReminders(query, type, status);
        remindersTable.setItems(FXCollections.observableArrayList(filtered));
        recordCountLabel.setText("Showing " + filtered.size() + " of " + reminderService.countTotal() + " reminders");
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        typeFilter.getSelectionModel().select(AppointmentReminder.ReminderType.ALL);
        statusFilter.getSelectionModel().select(AppointmentReminder.ReminderStatus.ALL);
        applyFilters();
    }

    @FXML
    private void handleGenerateReminders() {
        int count = reminderService.generateHighRiskReminders();
        setupMetrics();
        applyFilters();
        if (count > 0) {
            NotificationManager.showSuccess("Outreach Queue Updated",
                    "Generated " + count + " new targeted reminder outreach tasks for predicted high-risk visits.");
        } else {
            NotificationManager.showInfo("No New Outreach Needed",
                    "All high-risk appointments currently have active reminder tasks queued.");
        }
    }
}
