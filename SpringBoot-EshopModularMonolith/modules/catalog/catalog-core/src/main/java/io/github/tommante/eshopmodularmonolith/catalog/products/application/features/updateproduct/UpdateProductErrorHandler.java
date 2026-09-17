package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.updateproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
import io.github.tommante.eshopmodularmonolith.shared.contract.error.ApiError;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.github.tommante.eshopmodularmonolith.catalog.products.application.features.updateproduct")
public class UpdateProductErrorHandler {

    @ExceptionHandler(ProductNotFound.class)
    public ResponseEntity<ApiError> handler(ProductNotFound exception){
        return ApiErrors.notFound("product.not-found", exception.getLocalizedMessage());
    }
}
