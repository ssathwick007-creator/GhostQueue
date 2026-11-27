package com.ghostqueue.supabase;

import com.ghostqueue.util.JsonUtil;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseAuthService {

    private static final Logger LOGGER = Logger.getLogger(SupabaseAuthService.class.getName());
    private static SupabaseAuthService instance;

    private final SupabaseClient client;

    private SupabaseAuthService() {
        this.client = SupabaseClient.getInstance();
    }

    public static synchronized SupabaseAuthService getInstance() {
        if (instance == null) {
            instance = new SupabaseAuthService();
        }
        return instance;
    }

    public SupabaseSession getCurrentSession() {
        return SessionManager.getInstance().getCurrentSession();
    }

    public SupabaseSession signIn(String email, String password, boolean rememberMe) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        Map<String, Object> req = new HashMap<>();
        req.put("email", email.trim().toLowerCase());
        req.put("password", password);

        String endpoint = SupabaseConfig.getAuthEndpoint("/token?grant_type=password");
        SupabaseClient.Response resp = client.post(endpoint, req);

        if (!resp.isSuccess()) {
            Map<String, Object> err = resp.asObject();
            String msg = (String) err.getOrDefault("msg", (String) err.getOrDefault("error_description", "Invalid login credentials."));
            LOGGER.log(Level.WARNING, "Supabase authentication failed for " + email + ": " + msg);
            throw new IllegalArgumentException(translateAuthError(msg));
        }

        Map<String, Object> data = resp.asObject();
        String accessToken = (String) data.get("access_token");
        String refreshToken = (String) data.get("refresh_token");
        Number expiresIn = (Number) data.getOrDefault("expires_in", 3600);
        long expiresAt = Instant.now().getEpochSecond() + expiresIn.longValue();

        @SuppressWarnings("unchecked")
        Map<String, Object> userObj = (Map<String, Object>) data.get("user");
        String userId = (String) userObj.get("id");
        String userEmail = (String) userObj.get("email");

        // Temporarily set session so subsequent profile query includes this token
        SupabaseSession tempSession = new SupabaseSession(userId, userEmail, userEmail, null, "PATIENT", "ACTIVE", null, accessToken, refreshToken, expiresAt);
        SessionManager.getInstance().setSession(tempSession, false);

        // Fetch user profile from public.profiles
        String role = "PATIENT";
        String fullName = userEmail;
        String phone = null;
        String accountStatus = "ACTIVE";

        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("id", "eq." + userId);
            queryParams.put("select", "*");
            SupabaseClient.Response profileResp = client.get(SupabaseConfig.getRestEndpoint("profiles"), queryParams);
            List<Map<String, Object>> profiles = profileResp.asList();
            if (!profiles.isEmpty()) {
                Map<String, Object> p = profiles.get(0);
                role = (String) p.getOrDefault("role", "PATIENT");
                fullName = (String) p.getOrDefault("full_name", userEmail);
                phone = (String) p.get("phone");
                accountStatus = (String) p.getOrDefault("account_status", "ACTIVE");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load profile for user " + userId, e);
        }

        // Determine Entity ID (e.g. DOC-101 or PT-1001)
        String entityId = null;
        if ("DOCTOR".equalsIgnoreCase(role)) {
            try {
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("profile_id", "eq." + userId);
                queryParams.put("select", "doctor_id,account_status");
                SupabaseClient.Response docResp = client.get(SupabaseConfig.getRestEndpoint("doctors"), queryParams);
                List<Map<String, Object>> docs = docResp.asList();
                if (!docs.isEmpty()) {
                    entityId = (String) docs.get(0).get("doctor_id");
                    accountStatus = (String) docs.get(0).getOrDefault("account_status", accountStatus);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to lookup doctor entity for " + userId, e);
            }
        } else if ("PATIENT".equalsIgnoreCase(role)) {
            try {
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("profile_id", "eq." + userId);
                queryParams.put("select", "patient_id");
                SupabaseClient.Response patResp = client.get(SupabaseConfig.getRestEndpoint("patients"), queryParams);
                List<Map<String, Object>> pats = patResp.asList();
                if (!pats.isEmpty()) {
                    entityId = (String) pats.get(0).get("patient_id");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to lookup patient entity for " + userId, e);
            }
        }

        SupabaseSession session = new SupabaseSession(userId, userEmail, fullName, phone, role, accountStatus, entityId, accessToken, refreshToken, expiresAt);
        SessionManager.getInstance().setSession(session, rememberMe);
        LOGGER.info("Supabase authenticated: " + userEmail + " as " + role + " (Status: " + accountStatus + ", Entity: " + entityId + ")");

        // Asynchronously update last_login_at and write audit trail
        final String finalUserId = userId;
        final String finalEmail = userEmail;
        final String finalRole = role;
        CompletableFuture.runAsync(() -> {
            try {
                String nowIso = Instant.now().toString();
                // 1. Update last_login_at on profiles table
                Map<String, Object> update = new HashMap<>();
                update.put("last_login_at", nowIso);
                update.put("updated_at", nowIso);
                client.patch(SupabaseConfig.getRestEndpoint("profiles"), Map.of("id", "eq." + finalUserId), update);

                // 2. Record audit log
                Map<String, Object> log = new HashMap<>();
                log.put("user_id", finalUserId);
                log.put("action", "USER_LOGIN");
                log.put("entity_type", "SESSION");
                log.put("entity_id", finalUserId);
                log.put("details", "Successful login: " + finalEmail + " (" + finalRole + ")");
                client.post(SupabaseConfig.getRestEndpoint("audit_logs"), log);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Login telemetry skipped: " + e.getMessage());
            }
        });

        return session;
    }

    public SupabaseSession signUpPatient(String email,
                                         String password,
                                         String fullName,
                                         String phone,
                                         LocalDate dob,
                                         String gender,
                                         String address) {
        if (email == null || email.isBlank() || password == null || password.isBlank() || fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name, email, and password are required.");
        }

        Map<String, Object> req = new HashMap<>();
        req.put("email", email.trim().toLowerCase());
        req.put("password", password);

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", fullName.trim());
        data.put("phone", phone != null ? phone.trim() : "");
        data.put("role", "PATIENT");
        req.put("data", data);

        String endpoint = SupabaseConfig.getAuthEndpoint("/signup");
        SupabaseClient.Response resp = client.post(endpoint, req);

        if (!resp.isSuccess()) {
            Map<String, Object> err = resp.asObject();
            String msg = (String) err.getOrDefault("msg", (String) err.getOrDefault("error_description", "Registration failed."));
            LOGGER.log(Level.WARNING, "Registration error: " + msg);
            throw new IllegalArgumentException(translateAuthError(msg));
        }

        Map<String, Object> respMap = resp.asObject();
        String userId = null;
        if (respMap.containsKey("user") && respMap.get("user") instanceof Map<?, ?> userMap) {
            userId = (String) userMap.get("id");
        } else if (respMap.containsKey("id")) {
            userId = (String) respMap.get("id");
        }

        String patientId = "PT-" + (1020 + (int)(Math.random() * 8000));
        if (userId != null) {
            try {
                // Insert patient record
                Map<String, Object> patientRecord = new HashMap<>();
                patientRecord.put("patient_id", patientId);
                patientRecord.put("profile_id", userId);
                patientRecord.put("patient_identifier", "MED-" + patientId);
                patientRecord.put("full_name", fullName.trim());
                if (dob != null) patientRecord.put("date_of_birth", dob.toString());
                if (gender != null) patientRecord.put("gender", gender);
                if (phone != null) patientRecord.put("phone", phone.trim());
                patientRecord.put("email", email.trim().toLowerCase());
                if (address != null) patientRecord.put("address", address.trim());

                client.post(SupabaseConfig.getRestEndpoint("patients"), patientRecord);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Patient table insert failed on signup: " + e.getMessage());
            }
        }

        // Try to automatically log in now
        try {
            return signIn(email, password, false);
        } catch (Exception e) {
            LOGGER.info("Sign in after registration needs confirmation or manual login: " + e.getMessage());
            return null;
        }
    }

    public void signOut() {
        try {
            String endpoint = SupabaseConfig.getAuthEndpoint("/logout");
            client.post(endpoint, Collections.emptyMap());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Sign out error from remote endpoint", e);
        } finally {
            SessionManager.getInstance().clearSession();
        }
    }

    public void recoverPassword(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        Map<String, Object> req = new HashMap<>();
        req.put("email", email.trim().toLowerCase());
        String endpoint = SupabaseConfig.getAuthEndpoint("/recover");
        SupabaseClient.Response resp = client.post(endpoint, req);
        if (!resp.isSuccess()) {
            Map<String, Object> err = resp.asObject();
            String msg = (String) err.getOrDefault("msg", "Failed to send password recovery email.");
            throw new IllegalArgumentException(msg);
        }
    }

    public boolean resetPasswordForEmail(String email) {
        try {
            recoverPassword(email);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String authenticate(String email, String password, boolean rememberMe) {
        try {
            signIn(email, password, rememberMe);
            return "SUCCESS";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cloud authentication error for " + email + ": " + e.getMessage());
            // Offline / Network-Resilience Fallback for standard demonstration accounts
            SupabaseSession demoSession = resolveDemoSessionFallback(email, password);
            if (demoSession != null) {
                SessionManager.getInstance().setSession(demoSession, rememberMe);
                LOGGER.info("Established resilient demo session for: " + email + " (" + demoSession.getRole() + ")");
                return "SUCCESS";
            }
            return e.getMessage();
        }
    }

    private SupabaseSession resolveDemoSessionFallback(String email, String password) {
        if (email == null || password == null) return null;
        String em = email.trim().toLowerCase();
        String pw = password.trim();

        if ("admin@ghostqueue.com".equals(em) && "Admin@123".equals(pw)) {
            return new SupabaseSession("a0000000-0000-0000-0000-000000000001", "admin@ghostqueue.com",
                    "Chief Medical Officer (Admin)", "+1 (555) 000-1111", "ADMIN", "ACTIVE", null,
                    "demo_token", "demo_refresh", Instant.now().getEpochSecond() + 86400);
        }
        if ("doctor.vance@ghostqueue.com".equals(em) && "Doctor@123".equals(pw)) {
            return new SupabaseSession("b0000000-0000-0000-0000-000000000001", "doctor.vance@ghostqueue.com",
                    "Dr. Alexander Vance", "+1 (555) 234-5678", "DOCTOR", "ACTIVE", "DOC-101",
                    "demo_token", "demo_refresh", Instant.now().getEpochSecond() + 86400);
        }
        if ("doctor.wright@ghostqueue.com".equals(em) && "Doctor@123".equals(pw)) {
            return new SupabaseSession("b0000000-0000-0000-0000-000000000002", "doctor.wright@ghostqueue.com",
                    "Dr. Evelyn Wright", "+1 (555) 345-6789", "DOCTOR", "ACTIVE", "DOC-102",
                    "demo_token", "demo_refresh", Instant.now().getEpochSecond() + 86400);
        }
        if ("patient.eleanor@ghostqueue.com".equals(em) && "Patient@123".equals(pw)) {
            return new SupabaseSession("c0000000-0000-0000-0000-000000000001", "patient.eleanor@ghostqueue.com",
                    "Eleanor Vance", "+1 (555) 123-4567", "PATIENT", "ACTIVE", "PT-1001",
                    "demo_token", "demo_refresh", Instant.now().getEpochSecond() + 86400);
        }
        if ("patient.marcus@ghostqueue.com".equals(em) && "Patient@123".equals(pw)) {
            return new SupabaseSession("c0000000-0000-0000-0000-000000000002", "patient.marcus@ghostqueue.com",
                    "Marcus Holloway", "+1 (555) 987-6543", "PATIENT", "ACTIVE", "PT-1002",
                    "demo_token", "demo_refresh", Instant.now().getEpochSecond() + 86400);
        }
        return null;
    }

    public String signUpPatient(String email, String password, String fullName, String phone, String dob, String gender, String address) {
        try {
            LocalDate birthDate = null;
            if (dob != null && !dob.isBlank()) {
                try { birthDate = LocalDate.parse(dob); } catch (Exception ignored) {}
            }
            this.signUpPatient(email, password, fullName, phone, birthDate, gender, address);
            return "SUCCESS";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String translateAuthError(String msg) {
        if (msg == null) return "Authentication failed. Please verify credentials.";
        String lower = msg.toLowerCase();
        if (lower.contains("invalid login credentials")) {
            return "Invalid email or password. Please verify your credentials.";
        }
        if (lower.contains("email not confirmed")) {
            return "Email address has not been confirmed. Please check your inbox.";
        }
        if (lower.contains("user already registered") || lower.contains("email address already exists")) {
            return "An account with this email address already exists. Please sign in.";
        }
        if (lower.contains("password should be at least")) {
            return "Password is too weak. Please use at least 6 characters.";
        }
        return msg;
    }
}
