package com.ghostqueue.service;

import com.ghostqueue.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing administrative system configuration, hospital operating parameters,
 * and predictive risk thresholds persisted in MySQL system_settings.
 */
public class SettingsService {

    private static final Logger LOGGER = Logger.getLogger(SettingsService.class.getName());

    public static final String KEY_HOSPITAL_NAME = "hospital.name";
    public static final String KEY_HOSPITAL_CAMPUS = "hospital.campus";
    public static final String KEY_HOSPITAL_HOURS = "hospital.hours";
    public static final String KEY_HOSPITAL_TIMEZONE = "hospital.timezone";
    public static final String KEY_THRESHOLD_HIGH_RISK = "threshold.high_risk";
    public static final String KEY_THRESHOLD_MEDIUM_RISK = "threshold.medium_risk";
    public static final String KEY_REALLOC_STRICT_CONFIRM = "reallocation.strict_confirmation";
    public static final String KEY_REALLOC_URGENT_BOOST = "reallocation.urgent_priority_boost";
    public static final String KEY_REMINDERS_AUTO_HIGH_RISK = "reminders.auto_high_risk";
    public static final String KEY_REMINDERS_LEAD_HOURS = "reminders.default_lead_hours";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public SettingsService() {
        loadSettings();
    }

    /**
     * Loads all settings from MySQL system_settings table into memory cache.
     */
    public synchronized void loadSettings() {
        String sql = "SELECT setting_key, setting_value FROM system_settings";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String key = rs.getString("setting_key");
                String val = rs.getString("setting_value");
                if (key != null && val != null) {
                    cache.put(key, val);
                }
            }
            LOGGER.info("Loaded " + cache.size() + " system settings from database.");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error loading system_settings from database: " + e.getMessage());
            populateDefaultCache();
        }

        if (cache.isEmpty()) {
            populateDefaultCache();
        }
    }

    private void populateDefaultCache() {
        cache.put(KEY_HOSPITAL_NAME, "Metropolitan Academic Medical Center");
        cache.put(KEY_HOSPITAL_CAMPUS, "Main Medical Pavilion (North Tower)");
        cache.put(KEY_HOSPITAL_HOURS, "08:00 AM - 17:30 PM (Mon - Sat)");
        cache.put(KEY_HOSPITAL_TIMEZONE, "Eastern Time (US & Canada, UTC-05:00)");
        cache.put(KEY_THRESHOLD_HIGH_RISK, "0.65");
        cache.put(KEY_THRESHOLD_MEDIUM_RISK, "0.30");
        cache.put(KEY_REALLOC_STRICT_CONFIRM, "true");
        cache.put(KEY_REALLOC_URGENT_BOOST, "20");
        cache.put(KEY_REMINDERS_AUTO_HIGH_RISK, "true");
        cache.put(KEY_REMINDERS_LEAD_HOURS, "48");
    }

    public String getString(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    public double getHighRiskThreshold() {
        return getDouble(KEY_THRESHOLD_HIGH_RISK, 0.65);
    }

    public double getMediumRiskThreshold() {
        return getDouble(KEY_THRESHOLD_MEDIUM_RISK, 0.30);
    }

    public boolean isStrictConfirmationEnabled() {
        return getBoolean(KEY_REALLOC_STRICT_CONFIRM, true);
    }

    public boolean isAutoStandbyEnabled() {
        return getBoolean(KEY_REMINDERS_AUTO_HIGH_RISK, true);
    }

    public Map<String, String> getAllSettings() {
        return Collections.unmodifiableMap(new HashMap<>(cache));
    }

    /**
     * Persists or updates a single setting key in MySQL and updates in-memory cache.
     */
    public synchronized void saveSetting(String key, String value, String category, String description) {
        cache.put(key, value);

        String sql = """
            INSERT INTO system_settings (setting_key, setting_value, setting_category, description)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, category != null ? category : "SYSTEM");
            ps.setString(4, description != null ? description : "Administrative configuration");
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error persisting setting [" + key + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Persists a batch of settings keys in MySQL.
     */
    public synchronized void saveSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            String category = determineCategory(key);
            saveSetting(key, val, category, "Updated via Settings UI");
        }
    }

    private String determineCategory(String key) {
        if (key.startsWith("hospital.")) return "FACILITY";
        if (key.startsWith("threshold.")) return "PREDICTION";
        if (key.startsWith("reallocation.")) return "REALLOCATION";
        if (key.startsWith("reminders.")) return "REMINDERS";
        return "GENERAL";
    }

    /**
     * Resets settings back to baseline enterprise defaults.
     */
    public synchronized void resetToDefaults() {
        populateDefaultCache();
        saveSettings(cache);
    }
}
