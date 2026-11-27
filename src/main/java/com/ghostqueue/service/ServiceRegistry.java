package com.ghostqueue.service;

import com.ghostqueue.mongodb.repository.*;
import com.ghostqueue.prediction.PredictionRepository;
import com.ghostqueue.repository.*;

import java.util.logging.Logger;

public class ServiceRegistry {
    private static final Logger LOGGER = Logger.getLogger(ServiceRegistry.class.getName());
    private static ServiceRegistry instance;

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueRepository queueRepository;
    private final PredictionRepository predictionRepository;
    private final ReallocationHistoryRepository reallocationHistoryRepository;
    private final ReminderRepository reminderRepository;

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final PredictionService predictionService;
    private final QueueService queueService;
    private final ReallocationService reallocationService;
    private final AnalyticsService analyticsService;
    private final ReminderService reminderService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;

    private ServiceRegistry() {
        LOGGER.info("Initializing GhostQueue ServiceRegistry with MongoDB Atlas (Cluster0)...");

        this.patientRepository = new MongoPatientRepository();
        this.doctorRepository = new MongoDoctorRepository();
        this.historyRepository = new MongoAppointmentHistoryRepository();
        this.appointmentRepository = new MongoAppointmentRepository();
        this.queueRepository = new MongoQueueRepository();
        this.predictionRepository = new MongoPredictionRepository();
        this.reallocationHistoryRepository = new MongoReallocationHistoryRepository();
        this.reminderRepository = new MongoReminderRepository();

        this.patientService = new PatientService(patientRepository);
        this.doctorService = new DoctorService(doctorRepository);
        this.appointmentService = new AppointmentService(appointmentRepository, historyRepository, doctorRepository);
        this.predictionService = new PredictionService(appointmentService, patientService, predictionRepository);
        this.queueService = new QueueService(queueRepository);
        this.reallocationService = new ReallocationService(appointmentService, queueService, reallocationHistoryRepository);
        this.analyticsService = new AnalyticsService(
                appointmentService,
                predictionService,
                queueService,
                patientService,
                doctorService
        );
        this.reminderService = new ReminderService(
                reminderRepository,
                appointmentService,
                patientService,
                predictionService
        );
        this.settingsService = new SettingsService();
        this.notificationService = NotificationService.getInstance();

        // Sync initial model risk thresholds from persisted settings
        double highRisk = settingsService.getHighRiskThreshold();
        double medRisk = settingsService.getMediumRiskThreshold();
        this.predictionService.getRiskClassifier().setHighThreshold(highRisk);
        this.predictionService.getRiskClassifier().setLowThreshold(medRisk);

        LOGGER.info("GhostQueue ServiceRegistry initialized successfully with primary MongoDB Atlas backend.");
    }

    public static synchronized ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry();
        }
        return instance;
    }

    public PatientRepository getPatientRepository() { return patientRepository; }
    public DoctorRepository getDoctorRepository() { return doctorRepository; }
    public AppointmentHistoryRepository getHistoryRepository() { return historyRepository; }
    public AppointmentRepository getAppointmentRepository() { return appointmentRepository; }
    public QueueRepository getQueueRepository() { return queueRepository; }
    public PredictionRepository getPredictionRepository() { return predictionRepository; }
    public ReallocationHistoryRepository getReallocationHistoryRepository() { return reallocationHistoryRepository; }
    public ReminderRepository getReminderRepository() { return reminderRepository; }

    public static PatientRepository getPatientRepo() { return getInstance().patientRepository; }
    public static DoctorRepository getDoctorRepo() { return getInstance().doctorRepository; }
    public static AppointmentHistoryRepository getHistoryRepo() { return getInstance().historyRepository; }
    public static AppointmentRepository getAppointmentRepo() { return getInstance().appointmentRepository; }
    public static QueueRepository getQueueRepo() { return getInstance().queueRepository; }
    public static PredictionRepository getPredictionRepo() { return getInstance().predictionRepository; }
    public static ReallocationHistoryRepository getReallocationHistoryRepo() { return getInstance().reallocationHistoryRepository; }
    public static ReminderRepository getReminderRepo() { return getInstance().reminderRepository; }

    public static PatientRepository patientRepository() { return getInstance().patientRepository; }
    public static DoctorRepository doctorRepository() { return getInstance().doctorRepository; }
    public static AppointmentRepository appointmentRepository() { return getInstance().appointmentRepository; }
    public static QueueRepository queueRepository() { return getInstance().queueRepository; }
    public static AppointmentHistoryRepository historyRepository() { return getInstance().historyRepository; }

    public PatientService getPatientService() { return patientService; }
    public DoctorService getDoctorService() { return doctorService; }
    public AppointmentService getAppointmentService() { return appointmentService; }
    public PredictionService getPredictionService() { return predictionService; }
    public QueueService getQueueService() { return queueService; }
    public ReallocationService getReallocationService() { return reallocationService; }
    public AnalyticsService getAnalyticsService() { return analyticsService; }
    public ReminderService getReminderService() { return reminderService; }
    public SettingsService getSettingsService() { return settingsService; }
    public NotificationService getNotificationService() { return notificationService; }
}
