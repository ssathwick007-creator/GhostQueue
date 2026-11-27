package com.ghostqueue.controller.patient;

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

public class PatientLayoutController {

    @FXML private StackPane brandIconContainer;
    @FXML private Button navOverview;
    @FXML private Button navBook;
    @FXML private Button navMyAppointments;
    @FXML private Button navHistory;
    @FXML private Button navDoctors;
    @FXML private Button navNotifications;
    @FXML private Button navProfile;

    @FXML private Label breadcrumbLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label userAvatarLabel;
    @FXML private Label userNameLabel;

    @FXML private StackPane contentContainer;

    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private Button currentActiveButton;

    @FXML
    public void initialize() {
        if (brandIconContainer != null) {
            brandIconContainer.getChildren().setAll(IconHelper.createBrandLogo(0.95));
        }

        setupNavIcon(navOverview, "overview");
        setupNavIcon(navBook, "appointments");
        setupNavIcon(navMyAppointments, "calendar");
        setupNavIcon(navHistory, "records");
        setupNavIcon(navDoctors, "hospital");
        setupNavIcon(navNotifications, "bell");
        setupNavIcon(navProfile, "profile");

        currentDateLabel.setText(DateTimeUtils.formatHeaderDate(LocalDate.now()));

        SupabaseSession session = SessionManager.getInstance().getCurrentSession();
        if (session != null) {
            String name = session.getFullName();
            userNameLabel.setText(name);
            userAvatarLabel.setText(name != null && !name.isBlank() ? name.substring(0, 1).toUpperCase() : "P");
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
        loadView("overview", "/fxml/patient/patient_overview.fxml", "Patient Portal / Overview", "Patient Overview");
    }

    @FXML
    public void handleNavBook() {
        setActiveButton(navBook);
        loadView("book", "/fxml/patient/patient_book_appointment.fxml", "Patient Portal / Scheduling", "Book an Appointment");
    }

    @FXML
    public void handleNavMyAppointments() {
        setActiveButton(navMyAppointments);
        loadView("my_appointments", "/fxml/patient/patient_my_appointments.fxml", "Patient Portal / Consultations", "My Scheduled Appointments");
    }

    @FXML
    public void handleNavHistory() {
        setActiveButton(navHistory);
        loadView("history", "/fxml/patient/patient_history.fxml", "Patient Portal / History", "Appointment History & Outcomes");
    }

    @FXML
    public void handleNavDoctors() {
        setActiveButton(navDoctors);
        loadView("doctors", "/fxml/patient/patient_doctors.fxml", "Patient Portal / Medical Staff", "Physicians & Specialists");
    }

    @FXML
    public void handleNavNotifications() {
        setActiveButton(navNotifications);
        loadView("notifications", "/fxml/patient/patient_notifications.fxml", "Patient Portal / Notifications", "In-App Alerts & Reminders");
    }

    @FXML
    public void handleNavProfile() {
        setActiveButton(navProfile);
        loadView("profile", "/fxml/patient/patient_profile.fxml", "Patient Portal / Profile", "Patient Record & Preferences");
    }

    @FXML
    public void handleLogout() {
        NavigationManager.logout();
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
        if (button == navBook) return "appointments";
        if (button == navMyAppointments) return "calendar";
        if (button == navHistory) return "records";
        if (button == navDoctors) return "hospital";
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

                if (controller instanceof PatientOverviewController ctrl) {
                    ctrl.setLayoutController(this);
                } else if (controller instanceof PatientDoctorsController docCtrl) {
                    docCtrl.setLayoutController(this);
                }

                viewCache.put(viewKey, viewNode);
                controllerCache.put(viewKey, controller);
            }

            contentContainer.getChildren().setAll(viewNode);

            // Trigger refresh
            Object ctrl = controllerCache.get(viewKey);
            if (ctrl instanceof PatientOverviewController overviewCtrl) {
                overviewCtrl.loadData();
            } else if (ctrl instanceof PatientMyAppointmentsController apptCtrl) {
                apptCtrl.loadData();
            } else if (ctrl instanceof PatientHistoryController histCtrl) {
                histCtrl.loadData();
            } else if (ctrl instanceof PatientDoctorsController docCtrl) {
                docCtrl.loadData();
            } else if (ctrl instanceof PatientNotificationsController notifCtrl) {
                notifCtrl.loadData();
            } else if (ctrl instanceof PatientProfileController profCtrl) {
                profCtrl.loadData();
            }

        } catch (IOException e) {
            e.printStackTrace();
            NotificationManager.showWarning("Navigation Error", "Could not load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }
}
