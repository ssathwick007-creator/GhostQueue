package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.Patient;
import com.ghostqueue.repository.PatientRepository;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabasePatientRepository implements PatientRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabasePatientRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public List<Patient> findAll() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*");
            params.put("order", "full_name.asc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("patients"), params);
            return mapPatients(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch patients from Supabase", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<Patient> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("patient_id", "eq." + id.trim());
            params.put("select", "*");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("patients"), params);
            List<Patient> list = mapPatients(resp.asList());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding patient by ID: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<Patient> searchByNameOrId(String query) {
        if (query == null || query.isBlank()) return findAll();
        String q = query.trim().toLowerCase();
        List<Patient> all = findAll();
        List<Patient> filtered = new ArrayList<>();
        for (Patient p : all) {
            if ((p.getId() != null && p.getId().toLowerCase().contains(q)) ||
                (p.getFullName() != null && p.getFullName().toLowerCase().contains(q)) ||
                (p.getPhone() != null && p.getPhone().contains(q)) ||
                (p.getEmail() != null && p.getEmail().toLowerCase().contains(q))) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    @Override
    public void save(Patient patient) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("patient_id", patient.getId());
            data.put("full_name", patient.getFullName());
            data.put("gender", patient.getGender());
            data.put("phone", patient.getPhone());
            data.put("email", patient.getEmail());
            data.put("address", patient.getAddress());
            if (patient.getDateOfBirth() != null) {
                data.put("date_of_birth", patient.getDateOfBirth().toString());
            }

            client.post(SupabaseConfig.getRestEndpoint("patients"), data);
            LOGGER.info("Patient saved to Supabase: " + patient.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save patient: " + patient.getId(), e);
            throw new RuntimeException("Could not save patient: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Patient patient) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("full_name", patient.getFullName());
            data.put("gender", patient.getGender());
            data.put("phone", patient.getPhone());
            data.put("email", patient.getEmail());
            data.put("address", patient.getAddress());
            if (patient.getDateOfBirth() != null) {
                data.put("date_of_birth", patient.getDateOfBirth().toString());
            }

            Map<String, String> filter = Map.of("patient_id", "eq." + patient.getId());
            client.patch(SupabaseConfig.getRestEndpoint("patients"), filter, data);
            LOGGER.info("Patient updated in Supabase: " + patient.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update patient: " + patient.getId(), e);
            throw new RuntimeException("Could not update patient: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            Map<String, String> filter = Map.of("patient_id", "eq." + id);
            client.delete(SupabaseConfig.getRestEndpoint("patients"), filter);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete patient: " + id, e);
        }
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public boolean existsByPhone(String phone, String excludePatientId) {
        if (phone == null || phone.isBlank()) return false;
        List<Patient> all = findAll();
        for (Patient p : all) {
            if (excludePatientId != null && excludePatientId.equalsIgnoreCase(p.getId())) continue;
            if (phone.trim().equalsIgnoreCase(p.getPhone())) return true;
        }
        return false;
    }

    @Override
    public boolean hasAppointments(String patientId) {
        try {
            Map<String, String> params = Map.of("patient_id", "eq." + patientId, "select", "appointment_id", "limit", "1");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointments"), params);
            return !resp.asList().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    private List<Patient> mapPatients(List<Map<String, Object>> rows) {
        List<Patient> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("patient_id");
            String name = (String) r.get("full_name");
            String gender = (String) r.get("gender");
            String phone = (String) r.get("phone");
            String email = (String) r.get("email");
            String address = (String) r.get("address");

            LocalDate dob = null;
            int age = 35;
            Object dobObj = r.get("date_of_birth");
            if (dobObj instanceof String dobStr && !dobStr.isBlank()) {
                try {
                    dob = LocalDate.parse(dobStr);
                    age = Period.between(dob, LocalDate.now()).getYears();
                } catch (Exception ignored) {}
            }

            Patient p = new Patient(id, name, age, gender, phone, email, address);
            p.setDateOfBirth(dob);
            list.add(p);
        }
        return list;
    }
}
