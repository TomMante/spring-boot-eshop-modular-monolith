package io.github.tommante.eshopmodularmonolith.outboxmongo;

import io.github.tommante.eshopmodularmonolith.outboxcore.DueOutboxEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MongoOutboxEventStore implements OutboxEventStore {

    private final MongoTemplate mongoTemplate;

    public MongoOutboxEventStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(StoredOutboxEvent event) {
        mongoTemplate.save(new OutboxEventDocument(event));
    }

    @Override
    public List<DueOutboxEvent> fetchDue(Instant now) {
        // is(null) also matches documents where the field was never written
        var query = new Query(
                Criteria.where("processedOn").is(null)
                        .and("deadLetteredOn").is(null)
                        .and("nextAttemptAt").lte(now)
        ).with(Sort.by("occurredOn"));
        return mongoTemplate.find(query, OutboxEventDocument.class).stream()
                .map(OutboxEventDocument::toDue)
                .toList();
    }

    @Override
    public void markAsProcessed(UUID eventId, Instant processedOn) {
        update(eventId, new Update().set("processedOn", processedOn));
    }

    @Override
    public void recordFailedAttempt(UUID eventId, String error, Instant nextAttemptAt) {
        update(eventId, new Update()
                .inc("failedAttempts", 1)
                .set("lastError", error)
                .set("nextAttemptAt", nextAttemptAt));
    }

    @Override
    public void markAsDeadLettered(UUID eventId, String error, Instant deadLetteredOn) {
        update(eventId, new Update()
                .inc("failedAttempts", 1)
                .set("lastError", error)
                .set("deadLetteredOn", deadLetteredOn));
    }

    // Partial update: only the named fields change, and $inc is atomic, so concurrent writers don't overwrite each other
    private void update(UUID eventId, Update update) {
        var result = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(eventId)), update, OutboxEventDocument.class);
        // matched, not modified: setting a field to the value it already has modifies nothing
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Unknown outbox event: " + eventId);
        }
    }
}
