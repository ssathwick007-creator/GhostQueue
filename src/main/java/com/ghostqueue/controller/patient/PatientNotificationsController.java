package com.ghostqueue.controller.patient;

import com.ghostqueue.model.InAppNotification;
import com.ghostqueue.service.NotificationService;
import com.ghostqueue.util.DateTimeUtils;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.concurrent.CompletableFuture;

public class PatientNotificationsController {

    @FXML private TableView<InAppNotification> notificationsTable;
    @FXML private TableColumn<InAppNotification, String> colType;
    @FXML private TableColumn<InAppNotification, String> colTitle;
    @FXML private TableColumn<InAppNotification, String> colMessage;
    @FXML private TableColumn<InAppNotification, String> colDate;
    @FXML private TableColumn<InAppNotification, String> colStatus;

    private final ObservableList<InAppNotification> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colType.setCellValueFactory(new PropertyValueFactory<>("notificationType"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        colDate.setCellValueFactory(cellData -> {
            var dt = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(dt != null ? dt.toString().replace("T", " ").substring(0, 16) : "");
        });
        colStatus.setCellValueFactory(cellData -> {
            boolean read = cellData.getValue().isRead();
            return new javafx.beans.property.SimpleStringProperty(read ? "READ" : "UNREAD");
        });

        notificationsTable.setItems(tableData);

        loadData();
    }

    public void loadData() {
        CompletableFuture.supplyAsync(() -> {
            return NotificationService.getInstance().getNotificationsForCurrentUser();
        }).thenAccept(list -> Platform.runLater(() -> {
            tableData.clear();
            tableData.addAll(list);
        }));
    }

    @FXML
    private void handleMarkAllRead() {
        NotificationService.getInstance().markAllAsRead();
        NotificationManager.showSuccess("Notifications", "All notifications marked as read.");
        loadData();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
