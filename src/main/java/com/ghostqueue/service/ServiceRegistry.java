package com.ghostqueue.service;

import com.ghostqueue.repository.*;
import com.ghostqueue.util.DatabaseConnection;
import com.ghostqueue.util.DatabaseInitializer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceRegistry {
    private static final Logger LOGGER = Logger.getLogger(ServiceRegistry.class.getName());
    private static ServiceRegistry instance;

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueRepository queueRepository;
    private final com.ghostqueue.prediction.PredictionRepository predictionRepository;

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final PredictionService predictionService;
    private final QueueService queueService;
    private final ReallocationService reallocationService;
    private final AnalyticsService analyticsService;

    private ServiceRegistry() {
        LOGGER.info("Initializing GhostQueue ServiceRegistry with JDBC repositories...");

        // Ensure database schema and seed data are initialized
        boolean dbOk = DatabaseInitializer.initializeDatabase();
        if (!dbOk) {
            LOGGER.warning("GhostQueue database initialization returned false. Verifying connection...");
        }

        this.patientRepository = new JdbcPatientRepository();
        this.doctorRepository = new JdbcDoctorRepository();
        this.historyRepository = new JdbcAppointmentHistoryRepository();
        this.appointmentRepository = new JdbcAppointmentRepository();
        this.queueRepository = new JdbcQueueRepository();
        this.predictionRepository = new com.ghostqueue.prediction.JdbcPredictionRepository();

        this.patientService = new PatientService(patientRepository);
        this.doctorService = new DoctorService(doctorRepository);
        this.appointmentService = new AppointmentService(appointmentRepository, historyRepository, doctorRepository);
        this.predictionService = new PredictionService(appointmentService, patientService, predictionRepository);
        this.queueService = new QueueService(queueRepository);
        this.reallocationService = new ReallocationService(appointmentService, queueService);
        this.analyticsService = new AnalyticsService(
                appointmentService,
                predictionService,
                queueService,
                patientService,
                doctorService
        );

        LOGGER.info("GhostQueue ServiceRegistry initialized successfully with active MySQL database integration.");
    }

    public static synchronized ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry();
        }
        return instance;
    }

    public PatientRepository getPatientRepository() {
        return patientRepository;
    }

    public DoctorRepository getDoctorRepository() {
        return doctorRepository;
    }

    public AppointmentHistoryRepository getHistoryRepository() {
        return historyRepository;
    }

    public AppointmentRepository getAppointmentRepository() {
        return appointmentRepository;
    }

    public QueueRepository getQueueRepository() {
        return queueRepository;
    }

    public PatientService getPatientService() {
        return patientService;
    }

    public DoctorService getDoctorService() {
        return doctorService;
    }

    public AppointmentService getAppointmentService() {
        return appointmentService;
    }

    public PredictionService getPredictionService() {
        return predictionService;
    }

    public com.ghostqueue.prediction.PredictionRepository getPredictionRepository() {
        return predictionRepository;
    }

    public QueueService getQueueService() {
        return queueService;
    }

    public ReallocationService getReallocationService() {
        return reallocationService;
    }

    public AnalyticsService getAnalyticsService() {
        return analyticsService;
    }
}

