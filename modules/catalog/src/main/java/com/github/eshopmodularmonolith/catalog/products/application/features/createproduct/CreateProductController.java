package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateProductController {

    private final ProductCreationHandler handler;

    public CreateProductController(ProductCreationHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/product")
    @ResponseStatus(code = HttpStatus.CREATED)
    public boolean createProduct(@RequestBody CreateProductRequest request){
        var cmd = new CreateProductCommand(
                request.id(),
                request.name(),
                request.categories(),
                request.description(),
                request.imageFile(),
                request.price()
        );
        return handler.handle(cmd);
    }

}
