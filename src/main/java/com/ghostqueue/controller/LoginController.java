package com.ghostqueue.controller;

import com.ghostqueue.component.IconHelper;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseAuthService;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.NavigationManager;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.concurrent.CompletableFuture;

public class LoginController {

    @FXML private StackPane brandLogoContainer;
    @FXML private StackPane featAiIcon;
    @FXML private StackPane featQueueIcon;
    @FXML private StackPane featSecIcon;

    @FXML private HBox errorBanner;
    @FXML private StackPane errorIconContainer;
    @FXML private Label errorLabel;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Button signInBtn;
    @FXML private ProgressIndicator loadingIndicator;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        // Setup hero brand and feature icons
        if (brandLogoContainer != null) {
            brandLogoContainer.getChildren().setAll(IconHelper.createIcon("overview", "#168C83", 1.4));
        }
        if (featAiIcon != null) {
            featAiIcon.getChildren().setAll(IconHelper.createIcon("prediction", "#21A69A", 1.0));
        }
        if (featQueueIcon != null) {
            featQueueIcon.getChildren().setAll(IconHelper.createIcon("queue", "#21A69A", 1.0));
        }
        if (featSecIcon != null) {
            featSecIcon.getChildren().setAll(IconHelper.createIcon("accounts", "#21A69A", 1.0));
        }
        if (errorIconContainer != null) {
            errorIconContainer.getChildren().setAll(IconHelper.createIcon("alert", "#C95F5F", 0.9));
        }

        // Sync password text field with password field
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        // Fill default demo credentials
        handleAdminDemo();
    }

    @FXML
    private void handleAdminDemo() {
        emailField.setText("admin@ghostqueue.com");
        passwordField.setText("Admin@123");
        hideError();
    }

    @FXML
    private void handleDoctorDemo() {
        emailField.setText("doctor.vance@ghostqueue.com");
        passwordField.setText("Doctor@123");
        hideError();
    }

    @FXML
    private void handlePatientDemo() {
        emailField.setText("patient.eleanor@ghostqueue.com");
        passwordField.setText("Patient@123");
        hideError();
    }

    @FXML
    private void handleTogglePassword() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🔒");
        } else {
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordBtn.setText("👁");
        }
    }

    @FXML
    private void handleSignIn() {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().trim() : "";

        if (email.isBlank() || password.isBlank()) {
            showError("Please enter both your work email and password.");
            return;
        }

        hideError();
        setLoading(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                boolean remember = rememberMeCheck != null && rememberMeCheck.isSelected();
                return SupabaseAuthService.getInstance().authenticate(email, password, remember);
            } catch (Exception e) {
                return "Authentication error: " + e.getMessage();
            }
        }).thenAccept(result -> Platform.runLater(() -> {
            setLoading(false);

            if ("SUCCESS".equalsIgnoreCase(result)) {
                SupabaseSession session = SessionManager.getInstance().getCurrentSession();
                if (session == null) {
                    showError("Unable to establish active user session. Please try again.");
                    return;
                }

                NotificationManager.showSuccess("Welcome Back",
                        "Signed in as " + session.getFullName() + " (" + session.getRole() + ")");
                NavigationManager.navigateToPortal(session.getUserRole());

            } else {
                showError(result != null ? result : "Authentication failed. Invalid email or password.");
            }
        }));
    }

    @FXML
    private void handleOpenRegister() {
        NavigationManager.navigateToRegister();
    }

    @FXML
    private void handleForgotPassword() {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        if (email.isBlank()) {
            showError("Please enter your registered email address to receive password recovery instructions.");
            return;
        }

        setLoading(true);
        CompletableFuture.runAsync(() -> {
            boolean sent = SupabaseAuthService.getInstance().resetPasswordForEmail(email);
            Platform.runLater(() -> {
                setLoading(false);
                if (sent) {
                    NotificationManager.showInfo("Recovery Dispatched",
                            "Password recovery link sent to " + email + ". Please check your inbox.");
                } else {
                    showError("Failed to initiate password recovery. Please contact IT support.");
                }
            });
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    private void hideError() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    private void setLoading(boolean loading) {
        signInBtn.setDisable(loading);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
    }
}
