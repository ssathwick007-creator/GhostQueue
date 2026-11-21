package com.ghostqueue.controller;

import com.ghostqueue.model.AnalyticsKpis;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.Doctor;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.service.AnalyticsService;
import com.ghostqueue.service.DoctorService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Hospital Operational Intelligence & Analytics Dashboard.
 * Visualizes attendance trends, specialty capacity utilization, risk distributions,
 * peak time slots, physician workloads, and provides real CSV export capabilities.
 */
public class AnalyticsController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<DoctorFilterItem> doctorFilter;

    @FXML private Label attendanceRateLabel;
    @FXML private Label attendanceSubtextLabel;
    @FXML private Label noShowRateLabel;
    @FXML private Label noShowSubtextLabel;
    @FXML private Label slotReclaimLabel;
    @FXML private Label slotReclaimSubtextLabel;
    @FXML private Label queueStandbyLabel;
    @FXML private Label queueSubtextLabel;

    @FXML private LineChart<String, Number> attendanceTrendChart;
    @FXML private BarChart<String, Number> utilizationChart;
    @FXML private PieChart riskDistributionChart;
    @FXML private BarChart<String, Number> timeSlotChart;
    @FXML private BarChart<String, Number> doctorWorkloadChart;

    private final AnalyticsService analyticsService = ServiceRegistry.getInstance().getAnalyticsService();
    private final DoctorService doctorService = ServiceRegistry.getInstance().getDoctorService();

    public record DoctorFilterItem(String id, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    @FXML
    public void initialize() {
        setupFilterControls();
        loadData();
    }

    public void loadData() {
        populateKpis();
        setupAttendanceTrendChart();
        setupUtilizationChart();
        setupRiskDistributionChart();
        setupTimeSlotChart();
        setupDoctorWorkloadChart();
    }

    private void setupFilterControls() {
        departmentFilter.setItems(FXCollections.observableArrayList(Department.values()));
        departmentFilter.getSelectionModel().select(Department.ALL);
        departmentFilter.setOnAction(e -> updateDoctorFilterItems());

        updateDoctorFilterItems();
    }

    private void updateDoctorFilterItems() {
        ObservableList<DoctorFilterItem> docItems = FXCollections.observableArrayList();
        docItems.add(new DoctorFilterItem("ALL", "All Attending Physicians"));

        Department selectedDept = departmentFilter.getValue();
        List<Doctor> doctors;
        if (selectedDept != null && selectedDept != Department.ALL) {
            doctors = doctorService.getDoctorsByDepartment(selectedDept.name());
        } else {
            doctors = doctorService.getAllDoctors();
        }

        for (Doctor d : doctors) {
            docItems.add(new DoctorFilterItem(d.getId(), d.getFullName() + " (" + d.getSpecialization() + ")"));
        }

        doctorFilter.setItems(docItems);
        doctorFilter.getSelectionModel().select(0);
    }

    private void populateKpis() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        Department dept = departmentFilter.getValue();
        DoctorFilterItem docItem = doctorFilter.getValue();
        String docId = (docItem != null) ? docItem.id() : "ALL";

        AnalyticsKpis kpis = analyticsService.getFilteredKpis(start, end, dept, docId);

        attendanceRateLabel.setText(String.format("%.1f%%", kpis.attendanceRate()));
        attendanceSubtextLabel.setText(kpis.completedCount() + " of " + kpis.totalAppointments() + " visits completed");

        noShowRateLabel.setText(String.format("%.1f%%", kpis.noShowRate()));
        noShowSubtextLabel.setText(kpis.noShowCount() + " unannounced patient absences");

        slotReclaimLabel.setText(String.format("%.1f%%", kpis.slotRecoveryEfficiency()));
        slotReclaimSubtextLabel.setText("Reclaimed cancelled & high-risk slots");

        queueStandbyLabel.setText(kpis.activeQueueStandbys() + " Patients");
        queueSubtextLabel.setText("Triage patients ready for reallocation");
    }

    private void setupAttendanceTrendChart() {
        attendanceTrendChart.getData().clear();

        XYChart.Series<String, Number> attendedSeries = new XYChart.Series<>();
        attendedSeries.setName("Attended Consultations");
        Map<String, Integer> attendedMap = analyticsService.getMonthlyAttendanceData();
        for (Map.Entry<String, Integer> entry : attendedMap.entrySet()) {
            attendedSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        XYChart.Series<String, Number> noShowSeries = new XYChart.Series<>();
        noShowSeries.setName("Unannounced No-Shows");
        Map<String, Integer> noShowMap = analyticsService.getMonthlyNoShowData();
        for (Map.Entry<String, Integer> entry : noShowMap.entrySet()) {
            noShowSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        attendanceTrendChart.getData().addAll(attendedSeries, noShowSeries);
    }

    private void setupUtilizationChart() {
        utilizationChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Slot Utilization Rate %");

        Map<Department, Double> utilizationMap = analyticsService.getDepartmentSlotUtilization();
        for (Map.Entry<Department, Double> entry : utilizationMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().getDisplayName(), entry.getValue()));
        }

        utilizationChart.getData().add(series);
    }

    private void setupRiskDistributionChart() {
        riskDistributionChart.getData().clear();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        Department dept = departmentFilter.getValue();
        DoctorFilterItem docItem = doctorFilter.getValue();
        String docId = (docItem != null) ? docItem.id() : "ALL";

        Map<RiskLevel, Integer> map = analyticsService.getRiskDistributionFiltered(start, end, dept, docId);

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<RiskLevel, Integer> entry : map.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(entry.getKey().getDisplayName() + " (" + entry.getValue() + ")", entry.getValue()));
            }
        }
        riskDistributionChart.setData(pieData);
    }

    private void setupTimeSlotChart() {
        timeSlotChart.getData().clear();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        Department dept = departmentFilter.getValue();
        DoctorFilterItem docItem = doctorFilter.getValue();
        String docId = (docItem != null) ? docItem.id() : "ALL";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations");

        Map<String, Integer> map = analyticsService.getTimeSlotDistribution(start, end, dept, docId);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        timeSlotChart.getData().add(series);
    }

    private void setupDoctorWorkloadChart() {
        doctorWorkloadChart.getData().clear();
        Department dept = departmentFilter.getValue();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations");

        Map<String, Integer> map = analyticsService.getDoctorWorkloadDistribution(dept);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        doctorWorkloadChart.getData().add(series);
    }

    @FXML
    private void handleApplyFilters() {
        populateKpis();
        setupRiskDistributionChart();
        setupTimeSlotChart();
        setupDoctorWorkloadChart();
        NotificationManager.showInfo("Analytics Filtered", "Dashboard metrics and charts refreshed for the selected operational scope.");
    }

    @FXML
    private void handleResetFilters() {
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        departmentFilter.getSelectionModel().select(Department.ALL);
        updateDoctorFilterItems();
        populateKpis();
        setupRiskDistributionChart();
        setupTimeSlotChart();
        setupDoctorWorkloadChart();
        NotificationManager.showInfo("Filters Cleared", "Analytics reset to full hospital baseline view.");
    }

    @FXML
    private void handleExportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Hospital Operational Analytics Report");
        fileChooser.setInitialFileName("ghostqueue_analytics_report_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Comma Separated Files (*.csv)", "*.csv"));

        File targetFile = fileChooser.showSaveDialog(attendanceTrendChart.getScene().getWindow());
        if (targetFile != null) {
            try {
                LocalDate start = startDatePicker.getValue();
                LocalDate end = endDatePicker.getValue();
                Department dept = departmentFilter.getValue();
                DoctorFilterItem docItem = doctorFilter.getValue();
                String docId = (docItem != null) ? docItem.id() : "ALL";

                int exported = analyticsService.exportAnalyticsReportToCsv(targetFile, start, end, dept, docId);
                NotificationManager.showSuccess("CSV Report Generated",
                        "Successfully exported " + exported + " clinical appointment records with risk probabilities and outcomes to:\n" + targetFile.getAbsolutePath());
            } catch (Exception e) {
                NotificationManager.showError("Export Failed", "Could not generate CSV report: " + e.getMessage());
            }
        }
    }
}
