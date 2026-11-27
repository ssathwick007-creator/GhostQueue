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
            List<QueueEntry> list = mapQueueEntries(resp.asList());
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch waiting queue from Supabase, serving fallback: " + e.getMessage());
        }
        return getFallbackQueueEntries();
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
            if (!list.isEmpty()) {
                return Optional.of(list.get(0));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding queue entry by ID: " + id, e);
        }
        return getFallbackQueueEntries().stream().filter(q -> q.getQueueId().equalsIgnoreCase(id.trim())).findFirst();
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
    public List<QueueEntry> findByPriority(QueuePriority priority) {
        if (priority == null) return findAll();
        return findAll().stream().filter(q -> q.getPriority() == priority).toList();
    }

    public List<QueueEntry> findActiveByDepartment(Department department) {
        List<QueueEntry> all = findAll();
        List<QueueEntry> filtered = new ArrayList<>();
        for (QueueEntry q : all) {
            if (department != null && department != Department.ALL && department != q.getDepartment()) continue;
            if (q.getQueueStatus() == QueueStatus.WAITING || q.getQueueStatus() == QueueStatus.STANDBY || q.getQueueStatus() == QueueStatus.OFFERED) {
                filtered.add(q);
            }
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

        for (QueueEntry e : all) {
            if (q != null && !q.isEmpty()) {
                boolean matchId = e.getQueueId() != null && e.getQueueId().toLowerCase().contains(q);
                boolean matchPatient = e.getPatientName() != null && e.getPatientName().toLowerCase().contains(q);
                boolean matchDoc = e.getPreferredDoctor() != null && e.getPreferredDoctor().toLowerCase().contains(q);
                if (!matchId && !matchPatient && !matchDoc) continue;
            }
            if (department != null && department != Department.ALL && e.getDepartment() != department) continue;
            if (doctorId != null && !doctorId.isBlank() && !doctorId.equalsIgnoreCase(e.getDoctorId())) continue;
            if (priority != null && e.getPriorityLevel() != priority) continue;
            if (status != null && e.getQueueStatus() != status) continue;

            filtered.add(e);
        }
        return filtered;
    }

    @Override
    public void save(QueueEntry entry) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("queue_id", entry.getQueueId());
            data.put("patient_id", entry.getPatientId());
            data.put("doctor_id", entry.getDoctorId());
            data.put("department", entry.getDepartment() != null ? entry.getDepartment().getDisplayName() : "General Medicine");
            data.put("requested_date", entry.getRequestedDate() != null ? entry.getRequestedDate().toString() : LocalDate.now().toString());
            data.put("preferred_time", entry.getPreferredTimeSlot());
            data.put("priority_level", entry.getPriorityLevel() != null ? entry.getPriorityLevel().name() : "STANDARD");
            data.put("queue_status", entry.getQueueStatus() != null ? entry.getQueueStatus().name() : "WAITING");
            data.put("notes", entry.getClinicalNote());

            client.post(SupabaseConfig.getRestEndpoint("waiting_queue"), data);
            LOGGER.info("Queue entry created in Supabase: " + entry.getQueueId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save queue entry: " + entry.getQueueId(), e);
            throw new RuntimeException("Could not persist queue entry: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(QueueEntry entry) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("priority_level", entry.getPriorityLevel().name());
            data.put("queue_status", entry.getQueueStatus().name());
            if (entry.getClinicalNote() != null) {
                data.put("notes", entry.getClinicalNote());
            }

            Map<String, String> filter = Map.of("queue_id", "eq." + entry.getQueueId());
            client.patch(SupabaseConfig.getRestEndpoint("waiting_queue"), filter, data);
            LOGGER.info("Queue entry updated in Supabase: " + entry.getQueueId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update queue entry: " + entry.getQueueId(), e);
            throw new RuntimeException("Could not update queue entry: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(String queueId, QueueStatus newStatus) {
        try {
            Map<String, Object> data = Map.of("queue_status", newStatus.name());
            Map<String, String> filter = Map.of("queue_id", "eq." + queueId);
            client.patch(SupabaseConfig.getRestEndpoint("waiting_queue"), filter, data);
            LOGGER.info("Queue entry status updated in Supabase: " + queueId + " -> " + newStatus.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update queue entry status: " + queueId, e);
        }
    }

    @Override
    public void updatePriority(String queueId, QueuePriority newPriority) {
        try {
            Map<String, Object> data = Map.of("priority_level", newPriority.name());
            Map<String, String> filter = Map.of("queue_id", "eq." + queueId);
            client.patch(SupabaseConfig.getRestEndpoint("waiting_queue"), filter, data);
            LOGGER.info("Queue entry priority updated in Supabase: " + queueId + " -> " + newPriority.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update queue entry priority: " + queueId, e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            Map<String, String> filter = Map.of("queue_id", "eq." + id);
            client.delete(SupabaseConfig.getRestEndpoint("waiting_queue"), filter);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete queue entry: " + id, e);
        }
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
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

    public static List<QueueEntry> getFallbackQueueEntries() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<QueueEntry> list = new ArrayList<>();
        list.add(new QueueEntry("WQ-101", "PT-1002", "DOC-101", today, "09:00 - 11:00", QueuePriority.URGENT, QueueStatus.WAITING, now.minusHours(4), "Post-stent triage"));
        list.add(new QueueEntry("WQ-102", "PT-1003", "DOC-102", today, "10:00 - 12:00", QueuePriority.HIGH, QueueStatus.WAITING, now.minusHours(3), "Migraine assessment"));
        list.add(new QueueEntry("WQ-103", "PT-1005", "DOC-104", today, "Afternoon", QueuePriority.STANDARD, QueueStatus.WAITING, now.minusHours(2), "Chemo followup"));
        list.add(new QueueEntry("WQ-104", "PT-1007", "DOC-106", today, "Morning", QueuePriority.HIGH, QueueStatus.STANDBY, now.minusHours(2), "Routine physical standby"));
        list.add(new QueueEntry("WQ-105", "PT-1008", "DOC-101", today, "Anytime", QueuePriority.URGENT, QueueStatus.WAITING, now.minusHours(1), "Cardiac surveillance"));
        list.add(new QueueEntry("WQ-106", "PT-1009", "DOC-102", today, "14:00 - 16:00", QueuePriority.STANDARD, QueueStatus.OFFERED, now.minusMinutes(45), "Offered slot APT-1002"));
        list.add(new QueueEntry("WQ-107", "PT-1011", "DOC-106", today.plusDays(1), "Morning", QueuePriority.STANDARD, QueueStatus.WAITING, now.minusMinutes(30), "General consult"));
        list.add(new QueueEntry("WQ-108", "PT-1012", "DOC-103", today.plusDays(1), "11:00 - 13:00", QueuePriority.HIGH, QueueStatus.WAITING, now.minusMinutes(20), "Orthopedic joint review"));
        list.add(new QueueEntry("WQ-109", "PT-1013", "DOC-102", today.plusDays(1), "Afternoon", QueuePriority.STANDARD, QueueStatus.WAITING, now.minusMinutes(15), "Neurology standby"));
        list.add(new QueueEntry("WQ-110", "PT-1014", "DOC-106", today.plusDays(2), "Morning", QueuePriority.STANDARD, QueueStatus.WAITING, now.minusMinutes(5), "Preventative health"));

        // Set names and departments
        for (QueueEntry q : list) {
            if ("PT-1002".equals(q.getPatientId())) { q.setPatientName("Arthur Pendelton"); q.setRequestedDepartment(Department.CARDIOLOGY); q.setPreferredDoctor("Dr. Robert Vance"); }
            else if ("PT-1003".equals(q.getPatientId())) { q.setPatientName("Maria Gomez"); q.setRequestedDepartment(Department.NEUROLOGY); q.setPreferredDoctor("Dr. David Kim"); }
            else if ("PT-1005".equals(q.getPatientId())) { q.setPatientName("Amina Al-Mansoor"); q.setRequestedDepartment(Department.ONCOLOGY); q.setPreferredDoctor("Dr. Evelyn Reed"); }
            else if ("PT-1007".equals(q.getPatientId())) { q.setPatientName("Chloe Bennett"); q.setRequestedDepartment(Department.GENERAL_MEDICINE); q.setPreferredDoctor("Dr. Thomas Wright"); }
            else if ("PT-1008".equals(q.getPatientId())) { q.setPatientName("Samuel O Connor"); q.setRequestedDepartment(Department.CARDIOLOGY); q.setPreferredDoctor("Dr. Robert Vance"); }
            else if ("PT-1009".equals(q.getPatientId())) { q.setPatientName("Fatima Zahra"); q.setRequestedDepartment(Department.NEUROLOGY); q.setPreferredDoctor("Dr. David Kim"); }
            else if ("PT-1011".equals(q.getPatientId())) { q.setPatientName("Priya Sharma"); q.setRequestedDepartment(Department.GENERAL_MEDICINE); q.setPreferredDoctor("Dr. Thomas Wright"); }
            else if ("PT-1012".equals(q.getPatientId())) { q.setPatientName("Jonathan Hayes"); q.setRequestedDepartment(Department.ORTHOPEDICS); q.setPreferredDoctor("Dr. James Wilson"); }
            else if ("PT-1013".equals(q.getPatientId())) { q.setPatientName("Victoria Ross"); q.setRequestedDepartment(Department.NEUROLOGY); q.setPreferredDoctor("Dr. David Kim"); }
            else if ("PT-1014".equals(q.getPatientId())) { q.setPatientName("Daniel Kovacs"); q.setRequestedDepartment(Department.GENERAL_MEDICINE); q.setPreferredDoctor("Dr. Thomas Wright"); }
        }
        return list;
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
