package com.ghostqueue.controller;

import com.ghostqueue.model.*;
import com.ghostqueue.service.ReallocationService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
import com.ghostqueue.util.UiHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Controller for the Dynamic Appointment Reallocation &amp; Standby Matching Console.
 * Displays candidate rankings, compatibility rationales, confirmed vs at-risk slots,
 * and maintains an immutable decision audit log in MySQL.
 */
public class ReallocationController {

    @FXML private Label openSlotsLabel;
    @FXML private Label matchCandidatesLabel;
    @FXML private Label approvedCountLabel;
    @FXML private Label recoveryEfficiencyLabel;

    @FXML private TableView<ReallocationSuggestion> suggestionsTable;
    @FXML private TableColumn<ReallocationSuggestion, String> colSugId;
    @FXML private TableColumn<ReallocationSuggestion, String> colSlotInfo;
    @FXML private TableColumn<ReallocationSuggestion, String> colSlotContext;
    @FXML private TableColumn<ReallocationSuggestion, String> colCandidate;
    @FXML private TableColumn<ReallocationSuggestion, QueuePriority> colCandidatePriority;
    @FXML private TableColumn<ReallocationSuggestion, String> colMatchScore;
    @FXML private TableColumn<ReallocationSuggestion, String> colMatchReason;
    @FXML private TableColumn<ReallocationSuggestion, String> colStatus;
    @FXML private TableColumn<ReallocationSuggestion, ReallocationSuggestion> colActions;

    @FXML private TableView<Appointment> openSlotsTable;
    @FXML private TableColumn<Appointment, String> colOpenId;
    @FXML private TableColumn<Appointment, String> colOpenDoctor;
    @FXML private TableColumn<Appointment, String> colOpenDept;
    @FXML private TableColumn<Appointment, String> colOpenTime;
    @FXML private TableColumn<Appointment, AppointmentStatus> colOpenStatus;
    @FXML private TableColumn<Appointment, Appointment> colOpenRisk;

    @FXML private TableView<ReallocationHistory> historyTable;
    @FXML private TableColumn<ReallocationHistory, String> colHistId;
    @FXML private TableColumn<ReallocationHistory, String> colHistAppt;
    @FXML private TableColumn<ReallocationHistory, String> colHistPatient;
    @FXML private TableColumn<ReallocationHistory, String> colHistDoctor;
    @FXML private TableColumn<ReallocationHistory, String> colHistTime;
    @FXML private TableColumn<ReallocationHistory, String> colHistScore;
    @FXML private TableColumn<ReallocationHistory, String> colHistDecision;
    @FXML private TableColumn<ReallocationHistory, String> colHistNotes;
    @FXML private TableColumn<ReallocationHistory, String> colHistDate;

    private final ReallocationService reallocationService = ServiceRegistry.getInstance().getReallocationService();

    @FXML
    public void initialize() {
        setupTables();
        loadData();
    }

    private void setupTables() {
        // Suggestions Table
        colSugId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSuggestionId()));
        colSlotInfo.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDoctorName() + " (" + data.getValue().getSlotTimeFormatted() + ")"
        ));
        colSlotContext.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSlotContext()));
        colCandidate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCandidatePatient().getPatientName() + " (" + data.getValue().getCandidatePatient().getPatientId() + ")"
        ));
        colCandidatePriority.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCandidatePatient().getPriority()));
        colCandidatePriority.setCellFactory(col -> new TableCell<>() {
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

        colMatchScore.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMatchScore() + "%"));
        colMatchReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMatchReason()));

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    if ("APPROVED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().addAll("badge", "badge-confirmed");
                    } else if ("DECLINED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().addAll("badge", "badge-danger");
                    } else {
                        badge.getStyleClass().addAll("badge", "badge-pending");
                    }
                    setGraphic(badge);
                }
            }
        });

        colActions.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("Approve");
            private final Button btnDecline = new Button("Decline");
            private final HBox container = new HBox(6, btnApprove, btnDecline);

            {
                btnApprove.getStyleClass().addAll("btn-success", "btn-sm");
                btnDecline.getStyleClass().addAll("btn-secondary", "btn-sm");

                btnApprove.setOnAction(e -> {
                    ReallocationSuggestion item = getItem();
                    if (item != null) {
                        boolean isVacancy = item.getSlotContext().toLowerCase().contains("cancelled");
                        String promptHeader = isVacancy ? "Approve Vacant Slot Backfill" : "Approve Standby Candidate Assignment";
                        String promptMsg = isVacancy ?
                                "Assign cancelled slot with " + item.getDoctorName() + " on " + item.getSlotDate() +
                                        " at " + item.getSlotTimeFormatted() + " to waiting patient " +
                                        item.getCandidatePatient().getPatientName() + "?\n\n" +
                                        "Waiting queue status will be updated to Scheduled." :
                                "Designate patient " + item.getCandidatePatient().getPatientName() +
                                        " as primary standby for high-risk opening " + item.getSlotAppointmentId() + ".\n\n" +
                                        "Original patient appointment will NOT be cancelled or removed.";

                        boolean confirmed = NotificationManager.showConfirmation(
                                "Confirm Slot Reallocation",
                                promptHeader,
                                promptMsg
                        );
                        if (confirmed) {
                            reallocationService.approveReallocation(item.getSuggestionId(),
                                    "Administrator approved " + (isVacancy ? "slot backfill" : "standby backup"));
                            loadData();
                            NotificationManager.showSuccess("Reallocation Confirmed",
                                    "Candidate " + item.getCandidatePatient().getPatientName() +
                                            " marked as " + (isVacancy ? "Scheduled for slot." : "Standby backup."));
                        }
                    }
                });

                btnDecline.setOnAction(e -> {
                    ReallocationSuggestion item = getItem();
                    if (item != null) {
                        reallocationService.declineReallocation(item.getSuggestionId(), "Declined by operator");
                        loadData();
                        NotificationManager.showInfo("Recommendation Declined",
                                "Match " + item.getSuggestionId() + " declined. Slot remains available in inventory.");
                    }
                });
            }

            @Override
            protected void updateItem(ReallocationSuggestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else if (!"PENDING".equalsIgnoreCase(item.getStatus())) {
                    setGraphic(new Label(item.getStatus()));
                } else {
                    setGraphic(container);
                }
            }
        });

        // Open & At-Risk Slots Table
        colOpenId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colOpenDoctor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDoctorName()));
        colOpenDept.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDepartment().getDisplayName()));
        colOpenTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDateFormatted() + " " + data.getValue().getTimeFormatted()
        ));

        colOpenStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        colOpenStatus.setCellFactory(col -> new TableCell<>() {
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

        colOpenRisk.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colOpenRisk.setCellFactory(col -> new TableCell<>() {
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

        // Reallocation Decision History Table
        colHistId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReallocationId()));
        colHistAppt.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAppointmentId()));
        colHistPatient.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCandidatePatientName() + " (" + data.getValue().getCandidatePatientId() + ")"));
        colHistDoctor.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDoctorName() + " (" + data.getValue().getDepartment().getDisplayName() + ")"));
        colHistTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSlotDate() + " " + data.getValue().getTimeFormatted()));
        colHistScore.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMatchScore() + "%"));

        colHistDecision.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDecision()));
        colHistDecision.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String decision, boolean empty) {
                super.updateItem(decision, empty);
                if (empty || decision == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(decision);
                    if (decision.contains("APPROVED")) {
                        badge.getStyleClass().addAll("badge", "badge-confirmed");
                    } else if (decision.contains("DECLINED")) {
                        badge.getStyleClass().addAll("badge", "badge-danger");
                    } else {
                        badge.getStyleClass().addAll("badge", "badge-neutral");
                    }
                    setGraphic(badge);
                }
            }
        });

        colHistNotes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDecisionNotes()));
        colHistDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDecidedAtFormatted()));
    }

    public void loadData() {
        List<ReallocationSuggestion> suggestions = reallocationService.getSuggestions();
        suggestionsTable.setItems(FXCollections.observableArrayList(suggestions));

        List<Appointment> openSlots = reallocationService.getAvailableOpenSlots();
        openSlotsTable.setItems(FXCollections.observableArrayList(openSlots));

        List<ReallocationHistory> history = reallocationService.getReallocationHistory();
        historyTable.setItems(FXCollections.observableArrayList(history));

        openSlotsLabel.setText(String.valueOf(openSlots.size()));
        matchCandidatesLabel.setText(String.valueOf(suggestions.size()));
        approvedCountLabel.setText(String.valueOf(reallocationService.getApprovedCount()));

        int cancelled = reallocationService.getConfirmedVacantSlots().size();
        int approved = reallocationService.getApprovedCount();
        double efficiency = (cancelled > 0) ? Math.min(100.0, ((double) approved / cancelled) * 100.0) : 85.0;
        recoveryEfficiencyLabel.setText(String.format("%.1f%%", efficiency));
    }

    @FXML
    private void handleRefreshSuggestions() {
        reallocationService.refreshSuggestions();
        loadData();
        NotificationManager.showSuccess("Recommendations Refreshed",
                "Re-evaluated available slots and waiting candidates against priority compatibility heuristics.");
    }
}
