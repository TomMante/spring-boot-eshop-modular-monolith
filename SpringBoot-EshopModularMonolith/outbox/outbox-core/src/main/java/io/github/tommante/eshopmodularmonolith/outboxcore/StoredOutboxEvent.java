package io.github.tommante.eshopmodularmonolith.outboxcore;

import java.time.Instant;
import java.util.UUID;

public record StoredOutboxEvent(UUID eventId, Instant occurredOn, String eventType, String payload) {
}
