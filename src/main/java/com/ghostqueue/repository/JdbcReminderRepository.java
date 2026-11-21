package com.ghostqueue.repository;

import com.ghostqueue.model.AppointmentReminder;
import com.ghostqueue.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of ReminderRepository for MySQL appointment_reminders table.
 */
public class JdbcReminderRepository implements ReminderRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcReminderRepository.class.getName());

    private static final String BASE_SELECT = """
        SELECT r.reminder_id, r.appointment_id, r.patient_id, r.reminder_type,
               r.reminder_status, r.scheduled_for, r.completed_at, r.message_content,
               r.response_status, r.operator_notes, r.created_at,
               p.full_name AS patient_name, p.phone AS patient_phone
        FROM appointment_reminders r
        LEFT JOIN patients p ON r.patient_id = p.patient_id
        ORDER BY r.scheduled_for DESC
    """;

    @Override
    public List<AppointmentReminder> findAll() {
        List<AppointmentReminder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapReminder(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching appointment reminders: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<AppointmentReminder> findById(String id) {
        String sql = """
            SELECT r.reminder_id, r.appointment_id, r.patient_id, r.reminder_type,
                   r.reminder_status, r.scheduled_for, r.completed_at, r.message_content,
                   r.response_status, r.operator_notes, r.created_at,
                   p.full_name AS patient_name, p.phone AS patient_phone
            FROM appointment_reminders r
            LEFT JOIN patients p ON r.patient_id = p.patient_id
            WHERE r.reminder_id = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReminder(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding reminder by ID (" + id + "): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<AppointmentReminder> findByAppointmentId(String appointmentId) {
        String sql = """
            SELECT r.reminder_id, r.appointment_id, r.patient_id, r.reminder_type,
                   r.reminder_status, r.scheduled_for, r.completed_at, r.message_content,
                   r.response_status, r.operator_notes, r.created_at,
                   p.full_name AS patient_name, p.phone AS patient_phone
            FROM appointment_reminders r
            LEFT JOIN patients p ON r.patient_id = p.patient_id
            WHERE r.appointment_id = ?
            ORDER BY r.created_at DESC
        """;
        List<AppointmentReminder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReminder(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding reminders for appt (" + appointmentId + "): " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<AppointmentReminder> filter(String query, AppointmentReminder.ReminderType type, AppointmentReminder.ReminderStatus status) {
        StringBuilder sql = new StringBuilder("""
            SELECT r.reminder_id, r.appointment_id, r.patient_id, r.reminder_type,
                   r.reminder_status, r.scheduled_for, r.completed_at, r.message_content,
                   r.response_status, r.operator_notes, r.created_at,
                   p.full_name AS patient_name, p.phone AS patient_phone
            FROM appointment_reminders r
            LEFT JOIN patients p ON r.patient_id = p.patient_id
            WHERE 1=1
        """);

        List<String> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(r.reminder_id) LIKE ? OR LOWER(r.appointment_id) LIKE ? OR LOWER(p.full_name) LIKE ? OR LOWER(r.message_content) LIKE ?)");
            String p = "%" + query.toLowerCase().trim() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }

        if (type != null && type != AppointmentReminder.ReminderType.ALL) {
            sql.append(" AND r.reminder_type = ?");
            params.add(type.name());
        }

        if (status != null && status != AppointmentReminder.ReminderStatus.ALL) {
            sql.append(" AND r.reminder_status = ?");
            params.add(status.name());
        }

        sql.append(" ORDER BY r.scheduled_for DESC");

        List<AppointmentReminder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReminder(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error filtering reminders: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void save(AppointmentReminder reminder) {
        String sql = """
            INSERT INTO appointment_reminders
            (reminder_id, appointment_id, patient_id, reminder_type, reminder_status,
             scheduled_for, completed_at, message_content, response_status, operator_notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE reminder_status = VALUES(reminder_status),
                                    completed_at = VALUES(completed_at),
                                    response_status = VALUES(response_status),
                                    operator_notes = VALUES(operator_notes)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reminder.getReminderId());
            ps.setString(2, reminder.getAppointmentId());
            ps.setString(3, reminder.getPatientId());
            ps.setString(4, reminder.getReminderType().name());
            ps.setString(5, reminder.getReminderStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(reminder.getScheduledFor() != null ? reminder.getScheduledFor() : LocalDateTime.now()));
            ps.setTimestamp(7, reminder.getCompletedAt() != null ? Timestamp.valueOf(reminder.getCompletedAt()) : null);
            ps.setString(8, reminder.getMessageContent());
            ps.setString(9, reminder.getResponseStatus());
            ps.setString(10, reminder.getOperatorNotes());
            ps.setTimestamp(11, Timestamp.valueOf(reminder.getCreatedAt() != null ? reminder.getCreatedAt() : LocalDateTime.now()));

            ps.executeUpdate();
            LOGGER.info("Saved reminder: " + reminder.getReminderId() + " (" + reminder.getReminderStatus() + ")");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving reminder: " + e.getMessage(), e);
            throw new RuntimeException("Database error saving reminder: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(String reminderId, AppointmentReminder.ReminderStatus status, String responseStatus, String notes) {
        String sql = """
            UPDATE appointment_reminders
            SET reminder_status = ?, response_status = ?, operator_notes = ?, completed_at = ?
            WHERE reminder_id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, responseStatus != null ? responseStatus : "NO_RESPONSE");
            ps.setString(3, notes != null ? notes : "");
            ps.setTimestamp(4, status == AppointmentReminder.ReminderStatus.COMPLETED ? Timestamp.valueOf(LocalDateTime.now()) : null);
            ps.setString(5, reminderId);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating reminder status: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating reminder: " + e.getMessage(), e);
        }
    }

    @Override
    public int countPending() {
        return executeCount("SELECT COUNT(*) FROM appointment_reminders WHERE reminder_status = 'PENDING'");
    }

    @Override
    public int countCompleted() {
        return executeCount("SELECT COUNT(*) FROM appointment_reminders WHERE reminder_status = 'COMPLETED'");
    }

    @Override
    public int countTotal() {
        return executeCount("SELECT COUNT(*) FROM appointment_reminders");
    }

    private int executeCount(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting reminders: " + e.getMessage(), e);
        }
        return 0;
    }

    private AppointmentReminder mapReminder(ResultSet rs) throws SQLException {
        String id = rs.getString("reminder_id");
        String apptId = rs.getString("appointment_id");
        String patId = rs.getString("patient_id");
        String patName = rs.getString("patient_name");
        String patPhone = rs.getString("patient_phone");
        AppointmentReminder.ReminderType type = AppointmentReminder.ReminderType.fromString(rs.getString("reminder_type"));
        AppointmentReminder.ReminderStatus status = AppointmentReminder.ReminderStatus.fromString(rs.getString("reminder_status"));
        Timestamp tsSched = rs.getTimestamp("scheduled_for");
        LocalDateTime sched = tsSched != null ? tsSched.toLocalDateTime() : LocalDateTime.now();
        Timestamp tsComp = rs.getTimestamp("completed_at");
        LocalDateTime comp = tsComp != null ? tsComp.toLocalDateTime() : null;
        String msg = rs.getString("message_content");
        String resp = rs.getString("response_status");
        String notes = rs.getString("operator_notes");
        Timestamp tsCreate = rs.getTimestamp("created_at");
        LocalDateTime create = tsCreate != null ? tsCreate.toLocalDateTime() : LocalDateTime.now();

        return new AppointmentReminder(
                id, apptId, patId, patName, patPhone, type, status,
                sched, comp, msg, resp, notes, create
        );
    }
}
