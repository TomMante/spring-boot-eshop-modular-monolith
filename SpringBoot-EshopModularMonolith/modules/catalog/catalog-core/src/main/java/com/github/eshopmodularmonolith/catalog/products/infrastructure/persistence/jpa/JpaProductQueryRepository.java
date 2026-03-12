package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.application.query.ProductQueryRepository;
import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Repository
public class JpaProductQueryRepository implements ProductQueryRepository {

    private EntityManager entityManager;

    public JpaProductQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public PageResult<ProductDTO> paginatedProducts(PageRequest pageRequest) {
        var orderBy = pageRequest.sortOrders().stream()
                .map(s -> "p." + s.property() + " " + s.direction().name())
                .collect(Collectors.joining(", "));

        var query = entityManager.createQuery("""
                        select distinct p
                        from ProductDB p
                        left join fetch p.categories
                        order by %s 
                        """.formatted(orderBy.isEmpty() ? "p.name" : orderBy)
                , ProductDB.class);

        query.setFirstResult(pageRequest.skip());
        query.setMaxResults(pageRequest.pageSize());
        var items = query.getResultList().stream().map(ProductMapper::toDto).toList();
        var total = countTotal();

        return PageResult.of(items, pageRequest.pageIndex(), pageRequest.pageSize(), total);
    }

    @Override
    public Optional<ProductDTO> findById(UUID id) {
        var query = entityManager.createQuery("""
                select p
                from ProductDB p
                where p.id = :id
                """, ProductDB.class);
        query.setParameter("id", id);
        return Optional.ofNullable(query.getSingleResultOrNull()).map(ProductMapper::toDto);
    }

    private long countTotal() {
        return entityManager.createQuery("select count(p) from ProductDB p", Long.class).getSingleResult();
    }
}
