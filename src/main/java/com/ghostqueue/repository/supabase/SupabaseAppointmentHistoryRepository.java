package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.repository.AppointmentHistoryRepository;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseAppointmentHistoryRepository implements AppointmentHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabaseAppointmentHistoryRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public List<AppointmentHistory> findByAppointmentId(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) return Collections.emptyList();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appointment_id", "eq." + appointmentId.trim());
            params.put("select", "*");
            params.put("order", "changed_at.desc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointment_history"), params);
            return mapHistory(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch appointment history for " + appointmentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void save(AppointmentHistory history) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("appointment_id", history.getAppointmentId());
            data.put("previous_status", history.getPreviousStatus());
            data.put("new_status", history.getNewStatus());
            data.put("remarks", history.getRemarks());

            client.post(SupabaseConfig.getRestEndpoint("appointment_history"), data);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save appointment history: " + history.getAppointmentId(), e);
        }
    }

    @Override
    public void save(Connection conn, AppointmentHistory history) {
        // Post directly to Supabase PostgREST
        save(history);
    }

    private List<AppointmentHistory> mapHistory(List<Map<String, Object>> rows) {
        List<AppointmentHistory> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            int id = ((Number) r.getOrDefault("history_id", 0)).intValue();
            String apptId = (String) r.get("appointment_id");
            String prevStatus = (String) r.get("previous_status");
            String newStatus = (String) r.get("new_status");
            String remarks = (String) r.get("remarks");

            LocalDateTime changedAt = LocalDateTime.now();
            if (r.get("changed_at") instanceof String cs) {
                try {
                    changedAt = LocalDateTime.parse(cs.substring(0, 19));
                } catch (Exception ignored) {}
            }

            AppointmentHistory h = new AppointmentHistory(id, apptId, prevStatus, newStatus, changedAt, remarks);
            list.add(h);
        }
        return list;
    }
}
