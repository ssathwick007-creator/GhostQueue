package com.ghostqueue.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for GhostQueue Embedded REST API Server and Web Application.
 */
public class GhostQueueWebServerTest {

    private static GhostQueueWebServer webServer;
    private static int testPort;
    private static HttpClient client;
    private static String baseUrl;

    @BeforeAll
    public static void setUp() throws Exception {
        // Find an open port
        try (ServerSocket socket = new ServerSocket(0)) {
            testPort = socket.getLocalPort();
        }

        baseUrl = "http://localhost:" + testPort;
        webServer = new GhostQueueWebServer(testPort);
        webServer.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    public static void tearDown() {
        if (webServer != null) {
            webServer.stop();
        }
    }

    @Test
    @DisplayName("Verify /api/health returns 200 OK and valid health JSON")
    public void testHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("HEALTHY"));
        assertTrue(response.body().contains("status"));
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    @DisplayName("Verify /api/analytics/kpis returns 200 OK and clinical KPIs")
    public void testKpisEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/analytics/kpis"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("totalAppointments"));
        assertTrue(response.body().contains("noShowRate"));
        assertTrue(response.body().contains("reclaimedHours"));
    }

    @Test
    @DisplayName("Verify /api/appointments returns 200 OK and appointment list")
    public void testAppointmentsEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/appointments"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("appointments"));
    }

    @Test
    @DisplayName("Verify / serves Single-Page Web Application index.html")
    public void testStaticRootServesHtml() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("GhostQueue"));
        assertTrue(response.body().contains("<!DOCTYPE html>"));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    }

    @Test
    @DisplayName("Verify /api/predict calculates real-time risk score")
    public void testPredictEndpoint() throws Exception {
        String jsonPayload = """
                {
                    "leadTimeDays": 14,
                    "patientAge": 45,
                    "previousNoShows": 2,
                    "previousAppointments": 5,
                    "chronicCondition": true,
                    "appointmentHour": 9
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/predict"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("noShowProbability"));
        assertTrue(response.body().contains("riskLevel"));
    }
}
