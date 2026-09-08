package com.github.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.PaymentDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import com.github.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import com.github.eshopmodularmonolith.basket.basket.domain.model.Item;
import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;
import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.infrastructure.MessageBus;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventProcessor;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BasketOutboxDispatcher {

    private final MessageBus messageBus;
    private final OutboxEventProcessor outboxEventProcessor;

    public BasketOutboxDispatcher(@Qualifier("basketOutboxEventStore") OutboxEventStore outboxEventStore, MessageBus messageBus) {
        this.messageBus = messageBus;
        this.outboxEventProcessor = new OutboxEventProcessor(outboxEventStore);
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

    private static ItemDTO toDto(Item item) {
        return new ItemDTO(item.id(), item.productId(), item.quantity(), item.priceWhenAdded(), item.productName());
    }

    private static AddressDTO toDto(Address address) {
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    private static PaymentDTO toDto(Payment payment) {
        return new PaymentDTO(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }
}
