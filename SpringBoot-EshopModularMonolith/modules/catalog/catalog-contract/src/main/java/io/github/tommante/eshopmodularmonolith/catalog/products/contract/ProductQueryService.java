package io.github.tommante.eshopmodularmonolith.catalog.products.contract;

import java.util.Optional;
import java.util.UUID;

public interface ProductQueryService {

    Optional<ProductDTO> findProduct(UUID id);
}
