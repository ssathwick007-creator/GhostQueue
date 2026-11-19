package com.ghostqueue.service;

import com.ghostqueue.model.Doctor;
import com.ghostqueue.repository.DoctorRepository;

import java.util.List;
import java.util.Optional;

public class DoctorService {
    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Optional<Doctor> getDoctorById(String id) {
        return doctorRepository.findById(id);
    }

    public List<Doctor> getDoctorsByDepartment(String department) {
        return doctorRepository.findByDepartment(department);
    }

    public List<Doctor> searchDoctors(String query) {
        return doctorRepository.search(query);
    }

    public void registerDoctor(Doctor doctor) {
        validateDoctor(doctor);
        if (doctorRepository.existsById(doctor.getId())) {
            throw new IllegalArgumentException("Doctor with ID " + doctor.getId() + " already exists.");
        }
        doctorRepository.save(doctor);
    }

    public void updateDoctor(Doctor doctor) {
        validateDoctor(doctor);
        doctorRepository.update(doctor);
    }

    public void setAvailabilityStatus(String doctorId, String status) {
        doctorRepository.updateAvailability(doctorId, status);
    }

    public int countTotal() {
        return doctorRepository.countTotal();
    }

    private void validateDoctor(Doctor doctor) {
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor object cannot be null.");
        }
        if (doctor.getFullName() == null || doctor.getFullName().trim().length() < 3) {
            throw new IllegalArgumentException("Doctor full name must be at least 3 characters long.");
        }
        if (doctor.getDepartment() == null || doctor.getDepartment().isBlank()) {
            throw new IllegalArgumentException("Clinical department must be specified.");
        }
    }
}
