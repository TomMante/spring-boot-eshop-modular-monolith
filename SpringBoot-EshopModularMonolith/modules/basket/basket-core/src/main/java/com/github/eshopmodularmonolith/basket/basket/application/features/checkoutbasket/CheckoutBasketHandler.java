package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.basket.basket.domain.model.Item;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CheckoutBasketHandler {

    private BasketRepository repository;
    private ProductService productService;

    public CheckoutBasketHandler(BasketRepository repository, ProductService productService) {
        this.repository = repository;
        this.productService = productService;
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

        cart.checkout(currentPrices, cmd.billingAddress(), cmd.payment(), cmd.shippingAddress());

        // Remove basket
        repository.delete(cart);
    }
}
