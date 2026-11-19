package com.ghostqueue.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Appointment {
    private String id;
    private String patientId;
    private String doctorId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private AppointmentStatus status;
    private LocalDate bookingDate;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined / presentation fields
    private String patientName;
    private int patientAge;
    private String patientPhone;
    private String doctorName;
    private Department department;
    private String reasonForVisit;
    private int leadDays;
    private double predictedRiskScore;
    private RiskLevel riskLevel;
    private List<String> riskFactors;
    private boolean reallocated;

    public Appointment(String id, String patientId, String doctorId,
            LocalDate appointmentDate, LocalTime appointmentTime,
            AppointmentStatus status, LocalDate bookingDate, String cancellationReason) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.status = status != null ? status : AppointmentStatus.BOOKED;
        this.bookingDate = bookingDate != null ? bookingDate : LocalDate.now();
        this.cancellationReason = cancellationReason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.patientName = "";
        this.doctorName = "";
        this.department = Department.GENERAL_MEDICINE;
        this.reasonForVisit = "Clinical Consultation";
        this.leadDays = Math.max(0,
                (int) java.time.temporal.ChronoUnit.DAYS.between(this.bookingDate, this.appointmentDate));
        this.predictedRiskScore = 0.20;
        this.riskLevel = RiskLevel.LOW;
        this.riskFactors = new ArrayList<>();
        this.reallocated = false;
    }

    // Compatibility constructor for Phase 1 view bindings
    public Appointment(String id, String patientId, String patientName, int patientAge,
            String patientPhone, String doctorName, Department department,
            LocalDate appointmentDate, LocalTime appointmentTime,
            AppointmentStatus status, String reasonForVisit, int leadDays,
            double predictedRiskScore, List<String> riskFactors) {
        this(id, patientId, "DOC-101", appointmentDate, appointmentTime, status, LocalDate.now().minusDays(leadDays),
                null);
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.patientPhone = patientPhone;
        this.doctorName = doctorName;
        this.department = department != null ? department : Department.GENERAL_MEDICINE;
        this.reasonForVisit = reasonForVisit;
        this.leadDays = leadDays;
        this.predictedRiskScore = predictedRiskScore;
        this.riskLevel = RiskLevel.fromScore(predictedRiskScore);
        this.riskFactors = riskFactors != null ? new ArrayList<>(riskFactors) : new ArrayList<>();
    }

    // Full constructor including both patient details and attending doctorId
    public Appointment(String id, String patientId, String patientName, int patientAge,
            String patientPhone, String doctorId, String doctorName, Department department,
            LocalDate appointmentDate, LocalTime appointmentTime,
            AppointmentStatus status, String reasonForVisit, int leadDays,
            double predictedRiskScore, List<String> riskFactors) {
        this(id, patientId, doctorId, appointmentDate, appointmentTime, status, LocalDate.now().minusDays(leadDays),
                null);
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.patientPhone = patientPhone;
        this.doctorName = doctorName;
        this.department = department != null ? department : Department.GENERAL_MEDICINE;
        this.reasonForVisit = reasonForVisit;
        this.leadDays = leadDays;
        this.predictedRiskScore = predictedRiskScore;
        this.riskLevel = RiskLevel.fromScore(predictedRiskScore);
        this.riskFactors = riskFactors != null ? new ArrayList<>(riskFactors) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorId() {
        return doctorId != null ? doctorId : "";
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getCancellationReason() {
        return cancellationReason != null ? cancellationReason : "";
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPatientName() {
        return patientName != null && !patientName.isBlank() ? patientName : patientId;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public int getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(int patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientPhone() {
        return patientPhone != null ? patientPhone : "";
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getDoctorName() {
        return doctorName != null && !doctorName.isBlank() ? doctorName : doctorId;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Department getDepartment() {
        return department != null ? department : Department.GENERAL_MEDICINE;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getReasonForVisit() {
        return reasonForVisit != null ? reasonForVisit : "Clinical Consultation";
    }

    public void setReasonForVisit(String reasonForVisit) {
        this.reasonForVisit = reasonForVisit;
    }

    public int getLeadDays() {
        return leadDays;
    }

    public void setLeadDays(int leadDays) {
        this.leadDays = leadDays;
    }

    public double getPredictedRiskScore() {
        return predictedRiskScore;
    }

    public void setPredictedRiskScore(double score) {
        this.predictedRiskScore = score;
        this.riskLevel = RiskLevel.fromScore(score);
    }

    public RiskLevel getRiskLevel() {
        return riskLevel != null ? riskLevel : RiskLevel.fromScore(predictedRiskScore);
    }

    public List<String> getRiskFactors() {
        return riskFactors != null ? riskFactors : new ArrayList<>();
    }

    public void setRiskFactors(List<String> riskFactors) {
        this.riskFactors = riskFactors;
    }

    public boolean isReallocated() {
        return reallocated;
    }

    public void setReallocated(boolean reallocated) {
        this.reallocated = reallocated;
    }

    public String getTimeFormatted() {
        if (appointmentTime == null)
            return "";
        int hour = appointmentTime.getHour();
        int minute = appointmentTime.getMinute();
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0)
            displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, minute, period);
    }

    public String getDateFormatted() {
        return appointmentDate != null ? appointmentDate.toString() : "";
    }

    public int getRiskScorePercent() {
        return (int) Math.round(predictedRiskScore * 100);
    }
}
