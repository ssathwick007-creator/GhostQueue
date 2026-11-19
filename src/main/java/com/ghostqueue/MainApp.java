package com.ghostqueue;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_layout.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1340, 860);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

            primaryStage.setTitle("GhostQueue — Intelligent Predictive No-Show Detection & Dynamic Appointment Reallocation");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1150);
            primaryStage.setMinHeight(720);

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start GhostQueue: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
