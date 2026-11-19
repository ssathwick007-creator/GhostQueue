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
}
