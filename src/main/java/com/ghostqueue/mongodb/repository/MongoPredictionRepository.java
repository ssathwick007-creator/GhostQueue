package com.ghostqueue.mongodb.repository;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.mongodb.MongoConnection;
import com.ghostqueue.prediction.FactorExplanation;
import com.ghostqueue.prediction.PredictionRepository;
import com.ghostqueue.prediction.PredictionResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB Atlas Repository for Machine Learning No-Show Predictions.
 * Persists predictions in MongoDB collection 'predictions'.
 */
public class MongoPredictionRepository implements PredictionRepository {

    private static final Logger LOGGER = Logger.getLogger(MongoPredictionRepository.class.getName());
    private static final String COLLECTION = "predictions";
    private final Map<String, PredictionResult> localCache = new ConcurrentHashMap<>();

    public MongoPredictionRepository() {
        syncToMongoIfConnected();
    }

    private void syncToMongoIfConnected() {
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    MongoCollection<Document> coll = db.getCollection(COLLECTION);
                    if (coll.countDocuments() > 0) {
                        for (Document doc : coll.find()) {
                            PredictionResult r = fromDocument(doc);
                            localCache.put(r.getAppointmentId(), r);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MongoDB prediction sync notice: " + e.getMessage());
        }
    }

    @Override
    public void save(PredictionResult result) {
        if (result == null || result.getAppointmentId() == null) return;
        localCache.put(result.getAppointmentId(), result);
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    db.getCollection(COLLECTION).replaceOne(
                            Filters.eq("appointment_id", result.getAppointmentId()),
                            toDocument(result),
                            new ReplaceOptions().upsert(true)
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist prediction to MongoDB: " + e.getMessage());
        }
    }

    @Override
    public void saveBatch(List<PredictionResult> results) {
        if (results == null || results.isEmpty()) return;
        for (PredictionResult r : results) {
            localCache.put(r.getAppointmentId(), r);
        }
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    List<WriteModel<Document>> ops = new ArrayList<>();
                    for (PredictionResult r : results) {
                        ops.add(new ReplaceOneModel<>(
                                Filters.eq("appointment_id", r.getAppointmentId()),
                                toDocument(r),
                                new ReplaceOptions().upsert(true)
                        ));
                    }
                    db.getCollection(COLLECTION).bulkWrite(ops);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to bulk persist predictions to MongoDB: " + e.getMessage());
        }
    }

    @Override
    public Optional<PredictionResult> findById(String predictionId) {
        if (predictionId == null || predictionId.isBlank()) return Optional.empty();
        for (PredictionResult r : localCache.values()) {
            if (predictionId.equalsIgnoreCase(r.getPredictionId())) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PredictionResult> findLatestByAppointmentId(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) return Optional.empty();
        try {
            if (MongoConnection.isAtlasAvailable()) {
                MongoDatabase db = MongoConnection.getDatabase();
                if (db != null) {
                    Document doc = db.getCollection(COLLECTION).find(Filters.eq("appointment_id", appointmentId.trim())).first();
                    if (doc != null) {
                        PredictionResult r = fromDocument(doc);
                        localCache.put(r.getAppointmentId(), r);
                        return Optional.of(r);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error finding prediction by appointment: " + appointmentId);
        }
        return Optional.ofNullable(localCache.get(appointmentId.trim()));
    }

    @Override
    public List<PredictionResult> findAll() {
        return new ArrayList<>(localCache.values());
    }

    @Override
    public List<PredictionResult> findLatestPredictions() {
        return findAll();
    }

    @Override
    public List<PredictionResult> filter(String query, Department department, String doctorId, RiskLevel riskLevel, LocalDate date) {
        List<PredictionResult> all = findAll();
        return all.stream().filter(p -> {
            if (riskLevel != null && p.getRiskLevel() != riskLevel) return false;
            if (query != null && !query.isBlank()) {
                String q = query.trim().toLowerCase();
                boolean mAppt = p.getAppointmentId() != null && p.getAppointmentId().toLowerCase().contains(q);
                boolean mPat = p.getPatientName() != null && p.getPatientName().toLowerCase().contains(q);
                if (!mAppt && !mPat) return false;
            }
            return true;
        }).toList();
    }

    @Override
    public void updateActualOutcome(String predictionId, String outcome, String evaluationStatus) {
        Optional<PredictionResult> opt = findById(predictionId);
        if (opt.isPresent()) {
            PredictionResult r = opt.get();
            r.setActualOutcome(outcome);
            r.setEvaluationStatus(evaluationStatus);
            save(r);
        }
    }

    @Override
    public int countTotal() {
        return localCache.size();
    }

    @Override
    public int countByRiskLevel(RiskLevel riskLevel) {
        return (int) localCache.values().stream().filter(r -> r.getRiskLevel() == riskLevel).count();
    }

    @Override
    public double getAverageProbability() {
        if (localCache.isEmpty()) return 0.25;
        return localCache.values().stream().mapToDouble(PredictionResult::getNoShowProbability).average().orElse(0.25);
    }

    private Document toDocument(PredictionResult r) {
        return new Document("prediction_id", r.getPredictionId())
                .append("appointment_id", r.getAppointmentId())
                .append("patient_id", r.getPatientId())
                .append("patient_name", r.getPatientName())
                .append("doctor_name", r.getDoctorName())
                .append("department", r.getDepartment() != null ? r.getDepartment().name() : "GENERAL_MEDICINE")
                .append("appointment_date", r.getAppointmentDate() != null ? r.getAppointmentDate().toString() : LocalDate.now().toString())
                .append("appointment_time", r.getAppointmentTime() != null ? r.getAppointmentTime().toString() : "09:00")
                .append("no_show_probability", r.getNoShowProbability())
                .append("risk_level", r.getRiskLevel() != null ? r.getRiskLevel().name() : "LOW")
                .append("model_version", r.getModelVersion())
                .append("prediction_method", r.getPredictionMethod())
                .append("contributing_factors", serializeFactors(r.getContributingFactors()))
                .append("suggested_action", r.getSuggestedAction())
                .append("predicted_at", r.getPredictedAt() != null ? r.getPredictedAt().toString() : LocalDateTime.now().toString())
                .append("actual_outcome", r.getActualOutcome())
                .append("evaluation_status", r.getEvaluationStatus())
                .append("historical_summary", r.getHistoricalAttendanceSummary());
    }

    private PredictionResult fromDocument(Document d) {
        List<FactorExplanation> factors = deserializeFactors(d.getString("contributing_factors"));
        Double prob = d.getDouble("no_show_probability");

        LocalDateTime predAt = LocalDateTime.now();
        String ps = d.getString("predicted_at");
        if (ps != null) {
            try { predAt = LocalDateTime.parse(ps.substring(0, 19)); } catch (Exception ignored) {}
        }

        LocalDate apptDate = LocalDate.now();
        String ads = d.getString("appointment_date");
        if (ads != null) {
            try { apptDate = LocalDate.parse(ads); } catch (Exception ignored) {}
        }

        LocalTime apptTime = LocalTime.of(9, 0);
        String ats = d.getString("appointment_time");
        if (ats != null) {
            try {
                if (ats.length() > 5) ats = ats.substring(0, 5);
                apptTime = LocalTime.parse(ats);
            } catch (Exception ignored) {}
        }

        String deptStr = d.getString("department");
        Department dept = deptStr != null ? Department.fromString(deptStr) : Department.GENERAL_MEDICINE;

        String patientName = d.getString("patient_name");
        if (patientName == null) patientName = "Patient " + d.getString("patient_id");
        String docName = d.getString("doctor_name");
        if (docName == null) docName = "Attending Physician";

        String action = d.getString("suggested_action");
        if (action == null) action = "Standard clinical follow-up";

        String hist = d.getString("historical_summary");
        if (hist == null) hist = "MongoDB Atlas patient record.";

        return new PredictionResult(
                d.getString("prediction_id"),
                d.getString("appointment_id"),
                d.getString("patient_id"),
                patientName,
                docName,
                dept,
                apptDate,
                apptTime,
                prob != null ? prob : 0.20,
                RiskLevel.fromString(d.getString("risk_level")),
                d.getString("prediction_method") != null ? d.getString("prediction_method") : "LOGISTIC_REGRESSION",
                d.getString("model_version") != null ? d.getString("model_version") : "1.0.0-PROD",
                factors,
                action,
                predAt,
                d.getString("actual_outcome"),
                d.getString("evaluation_status") != null ? d.getString("evaluation_status") : "PENDING",
                hist
        );
    }

    private String serializeFactors(List<FactorExplanation> factors) {
        if (factors == null || factors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < factors.size(); i++) {
            FactorExplanation f = factors.get(i);
            if (i > 0) sb.append(";;;");
            sb.append(f.getFeatureName().replace("|", "/"))
              .append("|").append(f.getDirection().name())
              .append("|").append(String.format(Locale.US, "%.4f", f.getImpactScore()))
              .append("|").append(f.getDescription().replace("|", "/"));
        }
        return sb.toString();
    }

    private List<FactorExplanation> deserializeFactors(String raw) {
        List<FactorExplanation> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) return list;

        String[] parts = raw.split(";;;");
        for (String p : parts) {
            String[] tokens = p.split("\\|", 4);
            if (tokens.length >= 4) {
                String name = tokens[0];
                FactorExplanation.Direction dir;
                try {
                    dir = FactorExplanation.Direction.valueOf(tokens[1]);
                } catch (Exception e) {
                    dir = FactorExplanation.Direction.NEUTRAL;
                }
                double score = 0.0;
                try { score = Double.parseDouble(tokens[2]); } catch (Exception ignored) {}
                String desc = tokens[3];
                list.add(new FactorExplanation(name, dir, score, desc));
            }
        }
        return list;
    }
}
