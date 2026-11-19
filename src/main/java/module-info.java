module com.ghostqueue {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires java.sql;
    requires java.logging;

    opens com.ghostqueue to javafx.fxml, javafx.graphics;
    opens com.ghostqueue.controller to javafx.fxml, javafx.base;
    opens com.ghostqueue.model to javafx.base;
    opens com.ghostqueue.prediction to javafx.base, javafx.fxml;
    opens com.ghostqueue.component to javafx.fxml;

    exports com.ghostqueue;
    exports com.ghostqueue.controller;
    exports com.ghostqueue.model;
    exports com.ghostqueue.service;
    exports com.ghostqueue.repository;
    exports com.ghostqueue.prediction;
    exports com.ghostqueue.scheduling;
    exports com.ghostqueue.component;
    exports com.ghostqueue.util;
}
