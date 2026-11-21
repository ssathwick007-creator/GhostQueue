package com.ghostqueue.controller.doctor;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.RiskLevel;
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
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DoctorOverviewController {

    @FXML private VBox nextPatientHero;
    @FXML private VBox noPatientCard;
    @FXML private Label heroPatientName;
    @FXML private Label heroComplaint;
    @FXML private Label heroTimeLabel;
    @FXML private Label heroStatusLabel;
    @FXML private Label heroRiskBadge;

    @FXML private Label todayCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label highRiskCountLabel;
    @FXML private Label standbyCountLabel;

    @FXML private TableView<Appointment> scheduleTable;
    @FXML private TableColumn<Appointment, String> colId;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, RiskLevel> colRisk;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, String> colReason;

    private DoctorLayoutController layoutController;
    private Appointment nextAppointment;
    private final ObservableList<Appointment> todayList = FXCollections.observableArrayList();

    public void setLayoutController(DoctorLayoutController layoutController) {
        this.layoutController = layoutController;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colTime.setCellValueFactory(cellData -> {
            var t = cellData.getValue().getAppointmentTime();
            return new javafx.beans.property.SimpleStringProperty(t != null ? t.toString() : "TBD");
        });
        colRisk.setCellValueFactory(new PropertyValueFactory<>("riskLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reasonForVisit"));

        scheduleTable.setItems(todayList);

        loadData();
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        String doctorId = session.getDoctorId() != null ? session.getDoctorId() : "DOC-101";

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getAppointmentRepo().findByDoctor(doctorId);
        }).thenAccept(appts -> Platform.runLater(() -> {
            LocalDate today = LocalDate.now();
            int todayCount = 0;
            int completedCount = 0;
            int highRiskCount = 0;

            todayList.clear();
            Appointment closest = null;

            for (Appointment a : appts) {
                // If today's appointment or upcoming
                boolean isToday = today.equals(a.getAppointmentDate());
                if (isToday) {
                    todayCount++;
                    todayList.add(a);
                    if (a.getStatus() == AppointmentStatus.COMPLETED) {
                        completedCount++;
                    }
                    if (a.getRiskLevel() == RiskLevel.HIGH) {
                        highRiskCount++;
                    }
                    if (a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED) {
                        if (closest == null) {
                            closest = a;
                        }
                    }
                }
            }

            // If no appointment matches today, show all active for demo richness
            if (todayList.isEmpty()) {
                for (Appointment a : appts) {
                    if (a.getStatus() != AppointmentStatus.CANCELLED) {
                        todayList.add(a);
                        if (closest == null && (a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED)) {
                            closest = a;
                        }
                    }
                }
                todayCount = todayList.size();
            }

            todayCountLabel.setText(String.valueOf(todayCount));
            completedCountLabel.setText(String.valueOf(completedCount));
            highRiskCountLabel.setText(String.valueOf(highRiskCount));

            // Load standby count
            loadStandbyCount();

            nextAppointment = closest;
            if (nextAppointment != null) {
                nextPatientHero.setVisible(true);
                nextPatientHero.setManaged(true);
                noPatientCard.setVisible(false);
                noPatientCard.setManaged(false);

                heroPatientName.setText(nextAppointment.getPatientName() != null ? nextAppointment.getPatientName() : "Scheduled Patient");
                heroComplaint.setText("Chief Complaint: " + (nextAppointment.getReasonForVisit() != null ? nextAppointment.getReasonForVisit() : "Standard Consultation"));
                heroTimeLabel.setText((nextAppointment.getAppointmentTime() != null ? nextAppointment.getAppointmentTime().toString() : "09:00 AM") + " (" + nextAppointment.getAppointmentDate() + ")");
                heroStatusLabel.setText("Status: " + nextAppointment.getStatus().name());

                if (nextAppointment.getRiskLevel() == RiskLevel.HIGH) {
                    heroRiskBadge.setText("HIGH RISK (No-Show Alert)");
                    heroRiskBadge.setStyle("-fx-background-color: rgba(201, 95, 95, 0.25); -fx-text-fill: #C95F5F; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 4px;");
                } else if (nextAppointment.getRiskLevel() == RiskLevel.MEDIUM) {
                    heroRiskBadge.setText("MODERATE RISK");
                    heroRiskBadge.setStyle("-fx-background-color: rgba(217, 154, 66, 0.25); -fx-text-fill: #D99A42; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 4px;");
                } else {
                    heroRiskBadge.setText("LOW RISK");
                    heroRiskBadge.setStyle("-fx-background-color: rgba(67, 139, 112, 0.25); -fx-text-fill: #438B70; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 4px;");
                }
            } else {
                nextPatientHero.setVisible(false);
                nextPatientHero.setManaged(false);
                noPatientCard.setVisible(true);
                noPatientCard.setManaged(true);
            }
        }));
    }

    private void loadStandbyCount() {
        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getQueueRepo().findAll();
        }).thenAccept(list -> Platform.runLater(() -> {
            standbyCountLabel.setText(String.valueOf(list.size()));
        }));
    }

    @FXML
    private void handleHeroMarkArrived() {
        if (nextAppointment == null) return;
        ServiceRegistry.getAppointmentRepo().updateStatus(nextAppointment.getId(), AppointmentStatus.CONFIRMED);
        NotificationManager.showSuccess("Patient Checked In", nextAppointment.getPatientName() + " has arrived in clinic.");
        loadData();
    }

    @FXML
    private void handleHeroComplete() {
        if (nextAppointment == null) return;

        TextInputDialog dialog = new TextInputDialog("Normal assessment. Patient stable.");
        dialog.setTitle("Complete Consultation");
        dialog.setHeaderText("Clinical Outcome Notes for " + nextAppointment.getPatientName());
        dialog.setContentText("Enter diagnosis / consultation summary:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            ServiceRegistry.getAppointmentRepo().updateStatus(nextAppointment.getId(), AppointmentStatus.COMPLETED);
            NotificationManager.showSuccess("Consultation Completed", "Clinical record finalized for " + nextAppointment.getPatientName());
            loadData();
        }
    }

    @FXML
    private void handleViewFullSchedule() {
        if (layoutController != null) {
            layoutController.handleNavSchedule();
        }
    }
}
