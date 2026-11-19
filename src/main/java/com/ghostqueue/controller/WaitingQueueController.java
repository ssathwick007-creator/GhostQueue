package com.ghostqueue.controller;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.service.QueueService;
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

import java.util.List;

public class WaitingQueueController {

    @FXML private Label totalWaitingLabel;
    @FXML private Label urgentCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<Department> deptFilter;
    @FXML private ComboBox<QueuePriority> priorityFilter;
    @FXML private ComboBox<QueueStatus> statusFilter;

    @FXML private TableView<QueueEntry> queueTable;
    @FXML private TableColumn<QueueEntry, String> colQueueId;
    @FXML private TableColumn<QueueEntry, String> colPatient;
    @FXML private TableColumn<QueueEntry, QueuePriority> colPriority;
    @FXML private TableColumn<QueueEntry, String> colDept;
    @FXML private TableColumn<QueueEntry, String> colPrefDoctor;
    @FXML private TableColumn<QueueEntry, String> colPrefTime;
    @FXML private TableColumn<QueueEntry, String> colWaitDuration;
    @FXML private TableColumn<QueueEntry, QueueStatus> colStatus;
    @FXML private TableColumn<QueueEntry, QueueEntry> colActions;

    private final QueueService queueService = ServiceRegistry.getInstance().getQueueService();

    @FXML
    public void initialize() {
        setupMetrics();
        setupFilters();
        setupTable();
        applyFilters();
    }

    private void setupMetrics() {
        totalWaitingLabel.setText(String.valueOf(queueService.countTotalWaiting()));
        urgentCountLabel.setText(String.valueOf(queueService.countUrgentPriority()));
    }

    private void setupFilters() {
        deptFilter.setItems(FXCollections.observableArrayList(Department.values()));
        deptFilter.getSelectionModel().select(Department.ALL);
        deptFilter.setOnAction(e -> applyFilters());

        priorityFilter.setItems(FXCollections.observableArrayList(QueuePriority.values()));
        priorityFilter.getSelectionModel().select(QueuePriority.ALL);
        priorityFilter.setOnAction(e -> applyFilters());

        statusFilter.setItems(FXCollections.observableArrayList(QueueStatus.values()));
        statusFilter.getSelectionModel().select(QueueStatus.ALL);
        statusFilter.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void setupTable() {
        colQueueId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQueueId()));
        colPatient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientName() + " (" + data.getValue().getPatientId() + ")"));
        colDept.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRequestedDepartment().getDisplayName()));
        colPrefDoctor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPreferredDoctor()));
        colPrefTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPreferredTimeSlot()));
        colWaitDuration.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getWaitingDurationFormatted()));

        // Priority badge
        colPriority.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPriority()));
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(QueuePriority priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UiHelper.createPriorityBadge(priority));
                }
            }
        });

        // Status badge
        colStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(QueueStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UiHelper.createQueueStatusBadge(status));
                }
            }
        });

        // Actions
        colActions.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnOffer = new Button("Offer Slot");
            private final Button btnRemove = new Button("Remove");
            private final HBox container = new HBox(6, btnOffer, btnRemove);

            {
                btnOffer.getStyleClass().addAll("btn-primary", "btn-sm");
                btnRemove.getStyleClass().addAll("btn-secondary", "btn-sm");

                btnOffer.setOnAction(e -> {
                    QueueEntry entry = getItem();
                    if (entry != null) {
                        queueService.updateStatus(entry.getQueueId(), QueueStatus.OFFERED);
                        applyFilters();
                        NotificationManager.showSuccess("Slot Offer Dispatched",
                                "Notified " + entry.getPatientName() + " for an available slot in " +
                                        entry.getRequestedDepartment().getDisplayName() + ".\nStatus updated to Slot Offered.");
                    }
                });

                btnRemove.setOnAction(e -> {
                    QueueEntry entry = getItem();
                    if (entry != null) {
                        boolean confirmed = NotificationManager.showConfirmation(
                                "Remove from Queue",
                                "Remove patient " + entry.getPatientName() + " from queue?",
                                "Patient will be removed from the active waiting standby list."
                        );
                        if (confirmed) {
                            queueService.removeFromQueue(entry.getQueueId());
                            applyFilters();
                            setupMetrics();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(QueueEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void applyFilters() {
        String q = searchField.getText();
        Department d = deptFilter.getValue();
        QueuePriority p = priorityFilter.getValue();
        QueueStatus s = statusFilter.getValue();

        List<QueueEntry> list = queueService.filterQueue(q, d, p, s);
        queueTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        deptFilter.getSelectionModel().select(Department.ALL);
        priorityFilter.getSelectionModel().select(QueuePriority.ALL);
        statusFilter.getSelectionModel().select(QueueStatus.ALL);
        applyFilters();
    }

    @FXML
    public void handleOpenAddQueueDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/add_queue_dialog.fxml"));
            Parent root = loader.load();
            AddQueueDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Patient to Waiting Queue — GhostQueue");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                applyFilters();
                setupMetrics();
                NotificationManager.showSuccess("Patient Queued", "Patient successfully added to waiting queue.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Dialog Error", "Unable to open queue dialog: " + e.getMessage());
        }
    }
}
