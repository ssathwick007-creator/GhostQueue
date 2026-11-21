package com.ghostqueue.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class QueueEntry implements Comparable<QueueEntry> {
    private String queueId;
    private String patientId;
    private String doctorId;
    private LocalDate requestedDate;
    private String preferredTimeSlot;
    private QueuePriority priority;
    private QueueStatus status;
    private LocalDateTime registeredTime;

    // Joined / presentation fields
    private String patientName;
    private String patientPhone;
    private Department requestedDepartment;
    private String preferredDoctor;
    private String clinicalNote;

    public QueueEntry(String queueId, String patientId, String doctorId,
                      LocalDate requestedDate, String preferredTimeSlot,
                      QueuePriority priority, QueueStatus status, LocalDateTime registeredTime) {
        this.queueId = queueId;
        this.patientId = patientId;
        this.doctorId = doctorId != null ? doctorId : "";
        this.requestedDate = requestedDate != null ? requestedDate : LocalDate.now();
        this.preferredTimeSlot = preferredTimeSlot != null ? preferredTimeSlot : "Anytime";
        this.priority = priority != null ? priority : QueuePriority.STANDARD;
        this.status = status != null ? status : QueueStatus.WAITING;
        this.registeredTime = registeredTime != null ? registeredTime : LocalDateTime.now();
        this.patientName = "";
        this.patientPhone = "";
        this.requestedDepartment = Department.GENERAL_MEDICINE;
        this.preferredDoctor = "Any Specialist";
        this.clinicalNote = "Standard waiting triage";
    }

    public QueueEntry(String queueId, String patientId, String doctorId,
                      LocalDate requestedDate, String preferredTimeSlot,
                      QueuePriority priority, QueueStatus status,
                      LocalDateTime registeredTime, String clinicalNote) {
        this(queueId, patientId, doctorId, requestedDate, preferredTimeSlot, priority, status, registeredTime);
        this.clinicalNote = clinicalNote != null ? clinicalNote : "Standard triage";
    }

    // Compatibility constructor
    public QueueEntry(String queueId, String patientId, String patientName, String patientPhone,
                      QueuePriority priority, Department requestedDepartment,
                      String preferredDoctor, String preferredTimeSlot,
                      LocalDateTime registeredTime, QueueStatus status, String clinicalNote) {
        this(queueId, patientId, "DOC-101", LocalDate.now(), preferredTimeSlot, priority, status, registeredTime);
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.requestedDepartment = requestedDepartment != null ? requestedDepartment : Department.GENERAL_MEDICINE;
        this.preferredDoctor = preferredDoctor != null ? preferredDoctor : "Any Specialist";
        this.clinicalNote = clinicalNote != null ? clinicalNote : "Standard triage";
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getId() {
        return queueId;
    }

    public void setId(String id) {
        this.queueId = id;
    }

    public Department getDepartment() {
        return requestedDepartment;
    }

    public void setDepartment(Department department) {
        this.requestedDepartment = department;
    }

    public String getDoctorName() {
        return preferredDoctor;
    }

    public void setDoctorName(String doctorName) {
        this.preferredDoctor = doctorName;
    }

    public String getPreferredTime() {
        return preferredTimeSlot;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTimeSlot = preferredTime;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public String getPreferredTimeSlot() {
        return preferredTimeSlot;
    }

    public void setPreferredTimeSlot(String preferredTimeSlot) {
        this.preferredTimeSlot = preferredTimeSlot;
    }

    public QueuePriority getPriority() {
        return priority;
    }

    public QueuePriority getPriorityLevel() {
        return priority;
    }

    public void setPriority(QueuePriority priority) {
        this.priority = priority;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public QueueStatus getQueueStatus() {
        return status;
    }

    public void setQueueStatus(QueueStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredTime() {
        return registeredTime;
    }

    public void setRegisteredTime(LocalDateTime registeredTime) {
        this.registeredTime = registeredTime;
    }

    public String getPatientName() {
        return patientName != null && !patientName.isBlank() ? patientName : patientId;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone != null ? patientPhone : "";
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public Department getRequestedDepartment() {
        return requestedDepartment != null ? requestedDepartment : Department.GENERAL_MEDICINE;
    }

    public void setRequestedDepartment(Department requestedDepartment) {
        this.requestedDepartment = requestedDepartment;
    }

    public String getPreferredDoctor() {
        return preferredDoctor != null && !preferredDoctor.isBlank() ? preferredDoctor : "Any Specialist";
    }

    public void setPreferredDoctor(String preferredDoctor) {
        this.preferredDoctor = preferredDoctor;
    }

    public String getClinicalNote() {
        return clinicalNote != null ? clinicalNote : "";
    }

    public void setClinicalNote(String clinicalNote) {
        this.clinicalNote = clinicalNote;
    }

    public String getNotes() {
        return getClinicalNote();
    }

    public void setNotes(String notes) {
        setClinicalNote(notes);
    }

    /**
     * Transparent administrative prioritization score calculation.
     * Evaluates strictly clinical urgency and elapsed waiting duration.
     * Never confuses machine-learning no-show probability with medical urgency.
     */
    public int getTransparentPriorityScore() {
        int base = switch (priority) {
            case URGENT -> 70;
            case HIGH -> 45;
            case STANDARD -> 20;
            default -> 10;
        };
        long hoursWaiting = (registeredTime != null) ? Duration.between(registeredTime, LocalDateTime.now()).toHours() : 0;
        int timeBonus = (int) Math.min(30, Math.max(0, hoursWaiting));
        return Math.min(100, Math.max(0, base + timeBonus));
    }

    public String getPrioritizationExplanation() {
        String urgencyStr = switch (priority) {
            case URGENT -> "Clinical Urgent Flag (+70 pts)";
            case HIGH -> "High Clinical Need (+45 pts)";
            case STANDARD -> "Routine Outpatient (+20 pts)";
            default -> "Standard Baseline (+10 pts)";
        };
        long hours = (registeredTime != null) ? Duration.between(registeredTime, LocalDateTime.now()).toHours() : 0;
        return urgencyStr + " • Queue wait: " + hours + " hrs (Score: " + getTransparentPriorityScore() + ")";
    }

    public String getWaitingDurationFormatted() {
        if (registeredTime == null) return "0 min";
        Duration duration = Duration.between(registeredTime, LocalDateTime.now());
        long hours = duration.toHours();
        if (hours < 1) {
            return Math.max(1, duration.toMinutes()) + " mins";
        } else if (hours < 24) {
            return hours + " hrs " + (duration.toMinutes() % 60) + "m";
        } else {
            long days = duration.toDays();
            return days + " d " + (hours % 24) + "h";
        }
    }

    @Override
    public int compareTo(QueueEntry other) {
        // Priority ordering: URGENT > HIGH > STANDARD, then earlier registeredTime
        int priorityComp = this.priority.compareTo(other.priority);
        if (priorityComp != 0) {
            return priorityComp;
        }
        return this.registeredTime.compareTo(other.registeredTime);
    }
}
