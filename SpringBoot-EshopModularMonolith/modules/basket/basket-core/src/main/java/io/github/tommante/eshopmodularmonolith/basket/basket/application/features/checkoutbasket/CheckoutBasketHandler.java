package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CheckoutBasketHandler {

    private BasketRepository repository;
    private ProductService productService;
    private final Clock clock;

    public CheckoutBasketHandler(BasketRepository repository, ProductService productService, Clock clock) {
        this.repository = repository;
        this.productService = productService;
        this.clock = clock;
    }

    @Transactional("basketTransactionManager")
    public void handle(CheckoutBasketCommand cmd) {
        var cart = repository.findByUsername(cmd.userName()).orElseThrow(BasketNotFound::new);

        var currentPrices = cart.items().stream()
                .map(Item::productId)
                .map(productService::fetchProductInfo)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(
                        ProductInfo::id,
                        ProductInfo::price
                ));

        cart.checkout(currentPrices, clock.instant(), cmd.billingAddress(), cmd.payment(), cmd.shippingAddress());

        // Remove basket
        repository.delete(cart);
    }
}
