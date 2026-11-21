package com.ghostqueue.repository;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.QueueEntry;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;

import java.util.List;
import java.util.Optional;

public interface QueueRepository {
    List<QueueEntry> findAll();
    Optional<QueueEntry> findById(String id);
    List<QueueEntry> findByDepartment(Department department);
    List<QueueEntry> findByPriority(QueuePriority priority);
    List<QueueEntry> findByStatus(QueueStatus status);
    List<QueueEntry> filter(String query, Department department, QueuePriority priority, QueueStatus status);
    List<QueueEntry> filter(String query, Department department, String doctorId, QueuePriority priority, QueueStatus status);
    void save(QueueEntry entry);
    void update(QueueEntry entry);
    void updateStatus(String queueId, QueueStatus status);
    void updatePriority(String queueId, QueuePriority priority);
    void delete(String id);
    int countTotal();
    int countActiveWaiting();
    int countUrgentPriority();
}
