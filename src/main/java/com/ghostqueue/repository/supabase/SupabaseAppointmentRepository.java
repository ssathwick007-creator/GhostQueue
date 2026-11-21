package com.ghostqueue.repository.supabase;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.AppointmentHistory;
import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.Department;
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
            return mapAppointments(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch appointments from Supabase", e);
            return Collections.emptyList();
        }
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
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointment by ID: " + id, e);
            return Optional.empty();
        }
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
            return mapAppointments(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by date: " + date, e);
            return Collections.emptyList();
        }
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
            return mapAppointments(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by patient: " + patientId, e);
            return Collections.emptyList();
        }
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
            return mapAppointments(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding appointments by doctor: " + doctorId, e);
            return Collections.emptyList();
        }
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
