package com.ghostqueue.scheduling;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.model.ReallocationSuggestion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReallocationEngine {

    public List<ReallocationSuggestion> generateRecommendations(List<Appointment> openSlots, List<QueueEntry> waitingQueue) {
        List<ReallocationSuggestion> suggestions = new ArrayList<>();
        int counter = 101;

        for (Appointment slot : openSlots) {
            // Only consider cancelled slots or predicted high-risk slots
            boolean isEligibleSlot = (slot.getStatus() == AppointmentStatus.CANCELLED) ||
                    (slot.getStatus() == AppointmentStatus.CONFIRMED && slot.getPredictedRiskScore() >= 0.75);

            if (!isEligibleSlot) continue;

            String context = (slot.getStatus() == AppointmentStatus.CANCELLED)
                    ? "Cancelled Slot"
                    : "High-Risk Alert (" + slot.getRiskScorePercent() + "%)";

            // Find best matching patient in queue for this department
            List<QueueEntry> eligibleCandidates = waitingQueue.stream()
                    .filter(q -> q.getStatus() == QueueStatus.WAITING || q.getStatus() == QueueStatus.STANDBY)
                    .filter(q -> q.getRequestedDepartment() == slot.getDepartment())
                    .sorted(Comparator.comparing(QueueEntry::getPriority)
                            .thenComparing(QueueEntry::getRegisteredTime))
                    .toList();

            for (QueueEntry candidate : eligibleCandidates) {
                int matchScore = calculateMatchScore(slot, candidate);
                String matchReason = buildMatchReason(slot, candidate);

                suggestions.add(new ReallocationSuggestion(
                        "SR-" + (counter++),
                        slot.getId(),
                        slot.getDoctorName(),
                        slot.getDepartment(),
                        slot.getAppointmentDate(),
                        slot.getAppointmentTime(),
                        context,
                        candidate,
                        matchScore,
                        matchReason,
                        "PENDING"
                ));
                break; // Top 1 recommendation per slot
            }
        }

        return suggestions;
    }

    private int calculateMatchScore(Appointment slot, QueueEntry candidate) {
        int score = 50; // base score for same department

        if (slot.getDoctorName().equalsIgnoreCase(candidate.getPreferredDoctor())) {
            score += 25;
        }

        if (candidate.getPriority() == QueuePriority.URGENT) {
            score += 20;
        } else if (candidate.getPriority() == QueuePriority.HIGH) {
            score += 12;
        } else {
            score += 5;
        }

        if (candidate.getStatus() == QueueStatus.STANDBY) {
            score += 5; // Ready on short notice
        }

        return Math.min(99, score);
    }

    private String buildMatchReason(Appointment slot, QueueEntry candidate) {
        StringBuilder sb = new StringBuilder();
        sb.append("Direct department match (").append(slot.getDepartment().getDisplayName()).append(")");
        if (slot.getDoctorName().equalsIgnoreCase(candidate.getPreferredDoctor())) {
            sb.append("; Requested specialist match");
        }
        if (candidate.getPriority() == QueuePriority.URGENT) {
            sb.append("; Clinical urgent priority flag");
        }
        if (candidate.getStatus() == QueueStatus.STANDBY) {
            sb.append("; Confirmed standby availability");
        }
        return sb.toString();
    }
}
