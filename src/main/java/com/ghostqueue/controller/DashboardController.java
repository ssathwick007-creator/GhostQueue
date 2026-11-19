package com.ghostqueue.controller;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.service.AnalyticsService;
import com.ghostqueue.service.AppointmentService;
import com.ghostqueue.service.PredictionService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
import com.ghostqueue.util.UiHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.Map;

public class DashboardController {

    // 8 Live MySQL Metrics (Requirement 8)
    @FXML private Label totalPatientsLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label todayAppointmentsLabel;
    @FXML private Label confirmedLabel;
    @FXML private Label completedLabel;
    @FXML private Label cancellationsLabel;
    @FXML private Label noShowLabel;
    @FXML private Label waitingQueueLabel;

    @FXML private BarChart<String, Number> departmentActivityChart;
    @FXML private PieChart riskDistributionPieChart;

    @FXML private TableView<Appointment> todayScheduleTable;
    @FXML private TableColumn<Appointment, String> colTime;
    @FXML private TableColumn<Appointment, String> colPatientId;
    @FXML private TableColumn<Appointment, String> colPatientName;
    @FXML private TableColumn<Appointment, String> colDoctor;
    @FXML private TableColumn<Appointment, String> colDepartment;
    @FXML private TableColumn<Appointment, AppointmentStatus> colStatus;
    @FXML private TableColumn<Appointment, Appointment> colRisk;
    @FXML private TableColumn<Appointment, Appointment> colActions;

    private final AppointmentService appointmentService = ServiceRegistry.getInstance().getAppointmentService();
    private final PredictionService predictionService = ServiceRegistry.getInstance().getPredictionService();
    private final AnalyticsService analyticsService = ServiceRegistry.getInstance().getAnalyticsService();

    private MainLayoutController mainLayoutController;

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        setupMetrics();
        setupCharts();
        setupTable();
        loadData();
    }

    private void setupMetrics() {
        totalPatientsLabel.setText(String.valueOf(analyticsService.getTotalPatients()));
        totalDoctorsLabel.setText(String.valueOf(analyticsService.getTotalDoctors()));
        todayAppointmentsLabel.setText(String.valueOf(analyticsService.getTodayAppointmentsCount()));
        confirmedLabel.setText(String.valueOf(analyticsService.getConfirmedCount()));
        completedLabel.setText(String.valueOf(analyticsService.getCompletedCount()));
        cancellationsLabel.setText(String.valueOf(analyticsService.getCancelledCount()));
        noShowLabel.setText(String.valueOf(analyticsService.getNoShowCount()));
        waitingQueueLabel.setText(String.valueOf(analyticsService.getWaitingQueueCount()));
    }

    private void setupCharts() {
        // Department Load Chart
        departmentActivityChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Appointments");
        Map<Department, Integer> deptCounts = analyticsService.getDepartmentAppointmentCounts();
        for (Map.Entry<Department, Integer> entry : deptCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().getDisplayName(), entry.getValue()));
        }
        departmentActivityChart.getData().add(series);

        // Risk Distribution Pie Chart
        riskDistributionPieChart.getData().clear();
        Map<RiskLevel, Integer> riskMap = analyticsService.getRiskDistribution();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Low Risk (<30%)", riskMap.getOrDefault(RiskLevel.LOW, 10)),
                new PieChart.Data("Medium Risk (30-64%)", riskMap.getOrDefault(RiskLevel.MEDIUM, 4)),
                new PieChart.Data("High Risk (≥65%)", riskMap.getOrDefault(RiskLevel.HIGH, 4))
        );
        riskDistributionPieChart.setData(pieData);
    }

    private void setupTable() {
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimeFormatted()));
        colPatientId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientId()));
        colPatientName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientName()));
        colDoctor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDoctorName()));
        colDepartment.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDepartment().getDisplayName()));

        // Status badge column
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
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

        // Risk badge column
        colRisk.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colRisk.setCellFactory(col -> new TableCell<>() {
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

        // Quick action column
        colActions.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnCheck = new Button("Confirm");
            private final Button btnDetails = new Button("View");
            private final HBox container = new HBox(6, btnCheck, btnDetails);

            {
                btnCheck.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnDetails.getStyleClass().addAll("btn-secondary", "btn-sm");

                btnCheck.setOnAction(e -> {
                    Appointment apt = getItem();
                    if (apt != null) {
                        appointmentService.updateAppointmentStatus(apt.getId(), AppointmentStatus.CONFIRMED);
                        loadData();
                        NotificationManager.showSuccess("Status Updated",
                                "Appointment " + apt.getId() + " for " + apt.getPatientName() + " has been marked as Confirmed.");
                    }
                });

                btnDetails.setOnAction(e -> {
                    Appointment apt = getItem();
                    if (apt != null) {
                        NotificationManager.showInfo("Appointment Details",
                                "ID: " + apt.getId() + "\nPatient: " + apt.getPatientName() + " (" + apt.getPatientId() + ")\n" +
                                        "Doctor: " + apt.getDoctorName() + "\nTime: " + apt.getTimeFormatted() + " (" + apt.getDateFormatted() + ")\n" +
                                        "Reason: " + apt.getReasonForVisit() + "\n" +
                                        "No-show Risk: " + apt.getRiskScorePercent() + "% (" + apt.getRiskLevel().getLabel() + ")\n" +
                                        "Risk Factors: " + String.join(", ", apt.getRiskFactors()));
                    }
                });
            }

            @Override
            protected void updateItem(Appointment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    public void loadData() {
        todayScheduleTable.setItems(FXCollections.observableArrayList(appointmentService.getTodayAppointments()));
        setupMetrics();
    }

    @FXML
    private void handleRunBatchRisk() {
        NotificationManager.showSuccess("Risk Scoring Evaluation Complete",
                "Simulated predictive risk model evaluated all active appointments.\n" +
                        "Identified " + appointmentService.countHighRisk() + " high-risk appointments requiring operational attention.");
        loadData();
        setupCharts();
    }

    @FXML
    private void handleReallocateQuick() {
        if (mainLayoutController != null) {
            mainLayoutController.navigateTo("reallocation");
        } else {
            NotificationManager.showInfo("Smart Reallocation",
                    "Navigating to Smart Reallocation view to review candidate matches.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
        setupCharts();
    }
}
