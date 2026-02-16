package com.github.eshopmodularmonolith.catalog.products.application.query;

import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;


public interface ProductQueryRepository {

    PageResult<ProductDTO> paginatedProducts(PageRequest pageRequest);
    Optional<ProductDTO> findById(UUID id);
}
