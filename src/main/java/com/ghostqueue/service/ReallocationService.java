package com.ghostqueue.service;

import com.ghostqueue.model.*;
import com.ghostqueue.scheduling.ReallocationEngine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReallocationService {
    private final AppointmentService appointmentService;
    private final QueueService queueService;
    private final ReallocationEngine reallocationEngine;
    private final List<ReallocationSuggestion> cachedSuggestions = new ArrayList<>();

    public ReallocationService(AppointmentService appointmentService, QueueService queueService) {
        this.appointmentService = appointmentService;
        this.queueService = queueService;
        this.reallocationEngine = new ReallocationEngine();
        refreshSuggestions();
    }

    public synchronized void refreshSuggestions() {
        cachedSuggestions.clear();
        List<Appointment> allAppointments = appointmentService.getAllAppointments();
        List<QueueEntry> queueEntries = queueService.getAllQueueEntries();
        cachedSuggestions.addAll(reallocationEngine.generateRecommendations(allAppointments, queueEntries));
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
                        (a.getStatus() == AppointmentStatus.CONFIRMED && a.getPredictedRiskScore() >= 0.70))
                .collect(Collectors.toList());
    }

    public boolean approveReallocation(String suggestionId) {
        for (ReallocationSuggestion s : cachedSuggestions) {
            if (s.getSuggestionId().equals(suggestionId)) {
                s.setStatus("APPROVED");
                queueService.updateStatus(s.getCandidatePatient().getQueueId(), QueueStatus.REALLOCATED);
                appointmentService.getAppointmentById(s.getSlotAppointmentId()).ifPresent(apt -> {
                    apt.setReallocated(true);
                });
                return true;
            }
        }
        return false;
    }

    public boolean declineReallocation(String suggestionId) {
        for (ReallocationSuggestion s : cachedSuggestions) {
            if (s.getSuggestionId().equals(suggestionId)) {
                s.setStatus("DECLINED");
                return true;
            }
        }
        return false;
    }
}
