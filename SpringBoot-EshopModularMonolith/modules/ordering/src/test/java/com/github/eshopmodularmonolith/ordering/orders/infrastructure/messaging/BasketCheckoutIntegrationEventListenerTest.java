package com.github.eshopmodularmonolith.ordering.orders.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import com.github.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderHandler;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(BasketCheckoutIntegrationEventListenerTest.Config.class)
class BasketCheckoutIntegrationEventListenerTest {

    @Configuration
    static class Config {

        @Bean
        FakeOrderRepository orderRepository(){
            return new FakeOrderRepository();
        }

        @Bean
        CreateOrderHandler createOrderHandler(OrderRepository orderRepository) {
            return new CreateOrderHandler(orderRepository);
        }

        @Bean
        BasketCheckoutIntegrationEventListener basketCheckoutIntegrationEventListener(CreateOrderHandler createOrderHandler) {
            return new BasketCheckoutIntegrationEventListener(createOrderHandler);
        }
    }

    @Autowired
    ApplicationEventPublisher publisher;
    @Autowired
    FakeOrderRepository orderRepository;


    @Test
    @DisplayName("Given a basket checkout integration event, when it is published, then a pending order is created")
    void givenBasketCheckoutIntegrationEvent_whenPublished_thenPendingOrderIsCreated() {
        var itemId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var item = new ItemDTO(itemId, productId, 2, "Blue", BigDecimal.valueOf(12.50), "Boba");
        var event = new BasketCheckoutIntegrationEvent(
                UUID.randomUUID(), "john", List.of(item), BigDecimal.valueOf(25.00));

        publisher.publishEvent(event);

        assertThat(orderRepository.orders()).singleElement().satisfies(order -> {
            assertThat(order.customer()).isEqualTo("john");
            assertThat(order.items()).singleElement().satisfies(orderItem -> {
                assertThat(orderItem.id()).isEqualTo(itemId);
                assertThat(orderItem.productId()).isEqualTo(productId);
                assertThat(orderItem.quantity()).isEqualTo(2);
                assertThat(orderItem.price()).isEqualByComparingTo(BigDecimal.valueOf(12.50));
            });
            assertThat(order.name()).isNull();
            assertThat(order.shippingAddress()).isNull();
            assertThat(order.billingAddress()).isNull();
            assertThat(order.payment()).isNull();
        });
    }

}