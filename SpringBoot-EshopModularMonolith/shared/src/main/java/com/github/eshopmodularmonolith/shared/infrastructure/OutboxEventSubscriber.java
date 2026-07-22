package com.github.eshopmodularmonolith.shared.infrastructure;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Generic subscriber that persists in-memory {@link DomainEvent}s to an {@link OutboxEventStore}
 * within the currently active transaction (BEFORE_COMMIT phase), so the outbox write commits or
 * rolls back atomically with the module's own state change.
 *
 * <p>Intentionally not a {@code @Component}: each module must declare its own subscriber bean,
 * explicitly wiring it to the module-specific {@link OutboxEventStore} implementation.
 */
public class OutboxEventSubscriber {

    private final OutboxEventStore outbox;

    public OutboxEventSubscriber(OutboxEventStore outbox) {
        this.outbox = outbox;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DomainEvent event) {
        outbox.save(event);
    }
}
