package com.ghostqueue.repository.mock;

import com.ghostqueue.model.Patient;
import com.ghostqueue.repository.PatientRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MockPatientRepository implements PatientRepository {
    private final Map<String, Patient> patients = new ConcurrentHashMap<>();

    public MockPatientRepository() {
        seedPatients();
    }

    private void seedPatients() {
        add(new Patient("PT-1001", "Eleanor", "Vance", "Female", 68, "+1 (555) 234-8910", "eleanor.vance@email.com",
                "Dr. Robert Vance", 12, 11, 1, LocalDate.now().minusWeeks(2),
                "Hypertension Stage 2, Type 2 Diabetes. Highly compliant with follow-ups."));

        add(new Patient("PT-1002", "Arthur", "Pendelton", "Male", 74, "+1 (555) 876-1234", "a.pendelton@email.com",
                "Dr. Robert Vance", 8, 4, 3, LocalDate.now().minusMonths(3),
                "Chronic heart failure (NYHA Class II). Frequently misses morning appointments due to transport."));

        add(new Patient("PT-1003", "Maria", "Gomez", "Female", 42, "+1 (555) 345-6789", "maria.g@email.com",
                "Dr. David Kim", 6, 6, 0, LocalDate.now().minusDays(18),
                "Post-concussive syndrome follow-up. Excellent adherence record."));

        add(new Patient("PT-1004", "David", "O'Reilly", "Male", 51, "+1 (555) 456-7890", "d.oreilly@email.com",
                "Dr. James Wilson", 9, 6, 2, LocalDate.now().minusMonths(1),
                "Lumbar disc herniation L4-L5. Works construction; weather impacts attendance."));

        add(new Patient("PT-1005", "Amina", "Al-Mansoor", "Female", 37, "+1 (555) 567-8901", "amina.m@email.com",
                "Dr. Evelyn Reed", 4, 4, 0, LocalDate.now().minusWeeks(3),
                "Post-mastectomy reconstructive surveillance. Prompt communication."));

        add(new Patient("PT-1006", "Lucas", "Chen", "Male", 11, "+1 (555) 678-9012", "chen.family@email.com",
                "Dr. Sarah Lin", 5, 5, 0, LocalDate.now().minusMonths(1),
                "Pediatric asthma management. Accompanied by parent Linda Chen. Consistent follow-ups."));

        add(new Patient("PT-1007", "Chloe", "Bennett", "Female", 33, "+1 (555) 789-0123", "c.bennett@email.com",
                "Dr. Thomas Wright", 7, 3, 4, LocalDate.now().minusMonths(5),
                "Recurrent allergic dermatitis. Lives 42 miles away; history of missed morning slots."));

        add(new Patient("PT-1008", "Samuel", "O'Connor", "Male", 71, "+1 (555) 890-1234", "sam.oconnor@email.com",
                "Dr. Robert Vance", 15, 14, 1, LocalDate.now().minusDays(10),
                "Coronary artery disease, dual stent recipient. Routine echocardiogram follow-up."));

        add(new Patient("PT-1009", "Fatima", "Zahra", "Female", 46, "+1 (555) 901-2345", "f.zahra@email.com",
                "Dr. Amanda Torres", 6, 5, 1, LocalDate.now().minusWeeks(4),
                "Cervical radiculopathy follow-up. Completed physical therapy cycle."));

        add(new Patient("PT-1010", "Brandon", "Lee", "Male", 58, "+1 (555) 012-3456", "brandon.lee@email.com",
                "Dr. Michael Chang", 11, 7, 4, LocalDate.now().minusMonths(3),
                "Chronic lymphocytic leukemia active surveillance. History of last-minute cancellations."));

        add(new Patient("PT-1011", "Priya", "Sharma", "Female", 28, "+1 (555) 123-4567", "priya.s@email.com",
                "Dr. Sophia Miller", 3, 3, 0, LocalDate.now().minusWeeks(5),
                "Atypical melanocytic nevus biopsy consultation."));

        add(new Patient("PT-1012", "Jonathan", "Hayes", "Male", 63, "+1 (555) 234-5678", "j.hayes@email.com",
                "Dr. James Wilson", 8, 8, 0, LocalDate.now().minusDays(14),
                "Right total hip arthroplasty, 6-month evaluation. Excellent mobility recovery."));

        add(new Patient("PT-1013", "Victoria", "Ross", "Female", 50, "+1 (555) 345-6780", "v.ross@email.com",
                "Dr. David Kim", 5, 2, 3, LocalDate.now().minusMonths(2),
                "Carpal tunnel syndrome bilateral. High cancellation rate due to caregiving responsibilities."));

        add(new Patient("PT-1014", "Daniel", "Kovacs", "Male", 39, "+1 (555) 456-7891", "d.kovacs@email.com",
                "Dr. Thomas Wright", 4, 4, 0, LocalDate.now().minusMonths(1),
                "Annual executive physical review. Lab work within standard bounds."));

        add(new Patient("PT-1015", "Hannah", "Schmidt", "Female", 62, "+1 (555) 567-8902", "h.schmidt@email.com",
                "Dr. Evelyn Reed", 10, 9, 1, LocalDate.now().minusWeeks(1),
                "Colorectal surveillance post-polypectomy. Steady clinical condition."));
    }

    private void add(Patient p) {
        patients.put(p.getId(), p);
    }

    @Override
    public List<Patient> findAll() {
        return new ArrayList<>(patients.values());
    }

    @Override
    public Optional<Patient> findById(String id) {
        return Optional.ofNullable(patients.get(id));
    }

    @Override
    public List<Patient> searchByNameOrId(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String q = query.toLowerCase().trim();
        return patients.values().stream()
                .filter(p -> p.getId().toLowerCase().contains(q) ||
                        p.getFullName().toLowerCase().contains(q) ||
                        p.getPhone().toLowerCase().contains(q) ||
                        p.getPrimaryPhysician().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Patient patient) {
        patients.put(patient.getId(), patient);
    }

    @Override
    public void update(Patient patient) {
        patients.put(patient.getId(), patient);
    }

    @Override
    public void delete(String id) {
        patients.remove(id);
    }

    @Override
    public boolean existsById(String id) {
        return patients.containsKey(id);
    }

    @Override
    public boolean existsByPhone(String phone, String excludePatientId) {
        if (phone == null || phone.isBlank()) return false;
        String clean = phone.replaceAll("[^0-9]", "");
        return patients.values().stream()
                .filter(p -> excludePatientId == null || !p.getId().equalsIgnoreCase(excludePatientId))
                .anyMatch(p -> p.getPhone().replaceAll("[^0-9]", "").equals(clean));
    }

    @Override
    public boolean hasAppointments(String patientId) {
        Patient p = patients.get(patientId);
        return p != null && p.getTotalAppointments() > 0;
    }

    @Override
    public int countTotal() {
        return patients.size();
    }
}
