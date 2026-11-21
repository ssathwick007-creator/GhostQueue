package com.ghostqueue.util;

import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Navigation Coordinator for GhostQueue.
 * Handles role-based portal switching, login, registration, and logout workflows.
 */
public final class NavigationManager {

    private static final Logger LOGGER = Logger.getLogger(NavigationManager.class.getName());
    private static Stage primaryStage;

    private NavigationManager() {}

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void navigateToLogin() {
        loadScene("/fxml/login.fxml", "GhostQueue — Healthcare Intelligence & Access Portal", 1150, 720);
    }

    public static void navigateToRegister() {
        loadScene("/fxml/register.fxml", "GhostQueue — Patient Account Registration", 1150, 720);
    }

    public static void navigateToPortal(SupabaseSession.UserRole role) {
        if (role == null) {
            navigateToLogin();
            return;
        }

        switch (role) {
            case ADMIN -> loadScene("/fxml/main_layout.fxml",
                    "GhostQueue — Clinical Operations & Intelligence Platform (Admin)", 1340, 860);
            case DOCTOR -> loadScene("/fxml/doctor_layout.fxml",
                    "GhostQueue — Physician Clinical Portal", 1300, 840);
            case PATIENT -> loadScene("/fxml/patient_layout.fxml",
                    "GhostQueue — Patient Appointment & Health Portal", 1260, 820);
        }
    }

    public static void logout() {
        SessionManager.getInstance().clearSession();
        NotificationManager.showInfo("Session Terminated", "You have successfully signed out of GhostQueue.");
        navigateToLogin();
    }

    private static void loadScene(String fxmlPath, String title, double width, double height) {
        if (primaryStage == null) {
            LOGGER.severe("Cannot navigate: primaryStage is null in NavigationManager");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, width, height);
                scene.getStylesheets().add(NavigationManager.class.getResource("/css/theme.css").toExternalForm());
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
                if (scene.getStylesheets().isEmpty()) {
                    scene.getStylesheets().add(NavigationManager.class.getResource("/css/theme.css").toExternalForm());
                }
            }

            primaryStage.setTitle(title);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(700);
            primaryStage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load view: " + fxmlPath, e);
            NotificationManager.showWarning("Navigation Error", "Could not load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }
}
