package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.CheckedOutItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.PaymentDeclined;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.CatalogProductACL;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.fake.FakePaymentGateway;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.fake.FakeProductQueryService;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.contract.dto.ProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


class CheckoutBasketHandlerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private FakeBasketRepository repository;
    private FakeProductQueryService productQueryService;
    private ProductService productService;
    private CheckoutBasketHandler handler;
    private ProductDTO product;
    private FakePaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        repository = new FakeBasketRepository();
        productQueryService = new FakeProductQueryService();
        productService = new CatalogProductACL(productQueryService);
        paymentGateway = new FakePaymentGateway();
        handler = new CheckoutBasketHandler(
                repository,
                productService,
                paymentGateway,
                TransactionOperations.withoutTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        product = new ProductDTO(
                UUID.randomUUID(),
                "Hat",
                List.of("accessory"),
                "A nice hat.",
                "hat.png",
                BigDecimal.valueOf(783)
        );

        productQueryService.addProduct(product);
    }

    @Test
    @DisplayName("Given existing cart, when checkout command is received, then cart is checked out")
    void givenExistingCart_whenCheckoutCommandIsReceived_thenCartIsCheckedOut() {
        var addedPrice = BigDecimal.valueOf(400);
        var cartItem = new Item(UUID.randomUUID(), product.id(), 1, addedPrice, product.name());
        var existingCart = ShoppingCartMother.create().withItems(List.of(cartItem)).build();
        repository.save(existingCart);
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var cmd = new CheckoutBasketCommand(existingCart.id(), billingAddress, "pm_card_visa", shippingAddress);
        handler.handle(cmd);

        // Domain event sent
        assertThat(repository.domainEvents())
                .filteredOn(BasketCheckedOut.class::isInstance)
                .singleElement()
                .satisfies(e -> {
                            var event = (BasketCheckedOut) e;
                            assertThat(event.userName()).isEqualTo(existingCart.userName());
                            assertThat(event.items()).containsExactly(new CheckedOutItem(cartItem.id(), product.id(), 1, product.price(), product.name()));
                            assertThat(event.totalPrice()).isEqualTo(product.price());
                            assertThat(event.eventId()).isNotNull();
                            assertThat(event.occurredOn()).isEqualTo(NOW);
                            assertThat(event.billingAddress()).isEqualTo(billingAddress);
                            assertThat(event.shippingAddress()).isEqualTo(shippingAddress);
                            var charge = paymentGateway.chargeTakenFor(event.eventId()).orElseThrow();
                            assertThat(charge.amount()).isEqualByComparingTo(product.price());
                            assertThat(event.charge()).isEqualTo(charge);
                        }
                );
        //Cart is removed
        assertThat(repository.findByUsername(existingCart.id())).isEmpty();

    }

    @Test
    @DisplayName("Given existing cart, when checkout command is received without shipping address, then cart is checked out with shipping address as billing address")
    void givenExistingCart_whenCheckoutCommandIsReceivedWithoutShippingAddress_thenCartIsCheckedOutWithShippingAddressAsBillingAddress() {
        var addedPrice = BigDecimal.valueOf(400);
        var cartItem = new Item(UUID.randomUUID(), product.id(), 1, addedPrice, product.name());
        var existingCart = ShoppingCartMother.create().withItems(List.of(cartItem)).build();
        repository.save(existingCart);
        var billingAddress = AddressMother.address();
        var cmd = new CheckoutBasketCommand(existingCart.id(), billingAddress,"pm_card_visa", null);
        handler.handle(cmd);

        // Domain event sent
        assertThat(repository.domainEvents())
                .filteredOn(BasketCheckedOut.class::isInstance)
                .singleElement()
                .satisfies(e -> {
                            var event = (BasketCheckedOut) e;
                            assertThat(event.userName()).isEqualTo(existingCart.userName());
                            assertThat(event.items()).containsExactly(new CheckedOutItem(cartItem.id(), product.id(), 1, product.price(), product.name()));
                            assertThat(event.totalPrice()).isEqualTo(product.price());
                            assertThat(event.eventId()).isNotNull();
                            assertThat(event.occurredOn()).isEqualTo(NOW);
                            assertThat(event.billingAddress()).isEqualTo(billingAddress);
                            assertThat(event.shippingAddress()).isEqualTo(billingAddress);
                            var charge = paymentGateway.chargeTakenFor(event.eventId()).orElseThrow();
                            assertThat(charge.amount()).isEqualByComparingTo(product.price());
                            assertThat(event.charge()).isEqualTo(charge);
                        }
                );
        //Cart is removed
        assertThat(repository.findByUsername(existingCart.id())).isEmpty();

    }

    @Test
    @DisplayName("Given non existing cart, when checkout command is received, then exception is thrown")
    void givenNonExistingCart_whenCheckoutCommandIsReceived_thenExceptionIsThrown() {
        var cmd = new CheckoutBasketCommand("non-existing-user", null, null, null);
        assertThatExceptionOfType(BasketNotFound.class).isThrownBy(() -> handler.handle(cmd));
        assertThat(paymentGateway.charges()).isEmpty();
    }

    @Test
    @DisplayName("Given a cart item whose product is no longer in the catalog, when checkout command is received, then UnpriceableCartItem is thrown")
    void givenCartItemWithProductNoLongerInCatalog_whenCheckoutCommandIsReceived_thenUnpriceableCartItemIsThrown() {
        var removedProductId = UUID.randomUUID();
        var existingCart = ShoppingCartMother.create().withItems(List.of(new Item(UUID.randomUUID(), removedProductId, 1, BigDecimal.valueOf(400), "Removed Product"))).build();
        repository.save(existingCart);
        var cmd = new CheckoutBasketCommand(existingCart.id(), AddressMother.address(), "pm_card_visa", null);

        assertThatExceptionOfType(UnpriceableCartItem.class).isThrownBy(() -> handler.handle(cmd));
        assertThat(paymentGateway.charges()).isEmpty();
    }

    @Test
    @DisplayName("Given an empty cart, when checkout command is received, then EmptyCartCheckout is thrown")
    void givenEmptyCart_whenCheckoutCommandIsReceived_thenEmptyCartCheckoutIsThrown() {
        var existingCart = ShoppingCartMother.create().build();
        repository.save(existingCart);
        var cmd = new CheckoutBasketCommand(existingCart.id(), AddressMother.address(), "pm_card_visa", null);

        assertThatExceptionOfType(EmptyCartCheckout.class).isThrownBy(() -> handler.handle(cmd));
    }

    @Test
    @DisplayName("Given the payment is declined, when checkout command is received, then PaymentDeclined is thrown, no event is recorded and the cart is kept")
    void givenPaymentIsDeclined_whenCheckoutCommandIsReceived_thenPaymentDeclinedIsThrownNoEventIsRecordedAndCartIsKept() {
        var existingCart = ShoppingCartMother.create().withItems(List.of(new Item(UUID.randomUUID(), product.id(), 1, BigDecimal.valueOf(400), product.name()))).build();
        repository.save(existingCart);
        paymentGateway.failChargesWith(new PaymentDeclined("card_declined", "insufficient_funds"));
        var cmd = new CheckoutBasketCommand(existingCart.id(), AddressMother.address(), "pm_card_visa", null);

        assertThatExceptionOfType(PaymentDeclined.class).isThrownBy(() -> handler.handle(cmd));
        assertThat(repository.domainEvents()).isEmpty();
        assertThat(repository.findByUsername(existingCart.id())).isPresent();
    }
}