package com.ghostqueue.mongodb.repository;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.ReallocationHistory;
import com.ghostqueue.mongodb.MongoConnection;
import com.ghostqueue.repository.ReallocationHistoryRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Atlas Repository for Smart Slot Reallocation Ledger.
 * Persists reallocation decisions in MongoDB collection 'reallocation_history'.
 */
public class MongoReallocationHistoryRepository implements ReallocationHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(MongoReallocationHistoryRepository.class.getName());
    private static final String COLLECTION = "reallocation_history";
    private final List<ReallocationHistory> localCache = new CopyOnWriteArrayList<>();

    public MongoReallocationHistoryRepository() {
        for (ReallocationHistory r : getInitialReallocations()) {
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
                        for (ReallocationHistory r : localCache) {
                            docs.add(toDocument(r));
                        }
                        if (!docs.isEmpty()) {
                            coll.insertMany(docs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MongoDB reallocation sync notice: " + e.getMessage());
        }
    }

    @Override
    public List<ReallocationHistory> findAll() {
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    List<ReallocationHistory> list = new ArrayList<>();
                    for (Document doc : db.getCollection(COLLECTION).find()) {
                        list.add(fromDocument(doc));
                    }
                    if (!list.isEmpty()) {
                        list.sort(Comparator.comparing(ReallocationHistory::getDecidedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error reading reallocations from MongoDB: " + e.getMessage());
        }
        List<ReallocationHistory> list = new ArrayList<>(localCache);
        list.sort(Comparator.comparing(ReallocationHistory::getDecidedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    @Override
    public List<ReallocationHistory> findByAppointmentId(String appointmentId) {
        if (appointmentId == null) return Collections.emptyList();
        return findAll().stream().filter(r -> appointmentId.equalsIgnoreCase(r.getAppointmentId())).toList();
    }

    public List<ReallocationHistory> findByCandidatePatientId(String patientId) {
        if (patientId == null) return Collections.emptyList();
        return findAll().stream().filter(r -> patientId.equalsIgnoreCase(r.getCandidatePatientId())).toList();
    }

    public List<ReallocationHistory> filter(String query, String decision, LocalDate date) {
        return findAll().stream().filter(r -> {
            if (decision != null && !"ALL".equalsIgnoreCase(decision) && !decision.equalsIgnoreCase(r.getDecision())) return false;
            if (date != null && !date.equals(r.getSlotDate())) return false;
            if (query != null && !query.isBlank()) {
                String q = query.trim().toLowerCase();
                boolean mId = r.getReallocationId() != null && r.getReallocationId().toLowerCase().contains(q);
                boolean mPat = r.getCandidatePatientName() != null && r.getCandidatePatientName().toLowerCase().contains(q);
                if (!mId && !mPat) return false;
            }
            return true;
        }).toList();
    }

    @Override
    public void save(ReallocationHistory history) {
        if (history == null) return;
        localCache.add(0, history);
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).insertOne(toDocument(history));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist reallocation history: " + e.getMessage());
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public int countApproved() {
        return (int) findAll().stream().filter(r -> "APPROVED".equalsIgnoreCase(r.getDecision())).count();
    }

    public int countDeclined() {
        return (int) findAll().stream().filter(r -> "DECLINED".equalsIgnoreCase(r.getDecision())).count();
    }

    private Document toDocument(ReallocationHistory r) {
        return new Document("reallocation_id", r.getReallocationId())
                .append("appointment_id", r.getAppointmentId())
                .append("queue_id", r.getQueueId())
                .append("candidate_patient_id", r.getCandidatePatientId())
                .append("candidate_patient_name", r.getCandidatePatientName())
                .append("doctor_id", r.getDoctorId())
                .append("doctor_name", r.getDoctorName())
                .append("department", r.getDepartment() != null ? r.getDepartment().name() : "GENERAL_MEDICINE")
                .append("slot_date", r.getSlotDate() != null ? r.getSlotDate().toString() : LocalDate.now().toString())
                .append("slot_time", r.getSlotTime() != null ? r.getSlotTime().toString() : "09:00")
                .append("match_score", r.getMatchScore())
                .append("match_reason", r.getMatchReason())
                .append("decision", r.getDecision())
                .append("decision_notes", r.getDecisionNotes())
                .append("decided_at", r.getDecidedAt() != null ? r.getDecidedAt().toString() : LocalDateTime.now().toString());
    }

    private ReallocationHistory fromDocument(Document d) {
        LocalDate date = LocalDate.now();
        String ds = d.getString("slot_date");
        if (ds != null) {
            try { date = LocalDate.parse(ds); } catch (Exception ignored) {}
        }

        LocalTime time = LocalTime.of(9, 0);
        String ts = d.getString("slot_time");
        if (ts != null) {
            try { time = LocalTime.parse(ts); } catch (Exception ignored) {}
        }

        LocalDateTime decidedAt = LocalDateTime.now();
        String decs = d.getString("decided_at");
        if (decs != null) {
            try { decidedAt = LocalDateTime.parse(decs); } catch (Exception ignored) {}
        }

        Integer score = d.getInteger("match_score");

        return new ReallocationHistory(
                d.getString("reallocation_id"),
                d.getString("appointment_id"),
                d.getString("queue_id"),
                d.getString("candidate_patient_id"),
                d.getString("candidate_patient_name"),
                d.getString("doctor_id"),
                d.getString("doctor_name"),
                Department.fromString(d.getString("department")),
                date,
                time,
                score != null ? score : 90,
                d.getString("match_reason"),
                d.getString("decision"),
                d.getString("decision_notes"),
                decidedAt,
                decidedAt
        );
    }

    private List<ReallocationHistory> getInitialReallocations() {
        LocalDate today = LocalDate.now();
        List<ReallocationHistory> list = new ArrayList<>();
        ReallocationHistory r1 = new ReallocationHistory("REAL-101", "APT-1002", "WQ-101", "PT-1004", "David Sterling",
                "DOC-103", "Dr. Marcus Thorne", Department.ORTHOPEDICS,
                today, LocalTime.of(9, 15), 94, "Urgent queue priority + specialty match (Orthopedics)", "APPROVED",
                "Administrative approval by Dr. Sarah Chen", LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(2));
        list.add(r1);
        return list;
    }
}
