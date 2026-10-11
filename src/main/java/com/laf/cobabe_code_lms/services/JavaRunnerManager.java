
package com.laf.cobabe_code_lms.services;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JavaRunnerManager {

    // Maximum number of simultaneous student executions.
    private static final int MAX_CONCURRENT_RUNS = 2;

    // Maximum number of requests waiting to execute.
    private static final int MAX_QUEUED_RUNS = 30;

    private final AtomicInteger activeRuns = new AtomicInteger(0);

    private final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    MAX_CONCURRENT_RUNS,
                    MAX_CONCURRENT_RUNS,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(MAX_QUEUED_RUNS),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    /**
     * Submits a task to the execution queue.
     *
     * IMPORTANT:
     * This manages concurrency only. It does not
     * securely isolate student Java code.
     */
    public <T> CompletableFuture<T> submit(
            Callable<T> task) {

        CompletableFuture<T> future =
                new CompletableFuture<>();

        try {
            executor.execute(() -> {
                activeRuns.incrementAndGet();

                try {
                    future.complete(task.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    activeRuns.decrementAndGet();
                }
            });

        } catch (RejectedExecutionException e) {
            future.completeExceptionally(
                    new RejectedExecutionException(
                            "Java runner is busy. Please try again."
                    )
            );
        }

        return future;
    }

    public int getActiveRuns() {
        return activeRuns.get();
    }

    public int getQueuedRuns() {
        return executor.getQueue().size();
    }

    public int getMaxConcurrentRuns() {
        return MAX_CONCURRENT_RUNS;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
