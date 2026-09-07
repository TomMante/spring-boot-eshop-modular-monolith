package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.basket.basket.domain.AddressMother;
import com.github.eshopmodularmonolith.basket.basket.domain.PaymentMother;
import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import com.github.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import com.github.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import com.github.eshopmodularmonolith.basket.basket.domain.model.Item;
import com.github.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters.CatalogProductACL;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters.fake.FakeProductQueryService;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketRepository;
import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


class CheckoutBasketHandlerTest {

    private FakeBasketRepository repository;
    private FakeProductQueryService productQueryService;
    private ProductService productService;
    private CheckoutBasketHandler handler;
    private ProductDTO product;

    @BeforeEach
    void setUp() {
        repository = new FakeBasketRepository();
        productQueryService = new FakeProductQueryService();
        productService = new CatalogProductACL(productQueryService);
        handler = new CheckoutBasketHandler(repository, productService);

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
        var existingCart = ShoppingCartMother.create().withItems(List.of(new Item(UUID.randomUUID(), product.id(), 1, addedPrice, product.name()))).build();
        repository.save(existingCart);
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment();
        var cmd = new CheckoutBasketCommand(existingCart.id(), billingAddress, payment, shippingAddress);
        handler.handle(cmd);

        // Domain event sent
        assertThat(repository.domainEvents())
                .filteredOn(BasketCheckedOut.class::isInstance)
                .singleElement()
                .satisfies(e -> {
                            var event = (BasketCheckedOut) e;
                            assertThat(event.userName()).isEqualTo(existingCart.userName());
                            assertThat(event.items()).isEqualTo(existingCart.items());
                            assertThat(event.totalPrice()).isEqualTo(product.price());
                            assertThat(event.eventId()).isNotNull();
                            assertThat(event.occurredOn()).isNotNull();
                            assertThat(event.billingAddress()).isEqualTo(billingAddress);
                            assertThat(event.shippingAddress()).isEqualTo(shippingAddress);
                            assertThat(event.payment()).isEqualTo(payment);
                        }
                );
        //Cart is removed
        assertThat(repository.findByUsername(existingCart.id())).isEmpty();

    }

    @Test
    @DisplayName("Given existing cart, when checkout command is received without shipping address, then cart is checked out with shipping address as billing address")
    void givenExistingCart_whenCheckoutCommandIsReceivedWithoutShippingAddress_thenCartIsCheckedOutWithShippingAddressAsBillingAddress() {
        var addedPrice = BigDecimal.valueOf(400);
        var existingCart = ShoppingCartMother.create().withItems(List.of(new Item(UUID.randomUUID(), product.id(), 1, addedPrice, product.name()))).build();
        repository.save(existingCart);
        var billingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var cmd = new CheckoutBasketCommand(existingCart.id(), billingAddress, payment, null);
        handler.handle(cmd);

        // Domain event sent
        assertThat(repository.domainEvents())
                .filteredOn(BasketCheckedOut.class::isInstance)
                .singleElement()
                .satisfies(e -> {
                            var event = (BasketCheckedOut) e;
                            assertThat(event.userName()).isEqualTo(existingCart.userName());
                            assertThat(event.items()).isEqualTo(existingCart.items());
                            assertThat(event.totalPrice()).isEqualTo(product.price());
                            assertThat(event.eventId()).isNotNull();
                            assertThat(event.occurredOn()).isNotNull();
                            assertThat(event.billingAddress()).isEqualTo(billingAddress);
                            assertThat(event.shippingAddress()).isEqualTo(billingAddress);
                            assertThat(event.payment()).isEqualTo(payment);
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
    }

    @Test
    @DisplayName("Given a cart item whose product is no longer in the catalog, when checkout command is received, then UnpriceableCartItem is thrown")
    void givenCartItemWithProductNoLongerInCatalog_whenCheckoutCommandIsReceived_thenUnpriceableCartItemIsThrown() {
        var removedProductId = UUID.randomUUID();
        var existingCart = ShoppingCartMother.create().withItems(List.of(new Item(UUID.randomUUID(), removedProductId, 1, BigDecimal.valueOf(400), "Removed Product"))).build();
        repository.save(existingCart);
        var cmd = new CheckoutBasketCommand(existingCart.id(), AddressMother.address(), PaymentMother.payment(), null);

        assertThatExceptionOfType(UnpriceableCartItem.class).isThrownBy(() -> handler.handle(cmd));
    }
}