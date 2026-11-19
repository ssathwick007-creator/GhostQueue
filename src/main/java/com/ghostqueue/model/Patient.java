package com.ghostqueue.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient {
    private String id;
    private String fullName;
    private int age;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private LocalDateTime createdAt;

    // Derived / historical metrics
    private String primaryPhysician;
    private int totalAppointments;
    private int attendedAppointments;
    private int noShowCount;
    private LocalDate lastVisitDate;
    private String medicalNotes;

    public Patient(String id, String fullName, int age, String gender, String phone, String email, String address) {
        this(id, fullName, age, gender, phone, email, address, LocalDateTime.now());
    }

    public Patient(String id, String fullName, int age, String gender, String phone, String email, String address, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.primaryPhysician = "Hospital Clinical Staff";
        this.totalAppointments = 0;
        this.attendedAppointments = 0;
        this.noShowCount = 0;
        this.lastVisitDate = LocalDate.now().minusWeeks(2);
        this.medicalNotes = "Standard outpatient clinical registry";
    }

    public Patient(String id, String firstName, String lastName, String gender, int age, String phone,
                   String email, String primaryPhysician,
                   int totalAppointments, int attendedAppointments, int noShowCount,
                   LocalDate lastVisitDate, String medicalNotes) {
        this.id = id;
        this.fullName = firstName + " " + lastName;
        this.gender = gender;
        this.age = age;
        this.phone = phone;
        this.email = email;
        this.address = "Hospital Outpatient";
        this.createdAt = LocalDateTime.now();
        this.primaryPhysician = primaryPhysician;
        this.totalAppointments = totalAppointments;
        this.attendedAppointments = attendedAppointments;
        this.noShowCount = noShowCount;
        this.lastVisitDate = lastVisitDate;
        this.medicalNotes = medicalNotes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.split(" ");
        return parts[0];
    }

    public String getLastName() {
        if (fullName == null || fullName.isBlank()) return "";
        int idx = fullName.lastIndexOf(" ");
        return idx > 0 ? fullName.substring(idx + 1) : fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPrimaryPhysician() {
        return primaryPhysician;
    }

    public void setPrimaryPhysician(String primaryPhysician) {
        this.primaryPhysician = primaryPhysician;
    }

    public int getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(int totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public int getAttendedAppointments() {
        return attendedAppointments;
    }

    public void setAttendedAppointments(int attendedAppointments) {
        this.attendedAppointments = attendedAppointments;
    }

    public int getNoShowCount() {
        return noShowCount;
    }

    public void setNoShowCount(int noShowCount) {
        this.noShowCount = noShowCount;
    }

    public LocalDate getLastVisitDate() {
        return lastVisitDate != null ? lastVisitDate : LocalDate.now().minusWeeks(2);
    }

    public void setLastVisitDate(LocalDate lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }

    public String getMedicalNotes() {
        return medicalNotes;
    }

    public void setMedicalNotes(String medicalNotes) {
        this.medicalNotes = medicalNotes;
    }

    public double getAttendanceRate() {
        if (totalAppointments == 0) return 100.0;
        return (double) attendedAppointments / totalAppointments * 100.0;
    }

    @Override
    public String toString() {
        return fullName + " (" + id + ")";
    }
}
