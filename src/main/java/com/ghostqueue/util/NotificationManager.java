package com.ghostqueue.util;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

public final class NotificationManager {

    private NotificationManager() {}

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleDialog(alert, title, message);
        alert.showAndWait();
    }

    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleDialog(alert, title, message);
        alert.setHeaderText("Action Completed Successfully");
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        styleDialog(alert, title, message);
        alert.setHeaderText("Clinical Operational Warning");
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(alert, title, message);
        alert.setHeaderText(header);
        ButtonType btnConfirm = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnConfirm, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnConfirm;
    }

    private static void styleDialog(Alert alert, String title, String message) {
        alert.setTitle(title);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(NotificationManager.class.getResource("/css/theme.css").toExternalForm());
        dialogPane.getStyleClass().add("hospital-dialog");
    }
}
