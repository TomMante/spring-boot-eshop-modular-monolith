package io.github.tommante.eshopmodularmonolith.outboxcore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class OutboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventStore outboxEventStore;
    private final OutboxPayloadSerializer serializer;
    private final OutboxRetryPolicy retryPolicy;
    private final Clock clock;

    public OutboxEventProcessor(OutboxEventStore outboxEventStore, OutboxPayloadSerializer serializer, OutboxRetryPolicy retryPolicy, Clock clock) {
        this.outboxEventStore = outboxEventStore;
        this.serializer = serializer;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    public void dispatchPendingEvents(OutboxEventPublisher publisher) {
        var now = clock.instant();
        for (var dueEvent : outboxEventStore.fetchDue(now)) {
            var storedEvent = dueEvent.event();
            try {
                publisher.publish(serializer.read(storedEvent));
                outboxEventStore.markAsProcessed(storedEvent.eventId(), now);
            } catch (Exception e) {
                recordFailure(dueEvent, e, now);
            }
        }
    }

    private void recordFailure(DueOutboxEvent dueEvent, Exception failure, Instant now) {
        var storedEvent = dueEvent.event();
        var failedAttempts = dueEvent.failedAttempts() + 1;
        if (retryPolicy.isExhausted(failedAttempts)) {
            outboxEventStore.markAsDeadLettered(storedEvent.eventId(), failure.toString(), now);
            logger.error("Outbox event {} ({}) failed {} times and is dead-lettered; it will not be retried until requeued manually.",
                    storedEvent.eventId(), storedEvent.eventType(), failedAttempts, failure);
            return;
        }
        var nextAttemptAt = retryPolicy.nextAttemptAfter(failedAttempts, now);
        outboxEventStore.recordFailedAttempt(storedEvent.eventId(), failure.toString(), nextAttemptAt);
        logger.warn("Failed to dispatch outbox event {} ({}) on attempt {}; it will be retried at {}.",
                storedEvent.eventId(), storedEvent.eventType(), failedAttempts, nextAttemptAt, failure);
    }
}
