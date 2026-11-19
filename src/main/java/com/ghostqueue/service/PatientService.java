package com.ghostqueue.service;

import com.ghostqueue.model.Patient;
import com.ghostqueue.repository.PatientRepository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class PatientService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s()\\-\\.\\/]{7,25}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(String id) {
        return patientRepository.findById(id);
    }

    public List<Patient> searchPatients(String query) {
        return patientRepository.searchByNameOrId(query);
    }

    public void registerPatient(Patient patient) {
        validatePatient(patient, true);
        if (patientRepository.existsById(patient.getId())) {
            throw new IllegalArgumentException("Patient ID " + patient.getId() + " is already registered.");
        }
        if (patientRepository.existsByPhone(patient.getPhone(), null)) {
            throw new IllegalArgumentException("Phone number " + patient.getPhone() + " is already associated with an existing patient.");
        }
        patientRepository.save(patient);
    }

    public void updatePatient(Patient patient) {
        validatePatient(patient, false);
        if (patientRepository.existsByPhone(patient.getPhone(), patient.getId())) {
            throw new IllegalArgumentException("Phone number " + patient.getPhone() + " is already in use by another patient.");
        }
        patientRepository.update(patient);
    }

    public void deletePatient(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new IllegalArgumentException("Patient ID cannot be blank.");
        }
        if (patientRepository.hasAppointments(patientId)) {
            throw new IllegalStateException("Deletion Denied: Patient " + patientId + " has existing clinical appointments on record.\n" +
                    "To preserve medical audit history and legal compliance, records with associated appointments cannot be deleted.");
        }
        patientRepository.delete(patientId);
    }

    public boolean canDeletePatient(String patientId) {
        return !patientRepository.hasAppointments(patientId);
    }

    public int countTotal() {
        return patientRepository.countTotal();
    }

    private void validatePatient(Patient p, boolean isNew) {
        if (p == null) {
            throw new IllegalArgumentException("Patient information cannot be null.");
        }
        if (p.getId() == null || p.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Patient MRN / ID is mandatory.");
        }
        if (p.getFullName() == null || p.getFullName().trim().length() < 2) {
            throw new IllegalArgumentException("Patient full name must be at least 2 characters.");
        }
        if (p.getAge() < 0 || p.getAge() > 130) {
            throw new IllegalArgumentException("Patient age must be between 0 and 130 years.");
        }
        if (p.getGender() == null || p.getGender().trim().isEmpty()) {
            throw new IllegalArgumentException("Patient biological sex / gender is mandatory.");
        }
        if (p.getPhone() == null || p.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact phone number is mandatory.");
        }
        if (!PHONE_PATTERN.matcher(p.getPhone().trim()).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + p.getPhone());
        }
        if (p.getEmail() != null && !p.getEmail().isBlank()) {
            if (!EMAIL_PATTERN.matcher(p.getEmail().trim()).matches()) {
                throw new IllegalArgumentException("Invalid email address format: " + p.getEmail());
            }
        }
    }
}
