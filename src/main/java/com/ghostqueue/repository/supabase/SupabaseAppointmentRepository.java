package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.repository.AppointmentRepository;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseAppointmentRepository implements AppointmentRepository {

    private static final Logger LOGGER = Logger.getLogger(SupabaseAppointmentRepository.class.getName());
    private final SupabaseClient client = SupabaseClient.getInstance();

    @Override
    public List<Appointment> findAll() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("select", "*,patients(full_name),doctors(full_name)");
            params.put("order", "appointment_date.asc,appointment_time.asc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointments"), params);
            List<Appointment> list = mapAppointments(resp.asList());
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch appointments from Supabase, serving fallback: " + e.getMessage());
        }
        return getFallbackAppointments();
    }

    @Override
    public Optional<Appointment> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appointment_id", "eq." + id.trim());
            params.put("select", "*,patients(full_name),doctors(full_name)");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointments"), params);
            List<Appointment> list = mapAppointments(resp.asList());
            if (!list.isEmpty()) {
                return Optional.of(list.get(0));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointment by ID: " + id, e);
        }
        return getFallbackAppointments().stream().filter(a -> a.getId().equalsIgnoreCase(id.trim())).findFirst();
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        if (date == null) return Collections.emptyList();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appointment_date", "eq." + date);
            params.put("select", "*,patients(full_name),doctors(full_name)");
            params.put("order", "appointment_time.asc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointments"), params);
            List<Appointment> list = mapAppointments(resp.asList());
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by date: " + date, e);
        }
        return getFallbackAppointments().stream().filter(a -> date.equals(a.getAppointmentDate())).toList();
    }

    @Override
    public List<Appointment> findByDepartment(Department department) {
        if (department == null) return findAll();
        List<Appointment> all = findAll();
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment a : all) {
            if (department == a.getDepartment()) filtered.add(a);
        }
        return filtered;
    }

    @Override
    public List<Appointment> findByStatus(AppointmentStatus status) {
        if (status == null) return findAll();
        List<Appointment> all = findAll();
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment a : all) {
            if (status == a.getStatus()) filtered.add(a);
        }
        return filtered;
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) {
        if (patientId == null || patientId.isBlank()) return Collections.emptyList();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("patient_id", "eq." + patientId.trim());
            params.put("select", "*,patients(full_name),doctors(full_name)");
            params.put("order", "appointment_date.desc,appointment_time.desc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointments"), params);
            List<Appointment> list = mapAppointments(resp.asList());
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by patient: " + patientId, e);
        }
        return getFallbackAppointments().stream().filter(a -> patientId.trim().equalsIgnoreCase(a.getPatientId())).toList();
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) return Collections.emptyList();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("doctor_id", "eq." + doctorId.trim());
            params.put("select", "*,patients(full_name),doctors(full_name)");
            params.put("order", "appointment_date.asc,appointment_time.asc");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("appointments"), params);
            List<Appointment> list = mapAppointments(resp.asList());
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by doctor: " + doctorId, e);
        }
        return getFallbackAppointments().stream().filter(a -> doctorId.trim().equalsIgnoreCase(a.getDoctorId())).toList();
    }

    @Override
    public List<Appointment> filter(String query, Department department, String doctorId, AppointmentStatus status, LocalDate date) {
        List<Appointment> all = findAll();
        List<Appointment> filtered = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : null;

        for (Appointment a : all) {
            if (q != null && !q.isEmpty()) {
                boolean matchId = a.getId() != null && a.getId().toLowerCase().contains(q);
                boolean matchPatient = a.getPatientName() != null && a.getPatientName().toLowerCase().contains(q);
                boolean matchDoctor = a.getDoctorName() != null && a.getDoctorName().toLowerCase().contains(q);
                boolean matchPatientId = a.getPatientId() != null && a.getPatientId().toLowerCase().contains(q);
                if (!matchId && !matchPatient && !matchDoctor && !matchPatientId) continue;
            }
            if (department != null && a.getDepartment() != department) continue;
            if (doctorId != null && !doctorId.isBlank() && !doctorId.equalsIgnoreCase(a.getDoctorId())) continue;
            if (status != null && a.getStatus() != status) continue;
            if (date != null && !date.equals(a.getAppointmentDate())) continue;

            filtered.add(a);
        }
        return filtered;
    }

    @Override
    public void save(Appointment appointment) {
        saveTransactional(appointment, "Appointment created");
    }

    @Override
    public void saveTransactional(Appointment appointment, String remarks) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("appointment_id", appointment.getId());
            data.put("patient_id", appointment.getPatientId());
            data.put("doctor_id", appointment.getDoctorId());
            data.put("department", appointment.getDepartment() != null ? appointment.getDepartment().getDisplayName() : "General Medicine");
            data.put("appointment_date", appointment.getAppointmentDate() != null ? appointment.getAppointmentDate().toString() : LocalDate.now().toString());
            data.put("appointment_time", appointment.getAppointmentTime() != null ? appointment.getAppointmentTime().toString() : "09:00:00");
            data.put("appointment_status", appointment.getStatus() != null ? appointment.getStatus().name() : "CONFIRMED");
            data.put("booking_date", appointment.getBookingDate() != null ? appointment.getBookingDate().toString() : LocalDate.now().toString());
            if (appointment.getCancellationReason() != null) {
                data.put("cancellation_reason", appointment.getCancellationReason());
            }

            client.post(SupabaseConfig.getRestEndpoint("appointments"), data);

            // Audit history record
            Map<String, Object> history = new HashMap<>();
            history.put("appointment_id", appointment.getId());
            history.put("previous_status", null);
            history.put("new_status", appointment.getStatus() != null ? appointment.getStatus().name() : "CONFIRMED");
            history.put("remarks", remarks != null ? remarks : "Appointment created");
            client.post(SupabaseConfig.getRestEndpoint("appointment_history"), history);

            LOGGER.info("Appointment successfully saved to Supabase: " + appointment.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save appointment to Supabase: " + appointment.getId(), e);
            throw new RuntimeException("Could not save appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Appointment appointment) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("appointment_date", appointment.getAppointmentDate().toString());
            data.put("appointment_time", appointment.getAppointmentTime().toString());
            data.put("appointment_status", appointment.getStatus().name());
            data.put("department", appointment.getDepartment().getDisplayName());
            if (appointment.getCancellationReason() != null) {
                data.put("cancellation_reason", appointment.getCancellationReason());
            }

            Map<String, String> filter = Map.of("appointment_id", "eq." + appointment.getId());
            client.patch(SupabaseConfig.getRestEndpoint("appointments"), filter, data);
            LOGGER.info("Appointment updated in Supabase: " + appointment.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update appointment: " + appointment.getId(), e);
            throw new RuntimeException("Could not update appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(String appointmentId, AppointmentStatus newStatus, String cancellationReason, String remarks) {
        try {
            Optional<Appointment> opt = findById(appointmentId);
            String prevStatus = opt.map(a -> a.getStatus().name()).orElse("UNKNOWN");

            Map<String, Object> data = new HashMap<>();
            data.put("appointment_status", newStatus.name());
            if (cancellationReason != null) {
                data.put("cancellation_reason", cancellationReason);
            }
            if (remarks != null && !remarks.isBlank()) {
                data.put("clinical_notes", remarks);
            }

            Map<String, String> filter = Map.of("appointment_id", "eq." + appointmentId);
            client.patch(SupabaseConfig.getRestEndpoint("appointments"), filter, data);

            // Audit history
            Map<String, Object> history = new HashMap<>();
            history.put("appointment_id", appointmentId);
            history.put("previous_status", prevStatus);
            history.put("new_status", newStatus.name());
            history.put("remarks", remarks != null && !remarks.isBlank() ? remarks : ("Status updated to " + newStatus.name()));
            client.post(SupabaseConfig.getRestEndpoint("appointment_history"), history);

            LOGGER.info("Appointment status updated in Supabase: " + appointmentId + " -> " + newStatus.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update appointment status: " + appointmentId, e);
            throw new RuntimeException("Could not update status: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            Map<String, String> filter = Map.of("appointment_id", "eq." + id);
            client.delete(SupabaseConfig.getRestEndpoint("appointments"), filter);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete appointment: " + id, e);
        }
    }

    @Override
    public boolean hasConflictForDoctor(String doctorId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        List<Appointment> appts = findByDate(date);
        for (Appointment a : appts) {
            if (excludeAppointmentId != null && excludeAppointmentId.equalsIgnoreCase(a.getId())) continue;
            if (doctorId.equalsIgnoreCase(a.getDoctorId()) &&
                a.getAppointmentTime().equals(time) &&
                a.getStatus() != AppointmentStatus.CANCELLED &&
                a.getStatus() != AppointmentStatus.NO_SHOW) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasConflictForPatient(String patientId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        List<Appointment> appts = findByDate(date);
        for (Appointment a : appts) {
            if (excludeAppointmentId != null && excludeAppointmentId.equalsIgnoreCase(a.getId())) continue;
            if (patientId.equalsIgnoreCase(a.getPatientId()) &&
                a.getAppointmentTime().equals(time) &&
                a.getStatus() != AppointmentStatus.CANCELLED &&
                a.getStatus() != AppointmentStatus.NO_SHOW) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int countTotal() {
        return findAll().size();
    }

    @Override
    public int countToday() {
        return findByDate(LocalDate.now()).size();
    }

    @Override
    public int countByStatus(AppointmentStatus status) {
        return findByStatus(status).size();
    }

    private static Appointment makeFallback(String id, String patientId, String patientName, int patientAge, String patientPhone,
                                            String doctorId, String doctorName, Department department, LocalDate date, LocalTime time,
                                            AppointmentStatus status, String reason, int leadDays, double score, RiskLevel level) {
        Appointment a = new Appointment(id, patientId, doctorId, date, time, status, date.minusDays(leadDays), null);
        a.setPatientName(patientName);
        a.setPatientAge(patientAge);
        a.setPatientPhone(patientPhone);
        a.setDoctorName(doctorName);
        a.setDepartment(department);
        a.setReasonForVisit(reason);
        a.setLeadDays(leadDays);
        a.setPredictedRiskScore(score);
        return a;
    }

    public static List<Appointment> getFallbackAppointments() {
        LocalDate today = LocalDate.now();
        List<Appointment> list = new ArrayList<>();
        list.add(makeFallback("APT-1001", "PT-1001", "Eleanor Vance", 58, "+1 (555) 234-8910", "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY, today, LocalTime.of(8, 30), AppointmentStatus.COMPLETED, "BP stable 128/82", 14, 0.15, RiskLevel.LOW));
        list.add(makeFallback("APT-1002", "PT-1004", "David Sterling", 27, "+1 (555) 987-6543", "DOC-103", "Dr. James Wilson", Department.ORTHOPEDICS, today, LocalTime.of(9, 15), AppointmentStatus.CANCELLED, "Patient transport breakdown", 30, 0.85, RiskLevel.HIGH));
        list.add(makeFallback("APT-1003", "PT-1003", "Maria Gomez", 40, "+1 (555) 345-6789", "DOC-102", "Dr. David Kim", Department.NEUROLOGY, today, LocalTime.of(10, 0), AppointmentStatus.CONFIRMED, "Migraine follow-up", 7, 0.22, RiskLevel.LOW));
        list.add(makeFallback("APT-1004", "PT-1007", "Chloe Bennett", 31, "+1 (555) 789-0123", "DOC-106", "Dr. Thomas Wright", Department.GENERAL_MEDICINE, today, LocalTime.of(11, 0), AppointmentStatus.CONFIRMED, "Annual physical", 25, 0.45, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1005", "PT-1008", "Samuel O Connor", 69, "+1 (555) 890-1234", "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY, today, LocalTime.of(11, 45), AppointmentStatus.CONFIRMED, "Post-stent surveillance", 10, 0.28, RiskLevel.LOW));
        list.add(makeFallback("APT-1006", "PT-1010", "Brandon Lee", 56, "+1 (555) 012-3456", "DOC-104", "Dr. Evelyn Reed", Department.ONCOLOGY, today, LocalTime.of(13, 30), AppointmentStatus.BOOKED, "Chemotherapy review", 35, 0.72, RiskLevel.HIGH));
        list.add(makeFallback("APT-1007", "PT-1006", "Lucas Chen", 7, "+1 (555) 678-9012", "DOC-105", "Dr. Sarah Lin", Department.PEDIATRICS, today, LocalTime.of(14, 15), AppointmentStatus.CONFIRMED, "Pediatric allergy test", 4, 0.18, RiskLevel.LOW));
        list.add(makeFallback("APT-1008", "PT-1013", "Victoria Ross", 48, "+1 (555) 345-6780", "DOC-102", "Dr. David Kim", Department.NEUROLOGY, today, LocalTime.of(15, 0), AppointmentStatus.BOOKED, "Neuropathy consult", 28, 0.55, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1009", "PT-1014", "Daniel Kovacs", 37, "+1 (555) 456-7891", "DOC-106", "Dr. Thomas Wright", Department.GENERAL_MEDICINE, today, LocalTime.of(15, 45), AppointmentStatus.CONFIRMED, "Routine checkup", 5, 0.20, RiskLevel.LOW));
        list.add(makeFallback("APT-1010", "PT-1011", "Priya Sharma", 26, "+1 (555) 123-4567", "DOC-106", "Dr. Thomas Wright", Department.GENERAL_MEDICINE, today, LocalTime.of(16, 30), AppointmentStatus.NO_SHOW, "Unannounced absence", 18, 0.78, RiskLevel.HIGH));

        list.add(makeFallback("APT-1011", "PT-1002", "Arthur Pendelton", 52, "+1 (555) 876-1234", "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY, today.plusDays(1), LocalTime.of(9, 0), AppointmentStatus.CONFIRMED, "Hypertension follow-up", 12, 0.25, RiskLevel.LOW));
        list.add(makeFallback("APT-1012", "PT-1005", "Amina Al-Mansoor", 35, "+1 (555) 456-7890", "DOC-104", "Dr. Evelyn Reed", Department.ONCOLOGY, today.plusDays(1), LocalTime.of(10, 30), AppointmentStatus.CONFIRMED, "Oncology consult", 8, 0.19, RiskLevel.LOW));
        list.add(makeFallback("APT-1013", "PT-1012", "Jonathan Hayes", 61, "+1 (555) 234-5678", "DOC-103", "Dr. James Wilson", Department.ORTHOPEDICS, today.plusDays(1), LocalTime.of(11, 15), AppointmentStatus.CONFIRMED, "Total knee evaluation", 9, 0.32, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1014", "PT-1015", "Hannah Schmidt", 60, "+1 (555) 567-8902", "DOC-104", "Dr. Evelyn Reed", Department.ONCOLOGY, today.plusDays(1), LocalTime.of(13, 0), AppointmentStatus.BOOKED, "Follow-up lab review", 15, 0.40, RiskLevel.MEDIUM));
        list.add(makeFallback("APT-1015", "PT-1009", "Fatima Zahra", 44, "+1 (555) 901-2345", "DOC-102", "Dr. David Kim", Department.NEUROLOGY, today.plusDays(1), LocalTime.of(14, 0), AppointmentStatus.CONFIRMED, "Headache clinic", 11, 0.24, RiskLevel.LOW));
        list.add(makeFallback("APT-1016", "PT-1001", "Eleanor Vance", 58, "+1 (555) 234-8910", "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY, today.plusDays(1), LocalTime.of(15, 30), AppointmentStatus.BOOKED, "Echocardiogram consult", 14, 0.38, RiskLevel.MEDIUM));

        list.add(makeFallback("APT-1025", "PT-1001", "Eleanor Vance", 58, "+1 (555) 234-8910", "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY, today.minusDays(28), LocalTime.of(9, 0), AppointmentStatus.COMPLETED, "Echocardiogram normal", 45, 0.12, RiskLevel.LOW));
        list.add(makeFallback("APT-1026", "PT-1002", "Arthur Pendelton", 52, "+1 (555) 876-1234", "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY, today.minusDays(21), LocalTime.of(10, 0), AppointmentStatus.NO_SHOW, "Unreported absence", 35, 0.82, RiskLevel.HIGH));
        list.add(makeFallback("APT-1027", "PT-1008", "Samuel O Connor", 69, "+1 (555) 890-1234", "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY, today.minusDays(14), LocalTime.of(11, 0), AppointmentStatus.COMPLETED, "Stent check clear", 24, 0.15, RiskLevel.LOW));
        list.add(makeFallback("APT-1028", "PT-1012", "Jonathan Hayes", 61, "+1 (555) 234-5678", "DOC-103", "Dr. James Wilson", Department.ORTHOPEDICS, today.minusDays(10), LocalTime.of(14, 0), AppointmentStatus.COMPLETED, "Post-op flexion 115 deg", 20, 0.20, RiskLevel.LOW));
        list.add(makeFallback("APT-1029", "PT-1004", "David Sterling", 27, "+1 (555) 987-6543", "DOC-103", "Dr. James Wilson", Department.ORTHOPEDICS, today.minusDays(7), LocalTime.of(15, 0), AppointmentStatus.CANCELLED, "Work conflict", 14, 0.65, RiskLevel.HIGH));
        list.add(makeFallback("APT-1030", "PT-1014", "Daniel Kovacs", 37, "+1 (555) 456-7891", "DOC-106", "Dr. Thomas Wright", Department.GENERAL_MEDICINE, today.minusDays(3), LocalTime.of(9, 30), AppointmentStatus.COMPLETED, "Annual physical completed", 10, 0.14, RiskLevel.LOW));
        return list;
    }

    private List<Appointment> mapAppointments(List<Map<String, Object>> rows) {
        List<Appointment> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("appointment_id");
            String patientId = (String) r.get("patient_id");
            String doctorId = (String) r.get("doctor_id");
            String deptStr = (String) r.get("department");
            Department department = Department.fromString(deptStr);

            LocalDate date = LocalDate.now();
            if (r.get("appointment_date") instanceof String ds) {
                try { date = LocalDate.parse(ds); } catch (Exception ignored) {}
            }

            LocalTime time = LocalTime.of(9, 0);
            if (r.get("appointment_time") instanceof String ts) {
                try {
                    if (ts.length() > 5) ts = ts.substring(0, 5);
                    time = LocalTime.parse(ts);
                } catch (Exception ignored) {}
            }

            String statusStr = (String) r.getOrDefault("appointment_status", "CONFIRMED");
            AppointmentStatus status = AppointmentStatus.fromString(statusStr);

            LocalDate bookingDate = date.minusDays(7);
            if (r.get("booking_date") instanceof String bs) {
                try { bookingDate = LocalDate.parse(bs); } catch (Exception ignored) {}
            }

            String cancellationReason = (String) r.get("cancellation_reason");

            String patientName = "Patient " + patientId;
            if (r.get("patients") instanceof Map<?, ?> pm && pm.get("full_name") != null) {
                patientName = (String) pm.get("full_name");
            }

            String doctorName = "Physician " + doctorId;
            if (r.get("doctors") instanceof Map<?, ?> dm && dm.get("full_name") != null) {
                doctorName = (String) dm.get("full_name");
            }

            Appointment appt = new Appointment(
                    id, patientId, doctorId, date, time, status, bookingDate, cancellationReason
            );
            appt.setPatientName(patientName);
            appt.setDoctorName(doctorName);
            appt.setDepartment(department);
            list.add(appt);
        }
        return list;
    }
}
