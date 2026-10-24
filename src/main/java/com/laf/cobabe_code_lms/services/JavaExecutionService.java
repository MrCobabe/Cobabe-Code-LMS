
package com.laf.cobabe_code_lms.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
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

    public CompletableFuture<ExecutionResult> runJavaFiles(
            Map<String, String> files,
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

        if (files == null || files.isEmpty()
                || !files.containsKey("Main.java")) {
            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "Main.java is required."
                    )
            );
        }

        if (files.size() > 20) {
            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "A maximum of 20 Java files is allowed."
                    )
            );
        }

        long totalBytes = 0;

        for (Map.Entry<String, String> entry : files.entrySet()) {

            String filename = entry.getKey();
            String code = entry.getValue();

            if (filename == null
                    || filename.length() > 120
                    || !filename.matches(
                    "[A-Za-z_$][A-Za-z0-9_$]*\\.java")) {

                return CompletableFuture.completedFuture(
                        new ExecutionResult(
                                false,
                                "",
                                "Invalid Java filename."
                        )
                );
            }

            if (code == null) {
                return CompletableFuture.completedFuture(
                        new ExecutionResult(
                                false,
                                "",
                                "Java source code cannot be null."
                        )
                );
            }

            totalBytes += code.getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
            ).length;

            if (totalBytes > 64 * 1024) {
                return CompletableFuture.completedFuture(
                        new ExecutionResult(
                                false,
                                "",
                                "Combined Java source code exceeds 64 KB."
                        )
                );
            }
        }

        if (files.get("Main.java").isBlank()) {
            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "Main.java cannot be empty."
                    )
            );
        }

        if (programInput != null
                && programInput.getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        ).length > 8 * 1024) {

            return CompletableFuture.completedFuture(
                    new ExecutionResult(
                            false,
                            "",
                            "Program input exceeds 8 KB."
                    )
            );
        }

        Map<String, String> validatedFiles =
                Map.copyOf(files);

        String input =
                programInput == null ? "" : programInput;

        return runnerManager.submit(() -> {

            String response =
                    runnerConnection.testMultiFileJsonRequest(
                            validatedFiles,
                            input
                    );

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper();

                com.fasterxml.jackson.databind.JsonNode result =
                        mapper.readTree(response);

                return new ExecutionResult(
                        result.path("success").asBoolean(false),
                        result.path("output").asText(""),
                        result.path("error").asText("")
                );

            } catch (Exception e) {
                return new ExecutionResult(
                        false,
                        "",
                        "Unable to read the Java runner response."
                );
            }
        });
    }

}
