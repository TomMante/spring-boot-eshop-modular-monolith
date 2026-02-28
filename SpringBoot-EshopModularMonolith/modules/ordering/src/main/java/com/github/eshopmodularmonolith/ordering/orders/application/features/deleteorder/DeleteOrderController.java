package com.github.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DeleteOrderController {

    private DeleteOrderHandler handler;

    public DeleteOrderController(DeleteOrderHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/order/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") UUID id){
        handler.handle(new DeleteOrderCommand(id));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
