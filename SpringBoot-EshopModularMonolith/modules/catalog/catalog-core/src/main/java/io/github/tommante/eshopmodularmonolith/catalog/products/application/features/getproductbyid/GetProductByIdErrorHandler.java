package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.getproductbyid;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
import io.github.tommante.eshopmodularmonolith.shared.contract.error.ApiError;
import io.github.tommante.eshopmodularmonolith.shared.contract.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.github.tommante.eshopmodularmonolith.catalog.products.application.features.getproductbyid")
public class GetProductByIdErrorHandler {

    @ExceptionHandler(ProductNotFound.class)
    public ResponseEntity<ApiError> handler(ProductNotFound exception){
        return ApiErrors.notFound("product.not-found", exception.getLocalizedMessage());
    }
}
