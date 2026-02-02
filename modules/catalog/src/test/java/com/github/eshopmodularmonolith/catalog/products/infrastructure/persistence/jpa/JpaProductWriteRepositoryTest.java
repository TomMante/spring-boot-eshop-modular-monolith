package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;


import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
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

import static org.assertj.core.api.Assertions.assertThat;

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
        var product = ProductMother.product();
        assertThat(repository.findProductById(product.id())).isEmpty();
        repository.save(product);
        var result = repository.findProductById(product.id());
        assertThat(result).isPresent().get().usingRecursiveComparison()
                .isEqualTo(product);
    }

    @Test
    @DisplayName("Given existing product in database, when delete product is called, then product is removed forom database")
    void givenExistingProductInDatabases_whenDeleteProductIsCalled_thenProductIsRemovedFromDatabase(){
        var product = ProductMother.product();
        assertThat(repository.findProductById(product.id())).isEmpty();
        repository.save(product);
        assertThat(repository.findProductById(product.id())).isPresent();
        repository.delete(product.id());
        assertThat(repository.findProductById(product.id())).isEmpty();
    }
}