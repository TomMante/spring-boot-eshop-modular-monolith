package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.ProductService;
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
        var cart = repository.findByUsername(cmd.userName()).orElseThrow(BasketNotFound::new);
        var product = productService.fetchProductInfo(cmd.productId()).orElseThrow(() -> new CatalogProductNotFound(cmd.productId()));
        cart.addItem(cmd.productId(), cmd.quantity(), product.price(), product.name());
        repository.save(cart);
    }
}
