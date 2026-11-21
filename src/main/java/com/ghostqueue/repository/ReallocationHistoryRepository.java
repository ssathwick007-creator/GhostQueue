package com.ghostqueue.repository;

import com.ghostqueue.model.ReallocationHistory;
import java.util.List;

public interface ReallocationHistoryRepository {
    List<ReallocationHistory> findAll();
    List<ReallocationHistory> findByAppointmentId(String appointmentId);
    void save(ReallocationHistory record);
    int countApproved();
    int countTotal();
}
