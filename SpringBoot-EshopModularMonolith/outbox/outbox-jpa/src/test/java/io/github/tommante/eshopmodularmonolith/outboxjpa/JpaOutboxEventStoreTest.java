package io.github.tommante.eshopmodularmonolith.outboxjpa;

import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@ContextConfiguration(classes = JpaOutboxEventStoreTest.Config.class)
@TestPropertySource(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class JpaOutboxEventStoreTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {OutboxEventEntity.class})
    @EnableJpaRepositories(basePackageClasses = {JpaOutboxEventStore.class, OutboxEventJpaRepository.class})
    static class Config {
        @Bean
        JpaOutboxEventStore jpaOutboxEventStore(OutboxEventJpaRepository repository) {
            return new JpaOutboxEventStore(repository);
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine3.23");

    @Autowired
    private JpaOutboxEventStore jpaOutboxEventStore;
    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Test
    @DisplayName("Given domain event, when event is stored, then should be available as unprocessed event")
    void givenDomainEvent_whenEventIsStored_thenShouldBeAvailableAsUnprocessedEvent() {
        var row = new StoredOutboxEvent(UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.MILLIS), "AnyType", "{\"any\":\"payload\"}");
        jpaOutboxEventStore.save(row);
        assertThat(jpaOutboxEventStore.fetchPending()).containsExactly(row);
    }

    @Test
    @DisplayName("Given stored domain event, when marked as processed, then it is no longer unprocessed")
    void givenStoredDomainEvent_whenProcessEventIsCalled_thenDomainEventIsCorrectlyUpdated() {
        var row = new StoredOutboxEvent(UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.MILLIS), "AnyType", "{\"any\":\"payload\"}");
        jpaOutboxEventStore.save(row);
        assertThat(jpaOutboxEventStore.fetchPending()).containsExactly(row);
        jpaOutboxEventStore.markAsProcessed(row.eventId());
        assertThat(jpaOutboxEventStore.fetchPending()).isEmpty();
        assertThat(outboxEventJpaRepository.findById(row.eventId())).isPresent().get().extracting(OutboxEventEntity::processedOn).isNotNull();
    }

}