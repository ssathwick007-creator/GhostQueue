package com.ghostqueue.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit record tracking dynamic appointment slot reallocation decisions in MySQL.
 */
public class ReallocationHistory {

    private String reallocationId;
    private String appointmentId;
    private String queueId;
    private String candidatePatientId;
    private String candidatePatientName;
    private String doctorId;
    private String doctorName;
    private Department department;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private int matchScore;
    private String matchReason;
    private String decision; // APPROVED, DECLINED, PENDING
    private String decisionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public ReallocationHistory(String reallocationId, String appointmentId, String queueId,
                               String candidatePatientId, String candidatePatientName,
                               String doctorId, String doctorName, Department department,
                               LocalDate slotDate, LocalTime slotTime,
                               int matchScore, String matchReason,
                               String decision, String decisionNotes,
                               LocalDateTime createdAt, LocalDateTime decidedAt) {
        this.reallocationId = reallocationId;
        this.appointmentId = appointmentId;
        this.queueId = queueId;
        this.candidatePatientId = candidatePatientId;
        this.candidatePatientName = candidatePatientName != null ? candidatePatientName : candidatePatientId;
        this.doctorId = doctorId;
        this.doctorName = doctorName != null ? doctorName : doctorId;
        this.department = department != null ? department : Department.GENERAL_MEDICINE;
        this.slotDate = slotDate != null ? slotDate : LocalDate.now();
        this.slotTime = slotTime != null ? slotTime : LocalTime.of(10, 0);
        this.matchScore = matchScore;
        this.matchReason = matchReason;
        this.decision = decision;
        this.decisionNotes = decisionNotes != null ? decisionNotes : "";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.decidedAt = decidedAt;
    }

    public String getReallocationId() {
        return reallocationId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getQueueId() {
        return queueId;
    }

    public String getCandidatePatientId() {
        return candidatePatientId;
    }

    public String getCandidatePatientName() {
        return candidatePatientName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Department getDepartment() {
        return department;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public LocalTime getSlotTime() {
        return slotTime;
    }

    public String getTimeFormatted() {
        if (slotTime == null) return "";
        int hour = slotTime.getHour();
        int minute = slotTime.getMinute();
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, minute, period);
    }

    public int getMatchScore() {
        return matchScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public String getDecision() {
        return decision;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecidedAtFormatted() {
        if (decidedAt == null) return "Pending";
        return decidedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }
}
