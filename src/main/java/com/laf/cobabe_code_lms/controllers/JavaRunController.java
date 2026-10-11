
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.services.JavaExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;

@RestController
@RequestMapping("/api/student/java")
public class JavaRunController {

    private final JavaExecutionService executionService;

    public JavaRunController(
            JavaExecutionService executionService) {
        this.executionService = executionService;
    }

    public record RunRequest(
            String code,
            String input
    ) {}

    @PostMapping("/run")
    public CompletableFuture<ResponseEntity<
            JavaExecutionService.ExecutionResult>> run(
            @RequestBody RunRequest request,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(401).build()
            );
        }

        return executionService
                .runJava(request.code(), request.input())
                .handle((result, exception) -> {

                    if (exception != null) {
                        Throwable cause = exception;

                        while (cause instanceof
                                CompletionException
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
