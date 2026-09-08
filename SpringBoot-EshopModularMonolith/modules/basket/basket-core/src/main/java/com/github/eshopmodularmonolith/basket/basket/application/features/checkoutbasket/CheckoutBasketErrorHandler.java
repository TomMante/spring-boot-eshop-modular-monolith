package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout;
import com.github.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import com.github.eshopmodularmonolith.shared.contract.error.ApiError;
import com.github.eshopmodularmonolith.shared.contract.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket")
public class CheckoutBasketErrorHandler {

    @ExceptionHandler(UnpriceableCartItem.class)
    public ResponseEntity<ApiError> handler(UnpriceableCartItem exception) {
        return ApiErrors.badRequest("basket.checkout.unpriceable-item", exception.getLocalizedMessage());
    }

    @ExceptionHandler(BasketNotFound.class)
    public ResponseEntity<ApiError> handler(BasketNotFound exception) {
        return ApiErrors.notFound("basket.not-found", exception.getLocalizedMessage());
    }

    @ExceptionHandler(EmptyCartCheckout.class)
    public ResponseEntity<ApiError> handler(EmptyCartCheckout exception) {
        return ApiErrors.badRequest("basket.checkout.empty-cart", exception.getLocalizedMessage());
    }
}
