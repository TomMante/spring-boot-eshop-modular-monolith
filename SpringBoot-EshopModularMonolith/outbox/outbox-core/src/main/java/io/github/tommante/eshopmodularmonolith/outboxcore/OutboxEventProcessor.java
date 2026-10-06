package io.github.tommante.eshopmodularmonolith.outboxcore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventStore outboxEventStore;

    public OutboxEventProcessor(OutboxEventStore outboxEventStore) {
        this.outboxEventStore = outboxEventStore;
    }

    public void dispatchPendingEvents(OutboxEventPublisher publisher) {
        var events = outboxEventStore.fetchUnprocessedEvents();
        for (var event : events) {
            try {
                publisher.publish(event);
                outboxEventStore.markAsProcessed(event);
            } catch (Exception e) {
                logger.error("Failed to dispatch outbox event {} ({}); it remains unprocessed and will be retried.",
                        event.eventId(), event.eventType(), e);
            }
        }
    }
}
