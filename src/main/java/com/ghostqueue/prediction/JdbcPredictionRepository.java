package com.ghostqueue.prediction;

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

/**
 * JDBC implementation of PredictionRepository for the MySQL appointment_predictions table.
 */
public class JdbcPredictionRepository implements PredictionRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcPredictionRepository.class.getName());

    private static final String BASE_SELECT = """
        SELECT ap.prediction_id, ap.appointment_id, ap.model_version, ap.no_show_probability,
               ap.risk_level, ap.prediction_method, ap.contributing_factors, ap.suggested_action,
               ap.predicted_at, ap.actual_outcome, ap.evaluation_status,
               a.appointment_date, a.appointment_time, a.patient_id, a.doctor_id,
               p.full_name AS patient_name,
               d.full_name AS doctor_name, d.department,
               p.age, p.phone
        FROM appointment_predictions ap
        INNER JOIN appointments a ON ap.appointment_id = a.appointment_id
        INNER JOIN patients p ON a.patient_id = p.patient_id
        INNER JOIN doctors d ON a.doctor_id = d.doctor_id
    """;

    @Override
    public void save(PredictionResult result) {
        String sql = """
            INSERT INTO appointment_predictions (
                prediction_id, appointment_id, model_version, no_show_probability,
                risk_level, prediction_method, contributing_factors, suggested_action,
                predicted_at, actual_outcome, evaluation_status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                no_show_probability = VALUES(no_show_probability),
                risk_level = VALUES(risk_level),
                model_version = VALUES(model_version),
                prediction_method = VALUES(prediction_method),
                contributing_factors = VALUES(contributing_factors),
                suggested_action = VALUES(suggested_action),
                predicted_at = VALUES(predicted_at),
                actual_outcome = VALUES(actual_outcome),
                evaluation_status = VALUES(evaluation_status)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindPredictionParams(ps, result);
            ps.executeUpdate();
            LOGGER.info("Saved prediction: " + result.getPredictionId() + " for appointment " + result.getAppointmentId());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving prediction: " + e.getMessage(), e);
            throw new RuntimeException("Database error saving prediction: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveBatch(List<PredictionResult> results) {
        if (results == null || results.isEmpty()) return;

        String sql = """
            INSERT INTO appointment_predictions (
                prediction_id, appointment_id, model_version, no_show_probability,
                risk_level, prediction_method, contributing_factors, suggested_action,
                predicted_at, actual_outcome, evaluation_status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                no_show_probability = VALUES(no_show_probability),
                risk_level = VALUES(risk_level),
                model_version = VALUES(model_version),
                prediction_method = VALUES(prediction_method),
                contributing_factors = VALUES(contributing_factors),
                suggested_action = VALUES(suggested_action),
                predicted_at = VALUES(predicted_at),
                actual_outcome = VALUES(actual_outcome),
                evaluation_status = VALUES(evaluation_status)
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (PredictionResult r : results) {
                    bindPredictionParams(ps, r);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                LOGGER.info("Successfully executed batch save of " + results.size() + " appointment predictions.");
            } catch (SQLException ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Transaction rollback during batch prediction save: " + ex.getMessage(), ex);
                throw ex;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during batch prediction save: " + e.getMessage(), e);
            throw new RuntimeException("Database error saving batch predictions: " + e.getMessage(), e);
        }
    }

    private void bindPredictionParams(PreparedStatement ps, PredictionResult r) throws SQLException {
        ps.setString(1, r.getPredictionId());
        ps.setString(2, r.getAppointmentId());
        ps.setString(3, r.getModelVersion());
        ps.setDouble(4, r.getNoShowProbability());
        ps.setString(5, r.getRiskLevel().name());
        ps.setString(6, r.getPredictionMethod());
        ps.setString(7, serializeFactors(r.getContributingFactors()));
        ps.setString(8, r.getSuggestedAction());
        ps.setTimestamp(9, Timestamp.valueOf(r.getPredictedAt() != null ? r.getPredictedAt() : LocalDateTime.now()));
        ps.setString(10, r.getActualOutcome());
        ps.setString(11, r.getEvaluationStatus());
    }

    @Override
    public Optional<PredictionResult> findById(String predictionId) {
        String sql = BASE_SELECT + " WHERE ap.prediction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, predictionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPrediction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding prediction by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<PredictionResult> findLatestByAppointmentId(String appointmentId) {
        String sql = BASE_SELECT + " WHERE ap.appointment_id = ? ORDER BY ap.predicted_at DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPrediction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding latest prediction for appointment: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<PredictionResult> findAll() {
        String sql = BASE_SELECT + " ORDER BY ap.predicted_at DESC";
        List<PredictionResult> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapPrediction(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all predictions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PredictionResult> findLatestPredictions() {
        // Returns the most recent prediction for each appointment
        String sql = """
            SELECT ap.prediction_id, ap.appointment_id, ap.model_version, ap.no_show_probability,
                   ap.risk_level, ap.prediction_method, ap.contributing_factors, ap.suggested_action,
                   ap.predicted_at, ap.actual_outcome, ap.evaluation_status,
                   a.appointment_date, a.appointment_time, a.patient_id, a.doctor_id,
                   p.full_name AS patient_name,
                   d.full_name AS doctor_name, d.department,
                   p.age, p.phone
            FROM appointment_predictions ap
            INNER JOIN (
                SELECT appointment_id, MAX(predicted_at) AS max_predicted_at
                FROM appointment_predictions
                GROUP BY appointment_id
            ) latest ON ap.appointment_id = latest.appointment_id AND ap.predicted_at = latest.max_predicted_at
            INNER JOIN appointments a ON ap.appointment_id = a.appointment_id
            INNER JOIN patients p ON a.patient_id = p.patient_id
            INNER JOIN doctors d ON a.doctor_id = d.doctor_id
            ORDER BY ap.no_show_probability DESC, a.appointment_date ASC
        """;

        List<PredictionResult> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapPrediction(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching latest unique predictions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<PredictionResult> filter(String query, Department department, String doctorId, RiskLevel riskLevel, LocalDate date) {
        StringBuilder sb = new StringBuilder("""
            SELECT ap.prediction_id, ap.appointment_id, ap.model_version, ap.no_show_probability,
                   ap.risk_level, ap.prediction_method, ap.contributing_factors, ap.suggested_action,
                   ap.predicted_at, ap.actual_outcome, ap.evaluation_status,
                   a.appointment_date, a.appointment_time, a.patient_id, a.doctor_id,
                   p.full_name AS patient_name,
                   d.full_name AS doctor_name, d.department,
                   p.age, p.phone
            FROM appointment_predictions ap
            INNER JOIN (
                SELECT appointment_id, MAX(predicted_at) AS max_predicted_at
                FROM appointment_predictions
                GROUP BY appointment_id
            ) latest ON ap.appointment_id = latest.appointment_id AND ap.predicted_at = latest.max_predicted_at
            INNER JOIN appointments a ON ap.appointment_id = a.appointment_id
            INNER JOIN patients p ON a.patient_id = p.patient_id
            INNER JOIN doctors d ON a.doctor_id = d.doctor_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sb.append(" AND (LOWER(ap.appointment_id) LIKE ? OR LOWER(p.full_name) LIKE ? OR LOWER(d.full_name) LIKE ?)");
            String pattern = "%" + query.toLowerCase().trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (department != null && department != Department.ALL) {
            sb.append(" AND LOWER(d.department) = ?");
            params.add(department.getDisplayName().toLowerCase());
        }

        if (doctorId != null && !doctorId.isBlank() && !"All Doctors".equalsIgnoreCase(doctorId)) {
            sb.append(" AND a.doctor_id = ?");
            params.add(doctorId.trim());
        }

        if (riskLevel != null && riskLevel != RiskLevel.ALL) {
            sb.append(" AND ap.risk_level = ?");
            params.add(riskLevel.name());
        }

        if (date != null) {
            sb.append(" AND a.appointment_date = ?");
            params.add(Date.valueOf(date));
        }

        sb.append(" ORDER BY ap.no_show_probability DESC");

        List<PredictionResult> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPrediction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error filtering predictions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void updateActualOutcome(String predictionId, String outcome, String evaluationStatus) {
        String sql = "UPDATE appointment_predictions SET actual_outcome = ?, evaluation_status = ? WHERE prediction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, outcome);
            ps.setString(2, evaluationStatus);
            ps.setString(3, predictionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating prediction outcome: " + e.getMessage(), e);
        }
    }

    @Override
    public int countTotal() {
        String sql = "SELECT COUNT(DISTINCT appointment_id) FROM appointment_predictions";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting predictions: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int countByRiskLevel(RiskLevel riskLevel) {
        String sql = """
            SELECT COUNT(*) 
            FROM appointment_predictions ap
            INNER JOIN (
                SELECT appointment_id, MAX(predicted_at) AS max_predicted_at
                FROM appointment_predictions
                GROUP BY appointment_id
            ) latest ON ap.appointment_id = latest.appointment_id AND ap.predicted_at = latest.max_predicted_at
            WHERE ap.risk_level = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, riskLevel.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting predictions by risk level: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public double getAverageProbability() {
        String sql = """
            SELECT AVG(ap.no_show_probability) 
            FROM appointment_predictions ap
            INNER JOIN (
                SELECT appointment_id, MAX(predicted_at) AS max_predicted_at
                FROM appointment_predictions
                GROUP BY appointment_id
            ) latest ON ap.appointment_id = latest.appointment_id AND ap.predicted_at = latest.max_predicted_at
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating average prediction probability: " + e.getMessage(), e);
        }
        return 0.28;
    }

    private PredictionResult mapPrediction(ResultSet rs) throws SQLException {
        String predictionId = rs.getString("prediction_id");
        String appointmentId = rs.getString("appointment_id");
        String modelVersion = rs.getString("model_version");
        double prob = rs.getDouble("no_show_probability");
        String riskStr = rs.getString("risk_level");
        RiskLevel level;
        try {
            level = RiskLevel.valueOf(riskStr);
        } catch (Exception e) {
            level = RiskLevel.fromScore(prob);
        }

        String method = rs.getString("prediction_method");
        String factorsStr = rs.getString("contributing_factors");
        List<FactorExplanation> factors = deserializeFactors(factorsStr);
        String action = rs.getString("suggested_action");

        Timestamp predTs = rs.getTimestamp("predicted_at");
        LocalDateTime predictedAt = predTs != null ? predTs.toLocalDateTime() : LocalDateTime.now();

        String actualOutcome = rs.getString("actual_outcome");
        String evalStatus = rs.getString("evaluation_status");

        Date apptDate = rs.getDate("appointment_date");
        LocalDate date = apptDate != null ? apptDate.toLocalDate() : LocalDate.now();

        Time apptTime = rs.getTime("appointment_time");
        LocalTime time = apptTime != null ? apptTime.toLocalTime() : LocalTime.of(9, 0);

        String patientId = rs.getString("patient_id");
        String patientName = rs.getString("patient_name");
        String doctorName = rs.getString("doctor_name");
        String deptStr = rs.getString("department");
        Department dept = Department.fromDisplayName(deptStr);

        return new PredictionResult(
                predictionId, appointmentId, patientId, patientName, doctorName, dept,
                date, time, prob, level, method, modelVersion,
                factors, action, predictedAt, actualOutcome, evalStatus,
                "Clinical record for " + patientName
        );
    }

    private String serializeFactors(List<FactorExplanation> factors) {
        if (factors == null || factors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < factors.size(); i++) {
            FactorExplanation f = factors.get(i);
            if (i > 0) sb.append(";;;");
            sb.append(f.getFeatureName().replace("|", "/"))
              .append("|").append(f.getDirection().name())
              .append("|").append(String.format(java.util.Locale.US, "%.4f", f.getImpactScore()))
              .append("|").append(f.getDescription().replace("|", "/"));
        }
        return sb.toString();
    }

    private List<FactorExplanation> deserializeFactors(String raw) {
        List<FactorExplanation> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) return list;

        String[] parts = raw.split(";;;");
        for (String p : parts) {
            String[] tokens = p.split("\\|", 4);
            if (tokens.length >= 4) {
                String name = tokens[0];
                FactorExplanation.Direction dir;
                try {
                    dir = FactorExplanation.Direction.valueOf(tokens[1]);
                } catch (Exception e) {
                    dir = FactorExplanation.Direction.NEUTRAL;
                }
                double score = 0.0;
                try {
                    score = Double.parseDouble(tokens[2]);
                } catch (Exception ignored) {}
                String desc = tokens[3];
                list.add(new FactorExplanation(name, dir, score, desc));
            }
        }
        return list;
    }
}
