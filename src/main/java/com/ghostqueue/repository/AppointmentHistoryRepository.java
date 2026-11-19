package com.ghostqueue.repository;

import com.ghostqueue.model.AppointmentHistory;

import java.sql.Connection;
import java.util.List;

public interface AppointmentHistoryRepository {
    List<AppointmentHistory> findByAppointmentId(String appointmentId);
    void save(AppointmentHistory history);
    void save(Connection conn, AppointmentHistory history);
}
