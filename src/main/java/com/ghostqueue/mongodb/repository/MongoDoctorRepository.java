package com.ghostqueue.mongodb.repository;

import com.ghostqueue.model.Doctor;
import com.ghostqueue.mongodb.MongoConnection;
import com.ghostqueue.repository.DoctorRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Atlas Repository for Attending Physicians.
 * Persists and retrieves medical staff in MongoDB collection 'doctors'.
 */
public class MongoDoctorRepository implements DoctorRepository {

    private static final Logger LOGGER = Logger.getLogger(MongoDoctorRepository.class.getName());
    private static final String COLLECTION = "doctors";
    private final Map<String, Doctor> localCache = new ConcurrentHashMap<>();

    public MongoDoctorRepository() {
        for (Doctor d : getInitialDoctors()) {
            localCache.put(d.getId(), d);
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
                        for (Doctor d : localCache.values()) {
                            docs.add(toDocument(d));
                        }
                        if (!docs.isEmpty()) {
                            coll.insertMany(docs);
                            LOGGER.info("Seeded " + docs.size() + " physicians into MongoDB Atlas collection '" + COLLECTION + "'");
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MongoDB physician sync notice: " + e.getMessage());
        }
    }

    @Override
    public List<Doctor> findAll() {
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    List<Doctor> list = new ArrayList<>();
                    for (Document doc : db.getCollection(COLLECTION).find()) {
                        list.add(fromDocument(doc));
                    }
                    if (!list.isEmpty()) {
                        for (Doctor d : list) localCache.put(d.getId(), d);
                        list.sort(Comparator.comparing(Doctor::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)));
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error reading doctors from MongoDB: " + e.getMessage());
        }
        List<Doctor> list = new ArrayList<>(localCache.values());
        list.sort(Comparator.comparing(Doctor::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return list;
    }

    @Override
    public Optional<Doctor> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    Document doc = db.getCollection(COLLECTION).find(Filters.eq("doctor_id", id.trim())).first();
                    if (doc != null) {
                        Doctor d = fromDocument(doc);
                        localCache.put(d.getId(), d);
                        return Optional.of(d);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error finding doctor in MongoDB: " + e.getMessage());
        }
        return Optional.ofNullable(localCache.get(id.trim()));
    }

    @Override
    public List<Doctor> findByDepartment(String department) {
        if (department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)) {
            return findAll();
        }
        List<Doctor> all = findAll();
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : all) {
            if (department.equalsIgnoreCase(d.getDepartment())) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public List<Doctor> search(String query) {
        if (query == null || query.isBlank()) return findAll();
        String q = query.trim().toLowerCase();
        List<Doctor> all = findAll();
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : all) {
            if ((d.getId() != null && d.getId().toLowerCase().contains(q)) ||
                (d.getFullName() != null && d.getFullName().toLowerCase().contains(q)) ||
                (d.getDepartment() != null && d.getDepartment().toLowerCase().contains(q)) ||
                (d.getSpecialization() != null && d.getSpecialization().toLowerCase().contains(q))) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public void save(Doctor doctor) {
        if (doctor == null) return;
        localCache.put(doctor.getId(), doctor);
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).replaceOne(
                            Filters.eq("doctor_id", doctor.getId()),
                            toDocument(doctor),
                            new ReplaceOptions().upsert(true)
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist doctor to MongoDB: " + e.getMessage());
        }
    }

    @Override
    public void update(Doctor doctor) {
        save(doctor);
    }

    @Override
    public void updateAvailability(String doctorId, String status) {
        if (doctorId == null) return;
        Doctor doc = localCache.get(doctorId.trim());
        if (doc != null) {
            Doctor updated = new Doctor(doc.getId(), doc.getFullName(), doc.getDepartment(), doc.getSpecialization(), doc.getPhone(), status != null ? status : "AVAILABLE");
            save(updated);
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    private Document toDocument(Doctor d) {
        return new Document("doctor_id", d.getId())
                .append("full_name", d.getFullName())
                .append("department", d.getDepartment())
                .append("specialization", d.getSpecialization())
                .append("phone", d.getPhone())
                .append("availability_status", d.isAvailable() ? "AVAILABLE" : "UNAVAILABLE");
    }

    private Doctor fromDocument(Document d) {
        String status = d.getString("availability_status");
        return new Doctor(
                d.getString("doctor_id"),
                d.getString("full_name"),
                d.getString("department"),
                d.getString("specialization"),
                d.getString("phone"),
                status != null ? status : "AVAILABLE"
        );
    }

    private List<Doctor> getInitialDoctors() {
        return List.of(
            new Doctor("DOC-101", "Dr. Alexander Vance", "CARDIOLOGY", "Interventional Cardiology", "+1 (555) 234-5678", "AVAILABLE"),
            new Doctor("DOC-102", "Dr. Evelyn Wright", "CARDIOLOGY", "Pediatric Cardiology", "+1 (555) 345-6789", "AVAILABLE"),
            new Doctor("DOC-103", "Dr. Marcus Thorne", "ORTHOPEDICS", "Joint Reconstruction", "+1 (555) 456-7890", "AVAILABLE"),
            new Doctor("DOC-104", "Dr. Priya Patel", "NEUROLOGY", "Neurodegenerative Diseases", "+1 (555) 567-8901", "AVAILABLE"),
            new Doctor("DOC-105", "Dr. Sarah Chen", "PEDIATRICS", "General Pediatrics & Neonatology", "+1 (555) 678-9012", "AVAILABLE"),
            new Doctor("DOC-106", "Dr. Tariq Al-Mansoor", "GENERAL_MEDICINE", "Internal & Preventative Medicine", "+1 (555) 789-0123", "AVAILABLE")
        );
    }
}
