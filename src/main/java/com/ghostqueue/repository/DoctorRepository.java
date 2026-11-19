package com.ghostqueue.repository;

import com.ghostqueue.model.Doctor;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository {
    List<Doctor> findAll();
    Optional<Doctor> findById(String id);
    List<Doctor> findByDepartment(String department);
    List<Doctor> search(String query);
    void save(Doctor doctor);
    void update(Doctor doctor);
    void updateAvailability(String doctorId, String status);
    int countTotal();
    boolean existsById(String id);
}
