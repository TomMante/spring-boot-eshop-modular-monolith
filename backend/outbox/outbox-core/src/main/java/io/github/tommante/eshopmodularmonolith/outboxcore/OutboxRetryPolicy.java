package io.github.tommante.eshopmodularmonolith.outboxcore;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.Instant;

@ConfigurationProperties("eshop.outbox.retry")
public record OutboxRetryPolicy(
        @DefaultValue("8") int maxAttempts,
        @DefaultValue("5s") Duration initialDelay,
        @DefaultValue("5m") Duration maxDelay) {

    public OutboxRetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "eshop.outbox.retry.max-attempts must be at least 1, but was " + maxAttempts);
        }
        if (!initialDelay.isPositive()) {
            throw new IllegalArgumentException(
                    "eshop.outbox.retry.initial-delay must be positive, but was " + initialDelay);
        }
        if (initialDelay.compareTo(maxDelay) > 0) {
            throw new IllegalArgumentException("eshop.outbox.retry.initial-delay (" + initialDelay
                    + ") must not be longer than eshop.outbox.retry.max-delay (" + maxDelay + ")");
        }
    }


    public Instant nextAttemptAfter(int failedAttempts, Instant now) {
        if (failedAttempts < 1) {
            throw new IllegalArgumentException(
                    "failedAttempts counts the failure that just happened, so it must be at least 1, but was " + failedAttempts);
        }
        var delay = initialDelay;
        for (int attempt = 1; attempt < failedAttempts && delay.compareTo(maxDelay) < 0; attempt++) {
            delay = delay.multipliedBy(2);
        }
        return now.plus(delay.compareTo(maxDelay) < 0 ? delay : maxDelay);
    }


    public boolean isExhausted(int failedAttempts) {
        return failedAttempts >= maxAttempts;
    }
}
