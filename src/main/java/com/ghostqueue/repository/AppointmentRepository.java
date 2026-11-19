package com.ghostqueue.repository;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {
    List<Appointment> findAll();
    Optional<Appointment> findById(String id);
    List<Appointment> findByDate(LocalDate date);
    List<Appointment> findByDepartment(Department department);
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByPatientId(String patientId);
    List<Appointment> findByDoctorId(String doctorId);
    List<Appointment> filter(String query, Department department, String doctorId, AppointmentStatus status, LocalDate date);
    void save(Appointment appointment);
    void saveTransactional(Appointment appointment, String remarks);
    void update(Appointment appointment);
    void updateStatus(String appointmentId, AppointmentStatus newStatus, String cancellationReason, String remarks);
    void delete(String id);
    boolean hasConflictForDoctor(String doctorId, LocalDate date, LocalTime time, String excludeAppointmentId);
    boolean hasConflictForPatient(String patientId, LocalDate date, LocalTime time, String excludeAppointmentId);
    int countTotal();
    int countToday();
    int countByStatus(AppointmentStatus status);
}
