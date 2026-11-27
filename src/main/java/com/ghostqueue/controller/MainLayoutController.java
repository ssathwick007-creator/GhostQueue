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
    @FXML private Button navReminders;
    @FXML private Button navPatients;
    @FXML private Button navAnalytics;
    @FXML private Button navAccounts;
    @FXML private Button navSettings;

    @FXML private Label adminAvatarLabel;
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Button logoutButton;

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
        // Setup Brand vector logo emblem
        brandIconContainer.getChildren().setAll(IconHelper.createBrandLogo(1.0));

        // Setup Sidebar vector icons
        setupNavIcon(navOverview, "overview");
        setupNavIcon(navAppointments, "appointments");
        setupNavIcon(navPrediction, "prediction");
        setupNavIcon(navQueue, "queue");
        setupNavIcon(navReallocation, "reallocation");
        setupNavIcon(navReminders, "reminders");
        setupNavIcon(navPatients, "patients");
        setupNavIcon(navAnalytics, "analytics");
        setupNavIcon(navAccounts, "accounts");
        setupNavIcon(navSettings, "settings");

        // Setup User Info
        String fullName = "Dr. Sarah Chen";
        String email = "admin@ghostqueue.com";
        try {
            com.ghostqueue.supabase.SupabaseSession session = com.ghostqueue.supabase.SessionManager.getInstance().getCurrentSession();
            if (session != null && session.getFullName() != null && !session.getFullName().isBlank()) {
                fullName = session.getFullName();
                email = session.getEmail();
            }
        } catch (Exception ignored) {}
        if (adminNameLabel != null) adminNameLabel.setText(fullName);
        if (adminRoleLabel != null) adminRoleLabel.setText("System Administrator (" + email + ")");
        if (adminAvatarLabel != null) {
            adminAvatarLabel.setText(fullName.substring(0, 1).toUpperCase());
        }

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
            case "reminders" -> handleNavReminders();
            case "patients" -> handleNavPatients();
            case "analytics" -> handleNavAnalytics();
            case "accounts" -> handleNavAccounts();
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
    private void handleNavReminders() {
        setActiveButton(navReminders);
        loadView("reminders", "/fxml/reminders.fxml", "Patient Communication / Clinical Outreach", "Patient Reminders & Outreach");
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
    private void handleNavAccounts() {
        setActiveButton(navAccounts);
        loadView("accounts", "/fxml/account_management.fxml", "System Administration / Account Governance", "User Account Governance & Doctor Approvals");
    }

    @FXML
    private void handleNavSettings() {
        setActiveButton(navSettings);
        loadView("settings", "/fxml/settings.fxml", "System Preferences / Configuration", "System Configuration");
    }

    @FXML
    private void handleLogout() {
        com.ghostqueue.util.NavigationManager.logout();
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
        if (button == navReminders) return "reminders";
        if (button == navPatients) return "patients";
        if (button == navAnalytics) return "analytics";
        if (button == navAccounts) return "accounts";
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
            } else if (ctrl instanceof RemindersController remCtrl) {
                remCtrl.loadData();
            } else if (ctrl instanceof AnalyticsController analyticsCtrl) {
                analyticsCtrl.loadData();
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
                apptCtrl.setSearchQuery(query);
            }
        }
    }

    @FXML
    private void handleNotificationClick() {
        handleNavReminders();
        NotificationManager.showInfo("Active Outreach & Risk Notifications",
                "Navigated to Reminders & Clinical Outreach.\n\n" +
                        "• 4 High-Risk appointment slots identified for upcoming schedule.\n" +
                        "• Standby candidates ready for dynamic slot reallocation.\n" +
                        "• Multi-channel phone/SMS reminders queued.");
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
