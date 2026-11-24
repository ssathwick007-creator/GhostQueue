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

            // Development-only escape hatch for working on the UI while the
            // external authentication service is unavailable.  It is opt-in:
            // use -Dghostqueue.skipAuth=true or GHOSTQUEUE_SKIP_AUTH=true.
            if (isAuthenticationBypassed()) {
                NavigationManager.navigateToPortal(SupabaseSession.UserRole.ADMIN);
            } else if (SessionManager.getInstance().isAuthenticated()) {
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

    private boolean isAuthenticationBypassed() {
        return Boolean.parseBoolean(System.getProperty("ghostqueue.skipAuth", "false"))
                || Boolean.parseBoolean(System.getenv().getOrDefault("GHOSTQUEUE_SKIP_AUTH", "false"));
    }
}
