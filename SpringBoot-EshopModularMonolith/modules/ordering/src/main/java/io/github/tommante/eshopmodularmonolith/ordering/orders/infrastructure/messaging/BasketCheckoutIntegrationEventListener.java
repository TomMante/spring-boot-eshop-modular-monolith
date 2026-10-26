package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.ChargeDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.CheckedOutItemDTO;
import io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderCommand;
import io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderHandler;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Item;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderAlreadyExists;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BasketCheckoutIntegrationEventListener {

    private static final Logger log = LoggerFactory.getLogger(BasketCheckoutIntegrationEventListener.class);

    private CreateOrderHandler createOrderHandler;

    public BasketCheckoutIntegrationEventListener(CreateOrderHandler createOrderHandler) {
        this.createOrderHandler = createOrderHandler;
    }

    @EventListener
    public void handle(BasketCheckoutIntegrationEvent event) {
        var cmd = new CreateOrderCommand(event.eventId(), event.userName(), toDomain(event.billingAddress()), toDomain(event.charge()), toDomain(event.shippingAddress()), event.items().stream().map(this::toDomain).toList());
        try {
            createOrderHandler.handle(cmd);
            log.info("placed order for checkout {}.", event.eventId());
        }catch (OrderAlreadyExists alreadyPlaced){
            log.info("Checkout {} was already placed as an order; skipping redelivery", alreadyPlaced.checkoutId());
        }
    }


    private Item toDomain(CheckedOutItemDTO item){
        return new Item(item.id(), item.productId(), item.quantity(), item.price());
    }

    private Address toDomain(AddressDTO address){
        return new Address(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    private Payment toDomain(ChargeDTO charge) {
        var card = charge.card();
        return new Payment(charge.reference(), charge.amount(),
                new CardSummary(card.brand(), card.last4(), card.expiryMonth(), card.expiryYear()));
    }
}
