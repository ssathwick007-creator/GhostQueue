package com.ghostqueue.repository;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcAppointmentRepository implements AppointmentRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcAppointmentRepository.class.getName());
    private final AppointmentHistoryRepository historyRepository = new JdbcAppointmentHistoryRepository();

    private static final String BASE_SELECT = """
        SELECT a.appointment_id, a.patient_id, a.doctor_id, a.appointment_date, a.appointment_time,
               a.appointment_status, a.booking_date, a.cancellation_reason, a.created_at, a.updated_at,
               p.full_name AS patient_name, p.age AS patient_age, p.phone AS patient_phone,
               d.full_name AS doctor_name, d.department AS doctor_department
        FROM appointments a
        JOIN patients p ON a.patient_id = p.patient_id
        JOIN doctors d ON a.doctor_id = d.doctor_id
    """;

    @Override
    public List<Appointment> findAll() {
        String sql = BASE_SELECT + " ORDER BY a.appointment_date ASC, a.appointment_time ASC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapAppointment(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching appointments: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Appointment> findById(String id) {
        String sql = BASE_SELECT + " WHERE a.appointment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding appointment by ID (" + id + "): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        String sql = BASE_SELECT + " WHERE a.appointment_date = ? ORDER BY a.appointment_time ASC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching appointments by date: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Appointment> findByDepartment(Department department) {
        if (department == null || department == Department.ALL) {
            return findAll();
        }
        String sql = BASE_SELECT + " WHERE LOWER(d.department) = LOWER(?) ORDER BY a.appointment_date ASC, a.appointment_time ASC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, department.getDisplayName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching appointments by department: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Appointment> findByStatus(AppointmentStatus status) {
        if (status == null || status == AppointmentStatus.ALL) {
            return findAll();
        }
        String sql = BASE_SELECT + " WHERE a.appointment_status = ? ORDER BY a.appointment_date ASC, a.appointment_time ASC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching appointments by status: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) {
        String sql = BASE_SELECT + " WHERE a.patient_id = ? ORDER BY a.appointment_date DESC, a.appointment_time DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching patient appointments: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId) {
        String sql = BASE_SELECT + " WHERE a.doctor_id = ? ORDER BY a.appointment_date ASC, a.appointment_time ASC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching doctor appointments: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Appointment> filter(String query, Department department, String doctorId, AppointmentStatus status, LocalDate date) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String p = "%" + query.toLowerCase().trim() + "%";
            sql.append(" AND (LOWER(a.appointment_id) LIKE ? OR LOWER(p.full_name) LIKE ? OR LOWER(p.patient_id) LIKE ? OR LOWER(d.full_name) LIKE ?)");
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }

        if (department != null && department != Department.ALL) {
            sql.append(" AND LOWER(d.department) = LOWER(?)");
            params.add(department.getDisplayName());
        }

        if (doctorId != null && !doctorId.isBlank() && !doctorId.equalsIgnoreCase("ALL")) {
            sql.append(" AND a.doctor_id = ?");
            params.add(doctorId);
        }

        if (status != null && status != AppointmentStatus.ALL) {
            sql.append(" AND a.appointment_status = ?");
            params.add(status.name());
        }

        if (date != null) {
            sql.append(" AND a.appointment_date = ?");
            params.add(Date.valueOf(date));
        }

        sql.append(" ORDER BY a.appointment_date ASC, a.appointment_time ASC");

        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Date d) {
                    ps.setDate(i + 1, d);
                } else {
                    ps.setString(i + 1, param.toString());
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error filtering appointments: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void save(Appointment appointment) {
        saveTransactional(appointment, "Initial appointment scheduled");
    }

    @Override
    public void saveTransactional(Appointment appointment, String remarks) {
        String insertAptSql = """
            INSERT INTO appointments (appointment_id, patient_id, doctor_id, appointment_date, appointment_time, appointment_status, booking_date, cancellation_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String insertHistSql = """
            INSERT INTO appointment_history (appointment_id, previous_status, new_status, changed_at, remarks)
            VALUES (?, ?, ?, NOW(), ?)
        """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            try (PreparedStatement psApt = conn.prepareStatement(insertAptSql);
                 PreparedStatement psHist = conn.prepareStatement(insertHistSql)) {

                psApt.setString(1, appointment.getId());
                psApt.setString(2, appointment.getPatientId());
                psApt.setString(3, appointment.getDoctorId());
                psApt.setDate(4, Date.valueOf(appointment.getAppointmentDate()));
                psApt.setTime(5, Time.valueOf(appointment.getAppointmentTime()));
                psApt.setString(6, appointment.getStatus().name());
                psApt.setDate(7, Date.valueOf(appointment.getBookingDate() != null ? appointment.getBookingDate() : LocalDate.now()));
                psApt.setString(8, appointment.getCancellationReason());
                psApt.executeUpdate();

                // Audit history
                psHist.setString(1, appointment.getId());
                psHist.setString(2, null);
                psHist.setString(3, appointment.getStatus().name());
                psHist.setString(4, remarks != null ? remarks : "Appointment created in system");
                psHist.executeUpdate();

                conn.commit(); // Commit transaction
                LOGGER.info("Transaction committed: saved appointment " + appointment.getId());
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Transaction rolled back on save appointment: " + e.getMessage(), e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database transaction failed saving appointment: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public void update(Appointment appointment) {
        String sql = """
            UPDATE appointments
            SET patient_id = ?, doctor_id = ?, appointment_date = ?, appointment_time = ?,
                appointment_status = ?, cancellation_reason = ?
            WHERE appointment_id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, appointment.getPatientId());
            ps.setString(2, appointment.getDoctorId());
            ps.setDate(3, Date.valueOf(appointment.getAppointmentDate()));
            ps.setTime(4, Time.valueOf(appointment.getAppointmentTime()));
            ps.setString(5, appointment.getStatus().name());
            ps.setString(6, appointment.getCancellationReason());
            ps.setString(7, appointment.getId());

            ps.executeUpdate();
            LOGGER.info("Updated appointment: " + appointment.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating appointment: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(String appointmentId, AppointmentStatus newStatus, String cancellationReason, String remarks) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Get previous status
            String prevStatus = "BOOKED";
            String queryPrev = "SELECT appointment_status FROM appointments WHERE appointment_id = ?";
            try (PreparedStatement psPrev = conn.prepareStatement(queryPrev)) {
                psPrev.setString(1, appointmentId);
                try (ResultSet rs = psPrev.executeQuery()) {
                    if (rs.next()) {
                        prevStatus = rs.getString("appointment_status");
                    }
                }
            }

            // 2. Update status & cancellation reason
            String updateSql = """
                UPDATE appointments
                SET appointment_status = ?, cancellation_reason = ?
                WHERE appointment_id = ?
            """;
            try (PreparedStatement psUpd = conn.prepareStatement(updateSql)) {
                psUpd.setString(1, newStatus.name());
                psUpd.setString(2, cancellationReason);
                psUpd.setString(3, appointmentId);
                psUpd.executeUpdate();
            }

            // 3. Record history log
            String histSql = """
                INSERT INTO appointment_history (appointment_id, previous_status, new_status, changed_at, remarks)
                VALUES (?, ?, ?, NOW(), ?)
            """;
            try (PreparedStatement psHist = conn.prepareStatement(histSql)) {
                psHist.setString(1, appointmentId);
                psHist.setString(2, prevStatus);
                psHist.setString(3, newStatus.name());
                psHist.setString(4, remarks != null ? remarks : "Status updated from " + prevStatus + " to " + newStatus.name());
                psHist.executeUpdate();
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed: updated status for " + appointmentId + " to " + newStatus);
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            LOGGER.log(Level.SEVERE, "Transaction rolled back updating status: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating appointment status: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM appointments WHERE appointment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.executeUpdate();
            LOGGER.info("Deleted appointment: " + id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting appointment: " + e.getMessage(), e);
            throw new RuntimeException("Database error deleting appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasConflictForDoctor(String doctorId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        String sql = """
            SELECT 1 FROM appointments
            WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ?
              AND appointment_status NOT IN ('CANCELLED')
              AND appointment_id != ?
            LIMIT 1
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctorId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            ps.setString(4, excludeAppointmentId != null ? excludeAppointmentId : "");

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking doctor conflict: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean hasConflictForPatient(String patientId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        String sql = """
            SELECT 1 FROM appointments
            WHERE patient_id = ? AND appointment_date = ? AND appointment_time = ?
              AND appointment_status NOT IN ('CANCELLED')
              AND appointment_id != ?
            LIMIT 1
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, patientId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            ps.setString(4, excludeAppointmentId != null ? excludeAppointmentId : "");

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking patient conflict: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int countTotal() {
        return executeCountQuery("SELECT COUNT(*) FROM appointments");
    }

    @Override
    public int countToday() {
        return executeCountQuery("SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE");
    }

    @Override
    public int countByStatus(AppointmentStatus status) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting appointments by status: " + e.getMessage(), e);
        }
        return 0;
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing count query: " + e.getMessage(), e);
        }
        return 0;
    }

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        String id = rs.getString("appointment_id");
        String patientId = rs.getString("patient_id");
        String doctorId = rs.getString("doctor_id");
        LocalDate date = rs.getDate("appointment_date").toLocalDate();
        LocalTime time = rs.getTime("appointment_time").toLocalTime();
        AppointmentStatus status = AppointmentStatus.fromString(rs.getString("appointment_status"));
        Date bDate = rs.getDate("booking_date");
        LocalDate bookingDate = bDate != null ? bDate.toLocalDate() : date;
        String reason = rs.getString("cancellation_reason");

        Appointment apt = new Appointment(id, patientId, doctorId, date, time, status, bookingDate, reason);

        // Joined fields
        apt.setPatientName(rs.getString("patient_name"));
        apt.setPatientAge(rs.getInt("patient_age"));
        apt.setPatientPhone(rs.getString("patient_phone"));
        apt.setDoctorName(rs.getString("doctor_name"));
        apt.setDepartment(Department.fromString(rs.getString("doctor_department")));

        // Calculate simulated predictive score dynamically based on lead time & history
        int leadDays = Math.max(0, (int) java.time.temporal.ChronoUnit.DAYS.between(bookingDate, date));
        apt.setLeadDays(leadDays);

        double score = 0.12;
        List<String> factors = new ArrayList<>();
        if (leadDays > 25) {
            score += 0.35;
            factors.add("Extended lead time (" + leadDays + " days)");
        } else if (leadDays > 14) {
            score += 0.18;
            factors.add("Moderate booking interval (" + leadDays + " days)");
        } else {
            factors.add("Short lead time (" + leadDays + " days)");
        }

        if (time.getHour() >= 15) {
            score += 0.15;
            factors.add("Late afternoon clinic slot");
        }

        apt.setPredictedRiskScore(Math.min(0.92, score));
        apt.setRiskFactors(factors);

        return apt;
    }
}
