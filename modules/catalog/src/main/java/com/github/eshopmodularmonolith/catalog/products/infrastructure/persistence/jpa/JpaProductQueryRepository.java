package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductQueryRepository;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProductQueryRepository implements ProductQueryRepository {

    private SpringJpaProductRepository repository;

    public JpaProductQueryRepository(SpringJpaProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<Product> paginatedProducts(PageRequest pageRequest) {
        var sorts = Sort.by(pageRequest.sortOrders().stream().map(s -> new Sort.Order(Sort.Direction.valueOf(s.direction().name()), s.property())).toList());
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.pageIndex(), pageRequest.pageSize()).withSort(sorts);
        var pages = repository.findAll(pageable);
        return new PageResult<>(pages.get().map(ProductMapper::toDomain).toList(), pages.getNumber(), pages.getSize(), pages.getTotalElements(), pages.getTotalPages());
    }
}
