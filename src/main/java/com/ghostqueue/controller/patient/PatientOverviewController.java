package com.ghostqueue.controller.patient;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.service.NotificationService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.DateTimeUtils;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PatientOverviewController {

    @FXML private VBox nextAppointmentHero;
    @FXML private VBox noAppointmentCard;
    @FXML private Label heroDoctorName;
    @FXML private Label heroDepartment;
    @FXML private Label heroDateLabel;
    @FXML private Label heroTimeLabel;
    @FXML private Label heroStatusBadge;
    @FXML private Button heroCancelBtn;

    @FXML private Label upcomingCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label notifCountLabel;

    @FXML private TableView<Appointment> upcomingTable;
    @FXML private TableColumn<Appointment, String> colId;
    @FXML private TableColumn<Appointment, String> colDoctor;
    @FXML private TableColumn<Appointment, String> colDept;
    @FXML private TableColumn<Appointment, LocalDate> colDate;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, String> colReason;

    private PatientLayoutController layoutController;
    private Appointment nextAppointment;
    private final ObservableList<Appointment> upcomingList = FXCollections.observableArrayList();

    public void setLayoutController(PatientLayoutController layoutController) {
        this.layoutController = layoutController;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colDept.setCellValueFactory(cellData -> {
            String deptName = cellData.getValue().getDepartment() != null
                    ? cellData.getValue().getDepartment().getDisplayName() : "General Medicine";
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

        upcomingTable.setItems(upcomingList);

        loadData();
    }

    public void loadData() {
        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session == null) return;

        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getAppointmentRepo().findAll();
        }).thenAccept(allAppts -> Platform.runLater(() -> {
            int upcomingCount = 0;
            int completedCount = 0;

            upcomingList.clear();
            Appointment closest = null;

            for (Appointment a : allAppts) {
                if (a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.PENDING_REALLOCATION) {
                    upcomingCount++;
                    upcomingList.add(a);
                    if (closest == null || (a.getAppointmentDate() != null && a.getAppointmentDate().isBefore(closest.getAppointmentDate()))) {
                        closest = a;
                    }
                } else if (a.getStatus() == AppointmentStatus.COMPLETED) {
                    completedCount++;
                }
            }

            upcomingCountLabel.setText(String.valueOf(upcomingCount));
            completedCountLabel.setText(String.valueOf(completedCount));

            int unread = NotificationService.getInstance().getUnreadCount();
            notifCountLabel.setText(String.valueOf(unread));

            nextAppointment = closest;
            if (nextAppointment != null) {
                nextAppointmentHero.setVisible(true);
                nextAppointmentHero.setManaged(true);
                noAppointmentCard.setVisible(false);
                noAppointmentCard.setManaged(false);

                heroDoctorName.setText(nextAppointment.getDoctorName() != null ? nextAppointment.getDoctorName() : "Hospital Specialist");
                heroDepartment.setText(nextAppointment.getDepartment() != null ? nextAppointment.getDepartment().getDisplayName() : "General Consultation");
                heroDateLabel.setText(DateTimeUtils.formatHeaderDate(nextAppointment.getAppointmentDate()));
                heroTimeLabel.setText(nextAppointment.getAppointmentTime() != null ? nextAppointment.getAppointmentTime().toString() : "TBD");
                heroStatusBadge.setText(nextAppointment.getStatus().name());
            } else {
                nextAppointmentHero.setVisible(false);
                nextAppointmentHero.setManaged(false);
                noAppointmentCard.setVisible(true);
                noAppointmentCard.setManaged(true);
            }
        }));
    }

    @FXML
    private void handleHeroCancel() {
        if (nextAppointment == null) return;

        TextInputDialog dialog = new TextInputDialog("Personal scheduling conflict");
        dialog.setTitle("Cancel Appointment");
        dialog.setHeaderText("Cancel Consultation with " + nextAppointment.getDoctorName());
        dialog.setContentText("Please provide reason for cancellation:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().isBlank()) {
            String reason = result.get().trim();
            ServiceRegistry.getAppointmentRepo().cancelAppointment(nextAppointment.getId(), reason);
            NotificationManager.showSuccess("Appointment Cancelled", "Your appointment has been cancelled.");
            loadData();
        }
    }

    @FXML
    private void handleBookShortcut() {
        if (layoutController != null) {
            layoutController.handleNavBook();
        }
    }
}
