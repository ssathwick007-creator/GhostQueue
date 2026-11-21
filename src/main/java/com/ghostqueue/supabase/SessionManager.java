package com.ghostqueue.supabase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());
    private static final String SESSION_FILE = System.getProperty("user.home") + "/.ghostqueue_session.ser";

    private static SessionManager instance;
    private SupabaseSession currentSession;

    private SessionManager() {
        loadPersistedSession();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public synchronized void setSession(SupabaseSession session, boolean rememberMe) {
        this.currentSession = session;
        if (rememberMe && session != null) {
            persistSession(session);
        } else {
            clearPersistedSession();
        }
    }

    public synchronized void clearSession() {
        this.currentSession = null;
        clearPersistedSession();
    }

    public synchronized SupabaseSession getCurrentSession() {
        return currentSession;
    }

    public synchronized boolean isAuthenticated() {
        return currentSession != null && currentSession.getAccessToken() != null && !currentSession.getAccessToken().isBlank();
    }

    public synchronized String getAccessToken() {
        return currentSession != null ? currentSession.getAccessToken() : null;
    }

    public synchronized String getRole() {
        return currentSession != null ? currentSession.getRole() : "ANON";
    }

    public synchronized SupabaseSession.UserRole getCurrentRole() {
        return currentSession != null ? currentSession.getUserRole() : SupabaseSession.UserRole.PATIENT;
    }

    public synchronized boolean isAdmin() {
        return currentSession != null && currentSession.isAdmin();
    }

    public synchronized boolean isDoctor() {
        return currentSession != null && currentSession.isDoctor();
    }

    public synchronized boolean isPatient() {
        return currentSession != null && currentSession.isPatient();
    }

    private void persistSession(SupabaseSession session) {
        try {
            Path path = Paths.get(SESSION_FILE);
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path))) {
                oos.writeObject(session);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist session to disk", e);
        }
    }

    private void loadPersistedSession() {
        Path path = Paths.get(SESSION_FILE);
        if (!Files.exists(path)) return;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            Object obj = ois.readObject();
            if (obj instanceof SupabaseSession session) {
                if (!session.isExpired()) {
                    this.currentSession = session;
                    LOGGER.info("Restored valid active session for " + session.getEmail() + " (" + session.getRole() + ")");
                } else {
                    LOGGER.info("Saved session has expired, clearing.");
                    clearPersistedSession();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No valid persisted session found");
        }
    }

    private void clearPersistedSession() {
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
        } catch (Exception ignored) {}
    }
}
