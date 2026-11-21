package com.ghostqueue.controller.patient;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PatientHistoryController {

    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<Appointment> historyTable;
    @FXML private TableColumn<Appointment, String> colId;
    @FXML private TableColumn<Appointment, String> colDoctor;
    @FXML private TableColumn<Appointment, String> colDept;
    @FXML private TableColumn<Appointment, LocalDate> colDate;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, String> colNotes;

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
        colNotes.setCellValueFactory(cellData -> {
            String reason = cellData.getValue().getCancellationReason();
            if (reason == null || reason.isBlank()) {
                reason = cellData.getValue().getReasonForVisit();
            }
            return new javafx.beans.property.SimpleStringProperty(reason != null ? reason : "Clinical Record");
        });

        statusFilterCombo.setItems(FXCollections.observableArrayList("All History", "COMPLETED", "CANCELLED", "NO_SHOW", "REALLOCATED"));
        statusFilterCombo.setValue("All History");
        statusFilterCombo.setOnAction(e -> applyFilter());

        historyTable.setItems(tableData);

        loadData();
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getAppointmentRepo().findAll();
        }).thenAccept(list -> Platform.runLater(() -> {
            masterData.clear();
            for (Appointment a : list) {
                if (a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.CANCELLED
                        || a.getStatus() == AppointmentStatus.NO_SHOW || a.getStatus() == AppointmentStatus.REALLOCATED) {
                    masterData.add(a);
                }
            }
            applyFilter();
        }));
    }

    private void applyFilter() {
        String filter = statusFilterCombo.getValue();
        tableData.clear();

        if (filter == null || "All History".equalsIgnoreCase(filter)) {
            tableData.addAll(masterData);
            return;
        }

        for (Appointment a : masterData) {
            if (a.getStatus() != null && a.getStatus().name().equalsIgnoreCase(filter)) {
                tableData.add(a);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
