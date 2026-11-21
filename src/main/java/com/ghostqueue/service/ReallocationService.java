package com.ghostqueue.service;

import com.ghostqueue.model.*;
import com.ghostqueue.repository.JdbcReallocationHistoryRepository;
import com.ghostqueue.repository.ReallocationHistoryRepository;
import com.ghostqueue.scheduling.ReallocationEngine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enterprise service managing dynamic slot reallocation workflows, candidate evaluations,
 * and immutable decision audit logging in MySQL.
 */
public class ReallocationService {

    private static final Logger LOGGER = Logger.getLogger(ReallocationService.class.getName());

    private final AppointmentService appointmentService;
    private final QueueService queueService;
    private final ReallocationHistoryRepository historyRepository;
    private final ReallocationEngine reallocationEngine;
    private final List<ReallocationSuggestion> cachedSuggestions = new ArrayList<>();

    public ReallocationService(AppointmentService appointmentService,
                               QueueService queueService,
                               ReallocationHistoryRepository historyRepository) {
        this.appointmentService = appointmentService;
        this.queueService = queueService;
        this.historyRepository = historyRepository;
        this.reallocationEngine = new ReallocationEngine();
        refreshSuggestions();
    }

    public ReallocationService(AppointmentService appointmentService, QueueService queueService) {
        this(appointmentService, queueService, new JdbcReallocationHistoryRepository());
    }

    public synchronized void refreshSuggestions() {
        cachedSuggestions.clear();
        List<Appointment> allAppointments = appointmentService.getAllAppointments();
        List<QueueEntry> queueEntries = queueService.getAllQueueEntries();
        cachedSuggestions.addAll(reallocationEngine.generateRecommendations(allAppointments, queueEntries));
        LOGGER.info("Refreshed reallocation recommendations: " + cachedSuggestions.size() + " matches identified.");
    }

    public List<ReallocationSuggestion> getSuggestions() {
        if (cachedSuggestions.isEmpty()) {
            refreshSuggestions();
        }
        return new ArrayList<>(cachedSuggestions);
    }

    public List<Appointment> getAvailableOpenSlots() {
        return appointmentService.getAllAppointments().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED ||
                        (a.getStatus() == AppointmentStatus.CONFIRMED &&
                                (a.getPredictedRiskScore() >= 0.65 || a.getRiskLevel() == RiskLevel.HIGH)))
                .collect(Collectors.toList());
    }

    public List<Appointment> getConfirmedVacantSlots() {
        return appointmentService.getAllAppointments().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());
    }

    public List<Appointment> getHighRiskStandbySlots() {
        return appointmentService.getAllAppointments().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED &&
                        (a.getPredictedRiskScore() >= 0.65 || a.getRiskLevel() == RiskLevel.HIGH))
                .collect(Collectors.toList());
    }

    /**
     * Approves a candidate reallocation for an appointment slot.
     * Transactionally updates queue status, flags appointment, and records decision in MySQL.
     * Never automatically cancels booked patients on prediction alone.
     */
    public boolean approveReallocation(String suggestionId, String operatorNotes) {
        for (ReallocationSuggestion s : cachedSuggestions) {
            if (s.getSuggestionId().equals(suggestionId)) {
                boolean isVacancy = s.getSlotContext().toLowerCase().contains("cancelled");
                String decision = isVacancy ? "APPROVED (SLOT BACKFILL)" : "APPROVED (STANDBY BACKUP)";
                s.setStatus("APPROVED");

                // Update Queue status
                QueueStatus newQueueStatus = isVacancy ? QueueStatus.SCHEDULED : QueueStatus.STANDBY;
                queueService.updateStatus(s.getCandidatePatient().getQueueId(), newQueueStatus);

                // Update Appointment flag if actually cancelled
                if (isVacancy) {
                    appointmentService.getAppointmentById(s.getSlotAppointmentId()).ifPresent(apt -> {
                        apt.setReallocated(true);
                    });
                }

                // Record in MySQL reallocation_history audit log
                ReallocationHistory historyRecord = new ReallocationHistory(
                        "RH-" + System.currentTimeMillis() % 1000000,
                        s.getSlotAppointmentId(),
                        s.getCandidatePatient().getQueueId(),
                        s.getCandidatePatient().getPatientId(),
                        s.getCandidatePatient().getPatientName(),
                        s.getSlotAppointmentId(),
                        s.getDoctorName(),
                        s.getDepartment(),
                        s.getSlotDate(),
                        s.getSlotTime(),
                        s.getMatchScore(),
                        s.getMatchReason(),
                        decision,
                        operatorNotes != null ? operatorNotes : "Operator approved via Smart Reallocation Console",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
                historyRepository.save(historyRecord);

                LOGGER.info("Reallocation approved for " + s.getCandidatePatient().getPatientName() +
                        " on slot " + s.getSlotAppointmentId() + " (" + decision + ")");
                return true;
            }
        }
        return false;
    }

    public boolean approveReallocation(String suggestionId) {
        return approveReallocation(suggestionId, "Approved via Smart Reallocation Console");
    }

    public boolean declineReallocation(String suggestionId, String reason) {
        for (ReallocationSuggestion s : cachedSuggestions) {
            if (s.getSuggestionId().equals(suggestionId)) {
                s.setStatus("DECLINED");

                ReallocationHistory historyRecord = new ReallocationHistory(
                        "RH-" + System.currentTimeMillis() % 1000000,
                        s.getSlotAppointmentId(),
                        s.getCandidatePatient().getQueueId(),
                        s.getCandidatePatient().getPatientId(),
                        s.getCandidatePatient().getPatientName(),
                        s.getSlotAppointmentId(),
                        s.getDoctorName(),
                        s.getDepartment(),
                        s.getSlotDate(),
                        s.getSlotTime(),
                        s.getMatchScore(),
                        s.getMatchReason(),
                        "DECLINED",
                        reason != null ? reason : "Declined by operator",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
                historyRepository.save(historyRecord);
                return true;
            }
        }
        return false;
    }

    public boolean declineReallocation(String suggestionId) {
        return declineReallocation(suggestionId, "Declined by operator");
    }

    public List<ReallocationHistory> getReallocationHistory() {
        return historyRepository.findAll();
    }

    public int getApprovedCount() {
        return historyRepository.countApproved();
    }
}
