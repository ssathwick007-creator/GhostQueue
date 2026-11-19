package com.ghostqueue.repository;

import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcAppointmentHistoryRepository implements AppointmentHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcAppointmentHistoryRepository.class.getName());

    @Override
    public List<AppointmentHistory> findByAppointmentId(String appointmentId) {
        String sql = """
            SELECT history_id, appointment_id, previous_status, new_status, changed_at, remarks
            FROM appointment_history
            WHERE appointment_id = ?
            ORDER BY changed_at DESC
        """;

        List<AppointmentHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("changed_at");
                    list.add(new AppointmentHistory(
                            rs.getInt("history_id"),
                            rs.getString("appointment_id"),
                            rs.getString("previous_status"),
                            rs.getString("new_status"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                            rs.getString("remarks")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching appointment history for " + appointmentId + ": " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void save(AppointmentHistory history) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            save(conn, history);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving history log: " + e.getMessage(), e);
        }
    }

    @Override
    public void save(Connection conn, AppointmentHistory history) {
        String sql = """
            INSERT INTO appointment_history (appointment_id, previous_status, new_status, changed_at, remarks)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, history.getAppointmentId());
            ps.setString(2, history.getPreviousStatus());
            ps.setString(3, history.getNewStatus());
            ps.setTimestamp(4, Timestamp.valueOf(history.getChangedAt() != null ? history.getChangedAt() : LocalDateTime.now()));
            ps.setString(5, history.getRemarks());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing save history: " + e.getMessage(), e);
            throw new RuntimeException("Error persisting audit history: " + e.getMessage(), e);
        }
    }
}
