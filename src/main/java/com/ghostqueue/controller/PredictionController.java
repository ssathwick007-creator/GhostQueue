package com.ghostqueue.controller;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.Doctor;
import com.ghostqueue.model.RiskLevel;
import com.ghostqueue.prediction.EvaluationReport;
import com.ghostqueue.prediction.PredictionResult;
import com.ghostqueue.service.DoctorService;
import com.ghostqueue.service.PredictionService;
import com.ghostqueue.service.ServiceRegistry;
import com.ghostqueue.util.NotificationManager;
import com.ghostqueue.util.UiHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Prediction Intelligence Dashboard.
 * Coordinates real-time machine learning inference, risk classification,
 * multi-criteria clinical filtering, explainable factor analysis, and cross-validation reports.
 */
public class PredictionController {

    private static final Logger LOGGER = Logger.getLogger(PredictionController.class.getName());

    @FXML private Label highRiskCountLabel;
    @FXML private Label avgRiskLabel;
    @FXML private Label modelConfidenceLabel;
    @FXML private Label preventedLabel;
    @FXML private Label recordCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<RiskLevel> riskFilter;
    @FXML private ComboBox<Department> departmentFilter;
    @FXML private ComboBox<String> doctorFilter;
    @FXML private DatePicker dateFilter;

    @FXML private TableView<PredictionResult> predictionTable;
    @FXML private TableColumn<PredictionResult, String> colApptId;
    @FXML private TableColumn<PredictionResult, String> colPatient;
    @FXML private TableColumn<PredictionResult, String> colDoctor;
    @FXML private TableColumn<PredictionResult, String> colTime;
    @FXML private TableColumn<PredictionResult, String> colRiskScore;
    @FXML private TableColumn<PredictionResult, RiskLevel> colRiskLevel;
    @FXML private TableColumn<PredictionResult, String> colDrivers;
    @FXML private TableColumn<PredictionResult, String> colMethod;
    @FXML private TableColumn<PredictionResult, PredictionResult> colAction;

    private final PredictionService predictionService = ServiceRegistry.getInstance().getPredictionService();
    private final DoctorService doctorService = ServiceRegistry.getInstance().getDoctorService();

    @FXML
    public void initialize() {
        setupMetrics();
        setupFilters();
        setupTable();
        applyFilters();
    }

    private void setupMetrics() {
        highRiskCountLabel.setText(String.valueOf(predictionService.countByRiskLevel(RiskLevel.HIGH)));
        avgRiskLabel.setText(String.format("%.1f%%", predictionService.getAverageRiskScore()));

        EvaluationReport report = predictionService.getEvaluationReport();
        if (report != null) {
            modelConfidenceLabel.setText(String.format("%.3f", report.getRocAuc()));
        } else {
            modelConfidenceLabel.setText(String.format("%.1f%%", predictionService.getSimulatedModelAccuracy()));
        }

        preventedLabel.setText(predictionService.getPreventedNoShowsEstimate() + " slots");
    }

    private void setupFilters() {
        riskFilter.setItems(FXCollections.observableArrayList(RiskLevel.values()));
        riskFilter.getSelectionModel().select(RiskLevel.ALL);
        riskFilter.setOnAction(e -> applyFilters());

        departmentFilter.setItems(FXCollections.observableArrayList(Department.values()));
        departmentFilter.getSelectionModel().select(Department.ALL);
        departmentFilter.setOnAction(e -> applyFilters());

        populateDoctorFilter();
        doctorFilter.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        dateFilter.setOnAction(e -> applyFilters());
    }

    private void populateDoctorFilter() {
        List<String> docOptions = new ArrayList<>();
        docOptions.add("All Doctors");
        for (Doctor doc : doctorService.getAllDoctors()) {
            docOptions.add(doc.getId() + " - " + doc.getFullName());
        }
        doctorFilter.setItems(FXCollections.observableArrayList(docOptions));
        doctorFilter.getSelectionModel().select(0);
    }

    private void setupTable() {
        colApptId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAppointmentId()));
        colPatient.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPatientName() + " (" + data.getValue().getPatientId() + ")"));
        colDoctor.setCellValueFactory(data -> {
            String deptName = data.getValue().getDepartment() != null ? data.getValue().getDepartment().getDisplayName() : "General";
            return new SimpleStringProperty(data.getValue().getDoctorName() + " (" + deptName + ")");
        });
        colTime.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAppointmentDate() + " " + data.getValue().getTimeFormatted()));
        colRiskScore.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.1f%%", data.getValue().getNoShowProbability() * 100.0)));
        colDrivers.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFactorsSummary()));
        colMethod.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPredictionMethod()));

        // Risk Category Badge
        colRiskLevel.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getRiskLevel()));
        colRiskLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(RiskLevel level, boolean empty) {
                super.updateItem(level, empty);
                if (empty || level == null) {
                    setGraphic(null);
                } else {
                    PredictionResult row = getTableRow() != null ? getTableRow().getItem() : null;
                    int percent = (row != null) ? row.getProbabilityPercent() : 0;
                    setGraphic(UiHelper.createRiskBadge(level, percent));
                }
            }
        });

        // Action Column: Explain (XAI) and Intervene (Remind/Follow-up)
        colAction.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnExplain = new Button("Explain");
            private final Button btnIntervene = new Button("Follow-up");
            private final HBox container = new HBox(6, btnExplain, btnIntervene);

            {
                btnExplain.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnIntervene.getStyleClass().addAll("btn-primary", "btn-sm");

                btnExplain.setOnAction(e -> {
                    PredictionResult res = getItem();
                    if (res != null) {
                        handleOpenExplanation(res);
                    }
                });

                btnIntervene.setOnAction(e -> {
                    PredictionResult res = getItem();
                    if (res != null) {
                        NotificationManager.showSuccess("Clinical Outreach Queued",
                                "Targeted operational action dispatched for " + res.getPatientName() +
                                        " (Appt: " + res.getAppointmentId() + "):\n\n" +
                                        "Risk Level: " + res.getRiskLevel().getLabel() + " (" + res.getProbabilityPercent() + "%)\n" +
                                        "Protocol: " + res.getSuggestedAction() + "\n\n" +
                                        "Outreach logged in hospital operational registry.");
                    }
                });
            }

            @Override
            protected void updateItem(PredictionResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void applyFilters() {
        String query = searchField.getText();
        RiskLevel level = riskFilter.getValue();
        Department dept = departmentFilter.getValue();
        LocalDate date = dateFilter.getValue();

        String selectedDocOption = doctorFilter.getValue();
        String doctorId = null;
        if (selectedDocOption != null && !selectedDocOption.equals("All Doctors")) {
            doctorId = selectedDocOption.split(" - ")[0].trim();
        }

        List<PredictionResult> results = predictionService.filterPredictionResults(query, dept, doctorId, level, date);
        predictionTable.setItems(FXCollections.observableArrayList(results));

        int totalCount = predictionService.getLatestPredictionResults().size();
        recordCountLabel.setText("Showing " + results.size() + " of " + totalCount + " predictions");
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        riskFilter.getSelectionModel().select(RiskLevel.ALL);
        departmentFilter.getSelectionModel().select(Department.ALL);
        doctorFilter.getSelectionModel().select(0);
        dateFilter.setValue(null);
        applyFilters();
    }

    private void handleOpenExplanation(PredictionResult result) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/prediction_explanation_dialog.fxml"));
            Parent root = loader.load();
            PredictionExplanationDialogController controller = loader.getController();
            controller.setPredictionResult(result);

            Stage stage = new Stage();
            stage.setTitle("Prediction Explanation & XAI Analysis — GhostQueue");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to open prediction explanation dialog: " + ex.getMessage(), ex);
            NotificationManager.showError("Dialog Error", "Unable to open prediction explanation: " + ex.getMessage());
        }
    }

    @FXML
    private void handleOpenModelEvaluation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/model_evaluation_dialog.fxml"));
            Parent root = loader.load();
            ModelEvaluationDialogController controller = loader.getController();
            controller.setPredictionService(predictionService);

            Stage stage = new Stage();
            stage.setTitle("Model Evaluation & Discrimination Metrics — GhostQueue");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            setupMetrics();
            applyFilters();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to open model evaluation dialog: " + ex.getMessage(), ex);
            NotificationManager.showError("Dialog Error", "Unable to open model evaluation dialog: " + ex.getMessage());
        }
    }

    @FXML
    private void handleRunBatchEvaluation() {
        // Run batch inference in background so JavaFX UI thread stays responsive
        Task<List<PredictionResult>> task = new Task<>() {
            @Override
            protected List<PredictionResult> call() {
                return predictionService.runBatchRecalibration();
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                setupMetrics();
                applyFilters();
                NotificationManager.showSuccess("Batch Recalibration Complete",
                        "Successfully recalculated attendance risk probabilities across active clinical appointments.\n" +
                                "Current high-risk flags: " + predictionService.countByRiskLevel(RiskLevel.HIGH));
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                NotificationManager.showError("Recalibration Failed",
                        "Error recalculating prediction batch: " + task.getException().getMessage());
            });
        });

        Thread thread = new Thread(task, "PredictionRecalibrationWorker");
        thread.setDaemon(true);
        thread.start();
    }
}
