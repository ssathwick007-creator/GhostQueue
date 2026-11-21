package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.AppointmentReminder;
import com.ghostqueue.repository.ReminderRepository;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseReminderRepository implements ReminderRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabaseReminderRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public List<AppointmentReminder> findAll() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*,patients(full_name,phone)");
            params.put("order", "scheduled_time.desc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("reminder_records"), params);
            return mapReminders(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch reminders from Supabase", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<AppointmentReminder> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("reminder_id", "eq." + id.trim());
            params.put("select", "*,patients(full_name,phone)");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("reminder_records"), params);
            List<AppointmentReminder> list = mapReminders(resp.asList());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding reminder by ID: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<AppointmentReminder> findByAppointmentId(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) return Collections.emptyList();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appointment_id", "eq." + appointmentId.trim());
            params.put("select", "*,patients(full_name,phone)");
            params.put("order", "scheduled_time.desc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("reminder_records"), params);
            return mapReminders(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding reminders for appt: " + appointmentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<AppointmentReminder> filter(String query, AppointmentReminder.ReminderType type, AppointmentReminder.ReminderStatus status) {
        List<AppointmentReminder> all = findAll();
        List<AppointmentReminder> filtered = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : null;

        for (AppointmentReminder r : all) {
            if (q != null && !q.isEmpty()) {
                boolean matchAppt = r.getAppointmentId() != null && r.getAppointmentId().toLowerCase().contains(q);
                boolean matchPatient = r.getPatientName() != null && r.getPatientName().toLowerCase().contains(q);
                boolean matchPhone = r.getPatientPhone() != null && r.getPatientPhone().contains(q);
                if (!matchAppt && !matchPatient && !matchPhone) continue;
            }
            if (type != null && r.getReminderType() != type) continue;
            if (status != null && r.getReminderStatus() != status) continue;

            filtered.add(r);
        }
        return filtered;
    }

    @Override
    public void save(AppointmentReminder reminder) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("appointment_id", reminder.getAppointmentId());
            data.put("patient_id", reminder.getPatientId());
            data.put("reminder_type", reminder.getReminderType().name());
            data.put("reminder_status", reminder.getReminderStatus().name());
            data.put("scheduled_time", reminder.getScheduledFor() != null ? reminder.getScheduledFor().toString() : LocalDateTime.now().toString());
            data.put("channel_response", reminder.getResponseStatus());

            client.post(SupabaseConfig.getRestEndpoint("reminder_records"), data);
            LOGGER.info("Reminder saved to Supabase for appointment " + reminder.getAppointmentId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save reminder to Supabase", e);
        }
    }

    @Override
    public void updateStatus(String reminderId, AppointmentReminder.ReminderStatus status, String responseStatus, String notes) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("reminder_status", status.name());
            data.put("channel_response", responseStatus != null ? responseStatus : status.name());
            if (status == AppointmentReminder.ReminderStatus.COMPLETED) {
                data.put("sent_time", LocalDateTime.now().toString());
            }

            Map<String, String> filter = Map.of("reminder_id", "eq." + reminderId);
            client.patch(SupabaseConfig.getRestEndpoint("reminder_records"), filter, data);
            LOGGER.info("Reminder status updated: " + reminderId + " -> " + status.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update reminder status: " + reminderId, e);
        }
    }

    @Override
    public int countPending() {
        List<AppointmentReminder> all = findAll();
        int count = 0;
        for (AppointmentReminder r : all) {
            if (r.getReminderStatus() == AppointmentReminder.ReminderStatus.PENDING) count++;
        }
        return count;
    }

    @Override
    public int countCompleted() {
        List<AppointmentReminder> all = findAll();
        int count = 0;
        for (AppointmentReminder r : all) {
            if (r.getReminderStatus() == AppointmentReminder.ReminderStatus.COMPLETED) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    private List<AppointmentReminder> mapReminders(List<Map<String, Object>> rows) {
        List<AppointmentReminder> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String id = String.valueOf(r.get("reminder_id"));
            String apptId = (String) r.get("appointment_id");
            String patId = (String) r.get("patient_id");

            String typeStr = (String) r.getOrDefault("reminder_type", "SMS");
            AppointmentReminder.ReminderType type;
            try { type = AppointmentReminder.ReminderType.valueOf(typeStr); } catch (Exception e) { type = AppointmentReminder.ReminderType.SMS; }

            String statusStr = (String) r.getOrDefault("reminder_status", "PENDING");
            AppointmentReminder.ReminderStatus status;
            try { status = AppointmentReminder.ReminderStatus.valueOf(statusStr); } catch (Exception e) { status = AppointmentReminder.ReminderStatus.PENDING; }

            LocalDateTime scheduledFor = LocalDateTime.now();
            if (r.get("scheduled_time") instanceof String ss) {
                try { scheduledFor = LocalDateTime.parse(ss.substring(0, 19)); } catch (Exception ignored) {}
            }

            LocalDateTime completedAt = null;
            if (r.get("sent_time") instanceof String st) {
                try { completedAt = LocalDateTime.parse(st.substring(0, 19)); } catch (Exception ignored) {}
            }

            String patientName = "Patient " + patId;
            String patientPhone = "+1 (555) 000-0000";
            if (r.get("patients") instanceof Map<?, ?> pm) {
                if (pm.get("full_name") != null) patientName = (String) pm.get("full_name");
                if (pm.get("phone") != null) patientPhone = (String) pm.get("phone");
            }

            String responseStatus = (String) r.getOrDefault("channel_response", "NO_RESPONSE");
            String message = "Reminder for appointment " + apptId + " scheduled for " + scheduledFor;

            AppointmentReminder rem = new AppointmentReminder(
                    id, apptId, patId, patientName, patientPhone,
                    type, status, scheduledFor, completedAt,
                    message, responseStatus, "Supabase Outreach Task", scheduledFor
            );
            list.add(rem);
        }
        return list;
    }
}
