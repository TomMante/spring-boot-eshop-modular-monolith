package com.github.eshopmodularmonolith.ordering.orders.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BasketCheckoutIntegrationEventListener {

    private static final Logger log = LoggerFactory.getLogger(BasketCheckoutIntegrationEventListener.class);

    @EventListener
    public void handle(BasketCheckoutIntegrationEvent event) {
        log.info("Received BasketCheckoutIntegrationEvent for user: {}", event.userName());
    }
}
