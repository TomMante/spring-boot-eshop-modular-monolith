package com.github.eshopmodularmonolith.catalog.products.application.features.getproducts;

import com.github.eshopmodularmonolith.catalog.products.contract.ProductMapper;
import com.github.eshopmodularmonolith.shared.contract.pagination.PaginatedResults;
import com.github.eshopmodularmonolith.shared.contract.pagination.PaginationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GetProductsController {

    private final GetProductsHandler handler;

    public GetProductsController(GetProductsHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/products")
    public ResponseEntity<GetProductsResponse> getProducts(@ModelAttribute PaginationRequest request) {
        var query = new GetProductsQuery(request.pageIndex(), request.pageSize());
        var result = handler.handle(query);
        var response = convertToResponse(result);
        return ResponseEntity.ok(response);
    }

    private GetProductsResponse convertToResponse(GetProductsResult result) {
        var pagedProducts = new PaginatedResults<>(result.products().stream().map(ProductMapper::toProductDTO).toList(), result.pageIndex(), result.pageSize(), result.totalItems(), result.totalPages());
        return new GetProductsResponse(pagedProducts);
    }
}
