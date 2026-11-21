package com.ghostqueue.prediction;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabasePredictionRepository implements PredictionRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabasePredictionRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public void save(PredictionResult result) {
        try {
            Map<String, Object> data = new HashMap<>();
            String predId = result.getPredictionId();
            if (predId == null || predId.isBlank()) {
                predId = "PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
            data.put("prediction_id", predId);
            data.put("appointment_id", result.getAppointmentId());
            data.put("patient_id", result.getPatientId());
            data.put("probability", result.getNoShowProbability());
            data.put("risk_level", result.getRiskLevel().name());
            data.put("model_version", result.getModelVersion());
            data.put("prediction_method", result.getPredictionMethod());
            data.put("contributing_factors", serializeFactors(result.getContributingFactors()));
            data.put("predicted_at", result.getPredictedAt().toString());
            data.put("actual_outcome", result.getActualOutcome());
            data.put("evaluation_status", result.getEvaluationStatus());

            client.post(SupabaseConfig.getRestEndpoint("appointment_predictions"), data);
            LOGGER.info("Saved prediction to Supabase for appointment " + result.getAppointmentId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save prediction to Supabase", e);
        }
    }

    @Override
    public void saveBatch(List<PredictionResult> results) {
        if (results == null || results.isEmpty()) return;
        for (PredictionResult r : results) {
            save(r);
        }
    }

    @Override
    public Optional<PredictionResult> findById(String predictionId) {
        if (predictionId == null || predictionId.isBlank()) return Optional.empty();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("prediction_id", "eq." + predictionId.trim());
            params.put("select", "*,appointments(*,patients(full_name),doctors(full_name))");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointment_predictions"), params);
            List<PredictionResult> list = mapPredictions(resp.asList());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding prediction by ID: " + predictionId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<PredictionResult> findLatestByAppointmentId(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) return Optional.empty();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appointment_id", "eq." + appointmentId.trim());
            params.put("select", "*,appointments(*,patients(full_name),doctors(full_name))");
            params.put("order", "predicted_at.desc");
            params.put("limit", "1");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointment_predictions"), params);
            List<PredictionResult> list = mapPredictions(resp.asList());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding prediction by appointment: " + appointmentId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<PredictionResult> findAll() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*,appointments(*,patients(full_name),doctors(full_name))");
            params.put("order", "predicted_at.desc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointment_predictions"), params);
            return mapPredictions(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch predictions from Supabase", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<PredictionResult> findLatestPredictions() {
        return findAll();
    }

    @Override
    public List<PredictionResult> filter(String query, Department department, String doctorId, RiskLevel riskLevel, LocalDate date) {
        List<PredictionResult> all = findAll();
        List<PredictionResult> filtered = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : null;

        for (PredictionResult r : all) {
            if (q != null && !q.isEmpty()) {
                boolean matchId = r.getAppointmentId() != null && r.getAppointmentId().toLowerCase().contains(q);
                boolean matchPatient = r.getPatientName() != null && r.getPatientName().toLowerCase().contains(q);
                boolean matchDoctor = r.getDoctorName() != null && r.getDoctorName().toLowerCase().contains(q);
                if (!matchId && !matchPatient && !matchDoctor) continue;
            }
            if (department != null && r.getDepartment() != department) continue;
            if (riskLevel != null && r.getRiskLevel() != riskLevel) continue;
            if (date != null && !date.equals(r.getAppointmentDate())) continue;

            filtered.add(r);
        }
        return filtered;
    }

    @Override
    public void updateActualOutcome(String predictionId, String outcome, String evaluationStatus) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("actual_outcome", outcome);
            data.put("evaluation_status", evaluationStatus != null ? evaluationStatus : "EVALUATED");

            Map<String, String> filter = Map.of("prediction_id", "eq." + predictionId);
            client.patch(SupabaseConfig.getRestEndpoint("appointment_predictions"), filter, data);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update actual outcome in Supabase: " + predictionId, e);
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public int countByRiskLevel(RiskLevel riskLevel) {
        List<PredictionResult> all = findAll();
        int count = 0;
        for (PredictionResult r : all) {
            if (r.getRiskLevel() == riskLevel) count++;
        }
        return count;
    }

    @Override
    public double getAverageProbability() {
        List<PredictionResult> all = findAll();
        if (all.isEmpty()) return 0.28;
        double sum = 0.0;
        for (PredictionResult r : all) {
            sum += r.getNoShowProbability();
        }
        return sum / all.size();
    }

    private List<PredictionResult> mapPredictions(List<Map<String, Object>> rows) {
        List<PredictionResult> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String predId = String.valueOf(r.get("prediction_id"));
            String apptId = (String) r.get("appointment_id");
            String patId = (String) r.get("patient_id");
            Number probNum = (Number) r.getOrDefault("probability", 0.25);
            double prob = probNum.doubleValue();

            String riskStr = (String) r.getOrDefault("risk_level", "LOW");
            RiskLevel level = RiskLevel.fromString(riskStr);

            String modelVer = (String) r.getOrDefault("model_version", "1.0.0-PROD");
            String method = (String) r.getOrDefault("prediction_method", "LOGISTIC_REGRESSION");
            String factorsRaw = (String) r.get("contributing_factors");
            List<FactorExplanation> factors = deserializeFactors(factorsRaw);

            LocalDateTime predAt = LocalDateTime.now();
            if (r.get("predicted_at") instanceof String ps) {
                try { predAt = LocalDateTime.parse(ps.substring(0, 19)); } catch (Exception ignored) {}
            }

            String outcome = (String) r.get("actual_outcome");
            String evalStatus = (String) r.getOrDefault("evaluation_status", "PENDING");

            String patientName = "Patient " + patId;
            String doctorName = "Attending Physician";
            Department dept = Department.GENERAL_MEDICINE;
            LocalDate apptDate = LocalDate.now();
            LocalTime apptTime = LocalTime.of(9, 0);

            if (r.get("appointments") instanceof Map<?, ?> apptMap) {
                if (apptMap.get("department") instanceof String ds) {
                    dept = Department.fromString(ds);
                }
                if (apptMap.get("appointment_date") instanceof String ads) {
                    try { apptDate = LocalDate.parse(ads); } catch (Exception ignored) {}
                }
                if (apptMap.get("appointment_time") instanceof String ats) {
                    try {
                        if (ats.length() > 5) ats = ats.substring(0, 5);
                        apptTime = LocalTime.parse(ats);
                    } catch (Exception ignored) {}
                }
                if (apptMap.get("patients") instanceof Map<?, ?> pm && pm.get("full_name") != null) {
                    patientName = (String) pm.get("full_name");
                }
                if (apptMap.get("doctors") instanceof Map<?, ?> dm && dm.get("full_name") != null) {
                    doctorName = (String) dm.get("full_name");
                }
            }

            String suggestedAction = level == RiskLevel.HIGH ? "Trigger automated confirmation outreach" : "Standard appointment queue";

            PredictionResult res = new PredictionResult(
                    predId, apptId, patId, patientName, doctorName, dept,
                    apptDate, apptTime, prob, level, method, modelVer,
                    factors, suggestedAction, predAt, outcome, evalStatus,
                    "Supabase ML Record for " + patientName
            );
            list.add(res);
        }
        return list;
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
            } else if (p.contains(";")) {
                // Semicolon delimited plain strings fallback
                String[] textItems = raw.split(";");
                for (String t : textItems) {
                    if (!t.isBlank()) {
                        list.add(new FactorExplanation(t.trim(), FactorExplanation.Direction.INCREASES_RISK, 0.25, t.trim()));
                    }
                }
                break;
            }
        }
        return list;
    }
}
