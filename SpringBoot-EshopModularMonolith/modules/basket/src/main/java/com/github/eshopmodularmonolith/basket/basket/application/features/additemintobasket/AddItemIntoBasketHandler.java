package com.github.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import org.springframework.stereotype.Component;

@Component
public class AddItemIntoBasketHandler {

    private BasketRepository repository;
    private ProductService productService;

    public AddItemIntoBasketHandler(BasketRepository repository, ProductService productService) {
        this.repository = repository;
        this.productService = productService;
    }

    public void handle(AddItemIntoBasketCommand cmd){
        var cart = repository.findByUserName(cmd.userName()).orElseThrow(() -> new ShoppingCartNotFound(cmd.userName()));
        var product = productService.fetchProductInfo(cmd.productId()).orElseThrow(() -> new CatalogProductNotFound(cmd.productId()));
        cart.addItem(cmd.productId(), cmd.quantity(), cmd.color(), product.price(), product.name());
        repository.save(cart);
    }
}
