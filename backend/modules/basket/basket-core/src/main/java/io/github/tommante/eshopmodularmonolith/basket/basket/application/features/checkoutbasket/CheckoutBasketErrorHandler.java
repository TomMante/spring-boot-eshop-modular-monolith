package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.*;
import io.github.tommante.eshopmodularmonolith.shared.contract.error.ApiError;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket")
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

    @ExceptionHandler(InvalidPaymentMethod.class)
    public ResponseEntity<ApiError> handler(InvalidPaymentMethod exception) {
        return ApiErrors.badRequest("basket.checkout.payment-method-invalid", exception.getLocalizedMessage());
    }

    @ExceptionHandler(PaymentProviderUnavailable.class)
    public ResponseEntity<ApiError> handler(PaymentProviderUnavailable exception) {
        return ApiErrors.serviceUnavailable("basket.checkout.payment-unavailable", exception.getLocalizedMessage());
    }

    @ExceptionHandler(PaymentDeclined.class)
    public ResponseEntity<ApiError> handler(PaymentDeclined exception) {
        return ApiErrors.paymentRequired("basket.checkout.payment-declined", exception.getLocalizedMessage());
    }
}
