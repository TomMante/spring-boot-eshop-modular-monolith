package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.PaymentDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.CheckedOutItemDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.CheckedOutItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventProcessor;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxRetryPolicy;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.MessageBus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class BasketOutboxDispatcher {

    private final MessageBus messageBus;
    private final OutboxEventProcessor outboxEventProcessor;

    public BasketOutboxDispatcher(@Qualifier("basketOutboxEventStore") OutboxEventStore outboxEventStore,
                                  OutboxPayloadSerializer serializer,
                                  OutboxRetryPolicy retryPolicy,
                                  Clock clock,
                                  MessageBus messageBus) {
        this.messageBus = messageBus;
        this.outboxEventProcessor = new OutboxEventProcessor(outboxEventStore, serializer, retryPolicy, clock);
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        outboxEventProcessor.dispatchPendingEvents(this::publish);
    }

    private void publish(DomainEvent event) {
        if (event instanceof BasketCheckedOut e) {
            messageBus.publish(toIntegrationEvent(e));
        }
    }

    private BasketCheckoutIntegrationEvent toIntegrationEvent(BasketCheckedOut event) {
        var items = event.items().stream().map(BasketOutboxDispatcher::toDto).toList();
        return new BasketCheckoutIntegrationEvent(event.eventId(), event.userName(), items, event.totalPrice(), toDto(event.billingAddress()), toDto(event.payment()), toDto(event.shippingAddress()));
    }

    private static CheckedOutItemDTO toDto(CheckedOutItem item) {
        return new CheckedOutItemDTO(item.id(), item.productId(), item.quantity(), item.price(), item.productName());
    }

    private static AddressDTO toDto(Address address) {
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    private static PaymentDTO toDto(Payment payment) {
        return new PaymentDTO(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv());
    }
}
