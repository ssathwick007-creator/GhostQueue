package com.ghostqueue.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    private DatabaseInitializer() {}

    public static synchronized boolean initializeDatabase() {
        LOGGER.info("Verifying GhostQueue MySQL database initialization...");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if tables already exist
            boolean hasPatients = false;
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'patients'")) {
                if (rs.next()) {
                    hasPatients = true;
                }
            }

            if (hasPatients) {
                // Ensure Phase 3 appointment_predictions table exists
                try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'appointment_predictions'")) {
                    if (!rs.next()) {
                        LOGGER.info("Creating appointment_predictions table for Phase 3...");
                        stmt.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS appointment_predictions (
                                prediction_id VARCHAR(64) PRIMARY KEY,
                                appointment_id VARCHAR(32) NOT NULL,
                                model_version VARCHAR(32) NOT NULL,
                                no_show_probability DECIMAL(5, 4) NOT NULL,
                                risk_level VARCHAR(16) NOT NULL,
                                prediction_method VARCHAR(32) NOT NULL,
                                contributing_factors TEXT NOT NULL,
                                suggested_action VARCHAR(255) NOT NULL,
                                predicted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                actual_outcome VARCHAR(16) DEFAULT NULL,
                                evaluation_status VARCHAR(32) DEFAULT 'PENDING',
                                CONSTRAINT fk_pred_appointment FOREIGN KEY (appointment_id) 
                                    REFERENCES appointments (appointment_id) 
                                    ON DELETE CASCADE ON UPDATE CASCADE,
                                INDEX idx_pred_appointment (appointment_id),
                                INDEX idx_pred_risk_level (risk_level),
                                INDEX idx_pred_predicted_at (predicted_at)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """);
                    }
                }
                LOGGER.info("GhostQueue database tables already exist and are active.");
                return true;
            }

            LOGGER.info("Tables not found. Executing initialization script from /db/init.sql...");
            executeSqlScript(conn, "/db/init.sql");
            LOGGER.info("Database initialization script completed successfully.");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database initialization encounter an error: " + e.getMessage(), e);
            return false;
        }
    }

    private static void executeSqlScript(Connection conn, String resourcePath) throws Exception {
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    continue;
                }
                sb.append(line).append(" ");
                if (trimmed.endsWith(";")) {
                    String sql = sb.toString().trim();
                    if (!sql.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql);
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Warning executing statement: " + sql + " -> " + ex.getMessage());
                        }
                    }
                    sb.setLength(0);
                }
            }
        }
    }
}
