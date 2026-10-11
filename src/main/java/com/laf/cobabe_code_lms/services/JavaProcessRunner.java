
package com.laf.cobabe_code_lms.services;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.*;

@Service
public class JavaProcessRunner {

    private static final int MAX_OUTPUT_BYTES = 32 * 1024;
    private static final int MAX_INPUT_BYTES = 8 * 1024;

    private static final int COMPILE_TIMEOUT_SECONDS = 8;
    private static final int RUN_TIMEOUT_SECONDS = 3;

    public JavaExecutionService.ExecutionResult execute(
            String sourceCode,
            String programInput) {

        Path workDir = null;

        try {
            workDir = Files.createTempDirectory("cobabe-java-");

            Path sourceFile = workDir.resolve("Main.java");

            Files.writeString(
                    sourceFile,
                    sourceCode,
                    StandardCharsets.UTF_8
            );

            ProcessResult compilation = runProcess(
                    new ProcessBuilder(
                            "javac",
                            "--release", "21",
                            "-encoding", "UTF-8",
                            "-d", workDir.toString(),
                            sourceFile.toString()
                    ),
                    workDir,
                    "",
                    COMPILE_TIMEOUT_SECONDS
            );

            if (compilation.timedOut()) {
                return new JavaExecutionService.ExecutionResult(
                        false, "", "Compilation timed out."
                );
            }

            if (compilation.exitCode() != 0) {
                return new JavaExecutionService.ExecutionResult(
                        false, "", compilation.output()
                );
            }

            ProcessResult execution = runProcess(
                    new ProcessBuilder(
                            "java",
                            "-Xmx64m",
                            "-XX:ActiveProcessorCount=1",
                            "-cp", workDir.toString(),
                            "Main"
                    ),
                    workDir,
                    programInput == null ? "" : programInput,
                    RUN_TIMEOUT_SECONDS
            );

            if (execution.timedOut()) {
                return new JavaExecutionService.ExecutionResult(
                        false,
                        execution.output(),
                        "Program exceeded the 3-second time limit."
                );
            }

            return new JavaExecutionService.ExecutionResult(
                    execution.exitCode() == 0,
                    execution.output(),
                    execution.exitCode() == 0
                            ? ""
                            : "Program exited with code "
                            + execution.exitCode()
            );

        } catch (Exception e) {
            return new JavaExecutionService.ExecutionResult(
                    false, "", "Java execution failed."
            );

        } finally {
            if (workDir != null) {
                deleteDirectory(workDir);
            }
        }
    }

    private record ProcessResult(
            int exitCode,
            String output,
            boolean timedOut
    ) {}

    private ProcessResult runProcess(
            ProcessBuilder builder,
            Path workDir,
            String input,
            int timeoutSeconds
    ) throws Exception {

        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();

        ExecutorService ioExecutor =
                Executors.newVirtualThreadPerTaskExecutor();

        try {
            Future<String> outputFuture =
                    ioExecutor.submit(() ->
                            readLimited(process.getInputStream())
                    );

            Future<?> inputFuture = ioExecutor.submit(() -> {
                try (OutputStream stream =
                             process.getOutputStream()) {
                    byte[] bytes = input.getBytes(
                            StandardCharsets.UTF_8
                    );

                    if (bytes.length > MAX_INPUT_BYTES) {
                        throw new IOException("Input too large");
                    }

                    stream.write(bytes);
                }
                return null;
            });

            boolean finished = process.waitFor(
                    timeoutSeconds, TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);

                return new ProcessResult(
                        -1, "", true
                );
            }

            String output = outputFuture.get(
                    1, TimeUnit.SECONDS
            );

            // Observe any input-writing failure.
            if (inputFuture.isDone()) {
                try {
                    inputFuture.get();
                } catch (ExecutionException ignored) {
                    // The program may have closed stdin early.
                }
            }

            return new ProcessResult(
                    process.exitValue(),
                    output,
                    false
            );

        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }

            ioExecutor.shutdownNow();
        }
    }

    private String readLimited(InputStream stream)
            throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int count;

        while ((count = stream.read(buffer)) != -1) {
            int remaining =
                    MAX_OUTPUT_BYTES - output.size();

            if (remaining > 0) {
                output.write(
                        buffer, 0, Math.min(count, remaining)
                );
            }

            // Continue draining to avoid blocking the process.
        }

        String text = output.toString(
                StandardCharsets.UTF_8
        );

        if (output.size() >= MAX_OUTPUT_BYTES) {
            text += "\n[Output truncated]";
        }

        return text;
    }

    private void deleteDirectory(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
