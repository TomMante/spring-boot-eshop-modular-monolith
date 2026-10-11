package io.github.tommante.eshopmodularmonolith.outboxcore;

import java.util.List;
import java.util.UUID;

public interface OutboxEventStore {

    void save(StoredOutboxEvent event);
    List<StoredOutboxEvent> fetchPending();
    void markAsProcessed(UUID eventId);
}
