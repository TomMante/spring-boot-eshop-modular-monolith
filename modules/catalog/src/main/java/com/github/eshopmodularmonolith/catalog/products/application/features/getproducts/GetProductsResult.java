package com.github.eshopmodularmonolith.catalog.products.application.features.getproducts;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;

import java.util.List;

public record GetProductsResult(List<Product> products, int pageIndex, int pageSize, long totalItems, int totalPages) {
}
