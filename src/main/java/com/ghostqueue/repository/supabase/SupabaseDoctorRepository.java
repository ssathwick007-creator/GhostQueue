package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.Doctor;
import com.ghostqueue.repository.DoctorRepository;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseDoctorRepository implements DoctorRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabaseDoctorRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public List<Doctor> findAll() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*");
            params.put("order", "full_name.asc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("doctors"), params);
            return mapDoctors(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch doctors from Supabase", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<Doctor> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("doctor_id", "eq." + id.trim());
            params.put("select", "*");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("doctors"), params);
            List<Doctor> list = mapDoctors(resp.asList());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding doctor by ID: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<Doctor> findByDepartment(String department) {
        if (department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)) {
            return findAll();
        }
        List<Doctor> all = findAll();
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : all) {
            if (department.equalsIgnoreCase(d.getDepartment())) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public List<Doctor> search(String query) {
        if (query == null || query.isBlank()) return findAll();
        String q = query.trim().toLowerCase();
        List<Doctor> all = findAll();
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : all) {
            if ((d.getId() != null && d.getId().toLowerCase().contains(q)) ||
                (d.getFullName() != null && d.getFullName().toLowerCase().contains(q)) ||
                (d.getDepartment() != null && d.getDepartment().toLowerCase().contains(q)) ||
                (d.getSpecialization() != null && d.getSpecialization().toLowerCase().contains(q))) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public void save(Doctor doctor) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("doctor_id", doctor.getId());
            data.put("doctor_identifier", "DOC-" + doctor.getId());
            data.put("full_name", doctor.getFullName());
            data.put("department", doctor.getDepartment());
            data.put("specialization", doctor.getSpecialization());
            data.put("phone", doctor.getPhone());
            data.put("availability_status", doctor.getAvailabilityStatus());
            data.put("account_status", "ACTIVE");

            client.post(SupabaseConfig.getRestEndpoint("doctors"), data);
            LOGGER.info("Doctor saved to Supabase: " + doctor.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save doctor: " + doctor.getId(), e);
            throw new RuntimeException("Could not save doctor: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Doctor doctor) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("full_name", doctor.getFullName());
            data.put("department", doctor.getDepartment());
            data.put("specialization", doctor.getSpecialization());
            data.put("phone", doctor.getPhone());
            data.put("availability_status", doctor.getAvailabilityStatus());

            Map<String, String> filter = Map.of("doctor_id", "eq." + doctor.getId());
            client.patch(SupabaseConfig.getRestEndpoint("doctors"), filter, data);
            LOGGER.info("Doctor updated in Supabase: " + doctor.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update doctor: " + doctor.getId(), e);
            throw new RuntimeException("Could not update doctor: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateAvailability(String doctorId, String status) {
        try {
            Map<String, Object> data = Map.of("availability_status", status != null ? status : "AVAILABLE");
            Map<String, String> filter = Map.of("doctor_id", "eq." + doctorId);
            client.patch(SupabaseConfig.getRestEndpoint("doctors"), filter, data);
            LOGGER.info("Doctor availability updated in Supabase: " + doctorId + " -> " + status);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update doctor availability: " + doctorId, e);
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    private List<Doctor> mapDoctors(List<Map<String, Object>> rows) {
        List<Doctor> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("doctor_id");
            String name = (String) r.get("full_name");
            String dept = (String) r.get("department");
            String spec = (String) r.get("specialization");
            String phone = (String) r.get("phone");
            String status = (String) r.getOrDefault("availability_status", "AVAILABLE");

            Doctor d = new Doctor(id, name, dept, spec, phone, status);
            list.add(d);
        }
        return list;
    }
}
