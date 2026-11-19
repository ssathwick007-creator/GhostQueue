package com.ghostqueue.service;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.Doctor;
import com.ghostqueue.repository.AppointmentHistoryRepository;
import com.ghostqueue.repository.AppointmentRepository;
import com.ghostqueue.repository.DoctorRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              AppointmentHistoryRepository historyRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.historyRepository = historyRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(String id) {
        return appointmentRepository.findById(id);
    }

    public List<Appointment> getTodayAppointments() {
        return appointmentRepository.findByDate(LocalDate.now());
    }

    public List<Appointment> filterAppointments(String searchQuery, Department department, String doctorId, AppointmentStatus status, LocalDate date) {
        return appointmentRepository.filter(searchQuery, department, doctorId, status, date);
    }

    public List<Appointment> filterAppointments(String searchQuery, Department department, AppointmentStatus status, LocalDate date) {
        return appointmentRepository.filter(searchQuery, department, null, status, date);
    }

    public void createAppointment(Appointment appointment) {
        validateNewAppointment(appointment);

        // 1. Business Rule: Reject past dates for new bookings
        LocalDate today = LocalDate.now();
        if (appointment.getAppointmentDate().isBefore(today)) {
            throw new IllegalArgumentException("Booking Rejected: Cannot schedule an appointment on a past date (" + 
                    appointment.getAppointmentDate() + ").");
        }

        // If today, reject past time slots
        if (appointment.getAppointmentDate().isEqual(today)) {
            LocalTime now = LocalTime.now();
            if (appointment.getAppointmentTime().isBefore(now.minusMinutes(15))) {
                throw new IllegalArgumentException("Booking Rejected: Appointment time slot " + 
                        appointment.getTimeFormatted() + " has already elapsed today.");
            }
        }

        // 2. Business Rule: Validate Doctor Availability Status
        if (appointment.getDoctorId() != null && !appointment.getDoctorId().isBlank()) {
            doctorRepository.findById(appointment.getDoctorId()).ifPresent(doctor -> {
                if ("ON_LEAVE".equalsIgnoreCase(doctor.getAvailabilityStatus()) || "UNAVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
                    throw new IllegalStateException("Physician Unavailable: " + doctor.getFullName() + 
                            " is currently marked as " + doctor.getAvailabilityStatus() + ".");
                }
            });
        }

        // 3. Business Rule: Prevent overlapping appointments for the same doctor
        boolean doctorConflict = appointmentRepository.hasConflictForDoctor(
                appointment.getDoctorId(), appointment.getAppointmentDate(), appointment.getAppointmentTime(), appointment.getId());
        if (doctorConflict) {
            throw new IllegalStateException("Physician Schedule Conflict: Doctor already has an active appointment booked for " + 
                    appointment.getAppointmentDate() + " at " + appointment.getTimeFormatted() + ".\nPlease select an alternate time slot.");
        }

        // 4. Business Rule: Prevent conflicting appointments for the same patient
        boolean patientConflict = appointmentRepository.hasConflictForPatient(
                appointment.getPatientId(), appointment.getAppointmentDate(), appointment.getAppointmentTime(), appointment.getId());
        if (patientConflict) {
            throw new IllegalStateException("Patient Schedule Conflict: Patient (" + appointment.getPatientId() + 
                    ") already has an active appointment booked on " + appointment.getAppointmentDate() + " at " + appointment.getTimeFormatted() + ".");
        }

        // 5. Business Rule: Save transactionally with audit history
        String auditRemarks = "Appointment scheduled for " + appointment.getPatientName() + " with " + appointment.getDoctorName();
        appointmentRepository.saveTransactional(appointment, auditRemarks);
    }

    public void updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus, String cancellationReason, String remarks) {
        if (appointmentId == null || appointmentId.isBlank()) {
            throw new IllegalArgumentException("Appointment ID cannot be blank.");
        }
        if (newStatus == null || newStatus == AppointmentStatus.ALL) {
            throw new IllegalArgumentException("Valid new status must be provided.");
        }

        appointmentRepository.updateStatus(appointmentId, newStatus, cancellationReason, remarks);
    }

    public void updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus) {
        updateAppointmentStatus(appointmentId, newStatus, null, "Status updated to " + newStatus.getDisplayName());
    }

    public void cancelAppointment(String appointmentId, String reason) {
        String cleanReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Cancelled by clinic administration";
        updateAppointmentStatus(appointmentId, AppointmentStatus.CANCELLED, cleanReason, "Cancelled: " + cleanReason);
    }

    public void cancelAppointment(String appointmentId) {
        cancelAppointment(appointmentId, "Cancelled by user via operational dashboard");
    }

    public List<Appointment> getAppointmentsForPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<AppointmentHistory> getAppointmentHistory(String appointmentId) {
        return historyRepository.findByAppointmentId(appointmentId);
    }

    public int countTotal() {
        return appointmentRepository.countTotal();
    }

    public int countToday() {
        return appointmentRepository.countToday();
    }

    public int countConfirmed() {
        return appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED);
    }

    public int countCompleted() {
        return appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);
    }

    public int countCancelled() {
        return appointmentRepository.countByStatus(AppointmentStatus.CANCELLED);
    }

    public int countNoShow() {
        return appointmentRepository.countByStatus(AppointmentStatus.NO_SHOW);
    }

    public int countBooked() {
        return appointmentRepository.countByStatus(AppointmentStatus.BOOKED);
    }

    public int countHighRisk() {
        return (int) appointmentRepository.findAll().stream()
                .filter(a -> a.getPredictedRiskScore() >= 0.70)
                .count();
    }

    private void validateNewAppointment(Appointment a) {
        if (a == null) {
            throw new IllegalArgumentException("Appointment payload cannot be null.");
        }
        if (a.getId() == null || a.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment ID is mandatory.");
        }
        if (a.getPatientId() == null || a.getPatientId().trim().isEmpty()) {
            throw new IllegalArgumentException("Associated Patient ID is mandatory.");
        }
        if (a.getDoctorId() == null || a.getDoctorId().trim().isEmpty()) {
            throw new IllegalArgumentException("Attending Physician is mandatory.");
        }
        if (a.getAppointmentDate() == null) {
            throw new IllegalArgumentException("Appointment date is mandatory.");
        }
        if (a.getAppointmentTime() == null) {
            throw new IllegalArgumentException("Appointment time is mandatory.");
        }
    }
}
