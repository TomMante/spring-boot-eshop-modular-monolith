package io.github.tommante.eshopmodularmonolith.outboxmongo;

import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;

public class MongoOutboxEventStore implements OutboxEventStore {

    private final MongoTemplate mongoTemplate;

    public MongoOutboxEventStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(StoredOutboxEvent event) {
        mongoTemplate.save(new OutboxEventDocument(event.eventId(), event.occurredOn(), event.eventType(), event.payload()));
    }

    @Override
    public List<StoredOutboxEvent> fetchPending() {
        var query = new Query(Criteria.where("processedOn").is(null));
        return mongoTemplate.find(query, OutboxEventDocument.class).stream()
                .map(OutboxEventDocument::toStored)
                .toList();
    }

    @Override
    public void markAsProcessed(UUID eventId) {
        var document = mongoTemplate.findById(eventId, OutboxEventDocument.class);
        if (document == null) {
            throw new IllegalStateException("Unknown outbox event: " + eventId);
        }
        document.process();
        mongoTemplate.save(document);
    }
}
