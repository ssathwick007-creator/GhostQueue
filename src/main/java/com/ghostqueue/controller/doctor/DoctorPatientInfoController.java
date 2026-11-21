package com.ghostqueue.controller.doctor;

import com.ghostqueue.model.Patient;
import com.ghostqueue.service.ServiceRegistry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DoctorPatientInfoController {

    @FXML private TextField searchField;
    @FXML private TableView<Patient> patientsTable;
    @FXML private TableColumn<Patient, String> colId;
    @FXML private TableColumn<Patient, String> colName;
    @FXML private TableColumn<Patient, Integer> colAge;
    @FXML private TableColumn<Patient, String> colGender;
    @FXML private TableColumn<Patient, String> colPhone;
    @FXML private TableColumn<Patient, String> colEmail;
    @FXML private TableColumn<Patient, Integer> colTotal;
    @FXML private TableColumn<Patient, Integer> colAttended;
    @FXML private TableColumn<Patient, Integer> colNoShows;
    @FXML private TableColumn<Patient, String> colNotes;

    private final ObservableList<Patient> tableData = FXCollections.observableArrayList();
    private final List<Patient> masterData = new ArrayList<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colAge.setCellValueFactory(new PropertyValueFactory<>("age"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAppointments"));
        colAttended.setCellValueFactory(new PropertyValueFactory<>("attendedAppointments"));
        colNoShows.setCellValueFactory(new PropertyValueFactory<>("noShowCount"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("medicalNotes"));

        patientsTable.setItems(tableData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        loadData();
    }

    public void loadData() {
        CompletableFuture.supplyAsync(() -> {
            return ServiceRegistry.getPatientRepo().findAll();
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
        for (Patient p : masterData) {
            boolean matchName = p.getFullName() != null && p.getFullName().toLowerCase().contains(q);
            boolean matchPhone = p.getPhone() != null && p.getPhone().contains(q);
            boolean matchId = p.getId() != null && p.getId().toLowerCase().contains(q);
            if (matchName || matchPhone || matchId) {
                tableData.add(p);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
