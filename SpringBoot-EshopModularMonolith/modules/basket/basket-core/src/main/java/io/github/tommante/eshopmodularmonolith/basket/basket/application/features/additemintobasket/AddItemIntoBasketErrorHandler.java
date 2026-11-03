package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.InvalidItemQuantity;
import io.github.tommante.eshopmodularmonolith.shared.contract.error.ApiError;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.github.tommante.eshopmodularmonolith.basket.basket.application.features.additemintobasket")
public class AddItemIntoBasketErrorHandler {

    @ExceptionHandler(InvalidItemQuantity.class)
    public ResponseEntity<ApiError> handler(InvalidItemQuantity exception) {
        return ApiErrors.badRequest("basket.add-item.invalid-quantity", exception.getLocalizedMessage());
    }

    @ExceptionHandler(BasketNotFound.class)
    public ResponseEntity<ApiError> handler(BasketNotFound exception) {
        return ApiErrors.notFound("basket.not-found", exception.getLocalizedMessage());
    }

    @ExceptionHandler(CatalogProductNotFound.class)
    public ResponseEntity<ApiError> handler(CatalogProductNotFound exception) {
        return ApiErrors.notFound("basket.add-item.product-not-found", exception.getLocalizedMessage());
    }
}
