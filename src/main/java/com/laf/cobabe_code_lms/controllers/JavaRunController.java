
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.services.JavaExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;

@RestController
@RequestMapping({
        "/api/student/java",
        "/teacher/api/java"
})
public class JavaRunController {

    private final JavaExecutionService executionService;

    public JavaRunController(
            JavaExecutionService executionService) {

        this.executionService = executionService;
    }

    // Supports both request formats:
    //
    // Single-file:
    // {
    //   "code": "...",
    //   "input": ""
    // }
    //
    // Multi-file:
    // {
    //   "files": {
    //     "Main.java": "...",
    //     "Player.java": "..."
    //   },
    //   "input": ""
    // }

    public record RunRequest(
            String code,
            String input,
            Map<String, String> files
    ) {}

    @PostMapping("/run")
    public CompletableFuture<ResponseEntity<
            JavaExecutionService.ExecutionResult>> run(
            @RequestBody RunRequest request,
            Authentication authentication) {

        // ==========================================
        // AUTHENTICATION
        // ==========================================

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return CompletableFuture.completedFuture(
                    ResponseEntity.status(401).build()
            );
        }

        if (request == null) {

            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build()
            );
        }

        // ==========================================
        // CHOOSE EXECUTION MODE
        // ==========================================

        CompletableFuture<
                JavaExecutionService.ExecutionResult> execution;

        if (request.files() != null) {

            // Multi-file execution.
            //
            // All Java files are validated and
            // sent to the external Docker runner.

            execution = executionService.runJavaFiles(
                    request.files(),
                    request.input()
            );

        } else {

            // Legacy single-file execution.
            //
            // Preserve the existing behavior for
            // older Sandbox and assignment pages.

            execution = executionService.testRunnerConnection(
                    request.code(),
                    request.input()
            );
        }

        // ==========================================
        // HANDLE EXECUTION RESULT
        // ==========================================

        return execution.handle((result, exception) -> {

            if (exception != null) {

                Throwable cause = exception;

                while (cause instanceof CompletionException
                        && cause.getCause() != null) {

                    cause = cause.getCause();
                }

                if (cause instanceof
                        RejectedExecutionException) {

                    return ResponseEntity.status(429)
                            .body(
                                    new JavaExecutionService.ExecutionResult(
                                            false,
                                            "",
                                            "Runner busy. Try again shortly."
                                    )
                            );
                }

                return ResponseEntity.status(500)
                        .body(
                                new JavaExecutionService.ExecutionResult(
                                        false,
                                        "",
                                        "Java runner encountered an error."
                                )
                        );
            }

            return ResponseEntity.ok(result);
        });
    }
}
