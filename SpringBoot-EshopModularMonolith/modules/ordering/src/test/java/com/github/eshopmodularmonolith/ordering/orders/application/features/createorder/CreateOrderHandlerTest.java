package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;


import com.github.eshopmodularmonolith.ordering.orders.domain.AddressMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.ItemMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.PaymentMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrderHandlerTest {

    private CreateOrderHandler handler;
    private OrderRepository repository;
    private Clock clock;
    private Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(fixedInstant, Clock.systemUTC().getZone());
        repository = new FakeOrderRepository();
        handler = new CreateOrderHandler(repository, clock);
    }

    @Test
    @DisplayName("Given create order command, when handler is called, then order is created")
    void givenCreateOrderCommand_whenHandlerIsCalled_thenOrderIsCreated(){
        var createOrder = new CreateOrderCommand(
                "top_customer",
                AddressMother.address(),
                PaymentMother.payment(),
                AddressMother.address(),
                List.of(ItemMother.item()));
        var orderId = handler.handle(createOrder);
        var orderOpt = repository.findById(orderId);
        assertThat(orderOpt).isPresent();
        var order = orderOpt.get();
        assertThat(order).extracting(Order::id).isEqualTo(orderId);
        assertThat(order).extracting(Order::customer).isEqualTo(createOrder.customer());
        assertThat(order).extracting(Order::items).usingRecursiveComparison().isEqualTo(createOrder.items());
        assertThat(order).extracting(Order::billingAddress).isEqualTo(createOrder.billingAddress());
        assertThat(order).extracting(Order::shippingAddress).isEqualTo(createOrder.shippingAddress());
        assertThat(order).extracting(Order::payment).isEqualTo(createOrder.payment());
        assertThat(order).extracting(Order::placedAt).isEqualTo(fixedInstant);
    }
}