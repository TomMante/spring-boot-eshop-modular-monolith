package com.github.eshopmodularmonolith.basket.basket.application.features.deletebasket;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DeleteBasketController {

    private DeleteBasketHandler handler;

    public DeleteBasketController(DeleteBasketHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/basket/{id}")
    public ResponseEntity<Void> deleteBasket(@PathVariable("id") UUID id){
        handler.handle(new DeleteBasketCommand(id));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
