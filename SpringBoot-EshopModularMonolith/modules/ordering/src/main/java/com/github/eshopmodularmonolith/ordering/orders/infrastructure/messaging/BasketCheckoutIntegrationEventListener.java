package com.github.eshopmodularmonolith.ordering.orders.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.PaymentDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import com.github.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderCommand;
import com.github.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderHandler;
import com.github.eshopmodularmonolith.ordering.orders.domain.Item;
import com.github.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderAlreadyExists;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;
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
        var cmd = new CreateOrderCommand(event.eventId(), event.userName(), toDomain(event.billingAddress()), toDomain(event.payment()), toDomain(event.shippingAddress()), event.items().stream().map(this::toDomain).toList());
        try {
            createOrderHandler.handle(cmd);
            log.info("placed order for checkout {}.", event.eventId());
        }catch (OrderAlreadyExists alreadyPlaced){
            log.info("Checkout {} was already placed as an order; skipping redelivery", alreadyPlaced.checkoutId());
        }
    }


    private Item toDomain(ItemDTO item){
        return new Item(item.id(), item.productId(), item.quantity(), item.priceWhenAdded());
    }

    private Address toDomain(AddressDTO address){
        return new Address(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    private Payment toDomain(PaymentDTO payment) {
        return new Payment(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }
}
