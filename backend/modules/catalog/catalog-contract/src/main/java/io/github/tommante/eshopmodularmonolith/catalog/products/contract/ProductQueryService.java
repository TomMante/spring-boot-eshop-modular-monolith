package io.github.tommante.eshopmodularmonolith.catalog.products.contract;

import io.github.tommante.eshopmodularmonolith.catalog.products.contract.dto.ProductDTO;

import java.util.Optional;
import java.util.UUID;

public interface ProductQueryService {

    Optional<ProductDTO> findProduct(UUID id);
}
