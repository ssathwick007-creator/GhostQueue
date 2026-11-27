package com.ghostqueue.mongodb.repository;

import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.mongodb.MongoConnection;
import com.ghostqueue.repository.AppointmentHistoryRepository;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Atlas Repository for Appointment Status Transition History.
 * Persists status transitions in MongoDB collection 'appointment_history'.
 */
public class MongoAppointmentHistoryRepository implements AppointmentHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(MongoAppointmentHistoryRepository.class.getName());
    private static final String COLLECTION = "appointment_history";
    private final List<AppointmentHistory> localCache = new CopyOnWriteArrayList<>();

    @Override
    public List<AppointmentHistory> findByAppointmentId(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) return Collections.emptyList();
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    List<AppointmentHistory> list = new ArrayList<>();
                    for (Document doc : db.getCollection(COLLECTION).find(Filters.eq("appointment_id", appointmentId.trim()))) {
                        list.add(fromDocument(doc));
                    }
                    if (!list.isEmpty()) {
                        list.sort(Comparator.comparing(AppointmentHistory::getChangedAt).reversed());
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error reading appointment history from MongoDB: " + e.getMessage());
        }
        return localCache.stream().filter(h -> appointmentId.equalsIgnoreCase(h.getAppointmentId())).toList();
    }

    @Override
    public void save(AppointmentHistory history) {
        if (history == null) return;
        localCache.add(history);
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).insertOne(toDocument(history));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist appointment history to MongoDB: " + e.getMessage());
        }
    }

    @Override
    public void save(Connection conn, AppointmentHistory history) {
        save(history);
    }

    private Document toDocument(AppointmentHistory h) {
        return new Document("history_id", h.getHistoryId())
                .append("appointment_id", h.getAppointmentId())
                .append("previous_status", h.getPreviousStatus())
                .append("new_status", h.getNewStatus())
                .append("changed_at", h.getChangedAt() != null ? h.getChangedAt().toString() : LocalDateTime.now().toString())
                .append("remarks", h.getRemarks());
    }

    private AppointmentHistory fromDocument(Document d) {
        LocalDateTime changedAt = LocalDateTime.now();
        String cs = d.getString("changed_at");
        if (cs != null) {
            try { changedAt = LocalDateTime.parse(cs); } catch (Exception ignored) {}
        }
        Integer id = d.getInteger("history_id");
        return new AppointmentHistory(
                id != null ? id : (int) (System.currentTimeMillis() % 100000),
                d.getString("appointment_id"),
                d.getString("previous_status"),
                d.getString("new_status"),
                changedAt,
                d.getString("remarks")
        );
    }
}
