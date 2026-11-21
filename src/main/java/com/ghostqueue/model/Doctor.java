package com.ghostqueue.model;

public class Doctor {
    private String id;
    private String fullName;
    private String department;
    private String specialization;
    private String phone;
    private String availabilityStatus;

    public Doctor(String id, String fullName, String department, String specialization, String phone, String availabilityStatus) {
        this.id = id;
        this.fullName = fullName;
        this.department = department;
        this.specialization = specialization;
        this.phone = phone;
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus : "AVAILABLE";
    }

    public Doctor(String id, String name, Department department, String specialty, String room) {
        this(id, name, department.getDisplayName(), specialty, "", "AVAILABLE");
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

    public String getName() {
        return fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getSpecialty() {
        return specialization;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(availabilityStatus);
    }

    public void setAvailable(boolean available) {
        this.availabilityStatus = available ? "AVAILABLE" : "BUSY";
    }

    @Override
    public String toString() {
        return fullName + " (" + department + ")";
    }
}
