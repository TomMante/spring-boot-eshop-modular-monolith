package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;


import com.github.eshopmodularmonolith.ordering.orders.domain.AddressMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.ItemMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.PaymentMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrderHandlerTest {

    private CreateOrderHandler handler;
    private OrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeOrderRepository();
        handler = new CreateOrderHandler(repository);
    }

    @Test
    @DisplayName("Given create order command, when handler is called, then order is created")
    void givenCreateOrderCommand_whenHandlerIsCalled_thenOrderIsCreated(){
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "First Order",
                AddressMother.address(),
                AddressMother.address(),
                PaymentMother.payment(),
                List.of(ItemMother.item()));
        var orderId = handler.handle(createOrder);
        var orderOpt = repository.findById(orderId);
        assertThat(orderOpt).isPresent();
        var order = orderOpt.get();
        assertThat(order).extracting(Order::id).isEqualTo(orderId);
        assertThat(order).extracting(Order::customerId).isEqualTo(createOrder.customerId());
        assertThat(order).extracting(Order::name).isEqualTo(createOrder.name());
        assertThat(order).extracting(Order::items).usingRecursiveComparison().isEqualTo(createOrder.items());
        assertThat(order).extracting(Order::billingAddress).isEqualTo(createOrder.billingAddress());
        assertThat(order).extracting(Order::shippingAddress).isEqualTo(createOrder.shippingAddress());
        assertThat(order).extracting(Order::payment).isEqualTo(createOrder.payment());
    }
}