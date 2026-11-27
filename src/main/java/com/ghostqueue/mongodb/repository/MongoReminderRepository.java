package com.ghostqueue.mongodb.repository;

import com.ghostqueue.model.AppointmentReminder;
import com.ghostqueue.model.AppointmentReminder.ReminderStatus;
import com.ghostqueue.model.AppointmentReminder.ReminderType;
import com.ghostqueue.mongodb.MongoConnection;
import com.ghostqueue.repository.ReminderRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Atlas Repository for Multi-Channel Outreach and Reminders.
 * Persists entries in MongoDB collection 'reminders'.
 */
public class MongoReminderRepository implements ReminderRepository {

    private static final Logger LOGGER = Logger.getLogger(MongoReminderRepository.class.getName());
    private static final String COLLECTION = "reminders";
    private final List<AppointmentReminder> localCache = new CopyOnWriteArrayList<>();

    public MongoReminderRepository() {
        for (AppointmentReminder r : getInitialReminders()) {
            localCache.add(r);
        }
        syncToMongoIfConnected();
    }

    private void syncToMongoIfConnected() {
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    MongoCollection<Document> coll = db.getCollection(COLLECTION);
                    if (coll.countDocuments() == 0) {
                        List<Document> docs = new ArrayList<>();
                        for (AppointmentReminder r : localCache) {
                            docs.add(toDocument(r));
                        }
                        if (!docs.isEmpty()) {
                            coll.insertMany(docs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MongoDB reminders sync notice: " + e.getMessage());
        }
    }

    @Override
    public List<AppointmentReminder> findAll() {
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    List<AppointmentReminder> list = new ArrayList<>();
                    for (Document doc : db.getCollection(COLLECTION).find()) {
                        list.add(fromDocument(doc));
                    }
                    if (!list.isEmpty()) {
                        list.sort(Comparator.comparing(AppointmentReminder::getScheduledFor, Comparator.nullsLast(Comparator.reverseOrder())));
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error reading reminders: " + e.getMessage());
        }
        List<AppointmentReminder> list = new ArrayList<>(localCache);
        list.sort(Comparator.comparing(AppointmentReminder::getScheduledFor, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    @Override
    public Optional<AppointmentReminder> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream().filter(r -> id.equalsIgnoreCase(r.getReminderId())).findFirst();
    }

    @Override
    public List<AppointmentReminder> findByAppointmentId(String appointmentId) {
        if (appointmentId == null) return Collections.emptyList();
        return findAll().stream().filter(r -> appointmentId.equalsIgnoreCase(r.getAppointmentId())).toList();
    }

    public List<AppointmentReminder> findByPatientId(String patientId) {
        if (patientId == null) return Collections.emptyList();
        return findAll().stream().filter(r -> patientId.equalsIgnoreCase(r.getPatientId())).toList();
    }

    public List<AppointmentReminder> findPending() {
        return findAll().stream().filter(r -> r.getReminderStatus() == ReminderStatus.PENDING).toList();
    }

    @Override
    public List<AppointmentReminder> filter(String query, AppointmentReminder.ReminderType type, AppointmentReminder.ReminderStatus status) {
        return findAll().stream().filter(r -> {
            if (type != null && r.getReminderType() != type) return false;
            if (status != null && r.getReminderStatus() != status) return false;
            if (query != null && !query.isBlank()) {
                String q = query.trim().toLowerCase();
                boolean mId = r.getReminderId() != null && r.getReminderId().toLowerCase().contains(q);
                boolean mPat = r.getPatientName() != null && r.getPatientName().toLowerCase().contains(q);
                if (!mId && !mPat) return false;
            }
            return true;
        }).toList();
    }

    @Override
    public void save(AppointmentReminder reminder) {
        if (reminder == null || reminder.getReminderId() == null) return;
        localCache.removeIf(r -> reminder.getReminderId().equalsIgnoreCase(r.getReminderId()));
        localCache.add(0, reminder);
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).replaceOne(
                            Filters.eq("reminder_id", reminder.getReminderId()),
                            toDocument(reminder),
                            new ReplaceOptions().upsert(true)
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist reminder to MongoDB: " + e.getMessage());
        }
    }

    public void update(AppointmentReminder reminder) {
        save(reminder);
    }

    @Override
    public void updateStatus(String reminderId, AppointmentReminder.ReminderStatus status, String responseStatus, String notes) {
        Optional<AppointmentReminder> opt = findById(reminderId);
        if (opt.isPresent()) {
            AppointmentReminder r = opt.get();
            r.setReminderStatus(status);
            r.setResponseStatus(responseStatus);
            r.setOperatorNotes(notes);
            r.setCompletedAt(LocalDateTime.now());
            save(r);
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public int countPending() {
        return findPending().size();
    }

    @Override
    public int countCompleted() {
        return (int) findAll().stream().filter(r -> r.getReminderStatus() == ReminderStatus.COMPLETED).count();
    }

    public double getConfirmationRate() {
        List<AppointmentReminder> list = findAll();
        if (list.isEmpty()) return 0.75;
        long confirmed = list.stream().filter(r -> "CONFIRMED".equalsIgnoreCase(r.getResponseStatus())).count();
        return (double) confirmed / list.size();
    }

    private Document toDocument(AppointmentReminder r) {
        return new Document("reminder_id", r.getReminderId())
                .append("appointment_id", r.getAppointmentId())
                .append("patient_id", r.getPatientId())
                .append("patient_name", r.getPatientName())
                .append("patient_phone", r.getPatientPhone())
                .append("type", r.getReminderType() != null ? r.getReminderType().name() : "SMS")
                .append("status", r.getReminderStatus() != null ? r.getReminderStatus().name() : "PENDING")
                .append("scheduled_for", r.getScheduledFor() != null ? r.getScheduledFor().toString() : LocalDateTime.now().toString())
                .append("completed_at", r.getCompletedAt() != null ? r.getCompletedAt().toString() : null)
                .append("message_content", r.getMessageContent())
                .append("response_status", r.getResponseStatus())
                .append("operator_notes", r.getOperatorNotes());
    }

    private AppointmentReminder fromDocument(Document d) {
        LocalDateTime sched = LocalDateTime.now();
        String ss = d.getString("scheduled_for");
        if (ss != null) {
            try { sched = LocalDateTime.parse(ss); } catch (Exception ignored) {}
        }
        LocalDateTime comp = null;
        String cs = d.getString("completed_at");
        if (cs != null) {
            try { comp = LocalDateTime.parse(cs); } catch (Exception ignored) {}
        }

        return new AppointmentReminder(
                d.getString("reminder_id"),
                d.getString("appointment_id"),
                d.getString("patient_id"),
                d.getString("patient_name"),
                d.getString("patient_phone"),
                ReminderType.fromString(d.getString("type")),
                ReminderStatus.fromString(d.getString("status")),
                sched,
                comp,
                d.getString("message_content"),
                d.getString("response_status"),
                d.getString("operator_notes"),
                sched
        );
    }

    private List<AppointmentReminder> getInitialReminders() {
        List<AppointmentReminder> list = new ArrayList<>();
        list.add(new AppointmentReminder("REM-101", "APT-1002", "PT-1004",
                "David Sterling", "+1 (555) 987-6543",
                ReminderType.SMS, ReminderStatus.COMPLETED,
                LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(3),
                "GhostQueue Reminder: Orthopedics consult scheduled for today at 09:15 AM. Reply C to confirm.",
                "CONFIRMED", "Delivered via SMS Gateway", LocalDateTime.now().minusHours(5)));
        return list;
    }
}
