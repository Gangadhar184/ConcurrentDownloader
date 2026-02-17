package org.example.services;

import java.time.Duration;

public class RetryPolicy {
    private final int maxAttempts;
    private final Duration baseDelay;

    public RetryPolicy(int maxAttempts, Duration baseDelay) {
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
    }
    public <T> T execute(RetryableOperation<T> operation) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.run();
            } catch (Exception e) {
                last = e;
                if (attempt == maxAttempts) break;
                Thread.sleep(baseDelay.multipliedBy(attempt).toMillis());
            }
        }
        assert last != null;
        throw last;

    }

        @FunctionalInterface
        public interface RetryableOperation<T> {
            T run() throws Exception;
        }

}
