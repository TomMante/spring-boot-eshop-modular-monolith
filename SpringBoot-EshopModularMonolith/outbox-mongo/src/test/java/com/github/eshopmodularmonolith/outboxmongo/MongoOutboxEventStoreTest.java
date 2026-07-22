package com.github.eshopmodularmonolith.outboxmongo;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest
@ContextConfiguration(classes = MongoOutboxEventStoreTest.Config.class)
@TestPropertySource(properties = {
        "spring.mongodb.representation.uuid=java_legacy",
        "spring.data.mongodb.representation.big-decimal=decimal128"
})
class MongoOutboxEventStoreTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        DomainEventRegistry domainEventRegistry() {
            return new DomainEventRegistry();
        }

        @Bean
        OutboxDocumentMapper outboxDocumentMapper(DomainEventRegistry domainEventRegistry, ObjectMapper mapper) {
            return new OutboxDocumentMapper(domainEventRegistry, mapper);
        }

        @Bean
        MongoOutboxEventStore mongoOutboxEventStore(MongoTemplate mongoTemplate, OutboxDocumentMapper mapper) {
            return new MongoOutboxEventStore(mongoTemplate, mapper);
        }
    }

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDb = new MongoDBContainer("mongo:7.0.6");

    @Autowired
    private MongoOutboxEventStore mongoOutboxEventStore;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private DomainEventRegistry domainEventRegistry;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
        domainEventRegistry.registerDomainEvent(TestDomainEvent.class.getSimpleName(), TestDomainEvent.class);
    }

    @Test
    @DisplayName("Given domain event, when event is stored, then should be available as unprocessed event")
    void givenDomainEvent_whenEventIsStored_thenShouldBeAvailableAsUnprocessedEvent() {
        var event = new TestDomainEvent(UUID.randomUUID(), Instant.now(), List.of("test", "me", "now"));
        mongoOutboxEventStore.save(event);
        assertThat(mongoOutboxEventStore.fetchUnprocessedEvents()).containsExactly(event);
    }

    @Test
    @DisplayName("Given stored domain event, when marked as processed, then it is no longer unprocessed")
    void givenStoredDomainEvent_whenProcessEventIsCalled_thenDomainEventIsCorrectlyUpdated() {
        var event = new TestDomainEvent(UUID.randomUUID(), Instant.now(), List.of("test", "me", "now"));
        mongoOutboxEventStore.save(event);
        assertThat(mongoOutboxEventStore.fetchUnprocessedEvents()).containsExactly(event);
        mongoOutboxEventStore.markAsProcessed(event);
        assertThat(mongoOutboxEventStore.fetchUnprocessedEvents()).isEmpty();
        assertThat(mongoTemplate.findById(event.eventId(), OutboxEventDocument.class))
                .isNotNull()
                .extracting(OutboxEventDocument::processedOn)
                .isNotNull();
    }

    record TestDomainEvent(UUID eventId, Instant occurredOn, List<String> information) implements DomainEvent {

        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
    }
}
