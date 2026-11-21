package com.ghostqueue.controller.doctor;

import com.ghostqueue.component.IconHelper;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.DateTimeUtils;
import com.ghostqueue.util.NavigationManager;
import com.ghostqueue.util.NotificationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DoctorLayoutController {

    @FXML private StackPane brandIconContainer;
    @FXML private Button navOverview;
    @FXML private Button navSchedule;
    @FXML private Button navAppointments;
    @FXML private Button navPatients;
    @FXML private Button navAvailability;
    @FXML private Button navNotifications;
    @FXML private Button navProfile;

    @FXML private Label breadcrumbLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label userAvatarLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userDeptLabel;
    @FXML private Label statusPillLabel;

    @FXML private StackPane contentContainer;

    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private Button currentActiveButton;

    @FXML
    public void initialize() {
        if (brandIconContainer != null) {
            brandIconContainer.getChildren().setAll(IconHelper.createIcon("hospital", "#168C83", 1.2));
        }

        setupNavIcon(navOverview, "overview");
        setupNavIcon(navSchedule, "calendar");
        setupNavIcon(navAppointments, "appointments");
        setupNavIcon(navPatients, "patients");
        setupNavIcon(navAvailability, "clock");
        setupNavIcon(navNotifications, "bell");
        setupNavIcon(navProfile, "profile");

        currentDateLabel.setText(DateTimeUtils.formatHeaderDate(LocalDate.now()));

        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session != null) {
            String name = session.getFullName();
            userNameLabel.setText(name);
            userAvatarLabel.setText(name != null && !name.isBlank() ? name.substring(0, 1).toUpperCase() : "D");
            if (session.getDoctorId() != null) {
                userDeptLabel.setText("Attending Physician (" + session.getDoctorId() + ")");
            }
        }

        handleNavOverview();
    }

    private void setupNavIcon(Button button, String iconName) {
        if (button != null) {
            Node icon = IconHelper.createIcon(iconName, "#A0B2C6", 1.0);
            button.setGraphic(icon);
        }
    }

    @FXML
    public void handleNavOverview() {
        setActiveButton(navOverview);
        loadView("overview", "/fxml/doctor/doctor_overview.fxml", "Physician Portal / Overview", "Clinical Operations Overview");
    }

    @FXML
    public void handleNavSchedule() {
        setActiveButton(navSchedule);
        loadView("schedule", "/fxml/doctor/doctor_schedule.fxml", "Physician Portal / Roster", "Consultation Schedule & Timeline");
    }

    @FXML
    public void handleNavAppointments() {
        setActiveButton(navAppointments);
        loadView("appointments", "/fxml/doctor/doctor_appointments.fxml", "Physician Portal / Consultations", "Patient Queue & Consultations");
    }

    @FXML
    public void handleNavPatients() {
        setActiveButton(navPatients);
        loadView("patients", "/fxml/doctor/doctor_patient_info.fxml", "Physician Portal / Patient Records", "Patient Clinical Records");
    }

    @FXML
    public void handleNavAvailability() {
        setActiveButton(navAvailability);
        loadView("availability", "/fxml/doctor/doctor_availability.fxml", "Physician Portal / Working Hours", "Availability & Shift Preferences");
    }

    @FXML
    public void handleNavNotifications() {
        setActiveButton(navNotifications);
        loadView("notifications", "/fxml/doctor/doctor_notifications.fxml", "Physician Portal / Alerts", "Clinical Alerts & System Notices");
    }

    @FXML
    public void handleNavProfile() {
        setActiveButton(navProfile);
        loadView("profile", "/fxml/doctor/doctor_profile.fxml", "Physician Portal / Profile", "Specialist Profile & Credentials");
    }

    @FXML
    public void handleLogout() {
        NavigationManager.logout();
    }

    public void setStatusPill(String statusText) {
        if (statusPillLabel != null) {
            statusPillLabel.setText(statusText);
        }
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active");
            if (currentActiveButton.getGraphic() != null) {
                String iconKey = getIconKeyForButton(currentActiveButton);
                currentActiveButton.setGraphic(IconHelper.createIcon(iconKey, "#A0B2C6", 1.0));
            }
        }
        currentActiveButton = button;
        if (currentActiveButton != null) {
            if (!currentActiveButton.getStyleClass().contains("active")) {
                currentActiveButton.getStyleClass().add("active");
            }
            String iconKey = getIconKeyForButton(currentActiveButton);
            currentActiveButton.setGraphic(IconHelper.createIcon(iconKey, "#FFFFFF", 1.0));
        }
    }

    private String getIconKeyForButton(Button button) {
        if (button == navOverview) return "overview";
        if (button == navSchedule) return "calendar";
        if (button == navAppointments) return "appointments";
        if (button == navPatients) return "patients";
        if (button == navAvailability) return "clock";
        if (button == navNotifications) return "bell";
        if (button == navProfile) return "profile";
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

                if (controller instanceof DoctorOverviewController ctrl) {
                    ctrl.setLayoutController(this);
                } else if (controller instanceof DoctorAvailabilityController availCtrl) {
                    availCtrl.setLayoutController(this);
                }

                viewCache.put(viewKey, viewNode);
                controllerCache.put(viewKey, controller);
            }

            contentContainer.getChildren().setAll(viewNode);

            // Trigger refresh
            Object ctrl = controllerCache.get(viewKey);
            if (ctrl instanceof DoctorOverviewController overviewCtrl) {
                overviewCtrl.loadData();
            } else if (ctrl instanceof DoctorScheduleController schedCtrl) {
                schedCtrl.loadData();
            } else if (ctrl instanceof DoctorAppointmentsController apptCtrl) {
                apptCtrl.loadData();
            } else if (ctrl instanceof DoctorPatientInfoController patCtrl) {
                patCtrl.loadData();
            } else if (ctrl instanceof DoctorAvailabilityController availCtrl) {
                availCtrl.loadData();
            } else if (ctrl instanceof DoctorNotificationsController notifCtrl) {
                notifCtrl.loadData();
            } else if (ctrl instanceof DoctorProfileController profCtrl) {
                profCtrl.loadData();
            }

        } catch (IOException e) {
            e.printStackTrace();
            NotificationManager.showWarning("Navigation Error", "Could not load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }
}
