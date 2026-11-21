package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.ReallocationHistory;
import com.ghostqueue.repository.ReallocationHistoryRepository;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseReallocationHistoryRepository implements ReallocationHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabaseReallocationHistoryRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public List<ReallocationHistory> findAll() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*,patients!candidate_patient_id(full_name),doctors(full_name,department)");
            params.put("order", "decided_at.desc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("reallocation_history"), params);
            return mapRecords(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed with nested select, falling back to basic select", e);
            try {
                Map<String, String> fallbackParams = new HashMap<>();
                fallbackParams.put("select", "*");
                fallbackParams.put("order", "decided_at.desc");
                SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("reallocation_history"), fallbackParams);
                return mapRecords(resp.asList());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to fetch reallocation history from Supabase", ex);
                return Collections.emptyList();
            }
        }
    }

    @Override
    public List<ReallocationHistory> findByAppointmentId(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) return Collections.emptyList();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("original_appointment_id", "eq." + appointmentId.trim());
            params.put("select", "*,patients(full_name),doctors(full_name,department)");
            params.put("order", "decided_at.desc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("reallocation_history"), params);
            return mapRecords(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding reallocation history for appt: " + appointmentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void save(ReallocationHistory record) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("original_appointment_id", record.getAppointmentId());
            data.put("candidate_patient_id", record.getCandidatePatientId());
            data.put("doctor_id", record.getDoctorId());
            data.put("target_date", record.getSlotDate().toString());
            data.put("target_time", record.getSlotTime().toString());
            data.put("compatibility_score", record.getMatchScore());
            data.put("recommendation_reason", record.getMatchReason());
            data.put("decision", record.getDecision());
            data.put("decided_by", record.getDecisionNotes());

            client.post(SupabaseConfig.getRestEndpoint("reallocation_history"), data);
            LOGGER.info("Reallocation record saved to Supabase: " + record.getAppointmentId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save reallocation history", e);
        }
    }

    @Override
    public int countApproved() {
        List<ReallocationHistory> all = findAll();
        int count = 0;
        for (ReallocationHistory r : all) {
            if ("ACCEPTED".equalsIgnoreCase(r.getDecision()) || "APPROVED".equalsIgnoreCase(r.getDecision()) || "CONFIRMED".equalsIgnoreCase(r.getDecision())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    private List<ReallocationHistory> mapRecords(List<Map<String, Object>> rows) {
        List<ReallocationHistory> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String reallocId = String.valueOf(r.get("history_id"));
            String apptId = (String) r.get("original_appointment_id");
            String queueId = "WQ-" + reallocId;
            String candPatId = (String) r.get("candidate_patient_id");
            String docId = (String) r.get("doctor_id");

            LocalDate slotDate = LocalDate.now();
            if (r.get("target_date") instanceof String ds) {
                try { slotDate = LocalDate.parse(ds); } catch (Exception ignored) {}
            }

            LocalTime slotTime = LocalTime.of(9, 0);
            if (r.get("target_time") instanceof String ts) {
                try {
                    if (ts.length() > 5) ts = ts.substring(0, 5);
                    slotTime = LocalTime.parse(ts);
                } catch (Exception ignored) {}
            }

            Number scoreNum = (Number) r.getOrDefault("compatibility_score", 85);
            int score = scoreNum.intValue();
            String reason = (String) r.getOrDefault("recommendation_reason", "High triage compatibility");
            String decision = (String) r.getOrDefault("decision", "APPROVED");
            String notes = (String) r.get("decided_by");

            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime decidedAt = LocalDateTime.now();
            if (r.get("decided_at") instanceof String das) {
                try {
                    decidedAt = LocalDateTime.parse(das.substring(0, 19));
                    createdAt = decidedAt;
                } catch (Exception ignored) {}
            }

            String candName = "Patient " + candPatId;
            if (r.get("patients") instanceof Map<?, ?> pm && pm.get("full_name") != null) {
                candName = (String) pm.get("full_name");
            }

            String docName = "Physician " + docId;
            Department docDept = Department.GENERAL_MEDICINE;
            if (r.get("doctors") instanceof Map<?, ?> dm) {
                if (dm.get("full_name") != null) docName = (String) dm.get("full_name");
                if (dm.get("department") != null) docDept = Department.fromString((String) dm.get("department"));
            }

            ReallocationHistory h = new ReallocationHistory(
                    reallocId, apptId, queueId, candPatId, candName,
                    docId, docName, docDept, slotDate, slotTime,
                    score, reason, decision, notes, createdAt, decidedAt
            );
            list.add(h);
        }
        return list;
    }
}
