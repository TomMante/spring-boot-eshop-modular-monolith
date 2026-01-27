package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;


import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.exceptions.ProductNotFound;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Testcontainers
@DataJpaTest
@ContextConfiguration(classes = JpaProductWriteRepositoryTest.Config.class)
@Import(JpaProductWriteRepository.class)
@TestPropertySource(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class JpaProductWriteRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ProductDB.class})
    @EnableJpaRepositories(basePackageClasses = JpaProductWriteRepository.class)
    static class Config {

    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaProductWriteRepository repository;


    @Test
    @DisplayName("Given existing database, when saving product, product is saved")
    void givenExistingDatabase_whenSavingProduct_thenProductIsSaved() {
        var product = Product.create(UUID.randomUUID(),
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750));
        assertThatExceptionOfType(ProductNotFound.class).isThrownBy(() -> repository.getProductById(product.id()));
        repository.save(product);
        var result = repository.getProductById(product.id());
        assertThat(result).usingRecursiveComparison()
                .isEqualTo(product);
    }
}