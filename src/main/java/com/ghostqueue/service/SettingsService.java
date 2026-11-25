package com.ghostqueue.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service managing administrative system configuration, hospital operating parameters,
 * and predictive risk thresholds in a fast, resilient thread-safe cache.
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
        populateDefaultCache();
    }

    /**
     * Loads baseline settings into the in-memory cache.
     */
    public synchronized void loadSettings() {
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
     * Persists or updates a single setting key in memory cache.
     */
    public synchronized void saveSetting(String key, String value, String category, String description) {
        cache.put(key, value);
        LOGGER.info("Setting updated: [" + key + " = " + value + "]");
    }

    /**
     * Persists a batch of settings keys.
     */
    public synchronized void saveSettings(Map<String, String> settings) {
        if (settings == null) return;
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
    }
}
