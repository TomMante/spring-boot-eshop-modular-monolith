package io.github.tommante.eshopmodularmonolith.outboxjpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("""
        select e from OutboxEventEntity e
        where e.processedOn is null and e.deadLetteredOn is null and e.nextAttemptAt <= :now
        order by e.occurredOn""")
    List<OutboxEventEntity> findDue(@Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("update OutboxEventEntity e set e.processedOn = :processedOn where e.eventId = :eventId")
    int markAsProcessed(@Param("eventId") UUID eventId, @Param("processedOn") Instant processedOn);

    @Transactional
    @Modifying
    @Query("""
        update OutboxEventEntity e
        set e.failedAttempts = e.failedAttempts + 1, e.lastError = :error, e.nextAttemptAt = :nextAttemptAt
        where e.eventId = :eventId""")
    int recordFailedAttempt(@Param("eventId") UUID eventId, @Param("error") String error,
                            @Param("nextAttemptAt") Instant nextAttemptAt);

    @Transactional
    @Modifying
    @Query("""
        update OutboxEventEntity e
        set e.failedAttempts = e.failedAttempts + 1, e.lastError = :error, e.deadLetteredOn = :deadLetteredOn
        where e.eventId = :eventId""")
    int markAsDeadLettered(@Param("eventId") UUID eventId, @Param("error") String error,
                           @Param("deadLetteredOn") Instant deadLetteredOn);
}
