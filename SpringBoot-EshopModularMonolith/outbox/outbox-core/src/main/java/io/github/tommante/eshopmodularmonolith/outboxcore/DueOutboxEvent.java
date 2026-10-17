package io.github.tommante.eshopmodularmonolith.outboxcore;

public record DueOutboxEvent(StoredOutboxEvent event, int failedAttempts) {}
