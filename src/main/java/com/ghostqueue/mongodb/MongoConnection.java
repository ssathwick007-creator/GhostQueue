package com.ghostqueue.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MongoConnection {

    private static final Logger LOGGER = Logger.getLogger(MongoConnection.class.getName());
    private static MongoClient client;
    private static MongoDatabase database;

    private MongoConnection() {}

    public static synchronized MongoDatabase getDatabase() {
        if (database == null) {
            init();
        }
        return database;
    }

    public static synchronized MongoClient getClient() {
        if (client == null) {
            init();
        }
        return client;
    }

    public static synchronized boolean testConnection() {
        try {
            MongoDatabase db = getDatabase();
            Document ping = db.runCommand(new Document("ping", 1));
            return ping != null && (ping.get("ok") instanceof Number n && n.intValue() == 1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "MongoDB Atlas ping failed: " + e.getMessage(), e);
            return false;
        }
    }

    private static void init() {
        try {
            String uri = MongoConfig.getConnectionString();
            String masked = uri.replaceAll(":([^@]+)@", ":****@");
            LOGGER.info("Connecting to MongoDB Atlas at: " + masked);

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToSocketSettings(builder -> builder.connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(15, TimeUnit.SECONDS))
                    .build();

            client = MongoClients.create(settings);
            database = client.getDatabase(MongoConfig.getDatabaseName());

            Document ping = database.runCommand(new Document("ping", 1));
            LOGGER.info("MongoDB Atlas connected successfully! Ping result: " + ping.toJson());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize MongoDB Atlas client: " + e.getMessage(), e);
            throw new RuntimeException("Could not connect to MongoDB Atlas: " + e.getMessage(), e);
        }
    }

    public static synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
            database = null;
            LOGGER.info("MongoDB Atlas client connection closed.");
        }
    }
}
