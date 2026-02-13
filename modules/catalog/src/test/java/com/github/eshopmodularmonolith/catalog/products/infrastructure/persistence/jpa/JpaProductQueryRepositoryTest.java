package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;


import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.CatalogDatabaseTest;
import com.github.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = JpaProductQueryRepositoryTest.Config.class)
@Import(value = {JpaProductQueryRepository.class, JpaProductWriteRepository.class})
class JpaProductQueryRepositoryTest extends CatalogDatabaseTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ProductDB.class})
    @EnableJpaRepositories(basePackageClasses = {JpaProductQueryRepository.class, JpaProductWriteRepository.class})
    static class Config {

    }

    @Autowired
    private JpaProductQueryRepository queryRepository;
    @Autowired
    private JpaProductWriteRepository writeRepository;
    @MockitoBean
    private DomainEventPublisher domainEventPublisher;
    @MockitoBean
    private OutboxEventStore outboxEventStore;


    @Test
    @DisplayName("Given database with products, when paginated products are requested, then paginated products are returned")
    void givenDatabaseWithProducts_whenPaginatedProductsAreRequested_thenPaginatedProductAreReturned() {
        var product1 = ProductMother.productWithName("product1");
        writeRepository.save(product1);
        var product2 = ProductMother.productWithName("product2");
        writeRepository.save(product2);
        var product3 = ProductMother.productWithName("product3");
        writeRepository.save(product3);

        var page1 = queryRepository.paginatedProducts(new PageRequest(0, 2, List.of(new SortOrder("name", SortOrder.Direction.ASC))));
        assertThat(page1.items()).containsExactlyInAnyOrderElementsOf(List.of(product1, product2));
        assertThat(page1.pageIndex()).isZero();
        assertThat(page1.pageSize()).isEqualTo(2);
        assertThat(page1.totalItems()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);

        var page2 = queryRepository.paginatedProducts(new PageRequest(1, 2, List.of(new SortOrder("name", SortOrder.Direction.ASC))));
        assertThat(page2.items()).containsExactlyInAnyOrderElementsOf(List.of(product3));
        assertThat(page2.pageIndex()).isOne();
        assertThat(page2.pageSize()).isEqualTo(2);
        assertThat(page2.totalItems()).isEqualTo(3);
        assertThat(page2.totalPages()).isEqualTo(2);

    }

    @Test
    @DisplayName("Given database with products, when find by id is called, then correct product is retrieved")
    void givenDatabaseWithProducts_whenFindByIdIsCalled_thenCorrectProductIsRetrieved(){
        var product1 = ProductMother.productWithName("product1");
        writeRepository.save(product1);
        var product2 = ProductMother.productWithName("product2");
        writeRepository.save(product2);

        var foundProduct = queryRepository.findById(product1.id());

        assertThat(foundProduct).isPresent().get().usingRecursiveComparison().isEqualTo(product1);
    }
}