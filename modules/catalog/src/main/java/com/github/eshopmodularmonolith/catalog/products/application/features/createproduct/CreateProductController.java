package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateProductController {

    private final CreateProductHandler handler;

    public CreateProductController(CreateProductHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/product")
    public ResponseEntity<Void> createProduct(@RequestBody CreateProductRequest request){
        var cmd = new CreateProductCommand(
                request.name(),
                request.categories(),
                request.description(),
                request.imageFile(),
                request.price()
        );
        handler.handle(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
