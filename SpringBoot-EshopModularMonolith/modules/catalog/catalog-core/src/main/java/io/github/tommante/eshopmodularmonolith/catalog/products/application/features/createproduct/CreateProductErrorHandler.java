package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

import io.github.tommante.eshopmodularmonolith.shared.contract.error.ApiError;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct")
public class CreateProductErrorHandler {

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ApiError> handler(ProductAlreadyExistsException exception){
        return ApiErrors.conflict("product.aleready.exists", exception.getLocalizedMessage());
    }
}
