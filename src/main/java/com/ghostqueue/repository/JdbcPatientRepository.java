package com.ghostqueue.repository;

import com.ghostqueue.model.Patient;
import com.ghostqueue.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcPatientRepository implements PatientRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcPatientRepository.class.getName());

    @Override
    public List<Patient> findAll() {
        String sql = """
            SELECT p.patient_id, p.full_name, p.age, p.gender, p.phone, p.email, p.address, p.created_at,
                   COUNT(a.appointment_id) AS total_appts,
                   SUM(CASE WHEN a.appointment_status = 'COMPLETED' THEN 1 ELSE 0 END) AS attended_appts,
                   SUM(CASE WHEN a.appointment_status = 'NO_SHOW' THEN 1 ELSE 0 END) AS noshow_appts
            FROM patients p
            LEFT JOIN appointments a ON p.patient_id = a.patient_id
            GROUP BY p.patient_id, p.full_name, p.age, p.gender, p.phone, p.email, p.address, p.created_at
            ORDER BY p.full_name ASC
        """;

        List<Patient> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapPatientWithStats(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all patients from MySQL: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Patient> findById(String id) {
        String sql = """
            SELECT p.patient_id, p.full_name, p.age, p.gender, p.phone, p.email, p.address, p.created_at,
                   COUNT(a.appointment_id) AS total_appts,
                   SUM(CASE WHEN a.appointment_status = 'COMPLETED' THEN 1 ELSE 0 END) AS attended_appts,
                   SUM(CASE WHEN a.appointment_status = 'NO_SHOW' THEN 1 ELSE 0 END) AS noshow_appts
            FROM patients p
            LEFT JOIN appointments a ON p.patient_id = a.patient_id
            WHERE p.patient_id = ?
            GROUP BY p.patient_id, p.full_name, p.age, p.gender, p.phone, p.email, p.address, p.created_at
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPatientWithStats(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding patient by ID (" + id + "): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Patient> searchByNameOrId(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String sql = """
            SELECT p.patient_id, p.full_name, p.age, p.gender, p.phone, p.email, p.address, p.created_at,
                   COUNT(a.appointment_id) AS total_appts,
                   SUM(CASE WHEN a.appointment_status = 'COMPLETED' THEN 1 ELSE 0 END) AS attended_appts,
                   SUM(CASE WHEN a.appointment_status = 'NO_SHOW' THEN 1 ELSE 0 END) AS noshow_appts
            FROM patients p
            LEFT JOIN appointments a ON p.patient_id = a.patient_id
            WHERE LOWER(p.patient_id) LIKE ? OR LOWER(p.full_name) LIKE ? OR LOWER(p.phone) LIKE ?
            GROUP BY p.patient_id, p.full_name, p.age, p.gender, p.phone, p.email, p.address, p.created_at
            ORDER BY p.full_name ASC
        """;

        List<Patient> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase().trim() + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPatientWithStats(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching patients with query (" + query + "): " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void save(Patient patient) {
        String sql = """
            INSERT INTO patients (patient_id, full_name, age, gender, phone, email, address, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, patient.getId());
            ps.setString(2, patient.getFullName());
            ps.setInt(3, patient.getAge());
            ps.setString(4, patient.getGender());
            ps.setString(5, patient.getPhone());
            ps.setString(6, patient.getEmail());
            ps.setString(7, patient.getAddress());
            ps.setTimestamp(8, Timestamp.valueOf(patient.getCreatedAt() != null ? patient.getCreatedAt() : LocalDateTime.now()));

            ps.executeUpdate();
            LOGGER.info("Successfully registered new patient: " + patient.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving patient to MySQL: " + e.getMessage(), e);
            throw new RuntimeException("Database error saving patient: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Patient patient) {
        String sql = """
            UPDATE patients
            SET full_name = ?, age = ?, gender = ?, phone = ?, email = ?, address = ?
            WHERE patient_id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, patient.getFullName());
            ps.setInt(2, patient.getAge());
            ps.setString(3, patient.getGender());
            ps.setString(4, patient.getPhone());
            ps.setString(5, patient.getEmail());
            ps.setString(6, patient.getAddress());
            ps.setString(7, patient.getId());

            ps.executeUpdate();
            LOGGER.info("Updated patient information: " + patient.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating patient in MySQL: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating patient: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        if (hasAppointments(id)) {
            throw new IllegalStateException("Cannot delete patient " + id + ": patient has associated appointment history. Use archival policy.");
        }

        String sql = "DELETE FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.executeUpdate();
            LOGGER.info("Deleted eligible patient: " + id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting patient (" + id + "): " + e.getMessage(), e);
            throw new RuntimeException("Database error deleting patient: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM patients WHERE patient_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean existsByPhone(String phone, String excludePatientId) {
        String sql = "SELECT 1 FROM patients WHERE phone = ? AND patient_id != ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, excludePatientId != null ? excludePatientId : "");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean hasAppointments(String patientId) {
        String sql = "SELECT 1 FROM appointments WHERE patient_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM patients";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting patients: " + e.getMessage(), e);
        }
        return 0;
    }

    private Patient mapPatientWithStats(ResultSet rs) throws SQLException {
        String id = rs.getString("patient_id");
        String name = rs.getString("full_name");
        int age = rs.getInt("age");
        String gender = rs.getString("gender");
        String phone = rs.getString("phone");
        String email = rs.getString("email");
        String address = rs.getString("address");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        int total = rs.getInt("total_appts");
        int attended = rs.getInt("attended_appts");
        int noshow = rs.getInt("noshow_appts");

        Patient p = new Patient(id, name, age, gender, phone, email, address, createdAt);
        p.setTotalAppointments(total);
        p.setAttendedAppointments(attended);
        p.setNoShowCount(noshow);
        p.setMedicalNotes("Registered in GhostQueue enterprise database. Address: " + (address != null ? address : "Standard facility"));
        return p;
    }
}
