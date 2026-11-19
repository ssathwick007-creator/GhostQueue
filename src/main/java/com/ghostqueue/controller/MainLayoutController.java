package com.ghostqueue.controller;

import com.ghostqueue.component.IconHelper;
import com.ghostqueue.util.DateTimeUtils;
import com.ghostqueue.util.NotificationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MainLayoutController {

    @FXML private StackPane brandIconContainer;
    @FXML private Button navOverview;
    @FXML private Button navAppointments;
    @FXML private Button navPrediction;
    @FXML private Button navQueue;
    @FXML private Button navReallocation;
    @FXML private Button navPatients;
    @FXML private Button navAnalytics;
    @FXML private Button navSettings;

    @FXML private Label breadcrumbLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private TextField globalSearchField;
    @FXML private Button notificationButton;
    @FXML private Button quickActionBtn;

    @FXML private StackPane contentContainer;

    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private Button currentActiveButton;

    @FXML
    public void initialize() {
        // Setup Brand vector icon
        Node brandSvg = IconHelper.createIcon("overview", "#168C83", 1.3);
        brandIconContainer.getChildren().setAll(brandSvg);

        // Setup Sidebar vector icons
        setupNavIcon(navOverview, "overview");
        setupNavIcon(navAppointments, "appointments");
        setupNavIcon(navPrediction, "prediction");
        setupNavIcon(navQueue, "queue");
        setupNavIcon(navReallocation, "reallocation");
        setupNavIcon(navPatients, "patients");
        setupNavIcon(navAnalytics, "analytics");
        setupNavIcon(navSettings, "settings");

        // Setup Top Nav Date
        currentDateLabel.setText(DateTimeUtils.formatHeaderDate(LocalDate.now()));

        // Initial default view: Overview (Dashboard)
        currentActiveButton = navOverview;
        loadView("overview", "/fxml/dashboard.fxml", "Clinical Operations / Overview", "Operations Overview");
    }

    private void setupNavIcon(Button button, String iconName) {
        Node icon = IconHelper.createIcon(iconName, "#A0B2C6", 1.0);
        button.setGraphic(icon);
    }

    public void navigateTo(String destination) {
        switch (destination.toLowerCase()) {
            case "overview" -> handleNavOverview();
            case "appointments" -> handleNavAppointments();
            case "prediction" -> handleNavPrediction();
            case "queue" -> handleNavQueue();
            case "reallocation" -> handleNavReallocation();
            case "patients" -> handleNavPatients();
            case "analytics" -> handleNavAnalytics();
            case "settings" -> handleNavSettings();
        }
    }

    @FXML
    private void handleNavOverview() {
        setActiveButton(navOverview);
        loadView("overview", "/fxml/dashboard.fxml", "Clinical Operations / Overview", "Operations Overview");
    }

    @FXML
    private void handleNavAppointments() {
        setActiveButton(navAppointments);
        loadView("appointments", "/fxml/appointments.fxml", "Clinical Scheduling / Appointments", "Appointment Management");
    }

    @FXML
    private void handleNavPrediction() {
        setActiveButton(navPrediction);
        loadView("prediction", "/fxml/prediction.fxml", "AI Intelligence / No-show Detection", "Predictive No-Show Dashboard");
    }

    @FXML
    private void handleNavQueue() {
        setActiveButton(navQueue);
        loadView("queue", "/fxml/waiting_queue.fxml", "Queue Intelligence / Standby Pool", "Patient Waiting Queue");
    }

    @FXML
    private void handleNavReallocation() {
        setActiveButton(navReallocation);
        loadView("reallocation", "/fxml/reallocation.fxml", "Dynamic Scheduling / Slot Reallocation", "Smart Slot Reallocation");
    }

    @FXML
    private void handleNavPatients() {
        setActiveButton(navPatients);
        loadView("patients", "/fxml/patient_records.fxml", "Master Records / Patient Registry", "Patient Records & Attendance");
    }

    @FXML
    private void handleNavAnalytics() {
        setActiveButton(navAnalytics);
        loadView("analytics", "/fxml/analytics.fxml", "Performance Insights / Operational Metrics", "Hospital Attendance Analytics");
    }

    @FXML
    private void handleNavSettings() {
        setActiveButton(navSettings);
        loadView("settings", "/fxml/settings.fxml", "System Preferences / Configuration", "System Configuration");
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active");
            if (currentActiveButton.getGraphic() != null) {
                // Restore normal nav icon color
                String iconKey = getIconKeyForButton(currentActiveButton);
                currentActiveButton.setGraphic(IconHelper.createIcon(iconKey, "#A0B2C6", 1.0));
            }
        }
        currentActiveButton = button;
        if (!currentActiveButton.getStyleClass().contains("active")) {
            currentActiveButton.getStyleClass().add("active");
        }
        String iconKey = getIconKeyForButton(currentActiveButton);
        currentActiveButton.setGraphic(IconHelper.createIcon(iconKey, "#FFFFFF", 1.0));
    }

    private String getIconKeyForButton(Button button) {
        if (button == navOverview) return "overview";
        if (button == navAppointments) return "appointments";
        if (button == navPrediction) return "prediction";
        if (button == navQueue) return "queue";
        if (button == navReallocation) return "reallocation";
        if (button == navPatients) return "patients";
        if (button == navAnalytics) return "analytics";
        if (button == navSettings) return "settings";
        return "overview";
    }

    private void loadView(String viewKey, String fxmlPath, String breadcrumb, String title) {
        try {
            breadcrumbLabel.setText(breadcrumb);
            pageTitleLabel.setText(title);

            Node viewNode = viewCache.get(viewKey);
            if (viewNode == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                viewNode = loader.load();
                Object controller = loader.getController();

                if (controller instanceof DashboardController dashCtrl) {
                    dashCtrl.setMainLayoutController(this);
                }

                viewCache.put(viewKey, viewNode);
                controllerCache.put(viewKey, controller);
            }

            contentContainer.getChildren().setAll(viewNode);

            // If switching back to views, refresh their live data
            Object ctrl = controllerCache.get(viewKey);
            if (ctrl instanceof DashboardController dashCtrl) {
                dashCtrl.loadData();
            } else if (ctrl instanceof PatientRecordsController patientCtrl) {
                patientCtrl.loadPatients();
            } else if (ctrl instanceof ReallocationController reallocCtrl) {
                reallocCtrl.loadData();
            }

        } catch (IOException e) {
            e.printStackTrace();
            NotificationManager.showWarning("Navigation Error", "Could not load screen: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    @FXML
    private void handleGlobalSearch() {
        String query = globalSearchField.getText();
        if (query != null && !query.isBlank()) {
            handleNavAppointments();
            Object ctrl = controllerCache.get("appointments");
            if (ctrl instanceof AppointmentsController apptCtrl) {
                // Handled in appointments view
            }
        }
    }

    @FXML
    private void handleNotificationClick() {
        NotificationManager.showInfo("Hospital Operational Notifications",
                "1. [CRITICAL ALERT] 4 High-Risk appointment slots identified for today (Predicted No-Show ≥ 70%). Standby patients alerted.\n\n" +
                        "2. [REALLOCATION RECOMMENDATION] Cancelled Cardiology slot with Dr. Robert Vance has 1 urgent candidate match (Samuel O'Connor).\n\n" +
                        "3. [CAPACITY RECOVERY] 74.2% schedule reclamation rate maintained over the last 30 operational days.");
    }

    @FXML
    private void handleQuickAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/add_appointment_dialog.fxml"));
            Parent root = loader.load();
            AddAppointmentDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Quick Schedule — GhostQueue");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                NotificationManager.showSuccess("Appointment Scheduled", "Appointment created and registered across active rosters.");
                // Refresh active view
                Object overviewCtrl = controllerCache.get("overview");
                if (overviewCtrl instanceof DashboardController dashCtrl) {
                    dashCtrl.loadData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.showWarning("Error", "Could not open quick add appointment: " + e.getMessage());
        }
    }
}
