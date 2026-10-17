package io.github.tommante.eshopmodularmonolith.outboxjpa;

import io.github.tommante.eshopmodularmonolith.outboxcore.DueOutboxEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = JpaOutboxEventStoreTest.Config.class)
@TestPropertySource(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class JpaOutboxEventStoreTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = NOW.plus(Duration.ofHours(1));

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

    @BeforeEach
    void setUp() {
        outboxEventJpaRepository.deleteAll();
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine3.23");

    @Autowired
    private JpaOutboxEventStore outboxEventStore;
    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    private OutboxEventEntity stored(UUID eventId) {
        return outboxEventJpaRepository.findById(eventId).orElseThrow();
    }

    private static StoredOutboxEvent rowOccurredAt(Instant occurredOn) {
        return new StoredOutboxEvent(UUID.randomUUID(), occurredOn, "AnyType", "{\"any\":\"payload\"}");
    }

    @Test
    @DisplayName("Given a saved row, when fetching due rows, then it is due at its occurrence time and not before")
    void givenSavedRow_whenFetchingDueRows_thenItIsDueAtItsOccurrenceTimeAndNotBefore() {
        var row = rowOccurredAt(NOW);

        outboxEventStore.save(row);

        assertThat(outboxEventStore.fetchDue(NOW.minusMillis(1))).isEmpty();
        assertThat(outboxEventStore.fetchDue(NOW)).containsExactly(new DueOutboxEvent(row, 0));
    }

    @Test
    @DisplayName("Given a failed attempt, when fetching due rows, then the row is due again only at its next attempt time, with the count and error recorded")
    void givenFailedAttempt_whenFetchingDueRows_thenRowIsDueAgainOnlyAtNextAttemptTimeWithCountAndErrorRecorded() {
        var row = rowOccurredAt(NOW);
        outboxEventStore.save(row);

        outboxEventStore.recordFailedAttempt(row.eventId(), "boom", NOW.plusSeconds(5));

        assertThat(outboxEventStore.fetchDue(NOW.plusSeconds(5).minusMillis(1))).isEmpty();
        assertThat(outboxEventStore.fetchDue(NOW.plusSeconds(5))).containsExactly(new DueOutboxEvent(row, 1));
        assertThat(stored(row.eventId()).lastError()).isEqualTo("boom");
    }

    @Test
    @DisplayName("Given a processed row, when fetching due rows, then it is never fetched again and its processing time is stored")
    void givenProcessedRow_whenFetchingDueRows_thenItIsNeverFetchedAgainAndItsProcessingTimeIsStored() {
        var row = rowOccurredAt(NOW);
        outboxEventStore.save(row);

        outboxEventStore.markAsProcessed(row.eventId(), NOW);

        assertThat(outboxEventStore.fetchDue(LATER)).isEmpty();
        assertThat(stored(row.eventId()).processedOn()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Given a dead-lettered row, when fetching due rows, then it is never fetched again and the final attempt is stored")
    void givenDeadLetteredRow_whenFetchingDueRows_thenItIsNeverFetchedAgainAndTheFinalAttemptIsStored() {
        var row = rowOccurredAt(NOW);
        outboxEventStore.save(row);

        outboxEventStore.markAsDeadLettered(row.eventId(), "boom", NOW);

        assertThat(outboxEventStore.fetchDue(LATER)).isEmpty();
        var stored = stored(row.eventId());
        assertThat(stored.deadLetteredOn()).isEqualTo(NOW);
        assertThat(stored.lastError()).isEqualTo("boom");
        assertThat(stored.failedAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given rows saved out of occurrence order, when fetching due rows, then they come back in occurrence order")
    void givenRowsSavedOutOfOccurrenceOrder_whenFetchingDueRows_thenTheyComeBackInOccurrenceOrder() {
        var laterRow = rowOccurredAt(NOW.plusSeconds(1));
        var earlierRow = rowOccurredAt(NOW);
        outboxEventStore.save(laterRow);
        outboxEventStore.save(earlierRow);

        assertThat(outboxEventStore.fetchDue(LATER))
                .extracting(due -> due.event().eventId())
                .containsExactly(earlierRow.eventId(), laterRow.eventId());
    }

    @Test
    @DisplayName("Given an unknown event id, when recording a failed attempt, then it fails naming the event")
    void givenUnknownEventId_whenRecordingFailedAttempt_thenItFailsNamingTheEvent() {
        var unknownEventId = UUID.randomUUID();

        assertThatThrownBy(() -> outboxEventStore.recordFailedAttempt(unknownEventId, "boom", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown outbox event: " + unknownEventId);
    }

}