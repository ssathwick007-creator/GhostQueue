package com.ghostqueue.mongodb.repository;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.mongodb.MongoConnection;
import com.ghostqueue.repository.QueueRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Atlas Repository for Waiting Queue Standby Patients.
 * Persists entries in MongoDB collection 'waiting_queue'.
 */
public class MongoQueueRepository implements QueueRepository {

    private static final Logger LOGGER = Logger.getLogger(MongoQueueRepository.class.getName());
    private static final String COLLECTION = "waiting_queue";
    private final Map<String, QueueEntry> localCache = new ConcurrentHashMap<>();

    public MongoQueueRepository() {
        for (QueueEntry q : getInitialQueueEntries()) {
            localCache.put(q.getQueueId(), q);
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
                        for (QueueEntry q : localCache.values()) {
                            docs.add(toDocument(q));
                        }
                        if (!docs.isEmpty()) {
                            coll.insertMany(docs);
                            LOGGER.info("Seeded " + docs.size() + " queue standbys into MongoDB Atlas collection '" + COLLECTION + "'");
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MongoDB queue sync notice: " + e.getMessage());
        }
    }

    @Override
    public List<QueueEntry> findAll() {
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    List<QueueEntry> list = new ArrayList<>();
                    for (Document doc : db.getCollection(COLLECTION).find()) {
                        list.add(fromDocument(doc));
                    }
                    if (!list.isEmpty()) {
                        for (QueueEntry q : list) localCache.put(q.getQueueId(), q);
                        list.sort(Comparator.comparing(QueueEntry::getRegisteredTime));
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error reading queue from MongoDB: " + e.getMessage());
        }
        List<QueueEntry> list = new ArrayList<>(localCache.values());
        list.sort(Comparator.comparing(QueueEntry::getRegisteredTime));
        return list;
    }

    @Override
    public Optional<QueueEntry> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    Document doc = db.getCollection(COLLECTION).find(Filters.eq("queue_id", id.trim())).first();
                    if (doc != null) {
                        QueueEntry q = fromDocument(doc);
                        localCache.put(q.getQueueId(), q);
                        return Optional.of(q);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error finding queue entry in MongoDB: " + e.getMessage());
        }
        return Optional.ofNullable(localCache.get(id.trim()));
    }

    @Override
    public List<QueueEntry> findByDepartment(Department department) {
        if (department == null) return findAll();
        return findAll().stream().filter(q -> department == q.getDepartment()).toList();
    }

    @Override
    public List<QueueEntry> findByPriority(QueuePriority priority) {
        if (priority == null) return findAll();
        return findAll().stream().filter(q -> priority == q.getPriorityLevel()).toList();
    }

    @Override
    public List<QueueEntry> findByStatus(QueueStatus status) {
        if (status == null) return findAll();
        return findAll().stream().filter(q -> status == q.getQueueStatus()).toList();
    }

    @Override
    public List<QueueEntry> filter(String query, Department department, QueuePriority priority, QueueStatus status) {
        return filter(query, department, null, priority, status);
    }

    @Override
    public List<QueueEntry> filter(String query, Department department, String doctorId, QueuePriority priority, QueueStatus status) {
        List<QueueEntry> all = findAll();
        List<QueueEntry> filtered = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : null;

        for (QueueEntry e : all) {
            if (q != null && !q.isEmpty()) {
                boolean matchId = e.getQueueId() != null && e.getQueueId().toLowerCase().contains(q);
                boolean matchPat = e.getPatientName() != null && e.getPatientName().toLowerCase().contains(q);
                boolean matchDoc = e.getDoctorName() != null && e.getDoctorName().toLowerCase().contains(q);
                if (!matchId && !matchPat && !matchDoc) continue;
            }
            if (department != null && department != Department.ALL && e.getDepartment() != department) continue;
            if (doctorId != null && !doctorId.isBlank() && !"ALL".equalsIgnoreCase(doctorId) && !doctorId.equalsIgnoreCase(e.getDoctorId())) continue;
            if (priority != null && e.getPriorityLevel() != priority) continue;
            if (status != null && e.getQueueStatus() != status) continue;

            filtered.add(e);
        }
        return filtered;
    }

    @Override
    public void save(QueueEntry entry) {
        if (entry == null) return;
        localCache.put(entry.getQueueId(), entry);
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).replaceOne(
                            Filters.eq("queue_id", entry.getQueueId()),
                            toDocument(entry),
                            new ReplaceOptions().upsert(true)
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist queue entry to MongoDB: " + e.getMessage());
        }
    }

    @Override
    public void update(QueueEntry entry) {
        save(entry);
    }

    @Override
    public void updateStatus(String queueId, QueueStatus status) {
        Optional<QueueEntry> opt = findById(queueId);
        if (opt.isPresent()) {
            QueueEntry q = opt.get();
            q.setQueueStatus(status);
            save(q);
        }
    }

    @Override
    public void updatePriority(String queueId, QueuePriority priority) {
        Optional<QueueEntry> opt = findById(queueId);
        if (opt.isPresent()) {
            QueueEntry q = opt.get();
            q.setPriority(priority);
            save(q);
        }
    }

    @Override
    public void delete(String id) {
        if (id == null) return;
        localCache.remove(id.trim());
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).deleteOne(Filters.eq("queue_id", id.trim()));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error deleting queue entry: " + e.getMessage());
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public int countActiveWaiting() {
        return (int) findAll().stream()
                .filter(q -> q.getQueueStatus() == QueueStatus.WAITING || q.getQueueStatus() == QueueStatus.STANDBY)
                .count();
    }

    @Override
    public int countUrgentPriority() {
        return (int) findAll().stream()
                .filter(q -> q.getPriorityLevel() == QueuePriority.URGENT)
                .count();
    }

    private Document toDocument(QueueEntry q) {
        return new Document("queue_id", q.getQueueId())
                .append("patient_id", q.getPatientId())
                .append("patient_name", q.getPatientName())
                .append("patient_phone", q.getPatientPhone())
                .append("doctor_id", q.getDoctorId())
                .append("doctor_name", q.getDoctorName())
                .append("department", q.getRequestedDepartment() != null ? q.getRequestedDepartment().name() : "GENERAL_MEDICINE")
                .append("requested_date", q.getRequestedDate() != null ? q.getRequestedDate().toString() : LocalDate.now().toString())
                .append("preferred_time", q.getPreferredTimeSlot())
                .append("priority_level", q.getPriorityLevel() != null ? q.getPriorityLevel().name() : "STANDARD")
                .append("queue_status", q.getQueueStatus() != null ? q.getQueueStatus().name() : "WAITING")
                .append("notes", q.getNotes())
                .append("created_at", q.getRegisteredTime() != null ? q.getRegisteredTime().toString() : LocalDateTime.now().toString());
    }

    private QueueEntry fromDocument(Document d) {
        String id = d.getString("queue_id");
        String patId = d.getString("patient_id");
        String patName = d.getString("patient_name");
        String patPhone = d.getString("patient_phone");
        String docId = d.getString("doctor_id");
        String docName = d.getString("doctor_name");
        Department dept = Department.fromString(d.getString("department"));

        LocalDate date = LocalDate.now();
        String ds = d.getString("requested_date");
        if (ds != null) {
            try { date = LocalDate.parse(ds); } catch (Exception ignored) {}
        }

        String prefTime = d.getString("preferred_time");
        QueuePriority priority = QueuePriority.fromString(d.getString("priority_level"));
        QueueStatus status = QueueStatus.fromString(d.getString("queue_status"));
        String notes = d.getString("notes");

        LocalDateTime createdAt = LocalDateTime.now();
        String cs = d.getString("created_at");
        if (cs != null) {
            try { createdAt = LocalDateTime.parse(cs); } catch (Exception ignored) {}
        }

        QueueEntry e = new QueueEntry(id, patId, docId, date, prefTime, priority, status, createdAt, notes);
        e.setPatientName(patName);
        e.setPatientPhone(patPhone);
        e.setDoctorName(docName);
        e.setRequestedDepartment(dept);
        e.setRegisteredTime(createdAt);
        return e;
    }

    private List<QueueEntry> getInitialQueueEntries() {
        LocalDate today = LocalDate.now();
        List<QueueEntry> list = new ArrayList<>();
        list.add(makeEntry("WQ-101", "PT-1004", "David Sterling", "+1 (555) 987-6543", "DOC-103", "Dr. Marcus Thorne", Department.ORTHOPEDICS, today, "Morning (09:00 - 12:00)", QueuePriority.URGENT, QueueStatus.STANDBY, "Severe knee swelling post-injury", LocalDateTime.now().minusHours(3)));
        list.add(makeEntry("WQ-102", "PT-1007", "Chloe Bennett", "+1 (555) 789-0123", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today, "Anytime (Immediate Standby)", QueuePriority.HIGH, QueueStatus.WAITING, "Arrhythmia palpitations follow-up", LocalDateTime.now().minusHours(4)));
        list.add(makeEntry("WQ-103", "PT-1003", "Sophia Rodriguez", "+1 (555) 345-6789", "DOC-102", "Dr. Evelyn Wright", Department.CARDIOLOGY, today, "Afternoon (13:00 - 17:00)", QueuePriority.STANDARD, QueueStatus.WAITING, "Routine cardiovascular screening", LocalDateTime.now().minusHours(5)));
        list.add(makeEntry("WQ-104", "PT-1005", "Amina Al-Mansoor", "+1 (555) 456-7890", "DOC-104", "Dr. Priya Patel", Department.NEUROLOGY, today, "Anytime (Immediate Standby)", QueuePriority.HIGH, QueueStatus.STANDBY, "Migraine aura cluster consult", LocalDateTime.now().minusHours(2)));
        list.add(makeEntry("WQ-105", "PT-1006", "Lucas Chen", "+1 (555) 678-9012", "DOC-105", "Dr. Sarah Chen", Department.PEDIATRICS, today.plusDays(1), "Afternoon", QueuePriority.STANDARD, QueueStatus.WAITING, "Childhood asthma maintenance", LocalDateTime.now().minusHours(6)));
        list.add(makeEntry("WQ-106", "PT-1011", "Grace Hopper", "+1 (555) 123-4567", "DOC-106", "Dr. Tariq Al-Mansoor", Department.GENERAL_MEDICINE, today, "Anytime", QueuePriority.URGENT, QueueStatus.OFFERED, "Acute fever and persistent cough", LocalDateTime.now().minusMinutes(45)));
        list.add(makeEntry("WQ-107", "PT-1014", "Charles Babbage", "+1 (555) 456-7891", "DOC-106", "Dr. Tariq Al-Mansoor", Department.GENERAL_MEDICINE, today.plusDays(1), "Morning", QueuePriority.STANDARD, QueueStatus.WAITING, "Prescription refill assessment", LocalDateTime.now().minusHours(7)));
        list.add(makeEntry("WQ-108", "PT-1002", "Liam Gallagher", "+1 (555) 876-1234", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today, "Morning", QueuePriority.HIGH, QueueStatus.WAITING, "Elevated systolic pressure tracking", LocalDateTime.now().minusHours(1)));
        return list;
    }

    private static QueueEntry makeEntry(String qId, String patId, String patName, String patPhone,
                                        String docId, String docName, Department dept,
                                        LocalDate reqDate, String prefTime, QueuePriority priority,
                                        QueueStatus status, String notes, LocalDateTime created) {
        QueueEntry e = new QueueEntry(qId, patId, docId, reqDate, prefTime, priority, status, created, notes);
        e.setPatientName(patName);
        e.setPatientPhone(patPhone);
        e.setDoctorName(docName);
        e.setRequestedDepartment(dept);
        e.setRegisteredTime(created);
        return e;
    }
}
