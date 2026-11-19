package com.ghostqueue.prediction;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.Patient;
import com.ghostqueue.model.RiskLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Explainability engine (XAI) for translating model mathematical weights and feature values
 * into clear, actionable, human-readable explanations and clinical administration recommendations.
 */
public class PredictionExplanation {

    /**
     * Decomposes model inference into ranked contributing factors and recommended actions.
     */
    public List<FactorExplanation> explain(LogisticRegressionModel model, FeatureVector features,
                                          Appointment appointment, Patient patient) {
        List<FactorExplanation> factors = new ArrayList<>();
        double[] x = features.getValues();
        double[] weights = model.getWeights();

        // 1. Lead Days Analysis
        long leadDays = appointment.getLeadDays();
        double leadContribution = weights[0] * x[0];
        if (leadDays >= 21) {
            factors.add(new FactorExplanation(
                    "Advance Booking Interval",
                    FactorExplanation.Direction.INCREASES_RISK,
                    leadContribution,
                    String.format("Scheduled %d days in advance. Extended booking horizons exhibit higher attrition.", leadDays)
            ));
        } else if (leadDays <= 2) {
            factors.add(new FactorExplanation(
                    "Short-Notice Scheduling",
                    FactorExplanation.Direction.MITIGATES_RISK,
                    weights[7] * x[7],
                    String.format("Booked only %d day(s) prior. Urgent or recent scheduling demonstrates strong attendance commitment.", leadDays)
            ));
        }

        // 2. Prior No-Show Record
        int priorNoShows = patient != null ? patient.getNoShowCount() : 0;
        if (priorNoShows >= 2) {
            factors.add(new FactorExplanation(
                    "Historical Missed Appointments",
                    FactorExplanation.Direction.INCREASES_RISK,
                    weights[1] * x[1],
                    String.format("Patient has %d previously recorded unannounced no-shows in clinic records.", priorNoShows)
            ));
        } else if (patient != null && patient.getTotalAppointments() >= 4 && priorNoShows == 0) {
            factors.add(new FactorExplanation(
                    "Exemplary Attendance History",
                    FactorExplanation.Direction.MITIGATES_RISK,
                    weights[2] * x[2],
                    String.format("Perfect historical attendance record across %d prior consultations.", patient.getTotalAppointments())
            ));
        }

        // 3. Historical Attendance Adherence
        if (patient != null && patient.getTotalAppointments() > 0) {
            double rate = patient.getAttendanceRate();
            if (rate < 65.0) {
                factors.add(new FactorExplanation(
                        "Low Clinical Adherence Rate",
                        FactorExplanation.Direction.INCREASES_RISK,
                        Math.abs(weights[2] * (1.0 - x[2])),
                        String.format("Historical clinic attendance rate is only %.1f%% across past appointments.", rate)
                ));
            } else if (rate >= 85.0 && priorNoShows > 0) {
                factors.add(new FactorExplanation(
                        "Reliable Overall Adherence",
                        FactorExplanation.Direction.MITIGATES_RISK,
                        Math.abs(weights[2] * x[2]),
                        String.format("Maintains a solid %.1f%% overall attendance adherence rate.", rate)
                ));
            }
        }

        // 4. Time of Day
        int hour = appointment.getAppointmentTime() != null ? appointment.getAppointmentTime().getHour() : 10;
        if (hour >= 15) {
            factors.add(new FactorExplanation(
                    "Late Afternoon Slot",
                    FactorExplanation.Direction.INCREASES_RISK,
                    weights[4] * x[4],
                    String.format("Consultation at %s. Late afternoon appointments have higher delay and cancellation rates.", appointment.getTimeFormatted())
            ));
        }

        // 5. Day of Week
        if (x[5] > 0.5) {
            factors.add(new FactorExplanation(
                    "Weekend Adjacency",
                    FactorExplanation.Direction.INCREASES_RISK,
                    weights[5] * x[5],
                    "Appointment on Monday/Friday. Weekend adjacencies show statistically elevated scheduling friction."
            ));
        }

        // 6. Specialty Sensitivity
        if (x[6] > 0.5) {
            factors.add(new FactorExplanation(
                    "Specialty Benchmark",
                    FactorExplanation.Direction.INCREASES_RISK,
                    weights[6] * x[6],
                    "Outpatient specialty clinic with historically variable patient follow-up patterns."
            ));
        }

        // Ensure at least one baseline factor
        if (factors.isEmpty()) {
            factors.add(new FactorExplanation(
                    "Standard Clinical Baseline",
                    FactorExplanation.Direction.NEUTRAL,
                    0.0,
                    "General hospital outpatient appointment baseline profile without significant outlier risk indicators."
            ));
        }

        // Sort factors by absolute impact score descending
        factors.sort(Comparator.comparingDouble((FactorExplanation f) -> Math.abs(f.getImpactScore())).reversed());

        return factors;
    }

    /**
     * Derives actionable administrative guidance from the predicted risk classification.
     */
    public String getRecommendedAction(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> "Direct telephone outreach by scheduling team. Request explicit attendance confirmation and prepare standby queue candidate.";
            case MEDIUM -> "Send automated digital reminder prompt 24h prior with one-click attendance confirmation link.";
            case LOW -> "Standard automated appointment notification 48h prior.";
            default -> "Standard outpatient scheduling review.";
        };
    }
}
