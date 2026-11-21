package com.ghostqueue.repository;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcQueueRepository implements QueueRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcQueueRepository.class.getName());

    private static final String BASE_SELECT = """
        SELECT w.queue_id, w.patient_id, w.doctor_id, w.requested_date, w.preferred_time,
               w.priority_level, w.queue_status, w.notes, w.created_at,
               p.full_name AS patient_name, p.phone AS patient_phone,
               d.full_name AS doctor_name, d.department AS doctor_department
        FROM waiting_queue w
        JOIN patients p ON w.patient_id = p.patient_id
        LEFT JOIN doctors d ON w.doctor_id = d.doctor_id
    """;

    private static final String ORDER_BY = """
        ORDER BY CASE w.priority_level 
                    WHEN 'URGENT' THEN 1 
                    WHEN 'HIGH' THEN 2 
                    WHEN 'STANDARD' THEN 3 
                    ELSE 4 END ASC, 
                 w.created_at ASC
    """;

    @Override
    public List<QueueEntry> findAll() {
        String sql = BASE_SELECT + " " + ORDER_BY;
        List<QueueEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapQueueEntry(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching waiting queue: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<QueueEntry> findById(String id) {
        String sql = BASE_SELECT + " WHERE w.queue_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapQueueEntry(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding queue entry (" + id + "): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<QueueEntry> findByDepartment(Department department) {
        if (department == null || department == Department.ALL) {
            return findAll();
        }
        String sql = BASE_SELECT + " WHERE LOWER(d.department) = LOWER(?) " + ORDER_BY;
        List<QueueEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, department.getDisplayName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQueueEntry(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding queue entries by department: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<QueueEntry> findByPriority(QueuePriority priority) {
        if (priority == null || priority == QueuePriority.ALL) {
            return findAll();
        }
        String sql = BASE_SELECT + " WHERE w.priority_level = ? " + ORDER_BY;
        List<QueueEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, priority.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQueueEntry(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding queue entries by priority: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<QueueEntry> findByStatus(QueueStatus status) {
        if (status == null || status == QueueStatus.ALL) {
            return findAll();
        }
        String sql = BASE_SELECT + " WHERE w.queue_status = ? " + ORDER_BY;
        List<QueueEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQueueEntry(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding queue entries by status: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<QueueEntry> filter(String query, Department department, QueuePriority priority, QueueStatus status) {
        return filter(query, department, null, priority, status);
    }

    @Override
    public List<QueueEntry> filter(String query, Department department, String doctorId, QueuePriority priority, QueueStatus status) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<String> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(w.queue_id) LIKE ? OR LOWER(p.full_name) LIKE ? OR LOWER(p.patient_id) LIKE ? OR LOWER(d.full_name) LIKE ?)");
            String p = "%" + query.toLowerCase().trim() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }

        if (department != null && department != Department.ALL) {
            sql.append(" AND LOWER(d.department) = LOWER(?)");
            params.add(department.getDisplayName());
        }

        if (doctorId != null && !doctorId.isBlank() && !doctorId.equalsIgnoreCase("All Doctors")) {
            sql.append(" AND w.doctor_id = ?");
            params.add(doctorId);
        }

        if (priority != null && priority != QueuePriority.ALL) {
            sql.append(" AND w.priority_level = ?");
            params.add(priority.name());
        }

        if (status != null && status != QueueStatus.ALL) {
            sql.append(" AND w.queue_status = ?");
            params.add(status.name());
        }

        sql.append(" ").append(ORDER_BY);

        List<QueueEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQueueEntry(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error filtering queue: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void save(QueueEntry entry) {
        String sql = """
            INSERT INTO waiting_queue (queue_id, patient_id, doctor_id, requested_date, preferred_time, priority_level, queue_status, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entry.getQueueId());
            ps.setString(2, entry.getPatientId());
            ps.setString(3, (entry.getDoctorId() != null && !entry.getDoctorId().isBlank()) ? entry.getDoctorId() : null);
            ps.setDate(4, Date.valueOf(entry.getRequestedDate() != null ? entry.getRequestedDate() : LocalDate.now()));
            ps.setString(5, entry.getPreferredTimeSlot());
            ps.setString(6, entry.getPriority().name());
            ps.setString(7, entry.getStatus().name());
            ps.setString(8, entry.getNotes() != null ? entry.getNotes() : "Standard waiting queue entry");
            ps.setTimestamp(9, Timestamp.valueOf(entry.getRegisteredTime() != null ? entry.getRegisteredTime() : LocalDateTime.now()));

            ps.executeUpdate();
            LOGGER.info("Saved queue entry: " + entry.getQueueId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving queue entry: " + e.getMessage(), e);
            throw new RuntimeException("Database error adding to queue: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(QueueEntry entry) {
        String sql = """
            UPDATE waiting_queue
            SET doctor_id = ?, requested_date = ?, preferred_time = ?, priority_level = ?, queue_status = ?, notes = ?
            WHERE queue_id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entry.getDoctorId());
            ps.setDate(2, Date.valueOf(entry.getRequestedDate()));
            ps.setString(3, entry.getPreferredTimeSlot());
            ps.setString(4, entry.getPriority().name());
            ps.setString(5, entry.getStatus().name());
            ps.setString(6, entry.getNotes() != null ? entry.getNotes() : "Standard waiting queue entry");
            ps.setString(7, entry.getQueueId());

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating queue entry: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating queue: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(String queueId, QueueStatus status) {
        String sql = "UPDATE waiting_queue SET queue_status = ? WHERE queue_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, queueId);
            ps.executeUpdate();
            LOGGER.info("Updated queue status for " + queueId + " to " + status.name());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating queue status: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating queue status: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePriority(String queueId, QueuePriority priority) {
        String sql = "UPDATE waiting_queue SET priority_level = ? WHERE queue_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, priority.name());
            ps.setString(2, queueId);
            ps.executeUpdate();
            LOGGER.info("Updated queue priority for " + queueId + " to " + priority.name());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating queue priority: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating queue priority: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM waiting_queue WHERE queue_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.executeUpdate();
            LOGGER.info("Deleted queue entry: " + id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting queue entry: " + e.getMessage(), e);
            throw new RuntimeException("Database error deleting queue entry: " + e.getMessage(), e);
        }
    }

    @Override
    public int countTotal() {
        return executeCount("SELECT COUNT(*) FROM waiting_queue");
    }

    @Override
    public int countActiveWaiting() {
        return executeCount("SELECT COUNT(*) FROM waiting_queue WHERE queue_status IN ('WAITING', 'STANDBY', 'OFFERED')");
    }

    @Override
    public int countUrgentPriority() {
        return executeCount("SELECT COUNT(*) FROM waiting_queue WHERE priority_level = 'URGENT' AND queue_status IN ('WAITING', 'STANDBY', 'OFFERED')");
    }

    private int executeCount(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting queue rows: " + e.getMessage(), e);
        }
        return 0;
    }

    private QueueEntry mapQueueEntry(ResultSet rs) throws SQLException {
        String queueId = rs.getString("queue_id");
        String patientId = rs.getString("patient_id");
        String doctorId = rs.getString("doctor_id");
        Date reqDate = rs.getDate("requested_date");
        LocalDate requestedDate = reqDate != null ? reqDate.toLocalDate() : LocalDate.now();
        String preferredTime = rs.getString("preferred_time");
        QueuePriority priority = QueuePriority.fromString(rs.getString("priority_level"));
        QueueStatus status = QueueStatus.fromString(rs.getString("queue_status"));
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime registered = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        QueueEntry entry = new QueueEntry(queueId, patientId, doctorId, requestedDate, preferredTime, priority, status, registered);

        entry.setPatientName(rs.getString("patient_name"));
        entry.setPatientPhone(rs.getString("patient_phone"));
        String docName = rs.getString("doctor_name");
        entry.setPreferredDoctor(docName != null ? docName : "Any Available Specialist");
        String dept = rs.getString("doctor_department");
        entry.setRequestedDepartment(dept != null ? Department.fromString(dept) : Department.GENERAL_MEDICINE);
        String notes = rs.getString("notes");
        entry.setClinicalNote(notes != null && !notes.isBlank() ? notes : "Triage registered at " + registered.toLocalDate());

        return entry;
    }
}
