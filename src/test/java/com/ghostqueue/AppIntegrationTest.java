package com.ghostqueue;

import com.ghostqueue.model.*;
import com.ghostqueue.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AppIntegrationTest {

        private ServiceRegistry registry;
        private AppointmentService appointmentService;
        private PatientService patientService;
        private DoctorService doctorService;
        private PredictionService predictionService;
        private QueueService queueService;
        private ReallocationService reallocationService;
        private AnalyticsService analyticsService;
        private ReminderService reminderService;
        private SettingsService settingsService;

        @BeforeEach
        void setUp() {
                registry = ServiceRegistry.getInstance();
                appointmentService = registry.getAppointmentService();
                patientService = registry.getPatientService();
                doctorService = registry.getDoctorService();
                predictionService = registry.getPredictionService();
                queueService = registry.getQueueService();
                reallocationService = registry.getReallocationService();
                analyticsService = registry.getAnalyticsService();
                reminderService = registry.getReminderService();
                settingsService = registry.getSettingsService();

                // Ensure consistent baseline risk thresholds for every test run
                predictionService.updateRiskThresholds(0.30, 0.65);

                // Ensure seed queue records are in active states for queue and reallocation tests
                try {
                        queueService.updateStatus("WQ-104", QueueStatus.STANDBY);
                        queueService.updateStatus("WQ-106", QueueStatus.OFFERED);
                        queueService.updateStatus("WQ-108", QueueStatus.WAITING);
                        queueService.updateStatus("WQ-110", QueueStatus.WAITING);
                        reallocationService.refreshSuggestions();
                } catch (Exception ignored) {}
        }

        @Test
        @DisplayName("Verify FXML templates and resources exist and are loadable")
        void testResourcesExist() {
                String[] fxmlFiles = {
                                "/fxml/main_layout.fxml",
                                "/fxml/dashboard.fxml",
                                "/fxml/appointments.fxml",
                                "/fxml/prediction.fxml",
                                "/fxml/waiting_queue.fxml",
                                "/fxml/reallocation.fxml",
                                "/fxml/patient_records.fxml",
                                "/fxml/reminders.fxml",
                                "/fxml/analytics.fxml",
                                "/fxml/settings.fxml",
                                "/fxml/dialogs/add_appointment_dialog.fxml",
                                "/fxml/dialogs/add_queue_dialog.fxml",
                                "/fxml/dialogs/patient_form_dialog.fxml",
                                "/fxml/dialogs/doctor_management_dialog.fxml",
                                "/fxml/dialogs/doctor_form_dialog.fxml",
                                "/fxml/dialogs/prediction_explanation_dialog.fxml",
                                "/fxml/dialogs/model_evaluation_dialog.fxml"
                };

                for (String fxml : fxmlFiles) {
                        InputStream stream = getClass().getResourceAsStream(fxml);
                        assertNotNull(stream, "Resource file must exist: " + fxml);
                }

                assertNotNull(getClass().getResourceAsStream("/css/theme.css"), "theme.css must exist");
                assertNotNull(getClass().getResourceAsStream("/db/init.sql"), "init.sql must exist");
        }

        @Test
        @DisplayName("Verify Patient Repository and Service operations")
        void testPatientService() {
                List<Patient> patients = patientService.getAllPatients();
                assertFalse(patients.isEmpty(), "Patients should be loaded from MySQL");
                assertTrue(patients.size() >= 10, "Should have at least 10 realistic patient records");

                // Test search by name/doctor
                List<Patient> searchResults = patientService.searchPatients("Vance");
                assertFalse(searchResults.isEmpty(), "Should find patients associated with Vance");
                assertTrue(searchResults.stream().anyMatch(p -> p.getFullName().contains("Vance")),
                                "Should include Eleanor Vance");

                // Test get by ID
                assertTrue(patientService.getPatientById("PT-1001").isPresent());

                // Test Register new patient
                String newPatId = "PT-TEST-" + System.currentTimeMillis() % 10000;
                Patient newPat = new Patient(
                                newPatId, "Integration Test Patient", 34, "Female",
                                "+1 555-987-" + (1000 + (int) (Math.random() * 8999)),
                                "test.patient@example.com", "100 Test Ave", LocalDateTime.now());
                patientService.registerPatient(newPat);
                assertTrue(patientService.getPatientById(newPatId).isPresent());

                // Test safe deletion policy: new patient has no appointments, so can be deleted
                assertTrue(patientService.canDeletePatient(newPatId));
                patientService.deletePatient(newPatId);
                assertFalse(patientService.getPatientById(newPatId).isPresent());

                // Established patient PT-1001 has appointments and cannot be deleted
                assertFalse(patientService.canDeletePatient("PT-1001"),
                                "Patient with appointments must NOT be deletable");
        }

        @Test
        @DisplayName("Verify Doctor Service operations and availability")
        void testDoctorService() {
                List<Doctor> doctors = doctorService.getAllDoctors();
                assertFalse(doctors.isEmpty(), "Doctors should be loaded from MySQL");
                assertTrue(doctors.size() >= 6, "Should have at least 6 initial doctors");

                // Search doctor
                List<Doctor> vance = doctorService.searchDoctors("Vance");
                assertFalse(vance.isEmpty());
                assertEquals("DOC-101", vance.get(0).getId());

                // Update availability
                doctorService.setAvailabilityStatus("DOC-101", "AVAILABLE");
                Doctor doc = doctorService.getDoctorById("DOC-101").orElseThrow();
                assertEquals("AVAILABLE", doc.getAvailabilityStatus());
        }

        @Test
        @DisplayName("Verify Appointment Management and Business Rules")
        void testAppointmentBusinessRules() {
                List<Appointment> all = appointmentService.getAllAppointments();
                assertTrue(all.size() >= 15, "Should have initial appointment inventory");

                // Rule 1: Reject past dates for new bookings
                Appointment pastApt = new Appointment(
                                "APT-PAST-TEST", "PT-1001", "Eleanor Vance", 58, "+1 555-014-8821",
                                "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY,
                                LocalDate.now().minusDays(5), LocalTime.of(10, 0), AppointmentStatus.BOOKED,
                                "Checkup", 1, 0.1, List.of());
                assertThrows(IllegalArgumentException.class, () -> appointmentService.createAppointment(pastApt),
                                "Scheduling an appointment in the past must throw IllegalArgumentException");

                // Rule 2: Booking a valid future appointment
                LocalDate futureDate = LocalDate.now().plusDays(25);
                LocalTime slotTime = LocalTime.of(14, 0);
                String testAptId = "APT-VAL-" + (System.currentTimeMillis() % 100000);

                Appointment validApt = new Appointment(
                                testAptId, "PT-1001", "Eleanor Vance", 58, "+1 555-014-8821",
                                "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY,
                                futureDate, slotTime, AppointmentStatus.CONFIRMED,
                                "Cardiac check", 1, 0.15, List.of());
                appointmentService.createAppointment(validApt);
                assertTrue(appointmentService.getAppointmentById(testAptId).isPresent());

                // Rule 3: Doctor overlap conflict (same doctor, same date, same time)
                Appointment overlapDoctor = new Appointment(
                                "APT-DOC-CONFLICT-" + System.currentTimeMillis() % 10000, "PT-1002", "Arthur Pendelton",
                                74, "+1 555-876-1234",
                                "DOC-101", "Dr. Robert Vance", Department.CARDIOLOGY,
                                futureDate, slotTime, AppointmentStatus.CONFIRMED,
                                "Second patient trying same doctor slot", 1, 0.15, List.of());
                assertThrows(IllegalStateException.class, () -> appointmentService.createAppointment(overlapDoctor),
                                "Overlapping appointment for same doctor at same time must throw IllegalStateException");

                // Rule 4: Patient overlap conflict (same patient, same date, same time with
                // different doctor)
                Appointment overlapPatient = new Appointment(
                                "APT-PAT-CONFLICT-" + System.currentTimeMillis() % 10000, "PT-1001", "Eleanor Vance",
                                58, "+1 555-014-8821",
                                "DOC-102", "Dr. David Kim", Department.NEUROLOGY,
                                futureDate, slotTime, AppointmentStatus.CONFIRMED,
                                "Same patient double booked", 1, 0.15, List.of());
                assertThrows(IllegalStateException.class, () -> appointmentService.createAppointment(overlapPatient),
                                "Conflicting appointment for same patient at same time must throw IllegalStateException");

                // Rule 5: Audit history logging
                List<AppointmentHistory> history = appointmentService.getAppointmentHistory(testAptId);
                assertFalse(history.isEmpty(), "Appointment history audit log must record booking");

                // Rule 6: Cancellation transition with reason
                appointmentService.cancelAppointment(testAptId, "Test cancellation reason");
                Appointment cancelled = appointmentService.getAppointmentById(testAptId).orElseThrow();
                assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
                assertEquals("Test cancellation reason", cancelled.getCancellationReason());
        }

        @Test
        @DisplayName("Verify Waiting Queue Operations and Priority Queue")
        void testQueueService() {
                List<QueueEntry> entries = queueService.getAllQueueEntries();
                assertFalse(entries.isEmpty(), "Queue should have entries from MySQL");

                int urgentCount = queueService.countUrgentPriority();
                assertTrue(urgentCount > 0, "Should have urgent triage patients");

                // Priority Queue Ordering test
                List<QueueEntry> priorityOrdered = queueService.getPriorityOrderedWaitingPool();
                assertFalse(priorityOrdered.isEmpty());
                // Top should be URGENT
                assertEquals(QueuePriority.URGENT, priorityOrdered.get(0).getPriority());

                // Add to queue using existing patient in MySQL
                String qId = "WQ-TEST-" + (System.currentTimeMillis() % 10000);
                QueueEntry newEntry = new QueueEntry(
                                qId, "PT-1003", "Maria Gomez", "+1 (555) 345-6789",
                                QueuePriority.STANDARD, Department.GENERAL_MEDICINE,
                                "Dr. Wright", "Morning", LocalDateTime.now(),
                                QueueStatus.WAITING, "Test note");
                queueService.addToQueue(newEntry);
                assertTrue(queueService.getQueueEntryById(qId).isPresent());

                // Update status
                queueService.updateStatus(qId, QueueStatus.OFFERED);
                assertEquals(QueueStatus.OFFERED, queueService.getQueueEntryById(qId).get().getStatus());

                // Cleanup
                queueService.removeFromQueue(qId);
                assertFalse(queueService.getQueueEntryById(qId).isPresent());
        }

        @Test
        @DisplayName("Verify Live Analytics SQL Aggregate Metrics")
        void testLiveAnalytics() {
                assertTrue(analyticsService.getTotalPatients() >= 20, "Should report at least 20 registered patients");
                assertTrue(analyticsService.getTotalDoctors() >= 6, "Should report at least 6 doctors");
                assertTrue(analyticsService.getTodayAppointmentsCount() >= 0);
                assertTrue(analyticsService.getConfirmedCount() > 0);
                assertTrue(analyticsService.getWaitingQueueCount() > 0);

                Map<Department, Integer> deptCounts = analyticsService.getDepartmentAppointmentCounts();
                assertFalse(deptCounts.isEmpty());
                assertTrue(deptCounts.containsKey(Department.CARDIOLOGY));
        }

        @Test
        @DisplayName("Verify Phase 3 Intelligent Prediction Engine, MySQL Persistence, and XAI Explanations")
        void testPredictionEngineAndPersistence() {
                // 1. Ensure baseline predictions are generated and persisted in MySQL
                // appointment_predictions
                predictionService.ensureBaselinePredictions();
                List<com.ghostqueue.prediction.PredictionResult> latest = predictionService
                                .getLatestPredictionResults();
                assertFalse(latest.isEmpty(), "Prediction engine must generate and return prediction results");
                assertTrue(latest.size() >= 15, "Should have predictions for active appointment cohort");

                // 2. Validate domain contract and mathematical properties of prediction results
                for (com.ghostqueue.prediction.PredictionResult res : latest) {
                        assertNotNull(res.getPredictionId(), "Prediction ID must not be null");
                        assertNotNull(res.getAppointmentId(), "Linked Appointment ID must not be null");
                        assertNotNull(res.getPatientId(), "Patient ID must not be null");
                        assertTrue(res.getNoShowProbability() >= 0.02 && res.getNoShowProbability() <= 0.98,
                                        "Probability must be strictly bounded in [0.02, 0.98]");
                        assertNotNull(res.getRiskLevel(), "Risk level category must be assigned");

                        // Verify Risk Classifier threshold consistency
                        if (res.getNoShowProbability() >= 0.65) {
                                assertEquals(RiskLevel.HIGH, res.getRiskLevel());
                        } else if (res.getNoShowProbability() >= 0.30) {
                                assertEquals(RiskLevel.MEDIUM, res.getRiskLevel());
                        } else {
                                assertEquals(RiskLevel.LOW, res.getRiskLevel());
                        }

                        // Verify Explainable AI features
                        assertFalse(res.getContributingFactors().isEmpty(),
                                        "Every prediction must feature ranked explainable factors");
                        assertNotNull(res.getSuggestedAction(),
                                        "Every prediction must formulate administrative outreach guidance");
                        assertFalse(res.getSuggestedAction().isBlank());
                }

                // 3. Test multi-criteria filtering
                List<com.ghostqueue.prediction.PredictionResult> highRiskOnly = predictionService
                                .filterPredictionResults(null, null, null, RiskLevel.HIGH, null);
                for (com.ghostqueue.prediction.PredictionResult res : highRiskOnly) {
                        assertEquals(RiskLevel.HIGH, res.getRiskLevel());
                        assertTrue(res.getNoShowProbability() >= 0.65);
                }

                List<com.ghostqueue.prediction.PredictionResult> cardioOnly = predictionService
                                .filterPredictionResults(null, Department.CARDIOLOGY, null, RiskLevel.ALL, null);
                for (com.ghostqueue.prediction.PredictionResult res : cardioOnly) {
                        assertEquals(Department.CARDIOLOGY, res.getDepartment());
                }

                // 4. Test Model Evaluation Report & Training on held-out test split
                com.ghostqueue.prediction.EvaluationReport report = predictionService
                                .retrainModelOnDemonstrationCohort();
                assertNotNull(report, "Retraining must yield an EvaluationReport");
                assertEquals(24, report.getTotalTestSamples(), "Held-out test split must be 24 samples");
                assertTrue(report.getAccuracy() >= 0.70, "Test accuracy should be >= 70%");
                assertTrue(report.getRecall() >= 0.70,
                                "Recall for missed appointments (critical metric) should be >= 70%");
                assertTrue(report.getRocAuc() >= 0.75, "Model discrimination ROC-AUC should be >= 0.75");

                // 5. Test single appointment prediction and JDBC round-trip
                Appointment sampleApt = appointmentService.getAllAppointments().get(0);
                com.ghostqueue.prediction.PredictionResult single = predictionService.predictAndSave(sampleApt);
                assertNotNull(single);
                assertEquals(sampleApt.getId(), single.getAppointmentId());

                var fetched = predictionService.getPredictionRepository().findLatestByAppointmentId(sampleApt.getId());
                assertTrue(fetched.isPresent(), "Persisted prediction must be retrievable from MySQL");
                assertEquals(single.getPredictionId(), fetched.get().getPredictionId());
                assertEquals(single.getProbabilityPercent(), fetched.get().getProbabilityPercent());
                assertFalse(fetched.get().getContributingFactors().isEmpty(),
                                "Deserialized contributing factors must be intact");
        }

        @Test
        @DisplayName("PHASE 4: Verify Waiting Queue Management & Transparent Prioritization")
        void testQueueServiceAndPrioritization() {
                List<QueueEntry> allQueue = queueService.getAllQueueEntries();
                assertFalse(allQueue.isEmpty(), "Waiting queue should contain initial seed records in MySQL");

                // Test prioritization calculation
                for (QueueEntry entry : allQueue) {
                        int score = entry.getTransparentPriorityScore();
                        assertTrue(score >= 0 && score <= 100, "Transparent priority score must be in range [0, 100]");
                        String expl = entry.getPrioritizationExplanation();
                        assertNotNull(expl);
                        assertFalse(expl.isBlank());
                }

                // Test creating a queue entry
                String newQueueId = "WQ-TEST-" + (System.currentTimeMillis() % 10000);
                QueueEntry newEntry = new QueueEntry(
                                newQueueId, "PT-1001", "DOC-101", LocalDate.now().plusDays(2),
                                "Morning (08:00 - 12:00)", QueuePriority.URGENT, QueueStatus.WAITING,
                                LocalDateTime.now(), "Emergency standby evaluation"
                );
                queueService.addToQueue(newEntry);

                var retrieved = queueService.getQueueEntryById(newQueueId);
                assertTrue(retrieved.isPresent(), "Newly added queue entry must be retrievable");
                assertEquals(QueuePriority.URGENT, retrieved.get().getPriority());
                assertEquals("Emergency standby evaluation", retrieved.get().getNotes());

                // Urgent priority should have higher priority score
                assertTrue(retrieved.get().getTransparentPriorityScore() >= 50, "Urgent queue entry score should be >= 50");

                // Test status transition
                queueService.updateStatus(newQueueId, QueueStatus.OFFERED);
                var updated = queueService.getQueueEntryById(newQueueId);
                assertTrue(updated.isPresent());
                assertEquals(QueueStatus.OFFERED, updated.get().getStatus());

                // Test removal from queue
                queueService.removeFromQueue(newQueueId);
                var removed = queueService.getQueueEntryById(newQueueId);
                assertTrue(removed.isEmpty(), "Deleted queue entry must no longer exist in database");
        }

        @Test
        @DisplayName("PHASE 5: Verify Reallocation Engine Scoring & Atomic Approval")
        void testReallocationEngineAndApproval() {
                // Find open/cancelled slots and generate recommendations
                var suggestions = reallocationService.getSuggestions();
                assertNotNull(suggestions, "Reallocation suggestions must not be null");
                assertFalse(suggestions.isEmpty(), "Should have candidate reallocation suggestions");

                ReallocationSuggestion topSuggestion = suggestions.get(0);
                assertTrue(topSuggestion.getMatchScore() > 0, "Compatibility score must be positive");
                assertNotNull(topSuggestion.getMatchReason(), "Match rationale must be provided");
                assertNotNull(topSuggestion.getPatientName(), "Candidate patient name must be populated");

                // Test approval execution
                boolean approved = reallocationService.approveReallocation(topSuggestion.getSuggestionId(), "Verified via integration test");
                assertTrue(approved, "Reallocation suggestion approval must succeed");

                // Verify reallocation history table contains records in MySQL
                var history = reallocationService.getReallocationHistory();
                assertNotNull(history, "Reallocation audit history must be queryable");
                assertFalse(history.isEmpty(), "Audit history must contain the approved transaction");

                // Restore candidate status so tests remain idempotent
                try {
                        queueService.updateStatus(topSuggestion.getCandidatePatient().getQueueId(), QueueStatus.WAITING);
                } catch (Exception ignored) {}
        }

        @Test
        @DisplayName("PHASE 6: Verify Notification & Outreach Reminder Management")
        void testReminderServiceAndOutreach() {
                // Generate automated outreach tasks for high-risk visits
                int generated = reminderService.generateHighRiskReminders();
                assertTrue(generated >= 0, "Generated count must be non-negative");

                List<AppointmentReminder> reminders = reminderService.getAllReminders();
                assertFalse(reminders.isEmpty(), "Reminders table should contain active outreach tasks");

                // Verify reminder attributes
                AppointmentReminder first = reminders.get(0);
                assertNotNull(first.getReminderId());
                assertNotNull(first.getAppointmentId());
                assertNotNull(first.getReminderType());
                assertNotNull(first.getReminderStatus());
                assertNotNull(first.getMessageContent());

                // Test response logging (patient confirms attendance)
                reminderService.markCompleted(first.getReminderId(), "CONFIRMED", "Patient confirmed through automated phone prompt");

                var updated = reminderService.getReminderById(first.getReminderId());
                assertTrue(updated.isPresent());
                assertEquals(AppointmentReminder.ReminderStatus.COMPLETED, updated.get().getReminderStatus());
                assertEquals("CONFIRMED", updated.get().getResponseStatus());
                assertNotNull(updated.get().getCompletedAt());
        }

        @Test
        @DisplayName("PHASE 7: Verify Hospital Analytics Queries & CSV Report Export")
        void testAnalyticsAndCsvExport() throws Exception {
                // 1. Verify KPIs
                var kpis = analyticsService.getFilteredKpis(null, null, Department.ALL, "ALL");
                assertNotNull(kpis);
                assertTrue(kpis.totalAppointments() > 0, "Total appointments should be > 0");
                assertTrue(kpis.attendanceRate() > 0.0, "Attendance rate should be > 0");
                assertTrue(kpis.noShowRate() >= 0.0, "No show rate should be >= 0");

                // 2. Verify distributions
                Map<Department, Integer> deptCounts = analyticsService.getDepartmentAppointmentCounts();
                assertFalse(deptCounts.isEmpty());

                Map<RiskLevel, Integer> riskMap = analyticsService.getRiskDistribution();
                assertFalse(riskMap.isEmpty());
                assertTrue(riskMap.containsKey(RiskLevel.HIGH));

                Map<String, Integer> timeSlotMap = analyticsService.getTimeSlotDistribution(null, null, Department.ALL, "ALL");
                assertFalse(timeSlotMap.isEmpty());

                Map<String, Integer> docWorkload = analyticsService.getDoctorWorkloadDistribution(Department.ALL);
                assertFalse(docWorkload.isEmpty());

                // 3. Test real CSV report export
                java.io.File tempCsv = java.io.File.createTempFile("ghostqueue_test_export_", ".csv");
                tempCsv.deleteOnExit();

                int exportedCount = analyticsService.exportAnalyticsReportToCsv(tempCsv, null, null, Department.ALL, "ALL");
                assertTrue(exportedCount > 0, "Exported appointment count should be > 0");
                assertTrue(tempCsv.exists() && tempCsv.length() > 0, "Export file must be created and non-empty");

                // Verify CSV contents
                String content = java.nio.file.Files.readString(tempCsv.toPath());
                assertTrue(content.contains("Appointment ID,Patient ID,Patient Name"), "Header line must be present");
                assertTrue(content.contains("No-Show Probability %"), "Probability header must be present");
        }

        @Test
        @DisplayName("PHASE 8: Verify System Settings & Dynamic Threshold Synchronization")
        void testSettingsServiceAndThresholdSync() {
                // Verify initial loaded settings
                String hospitalName = settingsService.getString(SettingsService.KEY_HOSPITAL_NAME, "");
                assertFalse(hospitalName.isBlank(), "Hospital name setting must be loaded");

                double highThreshold = settingsService.getHighRiskThreshold();
                double medThreshold = settingsService.getMediumRiskThreshold();
                assertTrue(highThreshold > medThreshold, "High risk threshold must be > Medium risk threshold");

                // Test saving and updating a setting
                settingsService.saveSetting("test.param.key", "VerifiedValue", "TEST", "Unit test setting");
                assertEquals("VerifiedValue", settingsService.getString("test.param.key", ""));

                // Test dynamic threshold update synchronization with prediction engine
                predictionService.updateRiskThresholds(0.35, 0.70);
                assertEquals(0.70, predictionService.getRiskClassifier().getHighThreshold(), 0.001);
                assertEquals(0.35, predictionService.getRiskClassifier().getLowThreshold(), 0.001);

                // Reset back to normal defaults
                predictionService.updateRiskThresholds(medThreshold, highThreshold);
                assertEquals(highThreshold, predictionService.getRiskClassifier().getHighThreshold(), 0.001);
                assertEquals(medThreshold, predictionService.getRiskClassifier().getLowThreshold(), 0.001);
        }
}
