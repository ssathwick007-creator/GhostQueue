package com.ghostqueue.prediction;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.Patient;

import java.util.ArrayList;
import java.util.List;

public class HeuristicRiskScorer implements PredictionModel {

    @Override
    public double predictNoShowProbability(Appointment appointment, Patient patient) {
        if (patient == null) {
            return appointment.getPredictedRiskScore();
        }

        double score = 0.15; // baseline clinical no-show base rate

        // Factor 1: Prior Attendance History
        if (patient.getTotalAppointments() > 0) {
            double historicalNoShowRate = (double) patient.getNoShowCount() / patient.getTotalAppointments();
            score += historicalNoShowRate * 0.45;
        }

        // Factor 2: Lead Time (booking advance interval)
        if (appointment.getLeadDays() > 30) {
            score += 0.22;
        } else if (appointment.getLeadDays() > 14) {
            score += 0.12;
        } else if (appointment.getLeadDays() <= 3) {
            score -= 0.08;
        }

        // Factor 3: Time of Day
        int hour = appointment.getAppointmentTime().getHour();
        if (hour >= 15) {
            score += 0.08; // late afternoon slots historically have higher attrition
        } else if (hour <= 9) {
            score += 0.05; // early rush hour traffic
        }

        // Clamp between 0.05 and 0.95
        return Math.max(0.05, Math.min(0.95, score));
    }

    @Override
    public List<String> extractRiskFactors(Appointment appointment, Patient patient) {
        List<String> factors = new ArrayList<>();
        if (patient != null) {
            if (patient.getNoShowCount() >= 3) {
                factors.add("Prior history: " + patient.getNoShowCount() + " missed visits (" + 
                        String.format("%.0f", (100 - patient.getAttendanceRate())) + "% no-show rate)");
            } else if (patient.getAttendanceRate() >= 90) {
                factors.add("High adherence history (" + String.format("%.0f", patient.getAttendanceRate()) + "% attendance)");
            }
        }

        if (appointment.getLeadDays() > 25) {
            factors.add("Extended lead time (" + appointment.getLeadDays() + " days)");
        } else if (appointment.getLeadDays() <= 3) {
            factors.add("Short booking interval (" + appointment.getLeadDays() + " days)");
        }

        if (appointment.getAppointmentTime().getHour() >= 15) {
            factors.add("Late afternoon scheduling slot");
        }

        if (factors.isEmpty()) {
            factors.add("Standard clinical scheduling baseline");
        }

        return factors;
    }

    @Override
    public double getModelConfidence() {
        return 0.88; // Simulated model confidence level (88%)
    }
}
