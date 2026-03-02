package com.github.eshopmodularmonolith.basket.basket.application.features.createbasket;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateBasketController {

    private CreateBasketHandler handler;

    public CreateBasketController(CreateBasketHandler handler) {
        this.handler = handler;
    }


    @PostMapping("/basket")
    public ResponseEntity<Void> createBasket(@RequestBody @Validated CreateBasketRequest request){
        var cmd = new CreateBasketCommand(request.userName());
        handler.handle(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
