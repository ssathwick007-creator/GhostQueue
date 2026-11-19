package com.ghostqueue.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class PredictionRecord {
    private final String appointmentId;
    private final String patientId;
    private final String patientName;
    private final String doctorName;
    private final Department department;
    private final LocalDate appointmentDate;
    private final LocalTime appointmentTime;
    private final double noShowProbability;
    private final RiskLevel riskLevel;
    private final List<String> primaryRiskFactors;
    private final String recommendedAction;
    private final double modelConfidence;

    public PredictionRecord(String appointmentId, String patientId, String patientName,
                            String doctorName, Department department, LocalDate appointmentDate,
                            LocalTime appointmentTime, double noShowProbability,
                            RiskLevel riskLevel, List<String> primaryRiskFactors,
                            String recommendedAction, double modelConfidence) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.department = department;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.noShowProbability = noShowProbability;
        this.riskLevel = riskLevel;
        this.primaryRiskFactors = primaryRiskFactors;
        this.recommendedAction = recommendedAction;
        this.modelConfidence = modelConfidence;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Department getDepartment() {
        return department;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public double getNoShowProbability() {
        return noShowProbability;
    }

    public int getProbabilityPercent() {
        return (int) Math.round(noShowProbability * 100);
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getPrimaryRiskFactors() {
        return primaryRiskFactors;
    }

    public String getRiskFactorsSummary() {
        if (primaryRiskFactors == null || primaryRiskFactors.isEmpty()) {
            return "Standard profile parameters";
        }
        return String.join(" • ", primaryRiskFactors);
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public double getModelConfidence() {
        return modelConfidence;
    }

    public int getConfidencePercent() {
        return (int) Math.round(modelConfidence * 100);
    }

    public String getTimeFormatted() {
        int hour = appointmentTime.getHour();
        int minute = appointmentTime.getMinute();
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, minute, period);
    }
}
