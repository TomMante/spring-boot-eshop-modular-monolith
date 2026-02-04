package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductQueryRepository;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FakeProductQueryRepository implements ProductQueryRepository {

    private Map<UUID, Product> products;

    public FakeProductQueryRepository() {
        this.products = new LinkedHashMap<>();
    }

    public void addProduct(Product product){
        products.put(product.id(), product);
    }

    @Override
    public PageResult<Product> paginatedProducts(PageRequest pageRequest) {
        List<Product> allProducts = new ArrayList<>(products.values());

        int pageIndex = pageRequest.pageIndex();
        int pageSize = pageRequest.pageSize();

        int fromIndex = pageIndex * pageSize;
        if (fromIndex >= allProducts.size()) {
            return new PageResult<>(Collections.emptyList(), pageIndex, pageSize, allProducts.size(),
                    (int) Math.ceil((double) allProducts.size() / pageSize));
        }

        int toIndex = Math.min(fromIndex + pageSize, allProducts.size());

        List<Product> pageItems = allProducts.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) allProducts.size() / pageSize);

        return new PageResult<>(pageItems, pageIndex, pageSize, allProducts.size(), totalPages);
    }
}