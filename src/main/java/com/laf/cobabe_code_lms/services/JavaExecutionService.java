
package com.laf.cobabe_code_lms.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class JavaExecutionService {

    private static final int MAX_CODE_LENGTH = 64 * 1024;
    private static final int MAX_INPUT_LENGTH = 8 * 1024;

    private final JavaRunnerManager runnerManager;
    private final boolean runnerEnabled;
    private final JavaRunnerConnection runnerConnection;

    public JavaExecutionService(
            JavaRunnerManager runnerManager,
            @Value("${cobabe.runner.enabled:false}")
            boolean runnerEnabled, JavaRunnerConnection runnerConnection) {

        this.runnerManager = runnerManager;
        this.runnerEnabled = runnerEnabled;
        this.runnerConnection = runnerConnection;
    }

    public record ExecutionResult(
            boolean success,
            String output,
            String error
    ) {}

    public CompletableFuture<ExecutionResult> runJava(
            String sourceCode,
            String programInput) {

        if (!runnerEnabled) {
            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "Java execution is not enabled yet."
                    )
            );
        }

        if (sourceCode == null || sourceCode.isBlank()) {
            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "Please enter some Java code."
                    )
            );
        }

        if (sourceCode.length() > MAX_CODE_LENGTH) {
            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "Java source code exceeds the size limit."
                    )
            );
        }

        if (programInput != null
                && programInput.length() > MAX_INPUT_LENGTH) {
            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "Program input exceeds the size limit."
                    )
            );
        }

        return runnerManager.submit(() ->
                new ExecutionResult(
                        false,
                        "",
                        "External Java runner is not configured yet."
                )
        );
    }

    public CompletableFuture<ExecutionResult> testRunnerConnection(
            String sourceCode,
            String programInput
    ) {
        return runnerManager.submit(() -> {

            String response = runnerConnection.testJsonRequest(
                    sourceCode,
                    programInput
            );

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper();

                com.fasterxml.jackson.databind.JsonNode result =
                        mapper.readTree(response);

                boolean success =
                        result.path("success").asBoolean(false);

                String output =
                        result.path("output").asText("");

                String error =
                        result.path("error").asText("");

                return new ExecutionResult(
                        success,
                        output,
                        error
                );

            } catch (Exception e) {
                return new ExecutionResult(
                        false,
                        "",
                        "Runner communication error: " + response
                );
            }
        });
    }

}
