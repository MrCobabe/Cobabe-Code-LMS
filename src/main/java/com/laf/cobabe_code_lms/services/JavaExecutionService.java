
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

    public JavaExecutionService(
            JavaRunnerManager runnerManager,
            @Value("${cobabe.runner.enabled:false}")
            boolean runnerEnabled) {

        this.runnerManager = runnerManager;
        this.runnerEnabled = runnerEnabled;
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
}
