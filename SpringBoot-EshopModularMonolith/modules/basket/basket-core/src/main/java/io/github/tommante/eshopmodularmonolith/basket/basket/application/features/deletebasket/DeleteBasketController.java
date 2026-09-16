package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.deletebasket;

import io.github.tommante.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeleteBasketController {

    private DeleteBasketHandler handler;

    public DeleteBasketController(DeleteBasketHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/basket")
    public ResponseEntity<Void> deleteBasket(@AuthenticatedUser CurrentUser currentUser){
        handler.handle(new DeleteBasketCommand(currentUser.username()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
