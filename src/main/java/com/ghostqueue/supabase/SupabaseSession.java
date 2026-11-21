package com.ghostqueue.supabase;

import java.io.Serializable;
import java.time.Instant;

public class SupabaseSession implements Serializable {

    private final String userId;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String role; // "PATIENT", "DOCTOR", "ADMIN"
    private final String accountStatus; // "ACTIVE", "PENDING_APPROVAL", "SUSPENDED"
    private final String entityId; // e.g. "PT-1001" or "DOC-101"
    private final String accessToken;
    private final String refreshToken;
    private final long expiresAt;

    public SupabaseSession(String userId,
                           String email,
                           String fullName,
                           String phone,
                           String role,
                           String accountStatus,
                           String entityId,
                           String accessToken,
                           String refreshToken,
                           long expiresAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role != null ? role.toUpperCase() : "PATIENT";
        this.accountStatus = accountStatus != null ? accountStatus.toUpperCase() : "ACTIVE";
        this.entityId = entityId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName != null && !fullName.isBlank() ? fullName : email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getAccountStatus() { return accountStatus; }
    public String getEntityId() { return entityId; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public long getExpiresAt() { return expiresAt; }

    public enum UserRole {
        ADMIN, DOCTOR, PATIENT;

        public static UserRole fromString(String r) {
            if (r == null) return PATIENT;
            try { return valueOf(r.toUpperCase()); } catch (Exception e) { return PATIENT; }
        }
    }

    public UserRole getUserRole() {
        return UserRole.fromString(role);
    }

    public String getDoctorId() {
        return entityId != null ? entityId : "DOC-101";
    }

    public String getPatientId() {
        return entityId != null ? entityId : "PT-1001";
    }

    public boolean isExpired() {
        return expiresAt > 0 && Instant.now().getEpochSecond() >= (expiresAt - 30);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isDoctor() {
        return "DOCTOR".equalsIgnoreCase(role);
    }

    public boolean isPatient() {
        return "PATIENT".equalsIgnoreCase(role);
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(accountStatus);
    }

    public boolean isPendingApproval() {
        return "PENDING_APPROVAL".equalsIgnoreCase(accountStatus);
    }

    public boolean isSuspended() {
        return "SUSPENDED".equalsIgnoreCase(accountStatus);
    }
}
