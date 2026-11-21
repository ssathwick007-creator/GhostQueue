package com.ghostqueue.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReallocationSuggestion {
    private final String suggestionId;
    private final String slotAppointmentId;
    private final String doctorName;
    private final Department department;
    private final LocalDate slotDate;
    private final LocalTime slotTime;
    private final String slotContext; // e.g. "Cancelled Slot", "High-Risk Slot (89%)"
    private final QueueEntry candidatePatient;
    private final int matchScore; // 0 - 100%
    private final String matchReason;
    private String status; // "PENDING", "APPROVED", "DECLINED"

    public ReallocationSuggestion(String suggestionId, String slotAppointmentId,
                                  String doctorName, Department department,
                                  LocalDate slotDate, LocalTime slotTime,
                                  String slotContext, QueueEntry candidatePatient,
                                  int matchScore, String matchReason, String status) {
        this.suggestionId = suggestionId;
        this.slotAppointmentId = slotAppointmentId;
        this.doctorName = doctorName;
        this.department = department;
        this.slotDate = slotDate;
        this.slotTime = slotTime;
        this.slotContext = slotContext;
        this.candidatePatient = candidatePatient;
        this.matchScore = matchScore;
        this.matchReason = matchReason;
        this.status = status;
    }

    public String getSuggestionId() {
        return suggestionId;
    }

    public String getSlotAppointmentId() {
        return slotAppointmentId;
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

    public String getSlotContext() {
        return slotContext;
    }

    public QueueEntry getCandidatePatient() {
        return candidatePatient;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public String getPatientName() {
        return candidatePatient != null ? candidatePatient.getPatientName() : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSlotTimeFormatted() {
        int hour = slotTime.getHour();
        int minute = slotTime.getMinute();
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, minute, period);
    }
}
