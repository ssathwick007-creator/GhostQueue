package com.ghostqueue.controller;

import com.ghostqueue.model.Department;
import com.ghostqueue.service.AnalyticsService;
import com.ghostqueue.service.ServiceRegistry;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.Map;

public class AnalyticsController {

    @FXML private LineChart<String, Number> attendanceTrendChart;
    @FXML private BarChart<String, Number> utilizationChart;

    private final AnalyticsService analyticsService = ServiceRegistry.getInstance().getAnalyticsService();

    @FXML
    public void initialize() {
        setupAttendanceTrendChart();
        setupUtilizationChart();
    }

    private void setupAttendanceTrendChart() {
        attendanceTrendChart.getData().clear();

        // Series 1: Attended
        XYChart.Series<String, Number> attendedSeries = new XYChart.Series<>();
        attendedSeries.setName("Attended Consultations");
        Map<String, Integer> attendedMap = analyticsService.getMonthlyAttendanceData();
        for (Map.Entry<String, Integer> entry : attendedMap.entrySet()) {
            attendedSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        // Series 2: No-Shows
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
        series.setName("Slot Utilization Rate");

        Map<Department, Double> utilizationMap = analyticsService.getDepartmentSlotUtilization();
        for (Map.Entry<Department, Double> entry : utilizationMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().getDisplayName(), entry.getValue()));
        }

        utilizationChart.getData().add(series);
    }
}
