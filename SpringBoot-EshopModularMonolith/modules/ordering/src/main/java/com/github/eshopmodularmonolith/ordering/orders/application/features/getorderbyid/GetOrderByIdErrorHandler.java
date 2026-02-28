package com.github.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import com.github.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import com.github.eshopmodularmonolith.shared.contract.error.ApiError;
import com.github.eshopmodularmonolith.shared.contract.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.github.eshopmodularmonolith.ordering.orders.application.features.getorderbyid")
public class GetOrderByIdErrorHandler {

    @ExceptionHandler(OrderNotFound.class)
    public ResponseEntity<ApiError> handler(OrderNotFound exception){
        return ApiErrors.notFound("order.not-found", exception.getLocalizedMessage());
    }
}
