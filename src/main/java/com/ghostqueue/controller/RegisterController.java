package com.ghostqueue.controller;

import com.ghostqueue.component.IconHelper;
import com.ghostqueue.supabase.SupabaseAuthService;
import com.ghostqueue.util.NavigationManager;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class RegisterController {

    @FXML private StackPane brandLogoContainer;
    @FXML private StackPane featCalendarIcon;
    @FXML private StackPane featRemindIcon;
    @FXML private StackPane featRecordsIcon;

    @FXML private HBox errorBanner;
    @FXML private StackPane errorIconContainer;
    @FXML private Label errorLabel;

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField addressField;
    @FXML private CheckBox termsCheck;
    @FXML private Button registerBtn;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML
    public void initialize() {
        if (brandLogoContainer != null) {
            brandLogoContainer.getChildren().setAll(IconHelper.createIcon("overview", "#168C83", 1.4));
        }
        if (featCalendarIcon != null) {
            featCalendarIcon.getChildren().setAll(IconHelper.createIcon("appointments", "#21A69A", 0.9));
        }
        if (featRemindIcon != null) {
            featRemindIcon.getChildren().setAll(IconHelper.createIcon("reminders", "#21A69A", 0.9));
        }
        if (featRecordsIcon != null) {
            featRecordsIcon.getChildren().setAll(IconHelper.createIcon("patients", "#21A69A", 0.9));
        }
        if (errorIconContainer != null) {
            errorIconContainer.getChildren().setAll(IconHelper.createIcon("alert", "#C95F5F", 0.9));
        }

        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other", "Prefer not to say"));
        genderCombo.setValue("Female");

        dobPicker.setValue(LocalDate.now().minusYears(25));
    }

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText() != null ? fullNameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().trim() : "";
        String confirmPass = confirmPasswordField.getText() != null ? confirmPasswordField.getText().trim() : "";
        LocalDate dob = dobPicker.getValue();
        String gender = genderCombo.getValue();
        String address = addressField.getText() != null ? addressField.getText().trim() : "";

        // Validation
        if (fullName.isBlank()) {
            showError("Please enter your full legal name.");
            return;
        }
        if (email.isBlank() || !email.contains("@")) {
            showError("Please enter a valid email address.");
            return;
        }
        if (phone.isBlank()) {
            showError("Please enter your contact phone number.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.");
            return;
        }
        if (!password.equals(confirmPass)) {
            showError("Passwords do not match.");
            return;
        }
        if (dob == null || dob.isAfter(LocalDate.now())) {
            showError("Please select a valid date of birth.");
            return;
        }
        if (!termsCheck.isSelected()) {
            showError("Please accept the terms of service and privacy policy to continue.");
            return;
        }

        hideError();
        setLoading(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                return SupabaseAuthService.getInstance().signUpPatient(
                        email, password, fullName, phone, dob.toString(), gender, address
                );
            } catch (Exception e) {
                return "Registration error: " + e.getMessage();
            }
        }).thenAccept(result -> Platform.runLater(() -> {
            setLoading(false);

            if ("SUCCESS".equalsIgnoreCase(result)) {
                NotificationManager.showSuccess("Registration Successful",
                        "Your patient account has been created! Please sign in with your credentials.");
                NavigationManager.navigateToLogin();
            } else {
                showError(result != null ? result : "Registration could not be completed. Please check your details.");
            }
        }));
    }

    @FXML
    private void handleBackToLogin() {
        NavigationManager.navigateToLogin();
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
        registerBtn.setDisable(loading);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
    }
}
