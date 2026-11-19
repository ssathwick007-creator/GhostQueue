package com.ghostqueue.repository;

import com.ghostqueue.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    List<Patient> findAll();
    Optional<Patient> findById(String id);
    List<Patient> searchByNameOrId(String query);
    void save(Patient patient);
    void update(Patient patient);
    void delete(String id);
    boolean existsById(String id);
    boolean existsByPhone(String phone, String excludePatientId);
    boolean hasAppointments(String patientId);
    int countTotal();
}
