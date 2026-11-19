package com.ghostqueue.repository.mock;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MockAppointmentRepository implements AppointmentRepository {
    private final Map<String, Appointment> appointments = new ConcurrentHashMap<>();

    public MockAppointmentRepository() {
        seedAppointments();
    }

    private void seedAppointments() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfter = today.plusDays(2);

        add(new Appointment("APT-801", "PT-1001", "Eleanor Vance", 68, "+1 (555) 234-8910",
                "Dr. Robert Vance", Department.CARDIOLOGY, today, LocalTime.of(8, 30),
                AppointmentStatus.CONFIRMED, "Echocardiogram Routine Review", 12,
                0.14, List.of("High patient adherence score", "Morning slot preference")));

        add(new Appointment("APT-802", "PT-1002", "Arthur Pendelton", 74, "+1 (555) 876-1234",
                "Dr. Robert Vance", Department.CARDIOLOGY, today, LocalTime.of(9, 15),
                AppointmentStatus.CONFIRMED, "Heart Failure Medication Adjustment", 28,
                0.82, List.of("Multiple previous no-shows", "Distance > 25 miles", "Adverse weather reported")));

        add(new Appointment("APT-803", "PT-1003", "Maria Gomez", 42, "+1 (555) 345-6789",
                "Dr. David Kim", Department.NEUROLOGY, today, LocalTime.of(10, 0),
                AppointmentStatus.IN_CONSULTATION, "Migraine Prophylaxis Assessment", 7,
                0.08, List.of("Excellent attendance record")));

        add(new Appointment("APT-804", "PT-1004", "David O'Reilly", 51, "+1 (555) 456-7890",
                "Dr. James Wilson", Department.ORTHOPEDICS, today, LocalTime.of(10, 45),
                AppointmentStatus.CANCELLED, "Lumbar Radiculopathy Follow-up", 18,
                0.73, List.of("Last-minute cancellation pattern", "Long travel transit")));

        add(new Appointment("APT-805", "PT-1005", "Amina Al-Mansoor", 37, "+1 (555) 567-8901",
                "Dr. Evelyn Reed", Department.ONCOLOGY, today, LocalTime.of(11, 30),
                AppointmentStatus.CONFIRMED, "Chemotherapy Tolerance Check", 4,
                0.19, List.of("High treatment adherence")));

        add(new Appointment("APT-806", "PT-1006", "Lucas Chen", 11, "+1 (555) 678-9012",
                "Dr. Sarah Lin", Department.PEDIATRICS, today, LocalTime.of(13, 15),
                AppointmentStatus.CONFIRMED, "Pediatric Asthma Follow-up", 9,
                0.25, List.of("Parent attended all visits")));

        add(new Appointment("APT-807", "PT-1007", "Chloe Bennett", 33, "+1 (555) 789-0123",
                "Dr. Thomas Wright", Department.DERMATOLOGY, today, LocalTime.of(14, 0),
                AppointmentStatus.PENDING, "Severe Atopic Dermatitis Review", 35,
                0.78, List.of("History of missed weekday slots", "Unconfirmed appointment notice")));

        add(new Appointment("APT-808", "PT-1008", "Samuel O'Connor", 71, "+1 (555) 890-1234",
                "Dr. Robert Vance", Department.CARDIOLOGY, today, LocalTime.of(14, 45),
                AppointmentStatus.CONFIRMED, "Post-Stent 90-Day Clinical Evaluation", 15,
                0.12, List.of("High reliability index")));

        add(new Appointment("APT-809", "PT-1009", "Fatima Zahra", 46, "+1 (555) 901-2345",
                "Dr. Amanda Torres", Department.NEUROLOGY, today, LocalTime.of(15, 30),
                AppointmentStatus.CONFIRMED, "Cervical Radiculopathy Nerve Study", 11,
                0.29, List.of("Standard confirmation received")));

        add(new Appointment("APT-810", "PT-1010", "Brandon Lee", 58, "+1 (555) 012-3456",
                "Dr. Michael Chang", Department.ONCOLOGY, today, LocalTime.of(16, 15),
                AppointmentStatus.CONFIRMED, "Chronic Leukemia Surveillance", 22,
                0.74, List.of("Late afternoon appointment slot", "Caregiver dependence")));

        add(new Appointment("APT-811", "PT-1011", "Priya Sharma", 28, "+1 (555) 123-4567",
                "Dr. Sophia Miller", Department.DERMATOLOGY, tomorrow, LocalTime.of(8, 45),
                AppointmentStatus.CONFIRMED, "Dysplastic Nevus Evaluation", 6,
                0.15, List.of("Timely SMS confirmation")));

        add(new Appointment("APT-812", "PT-1012", "Jonathan Hayes", 63, "+1 (555) 234-5678",
                "Dr. James Wilson", Department.ORTHOPEDICS, tomorrow, LocalTime.of(9, 30),
                AppointmentStatus.CONFIRMED, "Total Hip Replacement Assessment", 10,
                0.18, List.of("Regular rehabilitation clinic attendee")));

        add(new Appointment("APT-813", "PT-1013", "Victoria Ross", 50, "+1 (555) 345-6780",
                "Dr. David Kim", Department.NEUROLOGY, tomorrow, LocalTime.of(10, 15),
                AppointmentStatus.PENDING, "Carpal Tunnel EMG Study", 31,
                0.84, List.of("3 prior cancellations", "Unverified contact address")));

        add(new Appointment("APT-814", "PT-1014", "Daniel Kovacs", 39, "+1 (555) 456-7891",
                "Dr. Thomas Wright", Department.GENERAL_MEDICINE, tomorrow, LocalTime.of(11, 0),
                AppointmentStatus.CONFIRMED, "Annual Health Assessment", 5,
                0.20, List.of("Confirmed via patient portal")));

        add(new Appointment("APT-815", "PT-1015", "Hannah Schmidt", 62, "+1 (555) 567-8902",
                "Dr. Evelyn Reed", Department.ONCOLOGY, tomorrow, LocalTime.of(13, 30),
                AppointmentStatus.CONFIRMED, "Colorectal Surveillance Labs", 14,
                0.17, List.of("Steady appointment record")));

        add(new Appointment("APT-816", "PT-1002", "Arthur Pendelton", 74, "+1 (555) 876-1234",
                "Dr. Robert Vance", Department.CARDIOLOGY, dayAfter, LocalTime.of(9, 0),
                AppointmentStatus.CONFIRMED, "Repeat Blood Pressure Profiling", 21,
                0.76, List.of("High transport burden", "Age-related mobility friction")));

        add(new Appointment("APT-817", "PT-1007", "Chloe Bennett", 33, "+1 (555) 789-0123",
                "Dr. Sophia Miller", Department.DERMATOLOGY, dayAfter, LocalTime.of(11, 30),
                AppointmentStatus.CONFIRMED, "Topical Treatment Protocol", 19,
                0.68, List.of("Distant commute", "Incomplete check-in")));

        add(new Appointment("APT-818", "PT-1001", "Eleanor Vance", 68, "+1 (555) 234-8910",
                "Dr. Thomas Wright", Department.GENERAL_MEDICINE, dayAfter, LocalTime.of(14, 30),
                AppointmentStatus.CONFIRMED, "Blood Sugar Log Review", 8,
                0.11, List.of("High compliance score", "Short lead interval")));

        add(new Appointment("APT-819", "PT-1008", "Samuel O'Connor", 71, "+1 (555) 890-1234",
                "Dr. Robert Vance", Department.CARDIOLOGY, today.plusDays(3), LocalTime.of(9, 30),
                AppointmentStatus.CONFIRMED, "Lipid Optimization Check", 14,
                0.16, List.of("High adherence history")));

        add(new Appointment("APT-820", "PT-1010", "Brandon Lee", 58, "+1 (555) 012-3456",
                "Dr. Michael Chang", Department.ONCOLOGY, today.plusDays(4), LocalTime.of(10, 15),
                AppointmentStatus.PENDING, "Immunotherapy Tolerance Review", 27,
                0.79, List.of("Past no-shows detected", "Long commute transit")));
    }

    private void add(Appointment a) {
        appointments.put(a.getId(), a);
    }

    @Override
    public List<Appointment> findAll() {
        return new ArrayList<>(appointments.values()).stream()
                .sorted(Comparator.comparing(Appointment::getAppointmentDate)
                        .thenComparing(Appointment::getAppointmentTime))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return Optional.ofNullable(appointments.get(id));
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return appointments.values().stream()
                .filter(a -> a.getAppointmentDate().equals(date))
                .sorted(Comparator.comparing(Appointment::getAppointmentTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByDepartment(Department department) {
        if (department == Department.ALL) return findAll();
        return appointments.values().stream()
                .filter(a -> a.getDepartment() == department)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByStatus(AppointmentStatus status) {
        if (status == AppointmentStatus.ALL) return findAll();
        return appointments.values().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) {
        return appointments.values().stream()
                .filter(a -> a.getPatientId().equalsIgnoreCase(patientId))
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId) {
        return appointments.values().stream()
                .filter(a -> a.getDoctorId() != null && a.getDoctorId().equalsIgnoreCase(doctorId))
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).thenComparing(Appointment::getAppointmentTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> filter(String query, Department department, String doctorId, AppointmentStatus status, LocalDate date) {
        return appointments.values().stream()
                .filter(a -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase().trim();
                        boolean matchName = a.getPatientName().toLowerCase().contains(q);
                        boolean matchId = a.getPatientId().toLowerCase().contains(q);
                        boolean matchDoctor = a.getDoctorName().toLowerCase().contains(q);
                        boolean matchApptId = a.getId().toLowerCase().contains(q);
                        if (!matchName && !matchId && !matchDoctor && !matchApptId) return false;
                    }
                    if (department != null && department != Department.ALL && a.getDepartment() != department) {
                        return false;
                    }
                    if (doctorId != null && !doctorId.isBlank() && !doctorId.equalsIgnoreCase(a.getDoctorId())) {
                        return false;
                    }
                    if (status != null && status != AppointmentStatus.ALL && a.getStatus() != status) {
                        return false;
                    }
                    if (date != null && !a.getAppointmentDate().equals(date)) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).thenComparing(Appointment::getAppointmentTime))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Appointment appointment) {
        appointments.put(appointment.getId(), appointment);
    }

    @Override
    public void saveTransactional(Appointment appointment, String remarks) {
        appointments.put(appointment.getId(), appointment);
    }

    @Override
    public void update(Appointment appointment) {
        appointments.put(appointment.getId(), appointment);
    }

    @Override
    public void updateStatus(String appointmentId, AppointmentStatus newStatus, String cancellationReason, String remarks) {
        Appointment a = appointments.get(appointmentId);
        if (a != null) {
            a.setStatus(newStatus);
            if (cancellationReason != null) {
                a.setCancellationReason(cancellationReason);
            }
        }
    }

    @Override
    public void delete(String id) {
        appointments.remove(id);
    }

    @Override
    public boolean hasConflictForDoctor(String doctorId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        return appointments.values().stream()
                .filter(a -> excludeAppointmentId == null || !a.getId().equalsIgnoreCase(excludeAppointmentId))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .filter(a -> a.getAppointmentDate().equals(date) && a.getAppointmentTime().equals(time))
                .anyMatch(a -> a.getDoctorId() != null && a.getDoctorId().equalsIgnoreCase(doctorId));
    }

    @Override
    public boolean hasConflictForPatient(String patientId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        return appointments.values().stream()
                .filter(a -> excludeAppointmentId == null || !a.getId().equalsIgnoreCase(excludeAppointmentId))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .filter(a -> a.getAppointmentDate().equals(date) && a.getAppointmentTime().equals(time))
                .anyMatch(a -> a.getPatientId() != null && a.getPatientId().equalsIgnoreCase(patientId));
    }

    @Override
    public int countTotal() {
        return appointments.size();
    }

    @Override
    public int countToday() {
        LocalDate today = LocalDate.now();
        return (int) appointments.values().stream().filter(a -> a.getAppointmentDate().equals(today)).count();
    }

    @Override
    public int countByStatus(AppointmentStatus status) {
        return (int) appointments.values().stream().filter(a -> a.getStatus() == status).count();
    }
}
