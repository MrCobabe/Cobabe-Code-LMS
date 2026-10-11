
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.services.JavaRunnerConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@RestController
public class RunnerStatusController {

    private final JavaRunnerConnection connection;

    public RunnerStatusController(
            JavaRunnerConnection connection) {
        this.connection = connection;
    }

    @GetMapping("/teacher/api/runner/status")
    public ResponseEntity<Map<String, Object>> getStatus() {

        boolean online = connection.isRunnerOnline();

        return ResponseEntity.ok(
                Map.of(
                        "online", online,
                        "status", online ? "ONLINE" : "OFFLINE"
                )
        );
    }

    @GetMapping("/teacher/api/runner/auth-test")
    public ResponseEntity<Map<String, Object>> testAuthentication() {

        int status = connection.testAuthenticatedConnection();

        return ResponseEntity.ok(
                Map.of(
                        "authenticated", status == 200,
                        "runnerResponse", status
                )
        );
    }

    @GetMapping("/teacher/api/runner/local-test")
    public ResponseEntity<String> runLocalTest() {
        return ResponseEntity.ok(connection.runLocalTest());
    }

    @PostMapping("/teacher/api/runner/json-test")
    public ResponseEntity<String> testJson(
            @RequestBody Map<String, String> request) {

        String code = request.getOrDefault("code", "");
        String input = request.getOrDefault("input", "");

        String response = connection.testJsonRequest(code, input);

        return ResponseEntity.ok()
                .contentType(
                        org.springframework.http.MediaType.APPLICATION_JSON
                )
                .body(response);
    }



}
