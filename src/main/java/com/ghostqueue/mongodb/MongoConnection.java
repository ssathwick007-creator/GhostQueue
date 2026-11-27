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

/**
 * Enterprise MongoDB Atlas Connection Manager for GhostQueue.
 * Handles cluster connections to MongoDB Atlas with low-latency failover,
 * connection pooling, and resilient zero-downtime health inspection.
 */
public final class MongoConnection {

    private static final Logger LOGGER = Logger.getLogger(MongoConnection.class.getName());
    private static MongoClient client;
    private static MongoDatabase database;
    private static volatile Boolean atlasAvailable = null;
    private static long lastCheckTimestamp = 0;
    private static final long CHECK_COOLDOWN_MS = 20000;

    private MongoConnection() {}

    /**
     * Checks if MongoDB Atlas cluster is currently reachable and responding to ping.
     */
    public static synchronized boolean isAtlasAvailable() {
        long now = System.currentTimeMillis();
        if (atlasAvailable != null && (now - lastCheckTimestamp < CHECK_COOLDOWN_MS)) {
            return atlasAvailable;
        }

        try {
            MongoDatabase db = getDatabase();
            if (db != null) {
                Document ping = db.runCommand(new Document("ping", 1));
                atlasAvailable = (ping != null && (ping.get("ok") instanceof Number n && n.intValue() == 1));
                lastCheckTimestamp = now;
                return atlasAvailable;
            }
        } catch (Exception e) {
            atlasAvailable = false;
            lastCheckTimestamp = now;
        }
        return false;
    }

    public static synchronized MongoDatabase getDatabase() {
        if (database == null) {
            try {
                init();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "MongoDB Atlas not yet reachable: " + e.getMessage());
                return null;
            }
        }
        return database;
    }

    public static synchronized MongoClient getClient() {
        if (client == null) {
            try {
                init();
            } catch (Exception e) {
                return null;
            }
        }
        return client;
    }

    public static synchronized boolean testConnection() {
        return isAtlasAvailable();
    }

    private static void init() {
        try {
            String uri = MongoConfig.getConnectionString();
            String masked = uri.replaceAll(":([^@]+)@", ":****@");
            LOGGER.info("Connecting to MongoDB Atlas cluster at: " + masked);

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToSocketSettings(builder -> builder.connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(3, TimeUnit.SECONDS))
                    .build();

            client = MongoClients.create(settings);
            database = client.getDatabase(MongoConfig.getDatabaseName());

            Document ping = database.runCommand(new Document("ping", 1));
            atlasAvailable = true;
            lastCheckTimestamp = System.currentTimeMillis();
            LOGGER.info("MongoDB Atlas connected successfully! Ping: " + ping.toJson());
        } catch (Exception e) {
            atlasAvailable = false;
            lastCheckTimestamp = System.currentTimeMillis();
            LOGGER.warning("MongoDB Atlas initialization notice: " + e.getMessage());
            // Do not throw fatal exception so the platform operates in resilient mode
        }
    }

    public static synchronized void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {}
            client = null;
            database = null;
            atlasAvailable = null;
            LOGGER.info("MongoDB Atlas client connection closed.");
        }
    }
}
