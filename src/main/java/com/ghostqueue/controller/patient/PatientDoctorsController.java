package com.ghostqueue.controller.patient;

import com.ghostqueue.model.Doctor;
import com.ghostqueue.service.ServiceRegistry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PatientDoctorsController {

    @FXML private TextField searchField;
    @FXML private TableView<Doctor> doctorsTable;
    @FXML private TableColumn<Doctor, String> colId;
    @FXML private TableColumn<Doctor, String> colName;
    @FXML private TableColumn<Doctor, String> colDept;
    @FXML private TableColumn<Doctor, String> colSpec;
    @FXML private TableColumn<Doctor, String> colPhone;
    @FXML private TableColumn<Doctor, String> colStatus;
    @FXML private TableColumn<Doctor, Void> colActions;

    private PatientLayoutController layoutController;
    private final ObservableList<Doctor> tableData = FXCollections.observableArrayList();
    private final List<Doctor> masterData = new ArrayList<>();

    public void setLayoutController(PatientLayoutController layoutController) {
        this.layoutController = layoutController;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colSpec.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colStatus.setCellValueFactory(cellData -> {
            boolean avail = cellData.getValue().isAvailable();
            return new javafx.beans.property.SimpleStringProperty(avail ? "AVAILABLE" : "ON DUTY");
        });

        setupActionsColumn();

        doctorsTable.setItems(tableData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        loadData();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button bookBtn = new Button("Book Visit");

            {
                bookBtn.setStyle("-fx-background-color: #168C83; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 5px 14px; -fx-background-radius: 4px; -fx-cursor: hand;");
                bookBtn.setOnAction(event -> {
                    if (layoutController != null) {
                        layoutController.handleNavBook();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(bookBtn);
                }
            }
        });
    }

    public void loadData() {
        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getDoctorRepo().findAll();
        }).thenAccept(list -> Platform.runLater(() -> {
            masterData.clear();
            masterData.addAll(list);
            applyFilter(searchField.getText());
        }));
    }

    private void applyFilter(String query) {
        tableData.clear();
        if (query == null || query.isBlank()) {
            tableData.addAll(masterData);
            return;
        }

        String q = query.trim().toLowerCase();
        for (Doctor d : masterData) {
            boolean matchName = d.getFullName() != null && d.getFullName().toLowerCase().contains(q);
            boolean matchDept = d.getDepartment() != null && d.getDepartment().toLowerCase().contains(q);
            boolean matchSpec = d.getSpecialization() != null && d.getSpecialization().toLowerCase().contains(q);
            if (matchName || matchDept || matchSpec) {
                tableData.add(d);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
