package com.github.eshopmodularmonolith.basket.basket.application.features.deletebasket;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeleteBasketController {

    private DeleteBasketHandler handler;

    public DeleteBasketController(DeleteBasketHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/basket/{userName}")
    public ResponseEntity<Void> deleteBasket(@PathVariable("userName") String userName){
        handler.handle(new DeleteBasketCommand(userName));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
