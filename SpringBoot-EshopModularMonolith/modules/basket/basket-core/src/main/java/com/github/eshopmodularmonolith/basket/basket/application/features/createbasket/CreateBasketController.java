package com.github.eshopmodularmonolith.basket.basket.application.features.createbasket;

import com.github.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateBasketController {

    private CreateBasketHandler handler;

    public CreateBasketController(CreateBasketHandler handler) {
        this.handler = handler;
    }


    @PostMapping("/basket")
    public ResponseEntity<Void> createBasket(@AuthenticatedUser CurrentUser currentUser){
        var cmd = new CreateBasketCommand(currentUser.username());
        handler.handle(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
