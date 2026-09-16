package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.getproducts;


import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import io.github.tommante.eshopmodularmonolith.shared.contract.pagination.PaginatedResults;

public record GetProductsResponse(PaginatedResults<ProductDTO> pagedProducts) {
}
