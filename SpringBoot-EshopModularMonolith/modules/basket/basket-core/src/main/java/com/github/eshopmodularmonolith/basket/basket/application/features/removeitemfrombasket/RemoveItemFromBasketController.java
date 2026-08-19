package com.github.eshopmodularmonolith.basket.basket.application.features.removeitemfrombasket;

import com.github.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RemoveItemFromBasketController {

    private RemoveItemFromBasketHandler handler;



    public RemoveItemFromBasketController(RemoveItemFromBasketHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/basket/items")
    public ResponseEntity<Void> addItemIntoBasket(@RequestBody @Validated RemoveItemFromBasketRequest request,
                                                  @AuthenticatedUser CurrentUser currentUser){
        var cmd = new RemoveItemFromBasketCommand(currentUser.username(), request.productId());
        handler.handle(cmd);
        return ResponseEntity.accepted().build();
    }
}
