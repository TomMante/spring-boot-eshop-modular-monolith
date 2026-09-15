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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
                UUID.randomUUID(),
                "top_customer",
                AddressMother.billing(),
                PaymentMother.payment(),
                AddressMother.shipping(),
                List.of(ItemMother.item()));
        var orderId = handler.handle(createOrder);
        var orderOpt = repository.findById(orderId);
        assertThat(orderOpt).isPresent();
        var order = orderOpt.get();
        assertThat(order).extracting(Order::id).isEqualTo(orderId);
        assertThat(order).extracting(Order::checkoutId).isEqualTo(createOrder.checkoutId());
        assertThat(order).extracting(Order::customer).isEqualTo(createOrder.customer());
        assertThat(order).extracting(Order::items).usingRecursiveComparison().isEqualTo(createOrder.items());
        assertThat(order).extracting(Order::billingAddress).isEqualTo(createOrder.billingAddress());
        assertThat(order).extracting(Order::shippingAddress).isEqualTo(createOrder.shippingAddress());
        assertThat(order).extracting(Order::payment).isEqualTo(createOrder.payment());
        assertThat(order).extracting(Order::placedAt).isEqualTo(fixedInstant);
    }

    @Test
    @DisplayName("Given create order command with missing shipping info, when handler is called, then order is rejected")
    void givenCreateOrderCommandWithMissingShippingInfo_whenHandlerIsCalled_thenOrderIsRejected(){
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "top_customer",
                AddressMother.billing(),
                PaymentMother.payment(),
                null,
                List.of(ItemMother.item()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> handler.handle(createOrder)).withMessageContaining("Shipping address is required");
    }

    @Test
    @DisplayName("Given create order command with missing billing info, when handler is called, then order is rejected")
    void givenCreateOrderCommandWithMissingBillingInfo_whenHandlerIsCalled_thenOrderIsRejected(){
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "top_customer",
                null,
                PaymentMother.payment(),
                AddressMother.shipping(),
                List.of(ItemMother.item()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> handler.handle(createOrder)).withMessageContaining("Billing address is required");
    }

    @Test
    @DisplayName("Given create order command with missing payment info, when handler is called, then order is rejected")
    void givenCreateOrderCommandWithMissingPaymentInfo_whenHandlerIsCalled_thenOrderIsRejected(){
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "top_customer",
                AddressMother.billing(),
                null,
                AddressMother.shipping(),
                List.of(ItemMother.item()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> handler.handle(createOrder)).withMessageContaining("Payment info is required");
    }

    @Test
    @DisplayName("Given create order command with no items, when handler is called, then order is rejected")
    void givenCreateOrderCommandWithNoItems_whenHandlerIsCalled_thenOrderIsRejected(){
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "top_customer",
                AddressMother.billing(),
                PaymentMother.payment(),
                AddressMother.shipping(),
                List.of());
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> handler.handle(createOrder)).withMessageContaining("Order must contain at least one item");
    }

    @Test
    @DisplayName("Given create order command with no checkout id, when handler is called, then order is rejected")
    void givenCreateOrderCommandWithNoCheckoutId_whenHandlerIsCalled_thenOrderIsRejected(){
        var createOrder = new CreateOrderCommand(
                null,
                "top_customer",
                AddressMother.billing(),
                PaymentMother.payment(),
                AddressMother.shipping(),
                List.of(ItemMother.item()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> handler.handle(createOrder)).withMessageContaining("Order requires a checkout id");
    }
}