package com.ghostqueue.service;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.repository.QueueRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

public class QueueService {

    private final QueueRepository queueRepository;

    public QueueService(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    public List<QueueEntry> getAllQueueEntries() {
        return queueRepository.findAll();
    }

    /**
     * Retrieves active queue entries structured via a Java PriorityQueue
     * (ordered by priority tier: URGENT > HIGH > STANDARD, then chronological time).
     */
    public List<QueueEntry> getPriorityOrderedWaitingPool() {
        List<QueueEntry> all = queueRepository.findAll();
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(all);
        List<QueueEntry> sorted = new ArrayList<>();
        while (!pq.isEmpty()) {
            sorted.add(pq.poll());
        }
        return sorted;
    }

    public Optional<QueueEntry> getQueueEntryById(String id) {
        return queueRepository.findById(id);
    }

    public List<QueueEntry> filterQueue(String query, Department department, QueuePriority priority, QueueStatus status) {
        return queueRepository.filter(query, department, priority, status);
    }

    public List<QueueEntry> filterQueue(String query, Department department, String doctorId, QueuePriority priority, QueueStatus status) {
        return queueRepository.filter(query, department, doctorId, priority, status);
    }

    public void updateQueueEntry(QueueEntry entry) {
        if (entry == null || entry.getQueueId() == null) {
            throw new IllegalArgumentException("Queue entry and ID must not be null.");
        }
        queueRepository.update(entry);
    }

    public void addToQueue(QueueEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Queue entry cannot be null.");
        }
        if (entry.getQueueId() == null || entry.getQueueId().isBlank()) {
            throw new IllegalArgumentException("Queue ID is mandatory.");
        }
        if (entry.getPatientId() == null || entry.getPatientId().isBlank()) {
            throw new IllegalArgumentException("Patient ID is mandatory.");
        }
        if (entry.getPriority() == null) {
            entry.setPriority(QueuePriority.STANDARD);
        }
        if (entry.getStatus() == null) {
            entry.setStatus(QueueStatus.WAITING);
        }

        queueRepository.save(entry);
    }

    public void updateStatus(String queueId, QueueStatus status) {
        if (queueId == null || status == null) {
            throw new IllegalArgumentException("Queue ID and Status must not be null.");
        }
        queueRepository.updateStatus(queueId, status);
    }

    public void updatePriority(String queueId, QueuePriority priority) {
        if (queueId == null || priority == null) {
            throw new IllegalArgumentException("Queue ID and Priority must not be null.");
        }
        queueRepository.updatePriority(queueId, priority);
    }

    public void removeFromQueue(String queueId) {
        if (queueId == null || queueId.isBlank()) {
            throw new IllegalArgumentException("Queue ID cannot be blank.");
        }
        queueRepository.delete(queueId);
    }

    public void fulfillQueueRequest(String queueId) {
        updateStatus(queueId, QueueStatus.REALLOCATED);
    }

    public int countTotalWaiting() {
        return queueRepository.countActiveWaiting();
    }

    public int countUrgentPriority() {
        return queueRepository.countUrgentPriority();
    }
}
