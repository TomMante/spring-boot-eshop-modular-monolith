package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;


import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@ContextConfiguration(classes = JpaProductQueryRepositoryTest.Config.class)
@Import(value = {JpaProductQueryRepository.class, JpaProductWriteRepository.class})
@TestPropertySource(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class JpaProductQueryRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ProductDB.class})
    @EnableJpaRepositories(basePackageClasses = {JpaProductQueryRepository.class, JpaProductWriteRepository.class})
    static class Config {

    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaProductQueryRepository queryRepository;
    @Autowired
    private JpaProductWriteRepository writeRepository;


    @Test
    @DisplayName("Given existing database, when saving product, product is saved")
    void givenDatabaseWithProducts_whenPaginatedProductsAreRequested_thenPaginatedProductRequest() {
        var product1 = ProductMother.product();
        writeRepository.save(product1);
        var product2 = ProductMother.product();
        writeRepository.save(product2);
        var product3 = ProductMother.product();
        writeRepository.save(product3);

        var page1 = queryRepository.paginatedProducts(new PageRequest(0, 2));
        assertThat(page1.items()).containsAnyElementsOf(List.of(product1, product2));
        assertThat(page1.pageIndex()).isZero();
        assertThat(page1.pageSize()).isEqualTo(2);
        assertThat(page1.totalItems()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);

        var page2 = queryRepository.paginatedProducts(new PageRequest(1, 2));
        assertThat(page2.items()).containsAnyElementsOf(List.of(product3));
        assertThat(page2.pageIndex()).isOne();
        assertThat(page2.pageSize()).isEqualTo(2);
        assertThat(page2.totalItems()).isEqualTo(3);
        assertThat(page2.totalPages()).isEqualTo(2);

    }
}