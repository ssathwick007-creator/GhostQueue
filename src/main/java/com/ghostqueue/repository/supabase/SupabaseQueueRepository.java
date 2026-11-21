package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.repository.QueueRepository;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseQueueRepository implements QueueRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabaseQueueRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public List<QueueEntry> findAll() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*,patients(full_name),doctors(full_name)");
            params.put("order", "created_at.asc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("waiting_queue"), params);
            return mapQueueEntries(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch waiting queue from Supabase", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<QueueEntry> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("queue_id", "eq." + id.trim());
            params.put("select", "*,patients(full_name),doctors(full_name)");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("waiting_queue"), params);
            List<QueueEntry> list = mapQueueEntries(resp.asList());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding queue entry by ID: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<QueueEntry> findByDepartment(Department department) {
        if (department == null) return findAll();
        List<QueueEntry> all = findAll();
        List<QueueEntry> filtered = new ArrayList<>();
        for (QueueEntry q : all) {
            if (department == q.getDepartment()) filtered.add(q);
        }
        return filtered;
    }

    @Override
    public List<QueueEntry> findByPriority(QueuePriority priority) {
        if (priority == null) return findAll();
        List<QueueEntry> all = findAll();
        List<QueueEntry> filtered = new ArrayList<>();
        for (QueueEntry q : all) {
            if (priority == q.getPriorityLevel()) filtered.add(q);
        }
        return filtered;
    }

    @Override
    public List<QueueEntry> findByStatus(QueueStatus status) {
        if (status == null) return findAll();
        List<QueueEntry> all = findAll();
        List<QueueEntry> filtered = new ArrayList<>();
        for (QueueEntry q : all) {
            if (status == q.getQueueStatus()) filtered.add(q);
        }
        return filtered;
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

        for (QueueEntry entry : all) {
            if (q != null && !q.isEmpty()) {
                boolean matchId = entry.getId() != null && entry.getId().toLowerCase().contains(q);
                boolean matchPatient = entry.getPatientName() != null && entry.getPatientName().toLowerCase().contains(q);
                boolean matchDoctor = entry.getDoctorName() != null && entry.getDoctorName().toLowerCase().contains(q);
                boolean matchPatientId = entry.getPatientId() != null && entry.getPatientId().toLowerCase().contains(q);
                if (!matchId && !matchPatient && !matchDoctor && !matchPatientId) continue;
            }
            if (department != null && entry.getDepartment() != department) continue;
            if (doctorId != null && !doctorId.isBlank() && !doctorId.equalsIgnoreCase(entry.getDoctorId())) continue;
            if (priority != null && entry.getPriorityLevel() != priority) continue;
            if (status != null && entry.getQueueStatus() != status) continue;

            filtered.add(entry);
        }
        return filtered;
    }

    @Override
    public void save(QueueEntry entry) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("queue_id", entry.getId());
            data.put("patient_id", entry.getPatientId());
            String docId = entry.getDoctorId();
            if (docId != null && !docId.startsWith("DOC-")) {
                docId = null;
            }
            data.put("doctor_id", docId);
            data.put("department", entry.getDepartment() != null ? entry.getDepartment().getDisplayName() : "General Medicine");
            data.put("requested_date", entry.getRequestedDate() != null ? entry.getRequestedDate().toString() : LocalDate.now().toString());
            data.put("preferred_time", entry.getPreferredTime() != null ? entry.getPreferredTime() : "09:00:00");
            data.put("priority_level", entry.getPriorityLevel() != null ? entry.getPriorityLevel().name() : "STANDARD");
            data.put("queue_status", entry.getQueueStatus() != null ? entry.getQueueStatus().name() : "WAITING");
            data.put("notes", entry.getNotes());

            client.post(SupabaseConfig.getRestEndpoint("waiting_queue"), data);
            LOGGER.info("Queue entry saved to Supabase: " + entry.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save queue entry to Supabase", e);
            throw new RuntimeException("Could not save queue entry: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(QueueEntry entry) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("priority_level", entry.getPriorityLevel().name());
            data.put("queue_status", entry.getQueueStatus().name());
            data.put("requested_date", entry.getRequestedDate().toString());
            if (entry.getPreferredTime() != null) {
                data.put("preferred_time", entry.getPreferredTime().toString());
            }
            if (entry.getNotes() != null) {
                data.put("notes", entry.getNotes());
            }

            Map<String, String> filter = Map.of("queue_id", "eq." + entry.getId());
            client.patch(SupabaseConfig.getRestEndpoint("waiting_queue"), filter, data);
            LOGGER.info("Queue entry updated in Supabase: " + entry.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update queue entry: " + entry.getId(), e);
            throw new RuntimeException("Could not update queue entry: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(String queueId, QueueStatus status) {
        try {
            Map<String, Object> data = Map.of("queue_status", status.name());
            Map<String, String> filter = Map.of("queue_id", "eq." + queueId);
            client.patch(SupabaseConfig.getRestEndpoint("waiting_queue"), filter, data);
            LOGGER.info("Queue status updated: " + queueId + " -> " + status.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update queue status: " + queueId, e);
        }
    }

    @Override
    public void updatePriority(String queueId, QueuePriority priority) {
        try {
            Map<String, Object> data = Map.of("priority_level", priority.name());
            Map<String, String> filter = Map.of("queue_id", "eq." + queueId);
            client.patch(SupabaseConfig.getRestEndpoint("waiting_queue"), filter, data);
            LOGGER.info("Queue priority updated: " + queueId + " -> " + priority.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update queue priority: " + queueId, e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            Map<String, String> filter = Map.of("queue_id", "eq." + id);
            client.delete(SupabaseConfig.getRestEndpoint("waiting_queue"), filter);
            LOGGER.info("Queue entry deleted: " + id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete queue entry: " + id, e);
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public int countActiveWaiting() {
        List<QueueEntry> all = findAll();
        int count = 0;
        for (QueueEntry q : all) {
            if (q.getQueueStatus() == QueueStatus.WAITING || q.getQueueStatus() == QueueStatus.STANDBY || q.getQueueStatus() == QueueStatus.OFFERED) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int countUrgentPriority() {
        List<QueueEntry> all = findAll();
        int count = 0;
        for (QueueEntry q : all) {
            if (q.getPriorityLevel() == QueuePriority.URGENT &&
               (q.getQueueStatus() == QueueStatus.WAITING || q.getQueueStatus() == QueueStatus.STANDBY || q.getQueueStatus() == QueueStatus.OFFERED)) {
                count++;
            }
        }
        return count;
    }

    private List<QueueEntry> mapQueueEntries(List<Map<String, Object>> rows) {
        List<QueueEntry> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("queue_id");
            String patientId = (String) r.get("patient_id");
            String doctorId = (String) r.get("doctor_id");
            String deptStr = (String) r.get("department");
            Department dept = Department.fromString(deptStr);

            LocalDate date = LocalDate.now();
            if (r.get("requested_date") instanceof String ds) {
                try { date = LocalDate.parse(ds); } catch (Exception ignored) {}
            }

            String prefSlot = "Anytime";
            if (r.get("preferred_time") instanceof String ps && !ps.isBlank()) {
                prefSlot = ps;
            }

            String prioStr = (String) r.getOrDefault("priority_level", "STANDARD");
            QueuePriority priority = QueuePriority.fromString(prioStr);

            String statusStr = (String) r.getOrDefault("queue_status", "WAITING");
            QueueStatus status = QueueStatus.fromString(statusStr);

            LocalDateTime createdAt = LocalDateTime.now();
            if (r.get("created_at") instanceof String cs) {
                try {
                    createdAt = LocalDateTime.parse(cs.substring(0, 19));
                } catch (Exception ignored) {}
            }

            String patientName = "Patient " + patientId;
            if (r.get("patients") instanceof Map<?, ?> pm && pm.get("full_name") != null) {
                patientName = (String) pm.get("full_name");
            }

            String doctorName = doctorId != null ? ("Physician " + doctorId) : "Any Available Physician";
            if (r.get("doctors") instanceof Map<?, ?> dm && dm.get("full_name") != null) {
                doctorName = (String) dm.get("full_name");
            }

            String notes = (String) r.get("notes");

            QueueEntry entry = new QueueEntry(
                    id, patientId, doctorId, date, prefSlot, priority, status, createdAt, notes
            );
            entry.setPatientName(patientName);
            entry.setPreferredDoctor(doctorName);
            entry.setRequestedDepartment(dept);
            list.add(entry);
        }
        return list;
    }
}
