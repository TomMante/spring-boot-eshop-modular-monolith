package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.PaymentMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.BasketDatabaseTest;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.MongoBasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.SpringMongoBasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.ShoppingCartDB;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventSubscriber;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxmongo.MongoOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxmongo.OutboxEventDocument;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.SpringDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = CheckoutBasketHandlerTransactionalTest.Config.class)
class CheckoutBasketHandlerTransactionalTest extends BasketDatabaseTest {

    @Configuration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @EntityScan(basePackageClasses = ShoppingCartDB.class)
    @EnableMongoRepositories(basePackageClasses = MongoBasketRepository.class)
    static class Config {

        @Bean("basketTransactionManager")
        MongoTransactionManager basketTransactionManager(MongoDatabaseFactory dbFactory) {
            return new MongoTransactionManager(dbFactory);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        DomainEventRegistry domainEventRegistry() {
            var registry = new DomainEventRegistry();
            registry.registerDomainEvent("BasketCheckedOut", BasketCheckedOut.class);
            return registry;
        }

        @Bean
        DomainEventPublisher domainEventPublisher(ApplicationEventPublisher publisher) {
            return new SpringDomainEventPublisher(publisher);
        }

        @Bean
        OutboxPayloadSerializer outboxPayloadSerializer(DomainEventRegistry registry, ObjectMapper mapper) {
            return new OutboxPayloadSerializer(registry, mapper);
        }

        @Bean
        OutboxEventStore mongoOutboxEventStore(MongoTemplate mongoTemplate) {
            return new MongoOutboxEventStore(mongoTemplate);
        }

        @Bean
        OutboxEventSubscriber outboxEventSubscriber(OutboxEventStore outbox, OutboxPayloadSerializer serializer) {
            return new OutboxEventSubscriber(outbox, serializer);
        }

        @Bean
        MongoBasketRepository mongoBasketRepository(SpringMongoBasketRepository repository,
                                                    DomainEventPublisher publisher) {
            return new MongoBasketRepository(repository, publisher);
        }

        @Bean
        CheckoutBasketHandler checkoutBasketHandler(BasketRepository repository, ProductService productService) {
            return new CheckoutBasketHandler(repository, productService);
        }
    }

    @Autowired
    private CheckoutBasketHandler handler;
    @Autowired
    private BasketRepository basketRepository;
    @Autowired
    private OutboxPayloadSerializer serializer;
    @MockitoBean
    private ProductService productService;
    @MockitoSpyBean
    private OutboxEventStore outboxEventStore;

    private Item item;
    private String userName;

    @BeforeEach
    void setUpFixtures() {
        userName = "John";
        item = new Item(UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("400.00"), "hat");
        when(productService.fetchProductInfo(item.productId()))
                .thenReturn(Optional.of(new ProductInfo(item.productId(), "hat", new BigDecimal("783.00"))));
    }

    private void seedBasket() {
        basketRepository.save(ShoppingCartMother.create().withItems(List.of(item)).build());
    }

    @Test
    @DisplayName("Given checkout succeeds, when handling, then basket is removed and event is stored in the outbox")
    void givenCheckoutSucceeds_thenBasketRemovedAndEventInOutbox() {
        seedBasket();
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment();

        handler.handle(new CheckoutBasketCommand(userName, billingAddress, payment, shippingAddress));

        assertThat(basketRepository.findByUsername(userName)).isEmpty();
        assertThat(mongoTemplate.findAll(OutboxEventDocument.class)).hasSize(1);
        assertThat(outboxEventStore.fetchDue(Instant.now()))
                .singleElement()
                .extracting(due -> serializer.read(due.event()))
                .isInstanceOfSatisfying(BasketCheckedOut.class, event -> {
                    assertThat(event.billingAddress()).isEqualTo(billingAddress);
                    assertThat(event.shippingAddress()).isEqualTo(shippingAddress);
                    assertThat(event.payment()).isEqualTo(payment);
                });
    }

    @Test
    @DisplayName("Given the outbox save fails, when handling, then the transaction rolls back and the basket is not removed")
    void givenOutboxSaveFails_thenTransactionRollsBackAndBasketRemains() {
        seedBasket();
        doThrow(new RuntimeException("outbox down")).when(outboxEventStore).save(any());

        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment();

        assertThatThrownBy(() -> handler.handle(new CheckoutBasketCommand(userName, billingAddress, payment, shippingAddress)))
                .isInstanceOf(RuntimeException.class);

        assertThat(basketRepository.findByUsername(userName)).isPresent();
        assertThat(mongoTemplate.findAll(OutboxEventDocument.class)).isEmpty();
    }
}
