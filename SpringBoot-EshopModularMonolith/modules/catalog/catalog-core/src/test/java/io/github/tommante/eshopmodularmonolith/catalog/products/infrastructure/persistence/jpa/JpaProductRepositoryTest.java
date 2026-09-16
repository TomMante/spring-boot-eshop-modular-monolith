package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;


import io.github.tommante.eshopmodularmonolith.catalog.products.domain.ProductMother;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.Product;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = JpaProductRepositoryTest.Config.class)
@Import(JpaProductRepository.class)
class JpaProductRepositoryTest extends CatalogDatabaseTest {

    private static final String CONFIGURED_AUDITOR = "test-auditor";

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ProductDB.class})
    @EnableJpaRepositories(basePackageClasses = JpaProductRepository.class)
    static class Config {

        @Bean(name = {"catalogTransactionManager", "transactionManager"})
        PlatformTransactionManager catalogTransactionManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }

        // Without an auditor, @CreatedBy is null on every row and any assertion about it holds vacuously.
        @Bean
        AuditorAware<String> auditorProvider() {
            return () -> Optional.of(CONFIGURED_AUDITOR);
        }
    }

    @MockitoBean
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private JpaProductRepository repository;

    @PersistenceContext
    private EntityManager entityManager;


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

    @Test
    @DisplayName("Given existing product in database, when saving an updated product, then the creation audit columns are preserved")
    void givenExistingProductInDatabase_whenSavingAnUpdatedProduct_thenTheCreationAuditColumnsArePreserved() {
        var product = ProductMother.product();
        repository.save(product);
        var rowAfterInsert = storedRowFor(product.id());

        var updatedProduct = product.update("Galaxy Ultra", product.categories(), product.description(),
                product.imageFile(), product.price());
        repository.save(updatedProduct);
        var rowAfterUpdate = storedRowFor(product.id());

        assertThat(rowAfterInsert.createdBy()).isEqualTo(CONFIGURED_AUDITOR);
        assertThat(rowAfterUpdate.name()).isEqualTo("Galaxy Ultra");
        assertThat(rowAfterUpdate.createdAt()).isEqualTo(rowAfterInsert.createdAt());
        assertThat(rowAfterUpdate.createdBy()).isEqualTo(rowAfterInsert.createdBy());
    }

    // Reads the stored columns rather than the entity: merge copies the detached projection's null audit
    // fields onto the managed instance, so the persistence context reports nulls the database never took.
    private StoredProductRow storedRowFor(UUID productId) {
        entityManager.flush();
        var columns = (Object[]) entityManager
                .createNativeQuery("select created_at, created_by, name from catalog.products where id = :productId")
                .setParameter("productId", productId)
                .getSingleResult();
        return new StoredProductRow((LocalDateTime) columns[0], (String) columns[1], (String) columns[2]);
    }

    private record StoredProductRow(LocalDateTime createdAt, String createdBy, String name) {
    }
}
