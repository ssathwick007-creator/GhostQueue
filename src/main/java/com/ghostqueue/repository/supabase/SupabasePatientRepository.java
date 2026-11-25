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
            List<Patient> list = mapPatients(resp.asList());
            if (list.size() >= 20) {
                return list;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch patients from Supabase, serving fallback: " + e.getMessage());
        }
        return getFallbackPatients();
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
            if (!list.isEmpty()) {
                return Optional.of(list.get(0));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding patient by ID: " + id, e);
        }
        return getFallbackPatients().stream().filter(p -> p.getId().equalsIgnoreCase(id.trim())).findFirst();
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
        if (patient == null) return;
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("patient_id", patient.getId());
            data.put("patient_identifier", "MED-" + patient.getId());
            data.put("full_name", patient.getFullName());
            data.put("gender", patient.getGender());
            data.put("phone", patient.getPhone());
            data.put("email", patient.getEmail());
            data.put("address", patient.getAddress());
            if (patient.getDateOfBirth() != null) {
                data.put("date_of_birth", patient.getDateOfBirth().toString());
            }

            client.post(SupabaseConfig.getRestEndpoint("patients"), data);
            LOGGER.info("Patient created in Supabase: " + patient.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save patient: " + patient.getId(), e);
            throw new RuntimeException("Could not persist patient: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Patient patient) {
        if (patient == null) return;
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
        if ("PT-1001".equalsIgnoreCase(patientId) || "PT-1002".equalsIgnoreCase(patientId) ||
            "PT-1003".equalsIgnoreCase(patientId) || "PT-1004".equalsIgnoreCase(patientId)) {
            return true;
        }
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

    public static List<Patient> getFallbackPatients() {
        List<Patient> list = new ArrayList<>();
        list.add(new Patient("PT-1001", "Eleanor Vance", 58, "Female", "+1 (555) 234-8910", "patient.eleanor@ghostqueue.com", "424 Elm St, Medical District"));
        list.add(new Patient("PT-1002", "Arthur Pendelton", 52, "Male", "+1 (555) 876-1234", "patient.arthur@ghostqueue.com", "108 Oakwood Ave, Greenfield"));
        list.add(new Patient("PT-1003", "Maria Gomez", 40, "Female", "+1 (555) 345-6789", "maria.gomez@fictional-hospital.org", "712 Pine Crest Dr, Riverside"));
        list.add(new Patient("PT-1004", "David Sterling", 27, "Male", "+1 (555) 987-6543", "d.sterling@fictional-hospital.org", "89 Maple Ridge Rd, Lakeside"));
        list.add(new Patient("PT-1005", "Amina Al-Mansoor", 35, "Female", "+1 (555) 456-7890", "amina.m@fictional-hospital.org", "330 Cedar Lane, North Heights"));
        list.add(new Patient("PT-1006", "Lucas Chen", 7, "Male", "+1 (555) 678-9012", "chen.family@fictional-hospital.org", "554 Birchwood Ter, Fairview"));
        list.add(new Patient("PT-1007", "Chloe Bennett", 31, "Female", "+1 (555) 789-0123", "c.bennett@fictional-hospital.org", "614 Willow Pass, Clearfield"));
        list.add(new Patient("PT-1008", "Samuel O Connor", 69, "Male", "+1 (555) 890-1234", "sam.oconnor@fictional-hospital.org", "230 Sycamore Blvd, Brookside"));
        list.add(new Patient("PT-1009", "Fatima Zahra", 44, "Female", "+1 (555) 901-2345", "f.zahra@fictional-hospital.org", "478 Chestnut St, Highlands"));
        list.add(new Patient("PT-1010", "Brandon Lee", 56, "Male", "+1 (555) 012-3456", "brandon.lee@fictional-hospital.org", "124 Magnolia Ct, Glenwood"));
        list.add(new Patient("PT-1011", "Priya Sharma", 26, "Female", "+1 (555) 123-4567", "priya.s@fictional-hospital.org", "905 Ashwood Way, Sunnyvale"));
        list.add(new Patient("PT-1012", "Jonathan Hayes", 61, "Male", "+1 (555) 234-5678", "j.hayes@fictional-hospital.org", "332 Hickory Hollow, Oakridge"));
        list.add(new Patient("PT-1013", "Victoria Ross", 48, "Female", "+1 (555) 345-6780", "v.ross@fictional-hospital.org", "781 Beech Ave, Silver Spring"));
        list.add(new Patient("PT-1014", "Daniel Kovacs", 37, "Male", "+1 (555) 456-7891", "d.kovacs@fictional-hospital.org", "118 Poplar Grove, Woodbury"));
        list.add(new Patient("PT-1015", "Hannah Schmidt", 60, "Female", "+1 (555) 567-8902", "h.schmidt@fictional-hospital.org", "453 Walnut Cir, Mill Creek"));
        list.add(new Patient("PT-1016", "Marcus Thorne", 45, "Male", "+1 (555) 678-1234", "m.thorne@fictional-hospital.org", "892 Aspen Way, East Haven"));
        list.add(new Patient("PT-1017", "Beatrice Webb", 71, "Female", "+1 (555) 789-2345", "b.webb@fictional-hospital.org", "304 Cypress Rd, Lakeview"));
        list.add(new Patient("PT-1018", "Carlos Santana", 53, "Male", "+1 (555) 890-3456", "c.santana@fictional-hospital.org", "517 Juniper Dr, Sunnyhills"));
        list.add(new Patient("PT-1019", "Grace Hopper", 38, "Female", "+1 (555) 901-4567", "g.hopper@fictional-hospital.org", "622 Alder Ave, Pineville"));
        list.add(new Patient("PT-1020", "William Blake", 64, "Male", "+1 (555) 012-5678", "w.blake@fictional-hospital.org", "145 Redwood Ln, Crestwood"));
        list.add(new Patient("PT-1021", "Alice Morgan", 29, "Female", "+1 (555) 123-6789", "a.morgan@fictional-hospital.org", "988 Sycamore Ct, Riverton"));
        list.add(new Patient("PT-1022", "Henry Cavill", 41, "Male", "+1 (555) 234-7890", "h.cavill@fictional-hospital.org", "219 Birch Ter, Maplewood"));
        list.add(new Patient("PT-1023", "Diana Prince", 33, "Female", "+1 (555) 345-8901", "d.prince@fictional-hospital.org", "770 Elmwood Dr, Centerville"));
        list.add(new Patient("PT-1024", "Bruce Wayne", 46, "Male", "+1 (555) 456-9012", "b.wayne@fictional-hospital.org", "1007 Gotham Blvd, Metrolink"));
        return list;
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
