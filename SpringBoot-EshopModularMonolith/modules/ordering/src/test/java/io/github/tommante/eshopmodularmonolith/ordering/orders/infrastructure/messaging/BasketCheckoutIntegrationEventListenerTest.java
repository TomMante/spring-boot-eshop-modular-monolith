package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.CardSummaryDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.ChargeDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.CheckedOutItemDTO;
import io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.createorder.CreateOrderHandler;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Order;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.PaymentMother;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;
import io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringJUnitConfig(BasketCheckoutIntegrationEventListenerTest.Config.class)
class BasketCheckoutIntegrationEventListenerTest {

    private static Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");

    @Configuration
    static class Config {

        @Bean
        FakeOrderRepository orderRepository(){
            return new FakeOrderRepository();
        }

        @Bean
        Clock clock(){
            return Clock.fixed(fixedInstant, ZoneOffset.UTC);
        }

        @Bean
        CreateOrderHandler createOrderHandler(OrderRepository orderRepository, Clock clock) {
            return new CreateOrderHandler(orderRepository, clock);
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

    @BeforeEach
    void setUp() {
        orderRepository.clear();
    }

    private BasketCheckoutIntegrationEvent checkoutEvent(UUID eventId, String userName) {
        var item = new CheckedOutItemDTO(UUID.randomUUID(), UUID.randomUUID(), 2, BigDecimal.valueOf(12.50), "Boba");
        return new BasketCheckoutIntegrationEvent(eventId, userName, List.of(item), BigDecimal.valueOf(25.00),
                toDto(AddressMother.address()), toDto(PaymentMother.payment(new BigDecimal("25.00"))), toDto(AddressMother.address("Midgard")));
    }

    @Test
    @DisplayName("Given a basket checkout integration event, when it is published, then an order is created")
    void givenBasketCheckoutIntegrationEvent_whenPublished_thenOrderIsCreated() {
        var itemId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var item = new CheckedOutItemDTO(itemId, productId, 2, BigDecimal.valueOf(12.50), "Boba");
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment(new BigDecimal("25.00"));
        var event = new BasketCheckoutIntegrationEvent(
                UUID.randomUUID(),
                "john",
                List.of(item),
                BigDecimal.valueOf(25.00),
                toDto(billingAddress),
                toDto(payment),
                toDto(shippingAddress)
        );

        publisher.publishEvent(event);

        assertThat(orderRepository.orders()).singleElement().satisfies(order -> {
            assertThat(order.customer()).isEqualTo("john");
            assertThat(order.items()).singleElement().satisfies(orderItem -> {
                assertThat(orderItem.id()).isEqualTo(itemId);
                assertThat(orderItem.productId()).isEqualTo(productId);
                assertThat(orderItem.quantity()).isEqualTo(2);
                assertThat(orderItem.price()).isEqualByComparingTo(BigDecimal.valueOf(12.50));
            });
            assertThat(order.shippingAddress()).isEqualTo(shippingAddress);
            assertThat(order.billingAddress()).isEqualTo(billingAddress);
            assertThat(order.payment()).isEqualTo(payment);
            assertThat(order.placedAt()).isEqualTo(fixedInstant);
            assertThat(order.checkoutId()).isEqualTo(event.eventId());
        });
    }

    @Test
    @DisplayName("Given a checkout event already handled, when it is published again, then no second order is created")
    void givenCheckoutEventAlreadyHandled_whenPublishedAgain_thenNoSecondOrderIsCreated() {
        var event = checkoutEvent(UUID.randomUUID(), "john");
        publisher.publishEvent(event);

        assertThatCode(() -> publisher.publishEvent(event)).doesNotThrowAnyException();
        assertThat(orderRepository.orders()).singleElement()
                .satisfies(order -> assertThat(order.checkoutId()).isEqualTo(event.eventId()));
    }

    @Test
    @DisplayName("Given two checkout events from the same user, when both are published, then two orders are created")
    void givenTwoCheckoutEventsFromTheSameUser_whenBothArePublished_thenTwoOrdersAreCreated() {
        var event1 = checkoutEvent(UUID.randomUUID(), "john");
        var event2 = checkoutEvent(UUID.randomUUID(), "john");
        publisher.publishEvent(event1);
        publisher.publishEvent(event2);

        assertThat(orderRepository.orders())
                .extracting(Order::checkoutId)
                .containsExactlyInAnyOrder(event1.eventId(), event2.eventId());
    }

    private AddressDTO toDto(Address address) {
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    private ChargeDTO toDto(Payment payment) {
        var card = payment.card();
        return new ChargeDTO(payment.chargeReference(), payment.amount(),
                new CardSummaryDTO(card.brand(), card.last4(), card.expiryMonth(), card.expiryYear()));
    }

}