package com.ghostqueue.repository;

import com.ghostqueue.model.AppointmentReminder;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository {
    List<AppointmentReminder> findAll();
    Optional<AppointmentReminder> findById(String id);
    List<AppointmentReminder> findByAppointmentId(String appointmentId);
    List<AppointmentReminder> filter(String query, AppointmentReminder.ReminderType type, AppointmentReminder.ReminderStatus status);
    void save(AppointmentReminder reminder);
    void updateStatus(String reminderId, AppointmentReminder.ReminderStatus status, String responseStatus, String notes);
    int countPending();
    int countCompleted();
    int countTotal();
}
