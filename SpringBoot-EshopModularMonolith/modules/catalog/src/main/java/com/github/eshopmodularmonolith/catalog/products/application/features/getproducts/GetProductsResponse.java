package com.github.eshopmodularmonolith.catalog.products.application.features.getproducts;


import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import com.github.eshopmodularmonolith.shared.contract.pagination.PaginatedResults;

public record GetProductsResponse(PaginatedResults<ProductDTO> pagedProducts) {
}
