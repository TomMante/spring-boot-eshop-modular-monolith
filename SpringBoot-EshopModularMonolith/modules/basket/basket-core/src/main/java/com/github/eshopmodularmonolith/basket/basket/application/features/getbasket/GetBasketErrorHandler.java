package com.github.eshopmodularmonolith.basket.basket.application.features.getbasket;

import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.shared.contract.error.ApiError;
import com.github.eshopmodularmonolith.shared.contract.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.github.eshopmodularmonolith.basket.basket.application.features.getbasket")
public class GetBasketErrorHandler {

    @ExceptionHandler(BasketNotFound.class)
    public ResponseEntity<ApiError> handler(BasketNotFound exception){
        return ApiErrors.notFound("basket.not-found", exception.getLocalizedMessage());
    }
}
