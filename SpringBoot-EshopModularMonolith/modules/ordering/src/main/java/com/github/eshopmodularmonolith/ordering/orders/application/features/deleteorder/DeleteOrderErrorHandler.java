package com.github.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import com.github.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import com.github.eshopmodularmonolith.shared.contract.error.ApiError;
import com.github.eshopmodularmonolith.shared.contract.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.github.eshopmodularmonolith.ordering.orders.application.features.deleteorder")
public class DeleteOrderErrorHandler {

    @ExceptionHandler(OrderNotFound.class)
    public ResponseEntity<ApiError> handler(OrderNotFound exception){
        return ApiErrors.notFound("order.not-found", exception.getLocalizedMessage());
    }
}
