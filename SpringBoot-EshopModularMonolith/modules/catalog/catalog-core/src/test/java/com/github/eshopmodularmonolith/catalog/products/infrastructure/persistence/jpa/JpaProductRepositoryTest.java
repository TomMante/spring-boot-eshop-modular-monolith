package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;


import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = JpaProductRepositoryTest.Config.class)
@Import(JpaProductRepository.class)
class JpaProductRepositoryTest extends CatalogDatabaseTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ProductDB.class})
    @EnableJpaRepositories(basePackageClasses = JpaProductRepository.class)
    static class Config {

    }

    @MockitoBean
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private JpaProductRepository repository;


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
    @DisplayName("Given existing product in database, when delete product is called, then product is removed from database")
    void givenExistingProductInDatabases_whenDeleteProductIsCalled_thenProductIsRemovedFromDatabase(){
        var product = ProductMother.product();
        assertThat(repository.findProductById(product.id())).isEmpty();
        repository.save(product);
        assertThat(repository.findProductById(product.id())).isPresent();
        repository.delete(product.id());
        assertThat(repository.findProductById(product.id())).isEmpty();
    }

    @Test
    @DisplayName("Given existing products, when existingByName is called, then repository returns if product with provided name exists.")
    void givenExistingProducts_whenExistsByNameIsCalled_thenRepositoryReturnsIfProductWithProvidedNameExists() {
        var product = ProductMother.product();
        assertThat(repository.existsByName(product.name())).isFalse();
        repository.save(product);
        assertThat(repository.existsByName(product.name())).isTrue();
    }

    @Test
    @DisplayName("Given product with attached domain event, when saving, then domain events are dispatched")
    void givenProductWithAttachedDomainEvent_whenSaving_thenDomainEventsAreDispatched(){
        var product = Product.create(UUID.randomUUID(), "Galaxy", List.of("phone", "communication"), "nice one", "galaxy.png", BigDecimal.valueOf(560));
        var events = new ArrayList<>(product.domainEvents());
        repository.save(product);

        verify(domainEventPublisher, times(1)).publish(events);
    }
}