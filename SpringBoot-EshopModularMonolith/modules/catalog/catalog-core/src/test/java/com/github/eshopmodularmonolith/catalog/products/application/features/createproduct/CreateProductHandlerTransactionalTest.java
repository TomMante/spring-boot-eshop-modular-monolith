package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.CatalogDatabaseTest;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.JpaProductRepository;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductDB;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.SpringJpaProductRepository;
import com.github.eshopmodularmonolith.outboxjpa.JpaOutboxEventStore;
import com.github.eshopmodularmonolith.outboxjpa.OutboxEventEntity;
import com.github.eshopmodularmonolith.outboxjpa.OutboxEventJpaRepository;
import com.github.eshopmodularmonolith.outboxjpa.OutboxEventMapper;
import com.github.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventSubscriber;
import com.github.eshopmodularmonolith.shared.infrastructure.SpringDomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
            var registry = new DomainEventRegistry();
            registry.registerDomainEvent("ProductCreated", com.github.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated.class);
            return registry;
        }

        @Bean
        DomainEventPublisher domainEventPublisher(ApplicationEventPublisher publisher) {
            return new SpringDomainEventPublisher(publisher);
        }

        @Bean
        OutboxEventMapper outboxEventMapper(DomainEventRegistry registry, ObjectMapper mapper) {
            return new OutboxEventMapper(registry, mapper);
        }

        @Bean
        OutboxEventStore catalogOutboxEventStore(OutboxEventJpaRepository repository, OutboxEventMapper mapper) {
            return new JpaOutboxEventStore(repository, mapper);
        }

        @Bean
        OutboxEventSubscriber outboxEventSubscriber(OutboxEventStore outbox) {
            return new OutboxEventSubscriber(outbox);
        }

        @Bean
        JpaProductRepository jpaProductRepository(SpringJpaProductRepository repository, DomainEventPublisher publisher) {
            return new JpaProductRepository(repository, publisher);
        }

        @Bean
        CreateProductHandler createProductHandler(ProductRepository repository) {
            return new CreateProductHandler(repository);
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
