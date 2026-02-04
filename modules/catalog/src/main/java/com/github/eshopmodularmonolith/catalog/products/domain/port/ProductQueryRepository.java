package com.github.eshopmodularmonolith.catalog.products.domain.port;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;

public interface ProductQueryRepository {

    PageResult<Product> paginatedProducts(PageRequest pageRequest);
}
