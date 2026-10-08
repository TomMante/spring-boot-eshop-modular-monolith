package io.github.tommante.eshopmodularmonolith.outboxmongo;

import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest
@ContextConfiguration(classes = MongoOutboxEventStoreTest.Config.class)
@TestPropertySource(properties = {
        "spring.mongodb.representation.uuid=standard",
        "spring.data.mongodb.representation.big-decimal=decimal128"
})
class MongoOutboxEventStoreTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        MongoOutboxEventStore mongoOutboxEventStore(MongoTemplate mongoTemplate) {
            return new MongoOutboxEventStore(mongoTemplate);
        }
    }

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDb = new MongoDBContainer("mongo:8.2.5-noble");

    @Autowired
    private MongoOutboxEventStore mongoOutboxEventStore;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
    }

    @Test
    @DisplayName("Given domain event, when event is stored, then should be available as unprocessed event")
    void givenDomainEvent_whenEventIsStored_thenShouldBeAvailableAsUnprocessedEvent() {
        var row = new StoredOutboxEvent(UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.MILLIS), "AnyType", "{\"any\":\"payload\"}");
        mongoOutboxEventStore.save(row);
        assertThat(mongoOutboxEventStore.fetchPending()).containsExactly(row);
    }

    @Test
    @DisplayName("Given stored domain event, when marked as processed, then it is no longer unprocessed")
    void givenStoredDomainEvent_whenProcessEventIsCalled_thenDomainEventIsCorrectlyUpdated() {
        var row = new StoredOutboxEvent(UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.MILLIS), "AnyType", "{\"any\":\"payload\"}");
        mongoOutboxEventStore.save(row);
        assertThat(mongoOutboxEventStore.fetchPending()).containsExactly(row);
        mongoOutboxEventStore.markAsProcessed(row.eventId());
        assertThat(mongoOutboxEventStore.fetchPending()).isEmpty();
        assertThat(mongoTemplate.findById(row.eventId(), OutboxEventDocument.class))
                .isNotNull()
                .extracting(OutboxEventDocument::processedOn)
                .isNotNull();
    }
}
