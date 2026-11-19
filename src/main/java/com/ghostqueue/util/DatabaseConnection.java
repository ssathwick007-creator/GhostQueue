package com.ghostqueue.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    private DatabaseConnection() {}

    private static synchronized void loadConfiguration() {
        if (initialized) return;

        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getResourceAsStream("/database.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load database.properties from classpath, using defaults", e);
        }

        // Priority: Environment variables > properties file > defaults
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        dbUrl = (envUrl != null && !envUrl.isBlank()) 
                ? envUrl 
                : props.getProperty("db.url", "jdbc:mysql://localhost:3306/ghostqueue_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

        dbUser = (envUser != null && !envUser.isBlank()) 
                ? envUser 
                : props.getProperty("db.user", "root");

        dbPassword = (envPass != null) 
                ? envPass 
                : props.getProperty("db.password", "");

        initialized = true;
        LOGGER.info("Database configuration loaded. Target URL: " + dbUrl + " (User: " + dbUser + ")");
    }

    public static Connection getConnection() throws SQLException {
        loadConfiguration();
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to establish MySQL database connection: " + e.getMessage(), e);
            throw new SQLException("Unable to connect to GhostQueue MySQL Database at " + dbUrl + 
                    ". Please verify MySQL server is running and credentials are valid. Error: " + e.getMessage(), e);
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }

    public static String getTargetDatabaseUrl() {
        loadConfiguration();
        return dbUrl;
    }

    public static String getTargetUser() {
        loadConfiguration();
        return dbUser;
    }
}
