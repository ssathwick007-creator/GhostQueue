package com.ghostqueue.service;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentReminder;
import com.ghostqueue.model.Patient;
import com.ghostqueue.prediction.PredictionResult;
import com.ghostqueue.repository.JdbcReminderRepository;
import com.ghostqueue.repository.ReminderRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Enterprise service coordinating clinical reminders, high-risk outreach tasks,
 * multi-channel logging, and response tracking.
 */
public class ReminderService {

    private static final Logger LOGGER = Logger.getLogger(ReminderService.class.getName());

    private final ReminderRepository reminderRepository;
    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final PredictionService predictionService;

    public ReminderService(ReminderRepository reminderRepository,
                           AppointmentService appointmentService,
                           PatientService patientService,
                           PredictionService predictionService) {
        this.reminderRepository = reminderRepository;
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.predictionService = predictionService;

        // Ensure baseline initial reminder tasks
        ensureBaselineReminders();
    }

    public ReminderService(AppointmentService appointmentService, PatientService patientService, PredictionService predictionService) {
        this(new JdbcReminderRepository(), appointmentService, patientService, predictionService);
    }

    public synchronized void ensureBaselineReminders() {
        try {
            if (reminderRepository.countTotal() == 0) {
                generateHighRiskReminders();
            }
        } catch (Exception e) {
            LOGGER.warning("Could not verify baseline reminders in MySQL: " + e.getMessage());
        }
    }

    /**
     * Identifies high-risk appointment warnings and automatically queues proactive outreach tasks.
     */
    public synchronized int generateHighRiskReminders() {
        List<PredictionResult> predictions = predictionService.getLatestPredictionResults();
        int generated = 0;

        for (PredictionResult pred : predictions) {
            if (pred.getRiskLevel() == com.ghostqueue.model.RiskLevel.HIGH || pred.getNoShowProbability() >= 0.65) {
                List<AppointmentReminder> existing = reminderRepository.findByAppointmentId(pred.getAppointmentId());
                if (existing.isEmpty()) {
                    Appointment apt = appointmentService.getAppointmentById(pred.getAppointmentId()).orElse(null);
                    Patient patient = patientService.getPatientById(pred.getPatientId()).orElse(null);

                    String patientName = pred.getPatientName();
                    String phone = patient != null ? patient.getPhone() : "+1 (555) 000-0000";

                    // Formulate structured clinical outreach message
                    String msg = String.format(
                            "Hospital Reminder: Upcoming consultation for %s with %s on %s at %s. " +
                            "Please reply CONFIRM to secure your slot, or RESCHEDULE if you are unable to attend.",
                            patientName, pred.getDoctorName(), pred.getAppointmentDate(), pred.getTimeFormatted()
                    );

                    AppointmentReminder.ReminderType type = pred.getNoShowProbability() >= 0.75
                            ? AppointmentReminder.ReminderType.PHONE_CALL
                            : AppointmentReminder.ReminderType.SMS;

                    AppointmentReminder reminder = new AppointmentReminder(
                            "REM-" + System.currentTimeMillis() % 1000000 + "-" + (generated + 1),
                            pred.getAppointmentId(),
                            pred.getPatientId(),
                            patientName,
                            phone,
                            type,
                            AppointmentReminder.ReminderStatus.PENDING,
                            LocalDateTime.now().plusHours(24),
                            null,
                            msg,
                            "NO_RESPONSE",
                            "Auto-generated via No-Show Risk Warning (" + pred.getProbabilityPercent() + "%)",
                            LocalDateTime.now()
                    );

                    reminderRepository.save(reminder);
                    generated++;
                }
            }
        }

        LOGGER.info("Generated " + generated + " proactive reminder outreach tasks for high-risk appointments.");
        return generated;
    }

    public List<AppointmentReminder> getAllReminders() {
        return reminderRepository.findAll();
    }

    public Optional<AppointmentReminder> getReminderById(String id) {
        return reminderRepository.findById(id);
    }

    public List<AppointmentReminder> filterReminders(String query, AppointmentReminder.ReminderType type, AppointmentReminder.ReminderStatus status) {
        return reminderRepository.filter(query, type, status);
    }

    public void markCompleted(String reminderId, String responseStatus, String notes) {
        reminderRepository.updateStatus(reminderId, AppointmentReminder.ReminderStatus.COMPLETED, responseStatus, notes);
        LOGGER.info("Reminder " + reminderId + " marked as COMPLETED (" + responseStatus + ")");
    }

    public void cancelReminder(String reminderId) {
        reminderRepository.updateStatus(reminderId, AppointmentReminder.ReminderStatus.CANCELLED, "CANCELLED", "Administrative cancellation");
        LOGGER.info("Reminder " + reminderId + " cancelled.");
    }

    public int countPending() {
        return reminderRepository.countPending();
    }

    public int countCompleted() {
        return reminderRepository.countCompleted();
    }

    public int countTotal() {
        return reminderRepository.countTotal();
    }
}
