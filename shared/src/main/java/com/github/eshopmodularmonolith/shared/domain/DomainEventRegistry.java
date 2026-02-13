package com.github.eshopmodularmonolith.shared.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DomainEventRegistry {

    private final Map<String, Class<? extends DomainEvent>> mapping = new HashMap<>();

    public void registerDomainEvent(String key, Class<? extends DomainEvent> clazz){
        mapping.put(key, clazz);
    }

    public Class<? extends DomainEvent> classFor(String type){
        return Optional.ofNullable(mapping.get(type)).orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + type));
    }
}
