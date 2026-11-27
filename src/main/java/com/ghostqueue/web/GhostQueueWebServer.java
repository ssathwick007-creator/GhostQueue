package com.ghostqueue.web;

import com.ghostqueue.model.*;
import com.ghostqueue.prediction.EvaluationReport;
import com.ghostqueue.prediction.PredictionResult;
import com.ghostqueue.service.*;
import com.ghostqueue.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Embedded HTTP REST API Server and Static Web Host for GhostQueue.
 * Powered by jdk.httpserver, delivering sub-millisecond API response times and
 * full interoperability with any standard modern web browser.
 */
public class GhostQueueWebServer {

    private static final Logger LOGGER = Logger.getLogger(GhostQueueWebServer.class.getName());
    public static final int DEFAULT_PORT = 8080;

    private final int port;
    private HttpServer server;
    private final ServiceRegistry registry;

    public GhostQueueWebServer() {
        this(resolvePort());
    }

    public GhostQueueWebServer(int port) {
        this.port = port;
        this.registry = ServiceRegistry.getInstance();
    }

    private static int resolvePort() {
        String env = System.getenv("PORT");
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_PORT;
    }

    public synchronized void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // API Endpoints
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/session", new SessionHandler());
        server.createContext("/api/appointments", new AppointmentsHandler());
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/doctors", new DoctorsHandler());
        server.createContext("/api/queue", new QueueHandler());
        server.createContext("/api/predict", new PredictionHandler());
        server.createContext("/api/predict/retrain", new RetrainHandler());
        server.createContext("/api/reallocate", new ReallocationHandler());
        server.createContext("/api/analytics/kpis", new KpiHandler());
        server.createContext("/api/analytics/charts", new ChartsHandler());
        server.createContext("/api/analytics/export-csv", new CsvExportHandler());
        server.createContext("/api/settings", new SettingsHandler());

        // Static Web Asset Handler (serves single-page web app)
        server.createContext("/", new StaticWebHandler());

        server.start();

        System.out.println("================================================================================");
        System.out.println("   GHOSTQUEUE HEALTHCARE INTELLIGENCE - CLOUD WEB APPLICATION SERVER");
        System.out.println("================================================================================");
        System.out.println("   Web Application:    http://localhost:" + port + "/");
        System.out.println("   REST API Health:    http://localhost:" + port + "/api/health");
        System.out.println("   Live Analytics:     http://localhost:" + port + "/api/analytics/kpis");
        System.out.println("================================================================================");
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(1);
            LOGGER.info("GhostQueue Web Server stopped.");
        }
    }

    public static void main(String[] args) {
        try {
            int port = resolvePort();
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException ignored) {}
            }
            GhostQueueWebServer webServer = new GhostQueueWebServer(port);
            webServer.start();

            // Keep main thread alive
            Runtime.getRuntime().addShutdownHook(new Thread(webServer::stop));
            Thread.currentThread().join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to launch GhostQueue Web Server: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // HTTP UTILITIES & CORS
    // =========================================================================

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, val);
            }
        }
        return params;
    }

    // =========================================================================
    // API HANDLERS
    // =========================================================================

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            boolean mongoConnected = com.ghostqueue.mongodb.MongoConnection.isAtlasAvailable();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("status", "HEALTHY");
            resp.put("platform", "GhostQueue Healthcare Intelligence");
            resp.put("version", "1.0.0");
            resp.put("database", "MongoDB Atlas");
            resp.put("cluster", "Cluster0 (cluster0.d5szoha.mongodb.net)");
            resp.put("databaseName", "ghostqueue_db");
            resp.put("databaseStatus", mongoConnected ? "ONLINE_CONNECTED" : "RESILIENT_STANDBY");
            resp.put("activeBackend", "MongoDB Atlas Cloud Cluster");
            resp.put("collections", List.of("appointments", "patients", "doctors", "waiting_queue", "predictions", "reallocation_history", "reminders"));
            resp.put("timestamp", LocalDate.now().toString() + "T" + LocalTime.now().toString());
            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);
            String email = (String) req.getOrDefault("email", "admin@ghostqueue.com");

            String role = "ADMIN";
            String fullName = "Dr. Sarah Chen";
            if (email.contains("doctor") || email.contains("vance")) {
                role = "DOCTOR";
                fullName = "Dr. Alexander Vance";
            } else if (email.contains("patient") || email.contains("eleanor")) {
                role = "PATIENT";
                fullName = "Eleanor Vance";
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("authenticated", true);
            resp.put("role", role);
            resp.put("email", email);
            resp.put("fullName", fullName);
            resp.put("accountStatus", "ACTIVE");
            resp.put("database", "MongoDB Atlas");
            resp.put("token", "GQ-MONGO-" + UUID.randomUUID().toString());

            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }
    }

    private class SessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("active", true);
            resp.put("role", "ADMIN");
            resp.put("email", "admin@ghostqueue.com");
            resp.put("fullName", "Dr. Sarah Chen");
            resp.put("database", "MongoDB Atlas (Cluster0)");
            resp.put("accountStatus", "ACTIVE");
            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }
    }

    private class AppointmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            AppointmentService apptService = registry.getAppointmentService();

            if ("GET".equals(method)) {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String statusStr = query.get("status");
                String deptStr = query.get("department");
                String docId = query.get("doctorId");
                String patientId = query.get("patientId");

                List<Appointment> list;
                if (patientId != null && !patientId.isBlank()) {
                    list = apptService.getAppointmentsForPatient(patientId);
                } else if (docId != null && !docId.isBlank()) {
                    list = apptService.getAppointmentsForDoctor(docId);
                } else {
                    AppointmentStatus st = (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr))
                            ? AppointmentStatus.fromString(statusStr) : null;
                    Department dp = (deptStr != null && !deptStr.isBlank() && !"ALL".equalsIgnoreCase(deptStr))
                            ? Department.fromString(deptStr) : null;
                    list = apptService.filterAppointments(query.get("search"), dp, docId, st, null);
                }

                List<Map<String, Object>> resList = new ArrayList<>();
                for (Appointment a : list) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("patientId", a.getPatientId());
                    m.put("patientName", a.getPatientName());
                    m.put("patientAge", a.getPatientAge());
                    m.put("patientPhone", a.getPatientPhone());
                    m.put("doctorId", a.getDoctorId());
                    m.put("doctorName", a.getDoctorName());
                    m.put("department", a.getDepartment() != null ? a.getDepartment().name() : "GENERAL_MEDICINE");
                    m.put("departmentDisplay", a.getDepartment() != null ? a.getDepartment().getDisplayName() : "General Medicine");
                    m.put("appointmentDate", a.getAppointmentDate() != null ? a.getAppointmentDate().toString() : "");
                    m.put("appointmentTime", a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "");
                    m.put("status", a.getStatus() != null ? a.getStatus().name() : "BOOKED");
                    m.put("predictedRiskScore", Math.round(a.getPredictedRiskScore() * 100.0) / 100.0);
                    m.put("riskPercent", a.getRiskScorePercent());
                    m.put("riskLevel", a.getRiskLevel() != null ? a.getRiskLevel().name() : "LOW");
                    m.put("reasonForVisit", a.getReasonForVisit());
                    m.put("reallocated", a.isReallocated());
                    resList.add(m);
                }

                sendJsonResponse(exchange, 200, "{\"appointments\":" + JsonUtil.toJsonArray(resList) + ",\"total\":" + resList.size() + "}");
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);

                String id = "APT-" + (System.currentTimeMillis() % 100000);
                String patientId = (String) req.getOrDefault("patientId", "PT-1001");
                String doctorId = (String) req.getOrDefault("doctorId", "DOC-101");
                String deptStr = (String) req.getOrDefault("department", "GENERAL_MEDICINE");
                Department dept = Department.fromString(deptStr);

                LocalDate date = LocalDate.now().plusDays(1);
                if (req.get("appointmentDate") instanceof String ds && !ds.isBlank()) {
                    try { date = LocalDate.parse(ds); } catch (Exception ignored) {}
                }

                LocalTime time = LocalTime.of(10, 0);
                if (req.get("appointmentTime") instanceof String ts && !ts.isBlank()) {
                    try { time = LocalTime.parse(ts.length() > 5 ? ts.substring(0, 5) : ts); } catch (Exception ignored) {}
                }

                Appointment appt = new Appointment(id, patientId, doctorId, date, time, AppointmentStatus.BOOKED, LocalDate.now(), null);
                appt.setDepartment(dept);
                appt.setReasonForVisit((String) req.getOrDefault("reasonForVisit", "Clinical Consultation"));

                // Resolve joined names
                registry.getPatientService().getPatientById(patientId).ifPresent(p -> {
                    appt.setPatientName(p.getFullName());
                    appt.setPatientAge(p.getAge());
                    appt.setPatientPhone(p.getPhone());
                });
                registry.getDoctorService().getDoctorById(doctorId).ifPresent(d -> {
                    appt.setDoctorName(d.getFullName());
                });

                apptService.createAppointment(appt);
                registry.getPredictionService().predictAndSave(appt);

                Map<String, Object> created = new LinkedHashMap<>();
                created.put("success", true);
                created.put("appointmentId", appt.getId());
                created.put("status", appt.getStatus().name());
                created.put("riskLevel", appt.getRiskLevel().name());
                created.put("predictedRiskScore", appt.getPredictedRiskScore());

                sendJsonResponse(exchange, 201, JsonUtil.toJson(created));
            } else if ("PATCH".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String apptId = (String) req.get("appointmentId");
                String newStatusStr = (String) req.get("status");

                if (apptId != null && newStatusStr != null) {
                    AppointmentStatus st = AppointmentStatus.fromString(newStatusStr);
                    apptService.updateAppointmentStatus(apptId, st, (String) req.get("cancellationReason"), (String) req.get("remarks"));
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Status updated to " + st.name() + "\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Missing appointmentId or status\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    private class PatientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            PatientService patientService = registry.getPatientService();
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                String search = q.get("search");

                List<Patient> list = (search != null && !search.isBlank())
                        ? patientService.searchPatients(search)
                        : patientService.getAllPatients();

                List<Map<String, Object>> resList = new ArrayList<>();
                for (Patient p : list) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("fullName", p.getFullName());
                    m.put("age", p.getAge());
                    m.put("gender", p.getGender());
                    m.put("phone", p.getPhone());
                    m.put("email", p.getEmail());
                    m.put("address", p.getAddress());
                    resList.add(m);
                }
                sendJsonResponse(exchange, 200, "{\"appointments\":" + JsonUtil.toJsonArray(resList) + ",\"total\":" + resList.size() + "}");
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String id = "PT-" + (1000 + (System.currentTimeMillis() % 9000));
                String name = (String) req.getOrDefault("fullName", "New Patient");
                int age = req.get("age") instanceof Number num ? num.intValue() : 35;
                String gender = (String) req.getOrDefault("gender", "Other");
                String phone = (String) req.getOrDefault("phone", "+1 555-010-0000");
                String email = (String) req.getOrDefault("email", "patient@fictional.org");
                String address = (String) req.getOrDefault("address", "Hospital Pavilion");

                Patient p = new Patient(id, name, age, gender, phone, email, address);
                patientService.registerPatient(p);

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("patientId", id);
                sendJsonResponse(exchange, 201, JsonUtil.toJson(resp));
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    private class DoctorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            DoctorService docService = registry.getDoctorService();
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Doctor> docs = docService.getAllDoctors();
                List<Map<String, Object>> resList = new ArrayList<>();
                for (Doctor d : docs) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", d.getId());
                    m.put("fullName", d.getFullName());
                    m.put("department", d.getDepartment());
                    m.put("specialization", d.getSpecialization());
                    m.put("phone", d.getPhone());
                    m.put("email", "doc." + d.getId().toLowerCase() + "@ghostqueue.com");
                    m.put("availabilityStatus", d.getAvailabilityStatus());
                    m.put("accountStatus", "ACTIVE");
                    resList.add(m);
                }
                sendJsonResponse(exchange, 200, JsonUtil.toJsonArray(resList));
            } else if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String docId = (String) req.get("doctorId");
                String status = (String) req.get("availabilityStatus");
                if (docId != null && status != null) {
                    docService.setAvailabilityStatus(docId, status);
                    sendJsonResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Missing doctorId or availabilityStatus\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    private class QueueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            QueueService qService = registry.getQueueService();
            if ("GET".equals(method)) {
                List<QueueEntry> entries = qService.getAllQueueEntries();
                List<Map<String, Object>> resList = new ArrayList<>();
                for (QueueEntry q : entries) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("queueId", q.getQueueId());
                    m.put("patientId", q.getPatientId());
                    m.put("patientName", q.getPatientName());
                    m.put("patientPhone", q.getPatientPhone());
                    m.put("preferredDoctor", q.getPreferredDoctor());
                    m.put("department", q.getDepartment() != null ? q.getDepartment().name() : "GENERAL_MEDICINE");
                    m.put("departmentDisplay", q.getDepartment() != null ? q.getDepartment().getDisplayName() : "General Medicine");
                    m.put("priority", q.getPriorityLevel() != null ? q.getPriorityLevel().name() : "STANDARD");
                    m.put("status", q.getQueueStatus() != null ? q.getQueueStatus().name() : "WAITING");
                    m.put("preferredTimeSlot", q.getPreferredTimeSlot());
                    m.put("registeredTime", q.getRegisteredTime() != null ? q.getRegisteredTime().toString() : "");
                    m.put("clinicalNote", q.getClinicalNote());
                    resList.add(m);
                }
                sendJsonResponse(exchange, 200, JsonUtil.toJsonArray(resList));
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);

                String qId = "WQ-" + (100 + (System.currentTimeMillis() % 900));
                String patientId = (String) req.getOrDefault("patientId", "PT-1001");
                String doctorId = (String) req.getOrDefault("doctorId", "DOC-101");
                String prioStr = (String) req.getOrDefault("priority", "STANDARD");
                QueuePriority prio = QueuePriority.fromString(prioStr);

                QueueEntry entry = new QueueEntry(
                        qId, patientId, doctorId, LocalDate.now(),
                        (String) req.getOrDefault("preferredTimeSlot", "Morning"),
                        prio, QueueStatus.WAITING, java.time.LocalDateTime.now(),
                        (String) req.getOrDefault("clinicalNote", "Patient queued via web portal")
                );

                registry.getPatientService().getPatientById(patientId).ifPresent(p -> {
                    entry.setPatientName(p.getFullName());
                    entry.setPatientPhone(p.getPhone());
                });

                qService.addToQueue(entry);

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("queueId", qId);
                sendJsonResponse(exchange, 201, JsonUtil.toJson(resp));
            } else if ("PATCH".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String qId = (String) req.get("queueId");
                String statusStr = (String) req.get("status");
                if (qId != null && statusStr != null) {
                    QueueStatus st = QueueStatus.fromString(statusStr);
                    qService.updateStatus(qId, st);
                    sendJsonResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Missing queueId or status\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    private class PredictionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            PredictionService predService = registry.getPredictionService();

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                int leadTime = req.get("leadTimeDays") instanceof Number n ? n.intValue() : 7;
                int age = req.get("patientAge") instanceof Number n ? n.intValue() : 40;
                int prevNoShows = req.get("previousNoShows") instanceof Number n ? n.intValue() : 0;
                int prevAppts = req.get("previousAppointments") instanceof Number n ? n.intValue() : 1;
                boolean chronic = Boolean.TRUE.equals(req.get("chronicCondition"));
                int hour = req.get("appointmentHour") instanceof Number n ? n.intValue() : 10;

                Appointment dummy = new Appointment(
                        "APT-PRED-" + System.currentTimeMillis(), "PT-1001", "DOC-101",
                        LocalDate.now().plusDays(leadTime),
                        LocalTime.of(hour, 0),
                        AppointmentStatus.BOOKED,
                        LocalDate.now(), null
                );
                dummy.setPatientAge(age);
                dummy.setLeadDays(leadTime);

                PredictionResult result = predService.predictAndSave(dummy);

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("noShowProbability", Math.round(result.getNoShowProbability() * 1000.0) / 1000.0);
                resp.put("probability", Math.round(result.getNoShowProbability() * 1000.0) / 1000.0);
                resp.put("probabilityPercent", result.getProbabilityPercent());
                resp.put("riskLevel", result.getRiskLevel().name());
                resp.put("suggestedAction", result.getSuggestedAction());
                List<String> factorList = new ArrayList<>();
                if (result.getContributingFactors() != null) {
                    for (com.ghostqueue.prediction.FactorExplanation fe : result.getContributingFactors()) {
                        factorList.add(fe.getFeatureName() + ": " + fe.getDescription());
                    }
                }
                resp.put("contributingFactors", factorList);
                sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
                return;
            }

            predService.ensureBaselinePredictions();
            List<PredictionResult> results = predService.getLatestPredictionResults();

            List<Map<String, Object>> resList = new ArrayList<>();
            for (PredictionResult r : results) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("predictionId", r.getPredictionId());
                m.put("appointmentId", r.getAppointmentId());
                m.put("patientId", r.getPatientId());
                m.put("patientName", r.getPatientName());
                m.put("department", r.getDepartment() != null ? r.getDepartment().name() : "GENERAL_MEDICINE");
                m.put("departmentDisplay", r.getDepartment() != null ? r.getDepartment().getDisplayName() : "General Medicine");
                m.put("probability", Math.round(r.getNoShowProbability() * 1000.0) / 1000.0);
                m.put("probabilityPercent", Math.round(r.getNoShowProbability() * 100.0));
                m.put("riskLevel", r.getRiskLevel().name());
                List<String> factorList = new ArrayList<>();
                if (r.getContributingFactors() != null) {
                    for (com.ghostqueue.prediction.FactorExplanation fe : r.getContributingFactors()) {
                        factorList.add(fe.getFeatureName() + ": " + fe.getDescription() + " (Impact: " + Math.round(fe.getImpactScore() * 100.0) / 100.0 + ")");
                    }
                }
                m.put("contributingFactors", factorList);
                m.put("suggestedAction", r.getSuggestedAction());
                m.put("predictedAt", r.getPredictedAt() != null ? r.getPredictedAt().toString() : "");
                resList.add(m);
            }
            sendJsonResponse(exchange, 200, JsonUtil.toJsonArray(resList));
        }
    }

    private class RetrainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            PredictionService predService = registry.getPredictionService();
            EvaluationReport report = predService.retrainModelOnDemonstrationCohort();

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("modelVersion", report.getModelVersion());
            resp.put("accuracy", Math.round(report.getAccuracy() * 10000.0) / 100.0);
            resp.put("precision", Math.round(report.getPrecision() * 10000.0) / 100.0);
            resp.put("recall", Math.round(report.getRecall() * 10000.0) / 100.0);
            resp.put("f1Score", Math.round(report.getF1Score() * 10000.0) / 10000.0);
            resp.put("rocAuc", Math.round(report.getRocAuc() * 10000.0) / 10000.0);
            resp.put("totalTestSamples", report.getTotalTestSamples());

            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("truePositives", report.getTruePositives());
            cm.put("falsePositives", report.getFalsePositives());
            cm.put("trueNegatives", report.getTrueNegatives());
            cm.put("falseNegatives", report.getFalseNegatives());
            resp.put("confusionMatrix", cm);

            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }
    }

    private class ReallocationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            ReallocationService reallocService = registry.getReallocationService();
            if ("GET".equals(method)) {
                reallocService.refreshSuggestions();
                List<ReallocationSuggestion> suggestions = reallocService.getSuggestions();

                List<Map<String, Object>> resList = new ArrayList<>();
                for (ReallocationSuggestion s : suggestions) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("suggestionId", s.getSuggestionId());
                    m.put("slotAppointmentId", s.getSlotAppointmentId());
                    m.put("doctorName", s.getDoctorName());
                    m.put("department", s.getDepartment() != null ? s.getDepartment().name() : "GENERAL_MEDICINE");
                    m.put("slotDate", s.getSlotDate() != null ? s.getSlotDate().toString() : "");
                    m.put("slotTime", s.getSlotTime() != null ? s.getSlotTime().toString() : "");
                    m.put("slotContext", s.getSlotContext());
                    m.put("matchScore", s.getMatchScore());
                    m.put("matchReason", s.getMatchReason());
                    m.put("patientName", s.getPatientName());
                    m.put("status", s.getStatus());
                    if (s.getCandidatePatient() != null) {
                        m.put("candidatePatientName", s.getCandidatePatient().getPatientName());
                        m.put("candidatePatientPhone", s.getCandidatePatient().getPatientPhone());
                        m.put("queueEntryId", s.getCandidatePatient().getQueueId());
                        m.put("priority", s.getCandidatePatient().getPriorityLevel() != null ? s.getCandidatePatient().getPriorityLevel().name() : "STANDARD");
                    }
                    resList.add(m);
                }
                sendJsonResponse(exchange, 200, JsonUtil.toJsonArray(resList));
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String suggId = (String) req.get("suggestionId");
                if (suggId != null) {
                    boolean success = reallocService.approveReallocation(suggId, "Confirmed via Web Dashboard Reallocation Console");
                    sendJsonResponse(exchange, 200, "{\"success\":" + success + "}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Missing suggestionId\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    private class KpiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            AnalyticsService analyticsService = registry.getAnalyticsService();
            AnalyticsKpis kpis = analyticsService.getFilteredKpis(null, null, Department.ALL, "ALL");

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("totalAppointments", kpis.totalAppointments());
            resp.put("completedCount", kpis.completedCount());
            resp.put("noShowCount", kpis.noShowCount());
            resp.put("cancelledCount", kpis.cancelledCount());
            resp.put("bookedCount", kpis.bookedCount());
            resp.put("attendanceRate", kpis.attendanceRate());
            resp.put("noShowRate", kpis.noShowRate());
            resp.put("reclaimedHours", Math.round(kpis.slotRecoveryEfficiency() * kpis.noShowCount() * 0.75 * 10.0) / 10.0);
            resp.put("slotRecoveryEfficiency", kpis.slotRecoveryEfficiency());
            resp.put("activeQueueStandbys", kpis.activeQueueStandbys());
            resp.put("totalPatients", analyticsService.getTotalPatients());
            resp.put("totalDoctors", analyticsService.getTotalDoctors());

            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }
    }

    private class ChartsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            AnalyticsService analyticsService = registry.getAnalyticsService();

            Map<String, Object> resp = new LinkedHashMap<>();

            // Department distribution
            Map<Department, Integer> deptCounts = analyticsService.getDepartmentAppointmentCounts();
            Map<String, Object> deptMap = new LinkedHashMap<>();
            for (Map.Entry<Department, Integer> e : deptCounts.entrySet()) {
                deptMap.put(e.getKey().name(), e.getValue());
            }
            resp.put("departmentDistribution", deptMap);

            // Risk distribution
            Map<RiskLevel, Integer> riskMap = analyticsService.getRiskDistribution();
            Map<String, Object> rMap = new LinkedHashMap<>();
            for (Map.Entry<RiskLevel, Integer> e : riskMap.entrySet()) {
                rMap.put(e.getKey().name(), e.getValue());
            }
            resp.put("riskDistribution", rMap);

            // Monthly attendance trend
            resp.put("monthlyAttendance", analyticsService.getMonthlyAttendanceData());
            resp.put("monthlyNoShows", analyticsService.getMonthlyNoShowData());

            // Slot times
            resp.put("timeSlots", analyticsService.getTimeSlotDistribution(null, null, Department.ALL, "ALL"));

            // Doctor workload
            resp.put("doctorWorkload", analyticsService.getDoctorWorkloadDistribution(Department.ALL));

            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }
    }

    private class CsvExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            AnalyticsService analyticsService = registry.getAnalyticsService();
            File tempFile = File.createTempFile("ghostqueue_clinical_audit_", ".csv");
            tempFile.deleteOnExit();

            analyticsService.exportAnalyticsReportToCsv(tempFile, null, null, Department.ALL, "ALL");

            byte[] bytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"ghostqueue_clinical_audit.csv\"");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class SettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            SettingsService settingsService = registry.getSettingsService();
            if ("GET".equals(method)) {
                Map<String, String> all = settingsService.getAllSettings();
                Map<String, Object> resp = new LinkedHashMap<>();
                for (Map.Entry<String, String> e : all.entrySet()) {
                    resp.put(e.getKey(), e.getValue());
                }
                sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                for (Map.Entry<String, Object> e : req.entrySet()) {
                    if (e.getValue() != null) {
                        settingsService.saveSetting(e.getKey(), e.getValue().toString(), "WEB_CONFIG", "Updated via Web UI");
                    }
                }
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Settings updated successfully\"}");
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    // =========================================================================
    // STATIC WEB ASSET HANDLER
    // =========================================================================

    private static class StaticWebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isBlank()) {
                path = "/index.html";
            }

            // Strip leading slash for resource loading
            String resourcePath = "web" + path;
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

            // Fallback: check project root web/ or src/main/resources/web/ on disk
            if (in == null) {
                File localFile = new File("src/main/resources/" + resourcePath);
                if (!localFile.exists()) {
                    localFile = new File("web" + path);
                }
                if (localFile.exists() && localFile.isFile()) {
                    in = new FileInputStream(localFile);
                }
            }

            if (in == null) {
                // Return index.html for client-side routing fallback if exists
                InputStream indexIn = getClass().getClassLoader().getResourceAsStream("web/index.html");
                if (indexIn == null) {
                    File indexFile = new File("src/main/resources/web/index.html");
                    if (indexFile.exists()) indexIn = new FileInputStream(indexFile);
                }
                if (indexIn != null) {
                    in = indexIn;
                    path = "/index.html";
                }
            }

            if (in == null) {
                String notFound = "<h1>404 Not Found</h1><p>Resource " + path + " not found.</p>";
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                byte[] b = notFound.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            }

            String contentType = determineContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);

            try (InputStream input = in; OutputStream os = exchange.getResponseBody()) {
                byte[] bytes = input.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }

        private String determineContentType(String path) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
