package io.github.tommante.eshopmodularmonolith.outboxmongo;

import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class MongoOutboxEventStore implements OutboxEventStore {

    private final MongoTemplate mongoTemplate;
    private final OutboxDocumentMapper eventMapper;

    public MongoOutboxEventStore(MongoTemplate mongoTemplate, OutboxDocumentMapper eventMapper) {
        this.mongoTemplate = mongoTemplate;
        this.eventMapper = eventMapper;
    }

    @Override
    public void save(DomainEvent event) {
        mongoTemplate.save(eventMapper.fromDomainEvent(event));
    }

    @Override
    public List<DomainEvent> fetchUnprocessedEvents() {
        var query = new Query(Criteria.where("processedOn").is(null));
        return mongoTemplate.find(query, OutboxEventDocument.class).stream()
                .map(eventMapper::toDomainEvent)
                .toList();
    }

    @Override
    public void markAsProcessed(DomainEvent event) {
        var document = mongoTemplate.findById(event.eventId(), OutboxEventDocument.class);
        if (document == null) {
            throw new IllegalStateException("Unknown outbox event: " + event.eventId());
        }
        document.process();
        mongoTemplate.save(document);
    }
}
