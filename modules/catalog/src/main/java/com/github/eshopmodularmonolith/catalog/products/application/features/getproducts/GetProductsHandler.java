package com.github.eshopmodularmonolith.catalog.products.application.features.getproducts;

import com.github.eshopmodularmonolith.catalog.products.application.query.ProductQueryRepository;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetProductsHandler {

    private final ProductQueryRepository repository;

    public GetProductsHandler(ProductQueryRepository repository) {
        this.repository = repository;
    }

    public GetProductsResult handle(GetProductsQuery query){
        var result = repository.paginatedProducts(new PageRequest(query.pageIndex(), query.pageSize(), List.of(new SortOrder("name", SortOrder.Direction.ASC))));
        return new GetProductsResult(result.items(), result.pageIndex(), result.pageSize(), result.totalItems(), result.totalPages());
    }
}
