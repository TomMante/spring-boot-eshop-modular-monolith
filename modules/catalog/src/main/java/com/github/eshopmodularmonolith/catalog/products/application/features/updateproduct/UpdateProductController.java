package com.github.eshopmodularmonolith.catalog.products.application.features.updateproduct;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UpdateProductController {

    private final UpdateProductHandler handler;

    public UpdateProductController(UpdateProductHandler handler) {
        this.handler = handler;
    }

    @PutMapping("/product")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProduct(@RequestBody UpdateProductRequest request) {
        var cmd = new UpdateProductCommand(request.id(), request.name(), request.categories(), request.description(), request.imageFile(), request.price());
        handler.handle(cmd);
    }
}
