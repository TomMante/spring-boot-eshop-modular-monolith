package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging.CatalogOutboxEvents;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.CatalogDatabaseTest;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.JpaProductRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductDB;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.SpringJpaProductRepository;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventSubscriber;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.outboxjpa.JpaOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxjpa.OutboxEventEntity;
import io.github.tommante.eshopmodularmonolith.outboxjpa.OutboxEventJpaRepository;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.SpringDomainEventPublisher;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ContextConfiguration(classes = CreateProductHandlerTransactionalTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // This is required to prevent base @DataJpaTest transaction. If this was not there the @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) inside OutboxEventSubscriber would not be triggerd.
class CreateProductHandlerTransactionalTest extends CatalogDatabaseTest {

    @Configuration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @EntityScan(basePackageClasses = {ProductDB.class, OutboxEventEntity.class})
    @EnableJpaRepositories(basePackageClasses = {JpaProductRepository.class, OutboxEventJpaRepository.class})
    static class Config {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        DomainEventRegistry domainEventRegistry() {
            return new DomainEventRegistry();
        }

        @Bean
        OutboxSchemaRegistry schemaRegistry() {
            return new OutboxSchemaRegistry();
        }

        @Bean
        DomainEventPublisher domainEventPublisher(ApplicationEventPublisher publisher) {
            return new SpringDomainEventPublisher(publisher);
        }

        @Bean
        OutboxPayloadSerializer outboxPayloadSerializer(DomainEventRegistry registry, OutboxSchemaRegistry schemaRegistry, ObjectMapper mapper) {
            CatalogOutboxEvents.registerIn(registry, schemaRegistry);
            return new OutboxPayloadSerializer(registry, schemaRegistry, mapper);
        }

        @Bean
        OutboxEventStore catalogOutboxEventStore(OutboxEventJpaRepository repository) {
            return new JpaOutboxEventStore(repository);
        }

        @Bean
        OutboxEventSubscriber outboxEventSubscriber(OutboxEventStore outbox, OutboxPayloadSerializer serializer) {
            return new OutboxEventSubscriber(outbox, serializer);
        }

        @Bean
        JpaProductRepository jpaProductRepository(SpringJpaProductRepository repository, DomainEventPublisher publisher) {
            return new JpaProductRepository(repository, publisher);
        }

        @Bean
        CreateProductHandler createProductHandler(ProductRepository repository) {
            return new CreateProductHandler(repository);
        }

        @Bean(name = {"catalogTransactionManager", "transactionManager"})
        PlatformTransactionManager catalogTransactionManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }
    }

    @Autowired
    private CreateProductHandler handler;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoSpyBean(name = "catalogOutboxEventStore")
    private OutboxEventStore outboxEventStore;

    @Test
    @DisplayName("Given create product succeeds, when handling, then product is saved and event is stored in the outbox")
    void givenCreateProductSucceeds_thenProductSavedAndEventInOutbox() {
        var createProduct = new CreateProductCommand(
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        var productId = handler.handle(createProduct);

        assertThat(repository.findProductById(productId)).isPresent();
        assertThat(outboxEventJpaRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Given the outbox save fails, when handling, then the transaction rolls back and the product is not saved")
    void givenOutboxSaveFails_thenTransactionRollsBackAndProductNotSaved() {
        var createProduct = new CreateProductCommand(
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        doThrow(new RuntimeException("outbox down")).when(outboxEventStore).save(any());

        assertThatThrownBy(() -> handler.handle(createProduct))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("outbox down");

        assertThat(repository.existsByName(createProduct.name())).isFalse();
        assertThat(outboxEventJpaRepository.findAll()).isEmpty();
    }
}
