package com.ghostqueue;

import com.ghostqueue.mongodb.MongoConnection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoConnectionTest {

    @Test
    void testAtlasConnection() {
        boolean connected = MongoConnection.testConnection();
        assertTrue(connected, "Should successfully ping MongoDB Atlas Cluster0");

        MongoDatabase db = MongoConnection.getDatabase();
        assertNotNull(db);
        System.out.println(">>> MongoDB Atlas Connected! Database: " + db.getName());
    }
}
