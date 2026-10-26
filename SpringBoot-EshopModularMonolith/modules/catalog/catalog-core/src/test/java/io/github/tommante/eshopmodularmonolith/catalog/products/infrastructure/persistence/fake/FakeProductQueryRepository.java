package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake;

import io.github.tommante.eshopmodularmonolith.catalog.products.application.query.ProductQueryRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.*;

public class FakeProductQueryRepository implements ProductQueryRepository {

    private Map<UUID, ProductDTO> products;

    public FakeProductQueryRepository() {
        this.products = new LinkedHashMap<>();
    }

    public void addProduct(ProductDTO product){
        products.put(product.id(), product);
    }

    @Override
    public PageResult<ProductDTO> paginatedProducts(PageRequest pageRequest) {
        List<ProductDTO> allProducts = new ArrayList<>(products.values());

        int pageIndex = pageRequest.pageIndex();
        int pageSize = pageRequest.pageSize();

        int fromIndex = pageIndex * pageSize;
        if (fromIndex >= allProducts.size()) {
            return new PageResult<>(Collections.emptyList(), pageIndex, pageSize, allProducts.size(),
                    (int) Math.ceil((double) allProducts.size() / pageSize));
        }

        int toIndex = Math.min(fromIndex + pageSize, allProducts.size());

        List<ProductDTO> pageItems = allProducts.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) allProducts.size() / pageSize);

        return new PageResult<>(pageItems, pageIndex, pageSize, allProducts.size(), totalPages);
    }

    @Override
    public Optional<ProductDTO> findById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }
}