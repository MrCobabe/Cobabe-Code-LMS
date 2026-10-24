
package com.laf.cobabe_code_lms.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class JavaRunnerConnection {

    private final HttpClient client;
    private final String runnerUrl;
    private final String runnerSecret;

    public JavaRunnerConnection(
            @Value("${cobabe.runner.url:http://127.0.0.1:8081}")
            String runnerUrl) {

        this.runnerUrl = runnerUrl;


        // Read from environment variable, not source code.
        this.runnerSecret =
                System.getenv("COBABE_RUNNER_SECRET");

        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public boolean isRunnerOnline() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(runnerUrl + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            return response.statusCode() == 200
                    && response.body().equals(
                    "Cobabe Java Runner is online"
            );

        } catch (Exception e) {
            return false;
        }
    }

    public int testAuthenticatedConnection() {
        if (runnerSecret == null || runnerSecret.isBlank()) {
            return 0;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(runnerUrl + "/auth-check"))
                    .timeout(Duration.ofSeconds(3))
                    .header("X-Runner-Secret", runnerSecret)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            return response.statusCode();

        } catch (Exception e) {
            return 0;
        }
    }

    public String runLocalTest() {

        if (runnerSecret == null || runnerSecret.isBlank()) {
            return "Runner secret is not configured.";
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(runnerUrl + "/run"))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Runner-Secret", runnerSecret)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            return "Runner connection failed: " + e.getMessage();
        }
    }

    public String testJsonRequest(String code, String input) {

        if (runnerSecret == null || runnerSecret.isBlank()) {
            return "{\"error\":\"Runner secret is not configured.\"}";
        }

        try {
            // Build JSON using Spring Boot's Jackson library.
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();

            String jsonBody = mapper.writeValueAsString(
                    java.util.Map.of(
                            "code", code == null ? "" : code,
                            "input", input == null ? "" : input
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(runnerUrl + "/run"))
                    .timeout(Duration.ofSeconds(60))
                    .header("X-Runner-Secret", runnerSecret)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            return "{\"error\":" +
                    quoteJson(e.getMessage()) + "}";
        }
    }

    public String testMultiFileJsonRequest(
            Map<String, String> files,
            String input) {

        if (runnerSecret == null || runnerSecret.isBlank()) {
            return "{\"success\":false,\"error\":\"Runner secret is not configured.\"}";
        }

        if (files == null || files.isEmpty()) {
            return "{\"success\":false,\"error\":\"No Java files provided.\"}";
        }

        try {
            // Build a JSON request containing all Java files.
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();

            String jsonBody = mapper.writeValueAsString(
                    Map.of(
                            "files", files,
                            "input", input == null ? "" : input
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(runnerUrl + "/run"))
                    .timeout(Duration.ofSeconds(60))
                    .header("X-Runner-Secret", runnerSecret)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            return "{\"success\":false,\"error\":" +
                    quoteJson(e.getMessage()) + "}";
        }
    }

    private String quoteJson(String value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(value == null ? "" : value);
        } catch (Exception e) {
            return "\"Connection error\"";
        }
    }

}
