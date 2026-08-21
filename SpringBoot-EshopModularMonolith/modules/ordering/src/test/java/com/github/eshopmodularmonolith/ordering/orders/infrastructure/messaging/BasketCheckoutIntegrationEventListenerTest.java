package com.github.eshopmodularmonolith.ordering.orders.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@SpringJUnitConfig(BasketCheckoutIntegrationEventListener.class)
class BasketCheckoutIntegrationEventListenerTest {

    @Autowired
    ApplicationEventPublisher publisher;
    @MockitoSpyBean
    BasketCheckoutIntegrationEventListener listener;

    @Test
    @DisplayName("Given a BasketCheckoutIntegrationEventListener, when a BasketCheckoutIntegrationEvent is received, then the handle method is called")
    void givenBasketCheckoutEventListener_whenBasketCheckoutIntegrationEvent_thenHandleMethodIsCalled() {
        var item = new ItemDTO(UUID.randomUUID(), UUID.randomUUID(), 1, "Blue", BigDecimal.ONE, "Boba");
        var event = new BasketCheckoutIntegrationEvent("john", List.of(item), BigDecimal.valueOf(100.0));
        publisher.publishEvent(event);
        verify(listener).handle(event);
    }

}