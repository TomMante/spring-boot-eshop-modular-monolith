package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.getbasket;

import io.github.tommante.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GetBasketController {

    private final GetBasketHandler handler;

    public GetBasketController(GetBasketHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/basket")
    public ResponseEntity<GetBasketResponse> getBasket(@AuthenticatedUser CurrentUser currentUser){
        var result = handler.handle(new GetBasketQuery(currentUser.username()));
        return ResponseEntity.ok(new GetBasketResponse(result.shoppingCart()));
    }
}
