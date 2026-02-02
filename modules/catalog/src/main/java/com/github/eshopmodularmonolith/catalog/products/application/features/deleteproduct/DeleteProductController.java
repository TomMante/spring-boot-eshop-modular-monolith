package com.github.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DeleteProductController {

    private DeleteProductHandler handler;

    public DeleteProductController(DeleteProductHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/product/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        handler.handle(new DeleteProductCommand(id));
    }
}
