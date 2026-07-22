package com.github.eshopmodularmonolith.outboxjpa;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
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
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        DomainEventRegistry domainEventRegistry() {
            return new DomainEventRegistry();
        }

        @Bean
        OutboxEventMapper outboxEventMapper(DomainEventRegistry domainEventRegistry, ObjectMapper mapper){
            return new OutboxEventMapper(domainEventRegistry, mapper);
        }

        @Bean
        JpaOutboxEventStore jpaOutboxEventStore(OutboxEventJpaRepository repository, OutboxEventMapper mapper) {
            return new JpaOutboxEventStore(repository, mapper);
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaOutboxEventStore jpaOutboxEventStore;
    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;
    @Autowired
    private DomainEventRegistry domainEventRegistry;

    @BeforeEach
    void setUp() {
        domainEventRegistry.registerDomainEvent(TestDomainEvent.class.getSimpleName(), TestDomainEvent.class);
    }

    @Test
    @DisplayName("Given domain event, when event is stored, then should be available as unprocessed event")
    void givenDomainEvent_whenEventIsStored_thenShouldBeAvailableAsUnprocessedEvent() {
        var event = new TestDomainEvent(UUID.randomUUID(), Instant.now(), List.of("test", "me", "now"));
        jpaOutboxEventStore.save(event);
        assertThat(jpaOutboxEventStore.fetchUnprocessedEvents()).containsExactly(event);
    }

    @Test
    @DisplayName("Given domain event, when event is stored, then should be available as unprocessed event")
    void givenStoredDomainEvent_whenProcessEventIsCalled_thenDomainEventIsCorrectlyUpdated() {
        var event = new TestDomainEvent(UUID.randomUUID(), Instant.now(), List.of("test", "me", "now"));
        jpaOutboxEventStore.save(event);
        assertThat(jpaOutboxEventStore.fetchUnprocessedEvents()).containsExactly(event);
        jpaOutboxEventStore.markAsProcessed(event);
        assertThat(jpaOutboxEventStore.fetchUnprocessedEvents()).isEmpty();
        assertThat(outboxEventJpaRepository.findById(event.eventId())).isPresent().get().extracting(OutboxEventEntity::processedOn).isNotNull();
    }


    record TestDomainEvent(UUID eventId, Instant occurredOn, List<String> information) implements DomainEvent {

        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
    }


}