package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
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
    public ResponseEntity<Void> checkoutBasket(
            @AuthenticatedUser CurrentUser currentUser,
            @Validated @RequestBody CheckoutBasketRequest request) {
        var cmd = new CheckoutBasketCommand(
                currentUser.username(),
                request.billingAddress().toDomain(),
                request.payment().paymentMethodId(),
                request.shippingAddress() == null? null : request.shippingAddress().toDomain());
        handler.handle(cmd);
        return ResponseEntity.accepted().build();
    }
}
