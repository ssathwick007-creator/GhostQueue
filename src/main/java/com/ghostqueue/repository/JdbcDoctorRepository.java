package com.ghostqueue.repository;

import com.ghostqueue.model.Doctor;
import com.ghostqueue.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcDoctorRepository implements DoctorRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcDoctorRepository.class.getName());

    @Override
    public List<Doctor> findAll() {
        String sql = "SELECT doctor_id, full_name, department, specialization, phone, availability_status FROM doctors ORDER BY full_name ASC";
        List<Doctor> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapDoctor(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving doctors: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Doctor> findById(String id) {
        String sql = "SELECT doctor_id, full_name, department, specialization, phone, availability_status FROM doctors WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDoctor(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding doctor by ID (" + id + "): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Doctor> findByDepartment(String department) {
        if (department == null || department.equalsIgnoreCase("All") || department.equalsIgnoreCase("All Departments")) {
            return findAll();
        }
        String sql = "SELECT doctor_id, full_name, department, specialization, phone, availability_status FROM doctors WHERE LOWER(department) = LOWER(?) ORDER BY full_name ASC";
        List<Doctor> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, department);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDoctor(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding doctors by department: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Doctor> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String sql = """
            SELECT doctor_id, full_name, department, specialization, phone, availability_status 
            FROM doctors 
            WHERE LOWER(full_name) LIKE ? OR LOWER(doctor_id) LIKE ? OR LOWER(department) LIKE ? OR LOWER(specialization) LIKE ?
            ORDER BY full_name ASC
        """;
        List<Doctor> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase().trim() + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDoctor(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching doctors: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void save(Doctor doctor) {
        String sql = "INSERT INTO doctors (doctor_id, full_name, department, specialization, phone, availability_status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctor.getId());
            ps.setString(2, doctor.getFullName());
            ps.setString(3, doctor.getDepartment());
            ps.setString(4, doctor.getSpecialization());
            ps.setString(5, doctor.getPhone());
            ps.setString(6, doctor.getAvailabilityStatus());

            ps.executeUpdate();
            LOGGER.info("Registered doctor: " + doctor.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving doctor: " + e.getMessage(), e);
            throw new RuntimeException("Database error saving doctor: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Doctor doctor) {
        String sql = "UPDATE doctors SET full_name = ?, department = ?, specialization = ?, phone = ?, availability_status = ? WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctor.getFullName());
            ps.setString(2, doctor.getDepartment());
            ps.setString(3, doctor.getSpecialization());
            ps.setString(4, doctor.getPhone());
            ps.setString(5, doctor.getAvailabilityStatus());
            ps.setString(6, doctor.getId());

            ps.executeUpdate();
            LOGGER.info("Updated doctor profile: " + doctor.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating doctor: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating doctor: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateAvailability(String doctorId, String status) {
        String sql = "UPDATE doctors SET availability_status = ? WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, doctorId);
            ps.executeUpdate();
            LOGGER.info("Updated availability for doctor " + doctorId + " to " + status);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating availability: " + e.getMessage(), e);
            throw new RuntimeException("Database error updating doctor availability: " + e.getMessage(), e);
        }
    }

    @Override
    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM doctors";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting doctors: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM doctors WHERE doctor_id = ? LIMIT 1";
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

    private Doctor mapDoctor(ResultSet rs) throws SQLException {
        return new Doctor(
                rs.getString("doctor_id"),
                rs.getString("full_name"),
                rs.getString("department"),
                rs.getString("specialization"),
                rs.getString("phone"),
                rs.getString("availability_status")
        );
    }
}
