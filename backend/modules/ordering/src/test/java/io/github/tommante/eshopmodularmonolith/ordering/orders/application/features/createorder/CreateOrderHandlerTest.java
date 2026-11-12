package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.createorder;


import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.*;
import io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CreateOrderHandlerTest {

    private CreateOrderHandler handler;
    private FakeOrderRepository repository;
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
    void givenCreateOrderCommand_whenHandlerIsCalled_thenOrderIsCreated() {
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
    void givenCreateOrderCommandWithMissingShippingInfo_whenHandlerIsCalled_thenOrderIsRejected() {
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
    void givenCreateOrderCommandWithMissingBillingInfo_whenHandlerIsCalled_thenOrderIsRejected() {
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
    void givenCreateOrderCommandWithMissingPaymentInfo_whenHandlerIsCalled_thenOrderIsRejected() {
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
    void givenCreateOrderCommandWithNoItems_whenHandlerIsCalled_thenOrderIsRejected() {
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
    void givenCreateOrderCommandWithNoCheckoutId_whenHandlerIsCalled_thenOrderIsRejected() {
        var createOrder = new CreateOrderCommand(
                null,
                "top_customer",
                AddressMother.billing(),
                PaymentMother.payment(),
                AddressMother.shipping(),
                List.of(ItemMother.item()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> handler.handle(createOrder)).withMessageContaining("Order requires a checkout id");
    }

    @Test
    @DisplayName("Given create order command with lines of different prices and quantities, when handler is called, then the order total is the sum of its lines")
    void givenCreateOrderCommandWithLinesOfDifferentPricesAndQuantities_whenHandlerIsCalled_thenOrderTotalIsTheSumOfItsLines() {
        var items = List.of(
                new Item(UUID.randomUUID(), UUID.randomUUID(), 3, new BigDecimal("12.50")),
                new Item(UUID.randomUUID(), UUID.randomUUID(), 2, new BigDecimal("7.25")));
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "top_customer",
                AddressMother.billing(),
                PaymentMother.paymentFor(items),
                AddressMother.shipping(),
                items
                );

        var orderId = handler.handle(createOrder);

        assertThat(repository.findById(orderId)).get()
                .extracting(Order::totalPrice)
                .satisfies(total -> assertThat(total).isEqualByComparingTo(new BigDecimal("52.00"))); // 3*12.50 + 2*7.25
    }

    @Test
    @DisplayName("Given create order command with an item without a price, when handler is called, then order is rejected")
    void givenCreateOrderCommandWithItemWithoutPrice_whenHandlerIsCalled_thenOrderIsRejected() {
        var unpricedItemId = UUID.randomUUID();
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "top_customer",
                AddressMother.billing(),
                PaymentMother.payment(),
                AddressMother.shipping(),
                List.of(new Item(unpricedItemId, UUID.randomUUID(), 1, null)));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> handler.handle(createOrder)).withMessageContaining(unpricedItemId.toString());
        assertThat(repository.orders()).isEmpty();
    }

    @Test
    @DisplayName("Given a payment whose amount differs from the order total, when handler is called, then the order is rejected and nothing is saved")
    void givenPaymentAmountDifferentFromOrderTotal_whenHandlerIsCalled_thenOrderIsRejectedAndNothingIsSaved() {
        var items = List.of(ItemMother.item());                    // 2 × 1234 = 2468
        var createOrder = new CreateOrderCommand(
                UUID.randomUUID(),
                "top_customer",
                AddressMother.billing(),
                PaymentMother.payment(new BigDecimal("2467.99")),
                AddressMother.shipping(),
                items);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> handler.handle(createOrder))
                .withMessageContaining("does not match the order total");
        assertThat(repository.orders()).isEmpty();
    }
}
