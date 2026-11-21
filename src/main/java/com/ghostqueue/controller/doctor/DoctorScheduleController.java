package com.ghostqueue.controller.doctor;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DoctorScheduleController {

    @FXML private Button btnFilterToday;
    @FXML private Button btnFilterTomorrow;
    @FXML private Button btnFilterWeek;
    @FXML private Button btnFilterAll;
    @FXML private TextField searchField;

    @FXML private TableView<Appointment> scheduleTable;
    @FXML private TableColumn<Appointment, String> colId;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, LocalDate> colDate;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, RiskLevel> colRisk;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, String> colReason;

    private final ObservableList<Appointment> tableData = FXCollections.observableArrayList();
    private final List<Appointment> masterData = new ArrayList<>();
    private String activeFilter = "TODAY";

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        colTime.setCellValueFactory(cellData -> {
            var t = cellData.getValue().getAppointmentTime();
            return new javafx.beans.property.SimpleStringProperty(t != null ? t.toString() : "TBD");
        });
        colRisk.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reasonForVisit"));

        scheduleTable.setItems(tableData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        loadData();
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

    @FXML
    private void handleFilterToday() {
        activeFilter = "TODAY";
        updateFilterButtonStyles(btnFilterToday);
        applyFilter();
    }

    @FXML
    private void handleFilterTomorrow() {
        activeFilter = "TOMORROW";
        updateFilterButtonStyles(btnFilterTomorrow);
        applyFilter();
    }

    @FXML
    private void handleFilterWeek() {
        activeFilter = "WEEK";
        updateFilterButtonStyles(btnFilterWeek);
        applyFilter();
    }

    @FXML
    private void handleFilterAll() {
        activeFilter = "ALL";
        updateFilterButtonStyles(btnFilterAll);
        applyFilter();
    }

    private void updateFilterButtonStyles(Button activeBtn) {
        Button[] buttons = {btnFilterToday, btnFilterTomorrow, btnFilterWeek, btnFilterAll};
        for (Button b : buttons) {
            if (b == activeBtn) {
                b.setStyle("-fx-background-color: #168C83; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6px 14px; -fx-background-radius: 6px; -fx-cursor: hand;");
            } else {
                b.setStyle("-fx-background-color: #F0F4F8; -fx-text-fill: #142A3A; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6px 14px; -fx-background-radius: 6px; -fx-cursor: hand;");
            }
        }
    }

    private void applyFilter() {
        tableData.clear();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekEnd = today.plusDays(7);

        String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        for (Appointment a : masterData) {
            LocalDate d = a.getAppointmentDate();
            boolean matchDate = switch (activeFilter) {
                case "TODAY" -> d != null && d.equals(today);
                case "TOMORROW" -> d != null && d.equals(tomorrow);
                case "WEEK" -> d != null && !d.isBefore(today) && !d.isAfter(weekEnd);
                default -> true;
            };

            // If empty today for demo, show upcoming
            if (activeFilter.equals("TODAY") && masterData.stream().noneMatch(x -> today.equals(x.getAppointmentDate()))) {
                matchDate = true;
            }

            if (!matchDate) continue;

            if (!q.isBlank()) {
                boolean matchPat = a.getPatientName() != null && a.getPatientName().toLowerCase().contains(q);
                boolean matchReason = a.getReasonForVisit() != null && a.getReasonForVisit().toLowerCase().contains(q);
                if (!matchPat && !matchReason) continue;
            }

            tableData.add(a);
        }
    }
}
