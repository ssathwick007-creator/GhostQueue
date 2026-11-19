package com.ghostqueue.prediction;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing the complete prediction artifact for a clinical appointment.
 * Persisted in the MySQL appointment_predictions table and displayed in prediction dashboards.
 */
public class PredictionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String predictionId;
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String doctorName;
    private Department department;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    private double noShowProbability;
    private RiskLevel riskLevel;
    private String predictionMethod;
    private String modelVersion;
    private List<FactorExplanation> contributingFactors;
    private String suggestedAction;
    private LocalDateTime predictedAt;
    private String actualOutcome;
    private String evaluationStatus;

    // Contextual historical data for explanation drawer
    private String historicalAttendanceSummary;

    public PredictionResult(String predictionId, String appointmentId, String patientId,
                            String patientName, String doctorName, Department department,
                            LocalDate appointmentDate, LocalTime appointmentTime,
                            double noShowProbability, RiskLevel riskLevel,
                            String predictionMethod, String modelVersion,
                            List<FactorExplanation> contributingFactors, String suggestedAction,
                            LocalDateTime predictedAt, String actualOutcome, String evaluationStatus,
                            String historicalAttendanceSummary) {
        this.predictionId = predictionId;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.department = department;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.noShowProbability = noShowProbability;
        this.riskLevel = riskLevel;
        this.predictionMethod = predictionMethod;
        this.modelVersion = modelVersion;
        this.contributingFactors = contributingFactors != null ? new ArrayList<>(contributingFactors) : new ArrayList<>();
        this.suggestedAction = suggestedAction;
        this.predictedAt = predictedAt != null ? predictedAt : LocalDateTime.now();
        this.actualOutcome = actualOutcome;
        this.evaluationStatus = evaluationStatus != null ? evaluationStatus : "PENDING";
        this.historicalAttendanceSummary = historicalAttendanceSummary != null ? historicalAttendanceSummary : "No prior clinical history recorded.";
    }

    public String getPredictionId() {
        return predictionId;
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

    public String getPredictionMethod() {
        return predictionMethod;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public List<FactorExplanation> getContributingFactors() {
        return contributingFactors;
    }

    public String getFactorsSummary() {
        if (contributingFactors.isEmpty()) {
            return "Standard outpatient baseline profile.";
        }
        return contributingFactors.get(0).getDescription();
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public LocalDateTime getPredictedAt() {
        return predictedAt;
    }

    public String getPredictedAtFormatted() {
        if (predictedAt == null) return "—";
        return predictedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public String getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(String evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public String getHistoricalAttendanceSummary() {
        return historicalAttendanceSummary;
    }

    public String getTimeFormatted() {
        if (appointmentTime == null) return "";
        int hour = appointmentTime.getHour();
        int minute = appointmentTime.getMinute();
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, minute, period);
    }
}
