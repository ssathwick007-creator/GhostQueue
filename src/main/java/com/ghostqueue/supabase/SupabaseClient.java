package com.ghostqueue.supabase;

import com.ghostqueue.util.JsonUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupabaseClient {

    private static final Logger LOGGER = Logger.getLogger(SupabaseClient.class.getName());
    private static SupabaseClient instance;

    private final HttpClient httpClient;

    private SupabaseClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    public static class Response {
        private final int statusCode;
        private final String body;

        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body != null ? body : "";
        }

        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }

        public Map<String, Object> asObject() {
            return JsonUtil.parseObject(body);
        }

        public List<Map<String, Object>> asList() {
            return JsonUtil.parseArray(body);
        }
    }

    public Response get(String endpoint, Map<String, String> queryParams) {
        try {
            String url = buildUrl(endpoint, queryParams);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET();

            attachHeaders(reqBuilder);

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(resp.statusCode(), resp.body());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "HTTP GET failed for endpoint: " + endpoint, e);
            throw new RuntimeException("Supabase GET error: " + e.getMessage(), e);
        }
    }

    public Response post(String endpoint, Object body) {
        return post(endpoint, body, Collections.emptyMap());
    }

    public Response post(String endpoint, Object body, Map<String, String> headers) {
        try {
            String jsonBody = (body instanceof String s) ? s : (body instanceof Map<?, ?> m ? JsonUtil.toJson((Map<String, Object>) m) : "");
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

            attachHeaders(reqBuilder);
            reqBuilder.header("Prefer", "return=representation");
            if (headers != null) {
                headers.forEach(reqBuilder::header);
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(resp.statusCode(), resp.body());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "HTTP POST failed for endpoint: " + endpoint, e);
            throw new RuntimeException("Supabase POST error: " + e.getMessage(), e);
        }
    }

    public Response patch(String endpoint, Map<String, String> queryParams, Object body) {
        try {
            String url = buildUrl(endpoint, queryParams);
            String jsonBody = (body instanceof String s) ? s : (body instanceof Map<?, ?> m ? JsonUtil.toJson((Map<String, Object>) m) : "");

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

            attachHeaders(reqBuilder);
            reqBuilder.header("Prefer", "return=representation");

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(resp.statusCode(), resp.body());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "HTTP PATCH failed for endpoint: " + endpoint, e);
            throw new RuntimeException("Supabase PATCH error: " + e.getMessage(), e);
        }
    }

    public Response delete(String endpoint, Map<String, String> queryParams) {
        try {
            String url = buildUrl(endpoint, queryParams);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .DELETE();

            attachHeaders(reqBuilder);

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(resp.statusCode(), resp.body());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "HTTP DELETE failed for endpoint: " + endpoint, e);
            throw new RuntimeException("Supabase DELETE error: " + e.getMessage(), e);
        }
    }

    public Response rpc(String functionName, Map<String, Object> params) {
        String endpoint = SupabaseConfig.getUrl() + "/rest/v1/rpc/" + functionName;
        return post(endpoint, params);
    }

    private void attachHeaders(HttpRequest.Builder builder) {
        builder.header("apikey", SupabaseConfig.getAnonKey());
        builder.header("Content-Type", "application/json");

        String token = SessionManager.getInstance().getAccessToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        } else {
            builder.header("Authorization", "Bearer " + SupabaseConfig.getAnonKey());
        }
    }

    private String buildUrl(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) return base;
        StringBuilder sb = new StringBuilder(base);
        sb.append(base.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
