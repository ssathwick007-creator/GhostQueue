package com.ghostqueue.prediction;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.Patient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Extracts and normalizes predictive features strictly available BEFORE an appointment takes place.
 * Enforces ethical guidelines by evaluating attendance history, clinical scheduling timing,
 * and advance booking intervals, while avoiding discriminatory demographic biases.
 */
public class PredictionFeatureExtractor {

    public static final int NUM_FEATURES = FeatureVector.FEATURE_NAMES.length;

    /**
     * Extracts a normalized feature vector for inference on a future/pending appointment.
     */
    public FeatureVector extractFeatures(Appointment appointment, Patient patient) {
        return extractFeatures(appointment, patient, null);
    }

    /**
     * Extracts a feature vector with optional ground truth label for model training/evaluation.
     * Label: 1.0 if actual outcome was NO_SHOW, 0.0 if ATTENDED/COMPLETED.
     */
    public FeatureVector extractFeatures(Appointment appointment, Patient patient, Double label) {
        double[] features = new double[NUM_FEATURES];

        // 1. Lead Days: Days between booking and scheduled appointment
        long leadDays = 7;
        if (appointment.getBookingDate() != null && appointment.getAppointmentDate() != null) {
            leadDays = Math.max(0, ChronoUnit.DAYS.between(appointment.getBookingDate(), appointment.getAppointmentDate()));
        } else if (appointment.getLeadDays() > 0) {
            leadDays = appointment.getLeadDays();
        }
        features[0] = Math.min(60.0, leadDays) / 30.0; // Scaled so ~30 days is 1.0

        // 2. Historical Attendance Behaviour (strictly prior to current visit)
        int totalAppts = 0;
        int noShows = 0;
        int attended = 0;
        int cancellations = 0;

        if (patient != null) {
            totalAppts = patient.getTotalAppointments();
            noShows = patient.getNoShowCount();
            attended = patient.getAttendedAppointments();
            // Estimate prior cancellations if tracked
            cancellations = Math.max(0, totalAppts - (attended + noShows));
        }

        features[1] = Math.min(10.0, noShows) / 5.0; // Scaled prior no-show count

        // 3. Historical Attendance Rate (0.0 to 1.0). If no prior visits, clinical prior is 0.88
        if (totalAppts > 0 && (attended + noShows) > 0) {
            features[2] = (double) attended / (attended + noShows);
        } else {
            features[2] = 0.88; // Neutral hospital benchmark prior
        }

        // 4. Prior Cancellations
        features[3] = Math.min(10.0, cancellations) / 5.0;

        // 5. Appointment Hour (normal operating range 08:00 - 17:00)
        LocalTime time = appointment.getAppointmentTime() != null ? appointment.getAppointmentTime() : LocalTime.of(10, 0);
        int hour = time.getHour();
        features[4] = (hour - 8.0) / 10.0; // Scaled from 0.0 (08:00) to 1.0 (18:00)

        // 6. Day of Week: Monday or Friday (weekend adjacencies exhibit higher friction)
        LocalDate date = appointment.getAppointmentDate() != null ? appointment.getAppointmentDate() : LocalDate.now();
        DayOfWeek dow = date.getDayOfWeek();
        boolean isMonOrFri = (dow == DayOfWeek.MONDAY || dow == DayOfWeek.FRIDAY);
        features[5] = isMonOrFri ? 1.0 : 0.0;

        // 7. Specialty Sensitivity Index (certain clinics face higher baseline friction)
        Department dept = appointment.getDepartment();
        boolean higherFriction = (dept == Department.DERMATOLOGY || dept == Department.ORTHOPEDICS);
        features[6] = higherFriction ? 1.0 : 0.0;

        // 8. Short-Notice Booking (< 3 days advance): typically high intent, lower no-show
        features[7] = (leadDays <= 2) ? 1.0 : 0.0;

        // 9. Extended Advance Booking (> 21 days): high decay in patient schedule recall
        features[8] = (leadDays >= 21) ? 1.0 : 0.0;

        return new FeatureVector(features, label);
    }
}
