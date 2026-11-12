package io.github.tommante.eshopmodularmonolith.outboxcore;

import java.util.*;

public class OutboxSchemaRegistry {

    private final Map<String, List<OutboxPayloadUpcaster>> upcastersByEventType = new HashMap<>();

    public void registerEvent(String eventType) {
        if (upcastersByEventType.containsKey(eventType))
            throw new IllegalArgumentException("Event type " + eventType + " is already registered; register each event type once, "
                    + "and add later versions with registerVersion(\"" + eventType + "\", nextVersion, upcaster)");
        upcastersByEventType.put(eventType, new ArrayList<>());
    }

    public void registerVersion(String eventType, int version, OutboxPayloadUpcaster upcaster) {
        Objects.requireNonNull(upcaster,
                () -> "Version " + version + " of event type " + eventType + " needs the upcaster that converts version "
                        + (version - 1) + " payloads to it, but the upcaster was null");
        var currentVersion = currentVersionOf(eventType);
        var nextVersion = currentVersion + 1;
        if (version != nextVersion)
            throw new IllegalArgumentException("Event type " + eventType + " is at version " + currentVersion
                    + ", so the next version to register is " + nextVersion + ", but was " + version
                    + "; register versions in order, each with the upcaster from the version before it");
        upcastersFor(eventType).add(upcaster);
    }

    public int currentVersionOf(String eventType) {
        return upcastersFor(eventType).size() + 1;
    }

    public List<OutboxPayloadUpcaster> upcastersFrom(String eventType, int storedVersion) {
        var upcasters = upcastersFor(eventType);
        var currentVersion = upcasters.size() + 1;
        if (storedVersion < 1 || storedVersion > currentVersion)
            throw new IllegalArgumentException("Stored " + eventType + " payload has schema version " + storedVersion
                    + ", but this application reads versions 1 to " + currentVersion + " of it; if a newer deployment wrote it, "
                    + "deploy a version that knows schema version " + storedVersion + " and requeue the event");
        return List.copyOf(upcasters.subList(storedVersion - 1, upcasters.size()));
    }

    private List<OutboxPayloadUpcaster> upcastersFor(String eventType) {
        if (!upcastersByEventType.containsKey(eventType))
            throw new IllegalArgumentException("No schema version registered for event type " + eventType
                    + "; call registerEvent(\"" + eventType + "\") in the module that owns the event before storing or reading it");
        return upcastersByEventType.get(eventType);
    }
}
