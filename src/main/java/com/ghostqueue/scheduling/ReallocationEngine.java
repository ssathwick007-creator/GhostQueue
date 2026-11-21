package com.ghostqueue.scheduling;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.model.ReallocationSuggestion;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Intelligent Dynamic Slot Reallocation Engine.
 * Evaluates open vacant slots (cancelled visits) and high-risk appointment warnings against
 * waiting queue candidates using multi-factor clinical compatibility scoring.
 */
public class ReallocationEngine {

    /**
     * Evaluates available appointment slots against active queue candidates,
     * returning prioritized matching recommendations.
     */
    public List<ReallocationSuggestion> generateRecommendations(List<Appointment> appointments, List<QueueEntry> waitingQueue) {
        List<ReallocationSuggestion> suggestions = new ArrayList<>();
        int counter = 101;

        for (Appointment slot : appointments) {
            // Determine eligibility: Confirmed vacancy (CANCELLED) OR High-risk prediction warning
            boolean isCancelled = (slot.getStatus() == AppointmentStatus.CANCELLED);
            boolean isHighRisk = (slot.getStatus() == AppointmentStatus.CONFIRMED &&
                    (slot.getPredictedRiskScore() >= 0.65 || slot.getRiskLevel() == com.ghostqueue.model.RiskLevel.HIGH));

            if (!isCancelled && !isHighRisk) {
                continue;
            }

            String context = isCancelled ? "Confirmed Vacancy (Cancelled Slot)" : "Standby Match (Predicted High-Risk Slot)";

            // Filter waiting candidates eligible for this slot
            List<QueueEntry> eligibleCandidates = waitingQueue.stream()
                    .filter(q -> q.getStatus() == QueueStatus.WAITING || q.getStatus() == QueueStatus.STANDBY || q.getStatus() == QueueStatus.OFFERED)
                    .filter(q -> q.getRequestedDepartment() == slot.getDepartment() || q.getRequestedDepartment() == com.ghostqueue.model.Department.ALL)
                    .toList();

            // Rank candidates for this slot by compatibility score descending
            List<RankedCandidate> ranked = new ArrayList<>();
            for (QueueEntry candidate : eligibleCandidates) {
                int score = calculateCompatibilityScore(slot, candidate);
                String reason = buildCompatibilityReason(slot, candidate);
                ranked.add(new RankedCandidate(candidate, score, reason));
            }

            ranked.sort(Comparator.comparingInt(RankedCandidate::score).reversed());

            // Provide top matches for this slot
            int limit = Math.min(2, ranked.size());
            for (int i = 0; i < limit; i++) {
                RankedCandidate rc = ranked.get(i);
                suggestions.add(new ReallocationSuggestion(
                        "SR-" + (counter++),
                        slot.getId(),
                        slot.getDoctorName(),
                        slot.getDepartment(),
                        slot.getAppointmentDate(),
                        slot.getAppointmentTime(),
                        context,
                        rc.candidate(),
                        rc.score(),
                        rc.reason(),
                        "PENDING"
                ));
            }
        }

        // Sort overall suggestions by match score descending
        suggestions.sort(Comparator.comparingInt(ReallocationSuggestion::getMatchScore).reversed());
        return suggestions;
    }

    /**
     * Computes multi-factor clinical compatibility score [0..99]
     */
    public int calculateCompatibilityScore(Appointment slot, QueueEntry candidate) {
        int score = 35; // Base score: Department compatibility

        // 1. Doctor compatibility
        if (slot.getDoctorName() != null && slot.getDoctorName().equalsIgnoreCase(candidate.getPreferredDoctor())) {
            score += 25;
        } else if (candidate.getDoctorId() != null && candidate.getDoctorId().equalsIgnoreCase(slot.getDoctorId())) {
            score += 25;
        }

        // 2. Date compatibility (slot is on or after candidate requested date)
        if (slot.getAppointmentDate() != null && candidate.getRequestedDate() != null) {
            if (!slot.getAppointmentDate().isBefore(candidate.getRequestedDate())) {
                score += 15;
            }
        }

        // 3. Time slot preference compatibility
        if (isTimeCompatible(slot.getAppointmentTime(), candidate.getPreferredTimeSlot())) {
            score += 10;
        }

        // 4. Clinical priority triage weighting
        if (candidate.getPriority() == QueuePriority.URGENT) {
            score += 15;
        } else if (candidate.getPriority() == QueuePriority.HIGH) {
            score += 10;
        } else {
            score += 4;
        }

        // 5. Standby readiness
        if (candidate.getStatus() == QueueStatus.STANDBY) {
            score += 5;
        }

        return Math.min(99, score);
    }

    private boolean isTimeCompatible(LocalTime slotTime, String prefSlot) {
        if (prefSlot == null || prefSlot.isBlank() || prefSlot.toLowerCase().contains("anytime")) {
            return true;
        }
        if (slotTime == null) return true;
        int hour = slotTime.getHour();
        if (prefSlot.toLowerCase().contains("morning") && hour < 12) {
            return true;
        }
        if (prefSlot.toLowerCase().contains("afternoon") && hour >= 12) {
            return true;
        }
        return false;
    }

    public String buildCompatibilityReason(Appointment slot, QueueEntry candidate) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Department match (" + slot.getDepartment().getDisplayName() + ")");

        if (slot.getDoctorName() != null && slot.getDoctorName().equalsIgnoreCase(candidate.getPreferredDoctor())) {
            reasons.add("Requested specialist match (" + slot.getDoctorName() + ")");
        }

        if (candidate.getPriority() == QueuePriority.URGENT) {
            reasons.add("Clinical Urgent Triage priority");
        } else if (candidate.getPriority() == QueuePriority.HIGH) {
            reasons.add("High clinical need");
        }

        if (isTimeCompatible(slot.getAppointmentTime(), candidate.getPreferredTimeSlot())) {
            reasons.add("Time window aligned (" + candidate.getPreferredTimeSlot() + ")");
        }

        if (candidate.getStatus() == QueueStatus.STANDBY) {
            reasons.add("Confirmed standby readiness");
        }

        return String.join(" • ", reasons);
    }

    private record RankedCandidate(QueueEntry candidate, int score, String reason) {}
}
