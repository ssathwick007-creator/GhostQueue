package com.ghostqueue.repository.mock;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.repository.QueueRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MockQueueRepository implements QueueRepository {
    private final Map<String, QueueEntry> queueEntries = new ConcurrentHashMap<>();

    public MockQueueRepository() {
        seedQueue();
    }

    private void seedQueue() {
        LocalDateTime now = LocalDateTime.now();

        add(new QueueEntry("WQ-301", "PT-1008", "Samuel O'Connor", "+1 (555) 890-1234",
                QueuePriority.URGENT, Department.CARDIOLOGY, "Dr. Robert Vance", "Morning (ASAP)",
                now.minusHours(3).minusMinutes(25), QueueStatus.WAITING,
                "Exertional dyspnea progression. Urgent evaluation requested by triage."));

        add(new QueueEntry("WQ-302", "PT-1012", "Jonathan Hayes", "+1 (555) 234-5678",
                QueuePriority.HIGH, Department.ORTHOPEDICS, "Dr. James Wilson", "Morning (09:00 - 12:00)",
                now.minusDays(1).minusHours(4), QueueStatus.WAITING,
                "Acute joint effusion right knee post-strenuous activity."));

        add(new QueueEntry("WQ-303", "PT-1003", "Maria Gomez", "+1 (555) 345-6789",
                QueuePriority.STANDARD, Department.NEUROLOGY, "Dr. David Kim", "Afternoon (14:00 - 17:00)",
                now.minusDays(2).minusHours(1), QueueStatus.WAITING,
                "Medication dosage titration review for migraine prophylaxis."));

        add(new QueueEntry("WQ-304", "PT-1005", "Amina Al-Mansoor", "+1 (555) 456-7890",
                QueuePriority.HIGH, Department.ONCOLOGY, "Dr. Evelyn Reed", "Anytime",
                now.minusDays(3).minusHours(6), QueueStatus.STANDBY,
                "Awaiting biopsy pathology review slot opening. Available on 45m notice."));

        add(new QueueEntry("WQ-305", "PT-1006", "Lucas Chen", "+1 (555) 678-9012",
                QueuePriority.STANDARD, Department.PEDIATRICS, "Dr. Sarah Lin", "Afternoon (After 14:00)",
                now.minusHours(5).minusMinutes(10), QueueStatus.WAITING,
                "School sports clearance and persistent mild wheeze check."));

        add(new QueueEntry("WQ-306", "PT-1011", "Priya Sharma", "+1 (555) 123-4567",
                QueuePriority.URGENT, Department.DERMATOLOGY, "Dr. Sophia Miller", "Anytime Today",
                now.minusHours(2).minusMinutes(40), QueueStatus.OFFERED,
                "Rapidly enlarging pigmented lesion on left forearm."));

        add(new QueueEntry("WQ-307", "PT-1014", "Daniel Kovacs", "+1 (555) 456-7891",
                QueuePriority.STANDARD, Department.GENERAL_MEDICINE, "Dr. Thomas Wright", "Early Morning",
                now.minusDays(4).minusHours(2), QueueStatus.WAITING,
                "Routine pre-travel vaccination clearance."));

        add(new QueueEntry("WQ-308", "PT-1002", "Arthur Pendelton", "+1 (555) 876-1234",
                QueuePriority.HIGH, Department.CARDIOLOGY, "Dr. Robert Vance", "Morning",
                now.minusDays(2).minusHours(5), QueueStatus.WAITING,
                "Recurrent peripheral edema and medication adjustment needed."));
    }

    private void add(QueueEntry e) {
        queueEntries.put(e.getQueueId(), e);
    }

    @Override
    public List<QueueEntry> findAll() {
        return queueEntries.values().stream()
                .sorted(Comparator.comparing(QueueEntry::getPriority)
                        .thenComparing(QueueEntry::getRegisteredTime))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<QueueEntry> findById(String id) {
        return Optional.ofNullable(queueEntries.get(id));
    }

    @Override
    public List<QueueEntry> findByDepartment(Department department) {
        if (department == Department.ALL) return findAll();
        return queueEntries.values().stream()
                .filter(e -> e.getRequestedDepartment() == department)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueueEntry> findByPriority(QueuePriority priority) {
        if (priority == QueuePriority.ALL) return findAll();
        return queueEntries.values().stream()
                .filter(e -> e.getPriority() == priority)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueueEntry> findByStatus(QueueStatus status) {
        if (status == QueueStatus.ALL) return findAll();
        return queueEntries.values().stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueueEntry> filter(String query, Department department, QueuePriority priority, QueueStatus status) {
        return queueEntries.values().stream()
                .filter(e -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase().trim();
                        boolean matchName = e.getPatientName().toLowerCase().contains(q);
                        boolean matchId = e.getPatientId().toLowerCase().contains(q);
                        boolean matchDoc = e.getPreferredDoctor().toLowerCase().contains(q);
                        if (!matchName && !matchId && !matchDoc) return false;
                    }
                    if (department != null && department != Department.ALL && e.getRequestedDepartment() != department) {
                        return false;
                    }
                    if (priority != null && priority != QueuePriority.ALL && e.getPriority() != priority) {
                        return false;
                    }
                    if (status != null && status != QueueStatus.ALL && e.getStatus() != status) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(QueueEntry::getPriority)
                        .thenComparing(QueueEntry::getRegisteredTime))
                .collect(Collectors.toList());
    }

    @Override
    public void save(QueueEntry entry) {
        queueEntries.put(entry.getQueueId(), entry);
    }

    @Override
    public void update(QueueEntry entry) {
        queueEntries.put(entry.getQueueId(), entry);
    }

    @Override
    public void updateStatus(String queueId, QueueStatus status) {
        QueueEntry e = queueEntries.get(queueId);
        if (e != null) {
            e.setStatus(status);
        }
    }

    @Override
    public void updatePriority(String queueId, QueuePriority priority) {
        QueueEntry e = queueEntries.get(queueId);
        if (e != null) {
            e.setPriority(priority);
        }
    }

    @Override
    public void delete(String id) {
        queueEntries.remove(id);
    }

    @Override
    public int countTotal() {
        return queueEntries.size();
    }

    @Override
    public int countActiveWaiting() {
        return (int) queueEntries.values().stream()
                .filter(e -> e.getStatus() == QueueStatus.WAITING || e.getStatus() == QueueStatus.STANDBY || e.getStatus() == QueueStatus.OFFERED)
                .count();
    }

    @Override
    public int countUrgentPriority() {
        return (int) queueEntries.values().stream()
                .filter(e -> e.getPriority() == QueuePriority.URGENT)
                .count();
    }
}
