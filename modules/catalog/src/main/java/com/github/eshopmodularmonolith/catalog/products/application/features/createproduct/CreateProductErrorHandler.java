package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import com.github.eshopmodularmonolith.shared.contract.error.ApiError;
import com.github.eshopmodularmonolith.shared.contract.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.github.eshopmodularmonolith.catalog.products")
public class CreateProductErrorHandler {

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ApiError> handler(ProductAlreadyExistsException exception){
        return ApiErrors.conflict("product.aleready.exists", exception.getLocalizedMessage());
    }
}
