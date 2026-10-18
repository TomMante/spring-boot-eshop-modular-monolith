package io.github.tommante.eshopmodularmonolith.outboxcore;

import tools.jackson.databind.node.ObjectNode;

@FunctionalInterface
public interface OutboxPayloadUpcaster {
    ObjectNode upcast(ObjectNode payload);
}