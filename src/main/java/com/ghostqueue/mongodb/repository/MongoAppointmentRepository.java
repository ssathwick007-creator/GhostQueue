package com.ghostqueue.mongodb.repository;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.mongodb.MongoConnection;
import com.ghostqueue.repository.AppointmentRepository;
import com.ghostqueue.service.ServiceRegistry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Atlas Repository for Patient Appointments.
 * Persists clinical appointments in MongoDB collection 'appointments' with real-time risk scores.
 */
public class MongoAppointmentRepository implements AppointmentRepository {

    private static final Logger LOGGER = Logger.getLogger(MongoAppointmentRepository.class.getName());
    private static final String COLLECTION = "appointments";
    private final Map<String, Appointment> localCache = new ConcurrentHashMap<>();

    public MongoAppointmentRepository() {
        for (Appointment a : getInitialAppointments()) {
            localCache.put(a.getId(), a);
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
                        for (Appointment a : localCache.values()) {
                            docs.add(toDocument(a));
                        }
                        if (!docs.isEmpty()) {
                            coll.insertMany(docs);
                            LOGGER.info("Seeded " + docs.size() + " appointments into MongoDB Atlas collection '" + COLLECTION + "'");
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MongoDB appointment sync notice: " + e.getMessage());
        }
    }

    @Override
    public List<Appointment> findAll() {
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    List<Appointment> list = new ArrayList<>();
                    for (Document doc : db.getCollection(COLLECTION).find()) {
                        list.add(fromDocument(doc));
                    }
                    if (!list.isEmpty()) {
                        for (Appointment a : list) localCache.put(a.getId(), a);
                        list.sort(Comparator.comparing(Appointment::getAppointmentDate)
                                .thenComparing(Appointment::getAppointmentTime));
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error reading appointments from MongoDB: " + e.getMessage());
        }
        List<Appointment> list = new ArrayList<>(localCache.values());
        list.sort(Comparator.comparing(Appointment::getAppointmentDate)
                .thenComparing(Appointment::getAppointmentTime));
        return list;
    }

    @Override
    public Optional<Appointment> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    Document doc = db.getCollection(COLLECTION).find(Filters.eq("appointment_id", id.trim())).first();
                    if (doc != null) {
                        Appointment a = fromDocument(doc);
                        localCache.put(a.getId(), a);
                        return Optional.of(a);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error finding appointment in MongoDB: " + e.getMessage());
        }
        return Optional.ofNullable(localCache.get(id.trim()));
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        if (date == null) return Collections.emptyList();
        List<Appointment> all = findAll();
        return all.stream().filter(a -> date.equals(a.getAppointmentDate())).toList();
    }

    @Override
    public List<Appointment> findByDepartment(Department department) {
        if (department == null) return findAll();
        return findAll().stream().filter(a -> department.equals(a.getDepartment())).toList();
    }

    @Override
    public List<Appointment> findByStatus(AppointmentStatus status) {
        if (status == null) return findAll();
        return findAll().stream().filter(a -> status.equals(a.getStatus())).toList();
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) {
        if (patientId == null || patientId.isBlank()) return Collections.emptyList();
        return findAll().stream().filter(a -> patientId.equalsIgnoreCase(a.getPatientId())).toList();
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) return Collections.emptyList();
        return findAll().stream().filter(a -> doctorId.equalsIgnoreCase(a.getDoctorId())).toList();
    }

    @Override
    public List<Appointment> filter(String query, Department department, String doctorId, AppointmentStatus status, LocalDate date) {
        List<Appointment> list = findAll();
        return list.stream().filter(a -> {
            if (date != null && !date.equals(a.getAppointmentDate())) return false;
            if (status != null && !status.equals(a.getStatus())) return false;
            if (department != null && department != Department.ALL && !department.equals(a.getDepartment())) return false;
            if (doctorId != null && !doctorId.isBlank() && !"ALL".equalsIgnoreCase(doctorId) && !doctorId.equalsIgnoreCase(a.getDoctorId())) return false;
            if (query != null && !query.isBlank()) {
                String q = query.trim().toLowerCase();
                boolean matchId = a.getId() != null && a.getId().toLowerCase().contains(q);
                boolean matchPat = a.getPatientName() != null && a.getPatientName().toLowerCase().contains(q);
                boolean matchDoc = a.getDoctorName() != null && a.getDoctorName().toLowerCase().contains(q);
                if (!matchId && !matchPat && !matchDoc) return false;
            }
            return true;
        }).toList();
    }

    @Override
    public void save(Appointment appointment) {
        if (appointment == null) return;
        localCache.put(appointment.getId(), appointment);
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).replaceOne(
                            Filters.eq("appointment_id", appointment.getId()),
                            toDocument(appointment),
                            new ReplaceOptions().upsert(true)
                    );
                    LOGGER.info("Saved appointment " + appointment.getId() + " to MongoDB Atlas.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist appointment to MongoDB Atlas: " + e.getMessage());
        }
    }

    @Override
    public void saveTransactional(Appointment appointment, String remarks) {
        save(appointment);
        try {
            AppointmentHistory hist = new AppointmentHistory(
                    0,
                    appointment.getId(),
                    null,
                    appointment.getStatus() != null ? appointment.getStatus().name() : "BOOKED",
                    LocalDateTime.now(),
                    remarks != null ? remarks : "Appointment scheduled"
            );
            ServiceRegistry.getInstance().getHistoryRepository().save(hist);
        } catch (Exception ignored) {}
    }

    @Override
    public void update(Appointment appointment) {
        save(appointment);
    }

    @Override
    public void updateStatus(String appointmentId, AppointmentStatus newStatus, String cancellationReason, String remarks) {
        Optional<Appointment> opt = findById(appointmentId);
        if (opt.isPresent()) {
            Appointment a = opt.get();
            AppointmentStatus prev = a.getStatus();
            a.setStatus(newStatus);
            if (cancellationReason != null) a.setCancellationReason(cancellationReason);
            save(a);
            try {
                AppointmentHistory hist = new AppointmentHistory(
                        0,
                        appointmentId,
                        prev != null ? prev.name() : null,
                        newStatus != null ? newStatus.name() : "BOOKED",
                        LocalDateTime.now(),
                        remarks != null ? remarks : "Status updated to " + newStatus
                );
                ServiceRegistry.getInstance().getHistoryRepository().save(hist);
            } catch (Exception ignored) {}
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
                    db.getCollection(COLLECTION).deleteOne(Filters.eq("appointment_id", id.trim()));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error deleting appointment from MongoDB: " + e.getMessage());
        }
    }

    @Override
    public boolean hasConflictForDoctor(String doctorId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        for (Appointment a : findByDate(date)) {
            if (excludeAppointmentId != null && excludeAppointmentId.equalsIgnoreCase(a.getId())) continue;
            if (doctorId.equalsIgnoreCase(a.getDoctorId()) &&
                a.getAppointmentTime().equals(time) &&
                a.getStatus() != AppointmentStatus.CANCELLED &&
                a.getStatus() != AppointmentStatus.NO_SHOW) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasConflictForPatient(String patientId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        for (Appointment a : findByDate(date)) {
            if (excludeAppointmentId != null && excludeAppointmentId.equalsIgnoreCase(a.getId())) continue;
            if (patientId.equalsIgnoreCase(a.getPatientId()) &&
                a.getAppointmentTime().equals(time) &&
                a.getStatus() != AppointmentStatus.CANCELLED &&
                a.getStatus() != AppointmentStatus.NO_SHOW) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public int countToday() {
        return findByDate(LocalDate.now()).size();
    }

    @Override
    public int countByStatus(AppointmentStatus status) {
        return findByStatus(status).size();
    }

    private Document toDocument(Appointment a) {
        return new Document("appointment_id", a.getId())
                .append("patient_id", a.getPatientId())
                .append("patient_name", a.getPatientName())
                .append("patient_phone", a.getPatientPhone())
                .append("patient_age", a.getPatientAge())
                .append("doctor_id", a.getDoctorId())
                .append("doctor_name", a.getDoctorName())
                .append("department", a.getDepartment() != null ? a.getDepartment().name() : "GENERAL_MEDICINE")
                .append("appointment_date", a.getAppointmentDate() != null ? a.getAppointmentDate().toString() : LocalDate.now().toString())
                .append("appointment_time", a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "09:00")
                .append("appointment_status", a.getStatus() != null ? a.getStatus().name() : "BOOKED")
                .append("cancellation_reason", a.getCancellationReason())
                .append("no_show_probability", a.getPredictedRiskScore())
                .append("risk_level", a.getRiskLevel() != null ? a.getRiskLevel().name() : "LOW")
                .append("lead_time_days", a.getLeadDays());
    }

    private Appointment fromDocument(Document d) {
        String id = d.getString("appointment_id");
        String patId = d.getString("patient_id");
        String patName = d.getString("patient_name");
        String patPhone = d.getString("patient_phone");
        Integer patAge = d.getInteger("patient_age");

        String docId = d.getString("doctor_id");
        String docName = d.getString("doctor_name");
        String deptStr = d.getString("department");
        Department dept = Department.fromString(deptStr);

        LocalDate date = LocalDate.now();
        String ds = d.getString("appointment_date");
        if (ds != null) {
            try { date = LocalDate.parse(ds); } catch (Exception ignored) {}
        }

        LocalTime time = LocalTime.of(9, 0);
        String ts = d.getString("appointment_time");
        if (ts != null) {
            try {
                if (ts.length() > 5) ts = ts.substring(0, 5);
                time = LocalTime.parse(ts);
            } catch (Exception ignored) {}
        }

        String stStr = d.getString("appointment_status");
        AppointmentStatus status = AppointmentStatus.fromString(stStr);
        String reason = d.getString("cancellation_reason");

        Double prob = d.getDouble("no_show_probability");
        String riskStr = d.getString("risk_level");
        RiskLevel risk = RiskLevel.fromString(riskStr);
        Integer leadTime = d.getInteger("lead_time_days");

        return makeFallback(id, patId, patName, patAge != null ? patAge : 40, patPhone != null ? patPhone : "",
                docId, docName, dept, date, time, status, reason, leadTime != null ? leadTime : 7,
                prob != null ? prob : 0.20, risk != null ? risk : RiskLevel.LOW);
    }

    private static Appointment makeFallback(String id, String patId, String patName, int age, String phone,
                                            String docId, String docName, Department dept,
                                            LocalDate date, LocalTime time, AppointmentStatus status,
                                            String reason, int leadTime, double prob, RiskLevel risk) {
        Appointment a = new Appointment(id, patId, docId, date, time, status, date.minusDays(leadTime), reason);
        a.setPatientName(patName);
        a.setPatientAge(age);
        a.setPatientPhone(phone);
        a.setDoctorName(docName);
        a.setDepartment(dept);
        a.setLeadDays(leadTime);
        a.setPredictedRiskScore(prob);
        return a;
    }

    private List<Appointment> getInitialAppointments() {
        LocalDate today = LocalDate.now();
        List<Appointment> list = new ArrayList<>();
        list.add(makeFallback("APT-1001", "PT-1001", "Eleanor Vance", 58, "+1 (555) 234-8910", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today, LocalTime.of(8, 30), AppointmentStatus.COMPLETED, "BP stable 128/82", 14, 0.15, RiskLevel.LOW));
        list.add(makeFallback("APT-1002", "PT-1004", "David Sterling", 27, "+1 (555) 987-6543", "DOC-103", "Dr. Marcus Thorne", Department.ORTHOPEDICS, today, LocalTime.of(9, 15), AppointmentStatus.CANCELLED, "Patient transport breakdown", 30, 0.85, RiskLevel.HIGH));
        list.add(makeFallback("APT-1003", "PT-1003", "Sophia Rodriguez", 40, "+1 (555) 345-6789", "DOC-102", "Dr. Evelyn Wright", Department.CARDIOLOGY, today, LocalTime.of(10, 0), AppointmentStatus.CONFIRMED, "Cardio check", 7, 0.22, RiskLevel.LOW));
        list.add(makeFallback("APT-1004", "PT-1007", "Chloe Bennett", 31, "+1 (555) 789-0123", "DOC-106", "Dr. Tariq Al-Mansoor", Department.GENERAL_MEDICINE, today, LocalTime.of(11, 0), AppointmentStatus.CONFIRMED, "Annual physical", 25, 0.45, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1005", "PT-1008", "Samuel O'Connor", 69, "+1 (555) 890-1234", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today, LocalTime.of(11, 45), AppointmentStatus.CONFIRMED, "Post-stent surveillance", 10, 0.28, RiskLevel.LOW));
        list.add(makeFallback("APT-1006", "PT-1010", "Brandon Lee", 56, "+1 (555) 012-3456", "DOC-104", "Dr. Priya Patel", Department.NEUROLOGY, today, LocalTime.of(13, 30), AppointmentStatus.BOOKED, "Neurology review", 35, 0.72, RiskLevel.HIGH));
        list.add(makeFallback("APT-1007", "PT-1006", "Lucas Chen", 7, "+1 (555) 678-9012", "DOC-105", "Dr. Sarah Chen", Department.PEDIATRICS, today, LocalTime.of(14, 15), AppointmentStatus.CONFIRMED, "Pediatric allergy test", 4, 0.18, RiskLevel.LOW));
        list.add(makeFallback("APT-1008", "PT-1013", "Ada Lovelace", 48, "+1 (555) 345-6780", "DOC-102", "Dr. Evelyn Wright", Department.CARDIOLOGY, today, LocalTime.of(15, 0), AppointmentStatus.BOOKED, "Routine consult", 28, 0.55, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1009", "PT-1014", "Charles Babbage", 37, "+1 (555) 456-7891", "DOC-106", "Dr. Tariq Al-Mansoor", Department.GENERAL_MEDICINE, today, LocalTime.of(15, 45), AppointmentStatus.CONFIRMED, "Routine checkup", 5, 0.20, RiskLevel.LOW));
        list.add(makeFallback("APT-1010", "PT-1011", "Grace Hopper", 26, "+1 (555) 123-4567", "DOC-106", "Dr. Tariq Al-Mansoor", Department.GENERAL_MEDICINE, today, LocalTime.of(16, 30), AppointmentStatus.NO_SHOW, "Unannounced absence", 18, 0.78, RiskLevel.HIGH));

        list.add(makeFallback("APT-1011", "PT-1002", "Liam Gallagher", 52, "+1 (555) 876-1234", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today.plusDays(1), LocalTime.of(9, 0), AppointmentStatus.CONFIRMED, "Hypertension follow-up", 12, 0.25, RiskLevel.LOW));
        list.add(makeFallback("APT-1012", "PT-1005", "Amina Al-Mansoor", 35, "+1 (555) 456-7890", "DOC-104", "Dr. Priya Patel", Department.NEUROLOGY, today.plusDays(1), LocalTime.of(10, 30), AppointmentStatus.CONFIRMED, "Headache consult", 8, 0.19, RiskLevel.LOW));
        list.add(makeFallback("APT-1013", "PT-1012", "Alan Turing", 61, "+1 (555) 234-5678", "DOC-103", "Dr. Marcus Thorne", Department.ORTHOPEDICS, today.plusDays(1), LocalTime.of(11, 15), AppointmentStatus.CONFIRMED, "Total knee evaluation", 9, 0.32, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1014", "PT-1015", "Margaret Hamilton", 60, "+1 (555) 567-8902", "DOC-104", "Dr. Priya Patel", Department.NEUROLOGY, today.plusDays(1), LocalTime.of(13, 0), AppointmentStatus.BOOKED, "Follow-up review", 15, 0.40, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1015", "PT-1009", "Fatima Zahra", 44, "+1 (555) 901-2345", "DOC-102", "Dr. Evelyn Wright", Department.CARDIOLOGY, today.plusDays(1), LocalTime.of(14, 0), AppointmentStatus.CONFIRMED, "Follow-up", 11, 0.24, RiskLevel.LOW));
        list.add(makeFallback("APT-1016", "PT-1001", "Eleanor Vance", 58, "+1 (555) 234-8910", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today.plusDays(1), LocalTime.of(15, 30), AppointmentStatus.BOOKED, "Echocardiogram consult", 14, 0.38, RiskLevel.MEDIUM));

        list.add(makeFallback("APT-1025", "PT-1001", "Eleanor Vance", 58, "+1 (555) 234-8910", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today.minusDays(28), LocalTime.of(9, 0), AppointmentStatus.COMPLETED, "Echocardiogram normal", 45, 0.12, RiskLevel.LOW));
        list.add(makeFallback("APT-1026", "PT-1002", "Liam Gallagher", 52, "+1 (555) 876-1234", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today.minusDays(21), LocalTime.of(10, 0), AppointmentStatus.NO_SHOW, "Unreported absence", 35, 0.82, RiskLevel.HIGH));
        list.add(makeFallback("APT-1027", "PT-1008", "Samuel O'Connor", 69, "+1 (555) 890-1234", "DOC-101", "Dr. Alexander Vance", Department.CARDIOLOGY, today.minusDays(14), LocalTime.of(11, 0), AppointmentStatus.COMPLETED, "Stent check clear", 24, 0.15, RiskLevel.LOW));
        list.add(makeFallback("APT-1028", "PT-1012", "Alan Turing", 61, "+1 (555) 234-5678", "DOC-103", "Dr. Marcus Thorne", Department.ORTHOPEDICS, today.minusDays(10), LocalTime.of(14, 0), AppointmentStatus.COMPLETED, "Post-op flexion clear", 20, 0.20, RiskLevel.LOW));
        list.add(makeFallback("APT-1029", "PT-1004", "David Sterling", 27, "+1 (555) 987-6543", "DOC-103", "Dr. Marcus Thorne", Department.ORTHOPEDICS, today.minusDays(7), LocalTime.of(15, 0), AppointmentStatus.CANCELLED, "Work conflict", 14, 0.65, RiskLevel.HIGH));
        list.add(makeFallback("APT-1030", "PT-1014", "Charles Babbage", 37, "+1 (555) 456-7891", "DOC-106", "Dr. Tariq Al-Mansoor", Department.GENERAL_MEDICINE, today.minusDays(3), LocalTime.of(9, 30), AppointmentStatus.COMPLETED, "Annual physical completed", 10, 0.14, RiskLevel.LOW));
        return list;
    }
}
