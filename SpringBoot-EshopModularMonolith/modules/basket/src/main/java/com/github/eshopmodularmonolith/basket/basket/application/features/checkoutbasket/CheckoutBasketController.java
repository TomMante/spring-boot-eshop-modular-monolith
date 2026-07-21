package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import com.github.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutBasketController {

    private CheckoutBasketHandler handler;

    public CheckoutBasketController(CheckoutBasketHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/basket/checkout")
    public ResponseEntity<Void> checkoutBasket(@AuthenticatedUser CurrentUser currentUser){
        var cmd = new CheckoutBasketCommand(currentUser.username());
        handler.handle(cmd);
        return ResponseEntity.accepted().build();
    }
}
