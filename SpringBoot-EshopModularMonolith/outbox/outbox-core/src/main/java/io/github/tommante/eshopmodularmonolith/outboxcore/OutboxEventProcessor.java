package io.github.tommante.eshopmodularmonolith.outboxcore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventStore outboxEventStore;
    private final OutboxPayloadSerializer serializer;

    public OutboxEventProcessor(OutboxEventStore outboxEventStore, OutboxPayloadSerializer serializer) {
        this.outboxEventStore = outboxEventStore;
        this.serializer = serializer;
    }

    public void dispatchPendingEvents(OutboxEventPublisher publisher) {
        for (var storedEvent : outboxEventStore.fetchPending()) {
            try {
                publisher.publish(serializer.read(storedEvent));
                outboxEventStore.markAsProcessed(storedEvent.eventId());
            } catch (Exception e) {
                logger.error("Failed to dispatch outbox event {} ({}); it remains unprocessed and will be retried.",
                        storedEvent.eventId(), storedEvent.eventType(), e);
            }
        }
    }
}
