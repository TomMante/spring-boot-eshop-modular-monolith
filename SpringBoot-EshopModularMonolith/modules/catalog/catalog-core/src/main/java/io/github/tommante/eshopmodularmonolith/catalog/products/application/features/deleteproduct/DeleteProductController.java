package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DeleteProductController {

    private DeleteProductHandler handler;

    public DeleteProductController(DeleteProductHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        handler.handle(new DeleteProductCommand(id));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
