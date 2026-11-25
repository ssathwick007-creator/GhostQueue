package com.ghostqueue;

import com.ghostqueue.mongodb.MongoConnection;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoConnectionTest {

    @Test
    void testAtlasConnection() {
        boolean connected = MongoConnection.testConnection();
        if (!connected) {
            System.out.println(">>> Note: MongoDB Atlas IP whitelist (0.0.0.0/0) not yet confirmed. Skipping Atlas ping test.");
        }
        Assumptions.assumeTrue(connected, "MongoDB Atlas IP whitelist (0.0.0.0/0) must be enabled in Atlas console to test live connection.");

        MongoDatabase db = MongoConnection.getDatabase();
        assertNotNull(db);
        System.out.println(">>> MongoDB Atlas Connected! Database: " + db.getName());
    }
}
