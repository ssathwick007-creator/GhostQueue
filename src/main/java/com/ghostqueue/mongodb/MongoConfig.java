package com.ghostqueue.mongodb;

public final class MongoConfig {

    private static final String DEFAULT_URI = "mongodb+srv://192525127simats_db_user:5dztdeq1lYHcqMrR@cluster0.d5szoha.mongodb.net/ghostqueue_db?retryWrites=true&w=majority";
    private static final String DEFAULT_DATABASE = "ghostqueue_db";

    private MongoConfig() {}

    public static String getConnectionString() {
        String env = System.getenv("MONGODB_URI");
        if (env != null && !env.isBlank()) return sanitize(env);
        String prop = System.getProperty("mongodb.uri");
        if (prop != null && !prop.isBlank()) return sanitize(prop);
        return DEFAULT_URI;
    }

    public static String getDatabaseName() {
        String env = System.getenv("MONGODB_DATABASE");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("mongodb.database");
        if (prop != null && !prop.isBlank()) return prop.trim();
        return DEFAULT_DATABASE;
    }

    private static String sanitize(String uri) {
        String s = uri.trim();
        if (s.contains("<") && s.contains(">")) {
            s = s.replace("<", "").replace(">", "");
        }
        if (!s.contains("/") || s.endsWith(".net/")) {
            if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
            s = s + "/" + DEFAULT_DATABASE + "?retryWrites=true&w=majority";
        }
        return s;
    }
}
