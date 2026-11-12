package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.getproducts;

import io.github.tommante.eshopmodularmonolith.catalog.products.contract.dto.ProductDTO;

import java.util.List;

public record GetProductsResult(List<ProductDTO> products, int pageIndex, int pageSize, long totalItems, int totalPages) {
}
