package com.github.eshopmodularmonolith.catalog.products.application.features.getproductbyid;

import com.github.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
import com.github.eshopmodularmonolith.shared.contract.error.ApiError;
import com.github.eshopmodularmonolith.shared.contract.error.ApiErrors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.github.eshopmodularmonolith.catalog.products.application.features.getproductbyid")
public class GetProductByIdErrorHandler {

    @ExceptionHandler(ProductNotFound.class)
    public ResponseEntity<ApiError> handler(ProductNotFound exception){
        return ApiErrors.notFound("product.not-found", exception.getLocalizedMessage());
    }
}
