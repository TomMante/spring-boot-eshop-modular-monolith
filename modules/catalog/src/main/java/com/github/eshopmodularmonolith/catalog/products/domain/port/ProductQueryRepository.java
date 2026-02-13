package com.github.eshopmodularmonolith.catalog.products.domain.port;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;

//FIXME this split does not make sense like this. REad model should not be part of the Domain.
public interface ProductQueryRepository {

    PageResult<Product> paginatedProducts(PageRequest pageRequest);
    Optional<Product> findById(UUID id);
}
