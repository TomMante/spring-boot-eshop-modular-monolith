package com.github.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AddItemIntoBasketController {

    private AddItemIntoBasketHandler handler;



    public AddItemIntoBasketController(AddItemIntoBasketHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/basket/items")
    public ResponseEntity<Void> addItemIntoBasket(@RequestBody @Validated AddItemIntoBasketRequest request,
                                                  @AuthenticationPrincipal UserDetails userDetails){
        var cmd = new AddItemIntoBasketCommand(userDetails.getUsername(), request.productId(), request.quantity(), request.color());
        handler.handle(cmd);
        return ResponseEntity.accepted().build();
    }
}
