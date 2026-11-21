package com.ghostqueue.controller;

import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;
import com.ghostqueue.util.NotificationManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AccountManagementController {

    public static class ProfileItem {
        private String id;
        private String email;
        private String fullName;
        private String role;
        private boolean isApproved;
        private String status;

        public ProfileItem(String id, String email, String fullName, String role, boolean isApproved, String status) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.isApproved = isApproved;
            this.status = status;
        }

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public boolean isApproved() { return isApproved; }
        public String getStatus() { return status; }

        public void setApproved(boolean approved) { isApproved = approved; }
        public void setStatus(String status) { this.status = status; }
    }

    @FXML private Label totalProfilesLabel;
    @FXML private Label approvedDocsLabel;
    @FXML private Label pendingDocsLabel;
    @FXML private Label patientsCountLabel;

    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private TextField searchField;

    @FXML private TableView<ProfileItem> profilesTable;
    @FXML private TableColumn<ProfileItem, String> colId;
    @FXML private TableColumn<ProfileItem, String> colName;
    @FXML private TableColumn<ProfileItem, String> colEmail;
    @FXML private TableColumn<ProfileItem, String> colRole;
    @FXML private TableColumn<ProfileItem, String> colApproval;
    @FXML private TableColumn<ProfileItem, String> colStatus;
    @FXML private TableColumn<ProfileItem, Void> colActions;

    private final ObservableList<ProfileItem> tableData = FXCollections.observableArrayList();
    private final List<ProfileItem> masterData = new ArrayList<>();
    private final SupabaseClient client = SupabaseClient.getInstance();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colApproval.setCellValueFactory(cellData -> {
            boolean approved = cellData.getValue().isApproved();
            return new javafx.beans.property.SimpleStringProperty(approved ? "APPROVED" : "PENDING APPROVAL");
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupActionsColumn();

        roleFilterCombo.setItems(FXCollections.observableArrayList("All Roles", "DOCTOR", "PATIENT", "ADMIN"));
        roleFilterCombo.setValue("All Roles");
        roleFilterCombo.setOnAction(e -> applyFilter());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        profilesTable.setItems(tableData);

        loadData();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve Doctor");
            private final Button suspendBtn = new Button("Suspend");
            private final Button activateBtn = new Button("Reactivate");
            private final HBox container = new HBox(6);

            {
                approveBtn.setStyle("-fx-background-color: #168C83; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4px 10px; -fx-background-radius: 4px; -fx-cursor: hand;");
                suspendBtn.setStyle("-fx-background-color: rgba(201, 95, 95, 0.15); -fx-text-fill: #C95F5F; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4px 10px; -fx-background-radius: 4px; -fx-cursor: hand;");
                activateBtn.setStyle("-fx-background-color: #E6F5F3; -fx-text-fill: #168C83; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4px 10px; -fx-background-radius: 4px; -fx-cursor: hand;");

                approveBtn.setOnAction(e -> {
                    ProfileItem item = getTableView().getItems().get(getIndex());
                    handleApprove(item);
                });
                suspendBtn.setOnAction(e -> {
                    ProfileItem item = getTableView().getItems().get(getIndex());
                    handleSuspend(item);
                });
                activateBtn.setOnAction(e -> {
                    ProfileItem item = getTableView().getItems().get(getIndex());
                    handleActivate(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    ProfileItem p = getTableView().getItems().get(getIndex());
                    container.getChildren().clear();

                    if ("DOCTOR".equalsIgnoreCase(p.getRole()) && !p.isApproved()) {
                        container.getChildren().add(approveBtn);
                    }

                    if (!"ADMIN".equalsIgnoreCase(p.getRole())) {
                        if ("SUSPENDED".equalsIgnoreCase(p.getStatus())) {
                            container.getChildren().add(activateBtn);
                        } else {
                            container.getChildren().add(suspendBtn);
                        }
                    }

                    setGraphic(container);
                }
            }
        });
    }

    public void loadData() {
        CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("select", "*");
                params.put("order", "created_at.desc");
                SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("profiles"), params);
                return resp.asList();
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.<Map<String, Object>>emptyList();
            }
        }).thenAccept(rows -> Platform.runLater(() -> {
            masterData.clear();
            int total = rows.size();
            int approvedDocs = 0;
            int pendingDocs = 0;
            int patients = 0;

            for (Map<String, Object> r : rows) {
                String id = (String) r.get("id");
                String email = (String) r.get("email");
                String fullName = (String) r.get("full_name");
                String role = (String) r.get("role");
                boolean approved = Boolean.TRUE.equals(r.get("is_approved"));
                String status = (String) r.getOrDefault("status", "ACTIVE");

                if ("DOCTOR".equalsIgnoreCase(role)) {
                    if (approved) approvedDocs++;
                    else pendingDocs++;
                } else if ("PATIENT".equalsIgnoreCase(role)) {
                    patients++;
                }

                masterData.add(new ProfileItem(id, email, fullName, role, approved, status));
            }

            totalProfilesLabel.setText(String.valueOf(total));
            approvedDocsLabel.setText(String.valueOf(approvedDocs));
            pendingDocsLabel.setText(String.valueOf(pendingDocs));
            patientsCountLabel.setText(String.valueOf(patients));

            applyFilter();
        }));
    }

    private void handleApprove(ProfileItem item) {
        if (item == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("is_approved", true);
                updates.put("status", "ACTIVE");
                client.patch(SupabaseConfig.getRestEndpoint("profiles"), Map.of("id", "eq." + item.getId()), updates);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(() -> Platform.runLater(() -> {
            NotificationManager.showSuccess("Doctor Approved", item.getFullName() + " is now authorized to practice in GhostQueue.");
            loadData();
        }));
    }

    private void handleSuspend(ProfileItem item) {
        if (item == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "SUSPENDED");
                client.patch(SupabaseConfig.getRestEndpoint("profiles"), Map.of("id", "eq." + item.getId()), updates);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(() -> Platform.runLater(() -> {
            NotificationManager.showWarning("Account Suspended", item.getFullName() + "'s credentials have been suspended.");
            loadData();
        }));
    }

    private void handleActivate(ProfileItem item) {
        if (item == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "ACTIVE");
                client.patch(SupabaseConfig.getRestEndpoint("profiles"), Map.of("id", "eq." + item.getId()), updates);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(() -> Platform.runLater(() -> {
            NotificationManager.showSuccess("Account Reactivated", item.getFullName() + "'s access has been restored.");
            loadData();
        }));
    }

    private void applyFilter() {
        tableData.clear();
        String roleFilter = roleFilterCombo.getValue();
        String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        for (ProfileItem p : masterData) {
            boolean matchRole = roleFilter == null || "All Roles".equalsIgnoreCase(roleFilter)
                    || (p.getRole() != null && p.getRole().equalsIgnoreCase(roleFilter));

            if (!matchRole) continue;

            if (!q.isBlank()) {
                boolean matchName = p.getFullName() != null && p.getFullName().toLowerCase().contains(q);
                boolean matchEmail = p.getEmail() != null && p.getEmail().toLowerCase().contains(q);
                if (!matchName && !matchEmail) continue;
            }

            tableData.add(p);
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
