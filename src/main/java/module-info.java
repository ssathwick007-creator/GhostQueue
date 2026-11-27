module com.ghostqueue {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires java.sql;
    requires java.logging;
    requires java.net.http;
    requires jdk.httpserver;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;

    opens com.ghostqueue to javafx.fxml, javafx.graphics;
    opens com.ghostqueue.controller to javafx.fxml, javafx.base;
    opens com.ghostqueue.controller.patient to javafx.fxml, javafx.base;
    opens com.ghostqueue.controller.doctor to javafx.fxml, javafx.base;
    opens com.ghostqueue.model to javafx.base;
    opens com.ghostqueue.prediction to javafx.base, javafx.fxml;
    opens com.ghostqueue.component to javafx.fxml;
    opens com.ghostqueue.supabase to javafx.base;
    opens com.ghostqueue.mongodb to javafx.base;
    opens com.ghostqueue.mongodb.repository to javafx.base;

    exports com.ghostqueue;
    exports com.ghostqueue.controller;
    exports com.ghostqueue.controller.patient;
    exports com.ghostqueue.controller.doctor;
    exports com.ghostqueue.model;
    exports com.ghostqueue.service;
    exports com.ghostqueue.repository;
    exports com.ghostqueue.prediction;
    exports com.ghostqueue.scheduling;
    exports com.ghostqueue.component;
    exports com.ghostqueue.util;
    exports com.ghostqueue.supabase;
    exports com.ghostqueue.repository.supabase;
    exports com.ghostqueue.mongodb;
    exports com.ghostqueue.mongodb.repository;
    exports com.ghostqueue.web;
}
