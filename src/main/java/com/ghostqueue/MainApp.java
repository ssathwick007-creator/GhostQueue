package com.ghostqueue;

import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseSession;
import com.ghostqueue.util.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            NavigationManager.setPrimaryStage(primaryStage);

            if (SessionManager.getInstance().isAuthenticated()) {
                SupabaseSession.UserRole role = SessionManager.getInstance().getCurrentRole();
                NavigationManager.navigateToPortal(role);
            } else {
                NavigationManager.navigateToLogin();
            }

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
