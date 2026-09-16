package io.github.tommante.eshopmodularmonolith.catalog.products.application.query;

import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;


public interface ProductQueryRepository {

    PageResult<ProductDTO> paginatedProducts(PageRequest pageRequest);
    Optional<ProductDTO> findById(UUID id);
}
