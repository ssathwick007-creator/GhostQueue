package com.ghostqueue.supabase;

public final class SupabaseConfig {

    private static final String DEFAULT_URL = "https://zvlmtheyizmpzcktfsva.supabase.co";
    private static final String DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp2bG10aGV5aXptcHpja3Rmc3ZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY1MDQ3NzksImV4cCI6MjA5MjA4MDc3OX0.X4o3jm0lAwi4GIMde7WCv8PjKxeXHMhlZF3oRX47Fk0";

    private SupabaseConfig() {}

    public static String getUrl() {
        String env = System.getenv("SUPABASE_URL");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("supabase.url");
        if (prop != null && !prop.isBlank()) return prop.trim();
        return DEFAULT_URL;
    }

    public static String getAnonKey() {
        String env = System.getenv("SUPABASE_ANON_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("supabase.anon_key");
        if (prop != null && !prop.isBlank()) return prop.trim();
        return DEFAULT_ANON_KEY;
    }

    public static String getAuthEndpoint(String path) {
        String base = getUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!path.startsWith("/")) path = "/" + path;
        return base + "/auth/v1" + path;
    }

    public static String getRestEndpoint(String tableOrView) {
        String base = getUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!tableOrView.startsWith("/")) tableOrView = "/" + tableOrView;
        return base + "/rest/v1" + tableOrView;
    }
}
