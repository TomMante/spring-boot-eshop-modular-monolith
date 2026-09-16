package io.github.tommante.eshopmodularmonolith.catalog.products.application.query;

import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProductCatalog implements ProductQueryService {

    private ProductQueryRepository repository;

    public ProductCatalog(ProductQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ProductDTO> findProduct(UUID id) {
        return repository.findById(id);
    }
}
