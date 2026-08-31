package com.github.eshopmodularmonolith.ordering.orders.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import com.github.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderCommand;
import com.github.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderHandler;
import com.github.eshopmodularmonolith.ordering.orders.domain.Item;
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
        var cmd = new CreateOrderCommand(event.userName(), null, null, null, event.items().stream().map(this::toDomain).toList());
        createOrderHandler.handle(cmd);
        log.info("Received BasketCheckoutIntegrationEvent for user: {}", event.userName());
    }


    private Item toDomain(ItemDTO item){
        return new Item(item.id(), item.productId(), item.quantity(), item.priceWhenAdded());
    }
}
