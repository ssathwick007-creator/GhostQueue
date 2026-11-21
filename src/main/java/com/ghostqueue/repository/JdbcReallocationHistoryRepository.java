package com.ghostqueue.repository;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.ReallocationHistory;
import com.ghostqueue.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of ReallocationHistoryRepository for MySQL.
 */
public class JdbcReallocationHistoryRepository implements ReallocationHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcReallocationHistoryRepository.class.getName());

    private static final String BASE_SELECT = """
        SELECT r.reallocation_id, r.appointment_id, r.queue_id, r.candidate_patient_id,
               r.doctor_id, r.slot_date, r.slot_time, r.match_score, r.match_reason,
               r.decision, r.decision_notes, r.created_at, r.decided_at,
               p.full_name AS candidate_name, d.full_name AS doctor_name, d.department AS doctor_department
        FROM reallocation_history r
        LEFT JOIN patients p ON r.candidate_patient_id = p.patient_id
        LEFT JOIN doctors d ON r.doctor_id = d.doctor_id
        ORDER BY r.created_at DESC
    """;

    @Override
    public List<ReallocationHistory> findAll() {
        List<ReallocationHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRecord(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching reallocation history: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<ReallocationHistory> findByAppointmentId(String appointmentId) {
        String sql = """
            SELECT r.reallocation_id, r.appointment_id, r.queue_id, r.candidate_patient_id,
                   r.doctor_id, r.slot_date, r.slot_time, r.match_score, r.match_reason,
                   r.decision, r.decision_notes, r.created_at, r.decided_at,
                   p.full_name AS candidate_name, d.full_name AS doctor_name, d.department AS doctor_department
            FROM reallocation_history r
            LEFT JOIN patients p ON r.candidate_patient_id = p.patient_id
            LEFT JOIN doctors d ON r.doctor_id = d.doctor_id
            WHERE r.appointment_id = ?
            ORDER BY r.created_at DESC
        """;
        List<ReallocationHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding reallocation records by appointment: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void save(ReallocationHistory record) {
        String sql = """
            INSERT INTO reallocation_history
            (reallocation_id, appointment_id, queue_id, candidate_patient_id, doctor_id,
             slot_date, slot_time, match_score, match_reason, decision, decision_notes, created_at, decided_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE decision = VALUES(decision), decision_notes = VALUES(decision_notes), decided_at = VALUES(decided_at)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, record.getReallocationId());
            ps.setString(2, record.getAppointmentId());
            ps.setString(3, record.getQueueId());
            ps.setString(4, record.getCandidatePatientId());
            ps.setString(5, record.getDoctorId());
            ps.setDate(6, Date.valueOf(record.getSlotDate()));
            ps.setTime(7, Time.valueOf(record.getSlotTime()));
            ps.setInt(8, record.getMatchScore());
            ps.setString(9, record.getMatchReason());
            ps.setString(10, record.getDecision());
            ps.setString(11, record.getDecisionNotes());
            ps.setTimestamp(12, Timestamp.valueOf(record.getCreatedAt() != null ? record.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(13, record.getDecidedAt() != null ? Timestamp.valueOf(record.getDecidedAt()) : null);

            ps.executeUpdate();
            LOGGER.info("Saved reallocation decision: " + record.getReallocationId() + " -> " + record.getDecision());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving reallocation history record: " + e.getMessage(), e);
            throw new RuntimeException("Database error saving reallocation history: " + e.getMessage(), e);
        }
    }

    @Override
    public int countApproved() {
        return executeCount("SELECT COUNT(*) FROM reallocation_history WHERE decision = 'APPROVED'");
    }

    @Override
    public int countTotal() {
        return executeCount("SELECT COUNT(*) FROM reallocation_history");
    }

    private int executeCount(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting reallocation records: " + e.getMessage(), e);
        }
        return 0;
    }

    private ReallocationHistory mapRecord(ResultSet rs) throws SQLException {
        String id = rs.getString("reallocation_id");
        String apptId = rs.getString("appointment_id");
        String qId = rs.getString("queue_id");
        String candPatId = rs.getString("candidate_patient_id");
        String candName = rs.getString("candidate_name");
        String docId = rs.getString("doctor_id");
        String docName = rs.getString("doctor_name");
        String deptStr = rs.getString("doctor_department");
        Department dept = deptStr != null ? Department.fromString(deptStr) : Department.GENERAL_MEDICINE;
        Date sDate = rs.getDate("slot_date");
        LocalDate slotDate = sDate != null ? sDate.toLocalDate() : LocalDate.now();
        Time sTime = rs.getTime("slot_time");
        LocalTime slotTime = sTime != null ? sTime.toLocalTime() : LocalTime.of(10, 0);
        int score = rs.getInt("match_score");
        String reason = rs.getString("match_reason");
        String decision = rs.getString("decision");
        String notes = rs.getString("decision_notes");
        Timestamp tsCreate = rs.getTimestamp("created_at");
        LocalDateTime created = tsCreate != null ? tsCreate.toLocalDateTime() : LocalDateTime.now();
        Timestamp tsDecided = rs.getTimestamp("decided_at");
        LocalDateTime decided = tsDecided != null ? tsDecided.toLocalDateTime() : null;

        return new ReallocationHistory(
                id, apptId, qId, candPatId, candName, docId, docName, dept,
                slotDate, slotTime, score, reason, decision, notes, created, decided
        );
    }
}
