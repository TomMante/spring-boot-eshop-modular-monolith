package io.github.tommante.eshopmodularmonolith.basket;

import com.mongodb.client.MongoClient;
import com.stripe.StripeClient;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.PaymentGateway;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.payment.StripePaymentGateway;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging.BasketOutboxEvents;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventSubscriber;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.outboxmongo.MongoOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableMongoRepositories(
        basePackages = {"io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo"},
        mongoTemplateRef = "basketMongoTemplate"
)
@ComponentScan(basePackages = "io.github.tommante.eshopmodularmonolith.basket.basket")
@EnableScheduling
public class BasketModuleConfig {

    private final DomainEventRegistry registry;
    private final OutboxSchemaRegistry schemaRegistry;

    public BasketModuleConfig(DomainEventRegistry registry, OutboxSchemaRegistry schemaRegistry) {
        this.registry = registry;
        this.schemaRegistry = schemaRegistry;
    }

    //Contributor pattern: the module registers the domain events it owns so the outbox can (de)serialize them.
    @PostConstruct
    private void postConstruct() {
        BasketOutboxEvents.registerIn(registry, schemaRegistry);
    }

    @Bean("basketMongoDatabaseFactory")
    public MongoDatabaseFactory basketMongoDatabaseFactory(MongoClient client) {
        return new SimpleMongoClientDatabaseFactory(client, "eshop-basket-db");
    }

    @Bean("basketTransactionManager")
    public MongoTransactionManager basketTransactionManager(@Qualifier("basketMongoDatabaseFactory") MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean("basketMongoTemplate")
    public MongoTemplate mongoTemplate(@Qualifier("basketMongoDatabaseFactory") MongoDatabaseFactory dbFactory){
        return new MongoTemplate(dbFactory);
    }

    @Bean("basketOutboxEventStore")
    public OutboxEventStore mongoOutboxEventStore(@Qualifier("basketMongoTemplate") MongoTemplate mongoTemplate) {
        return new MongoOutboxEventStore(mongoTemplate);
    }

    @Bean("basketOutboxEventSubscriber")
    public OutboxEventSubscriber outboxEventSubscriber(@Qualifier("basketOutboxEventStore") OutboxEventStore outboxEventStore, OutboxPayloadSerializer serializer) {
        return new OutboxEventSubscriber(outboxEventStore, serializer);
    }

    @Bean("basketTransactionOperations")
    public TransactionOperations basketTransactionOperations(@Qualifier("basketTransactionManager") PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean("basketStripeClient")
    public StripeClient basketStripeClient(@Value("${eshop.basket.payment.stripe.api-base}") String apiBase,
                                           @Value("${eshop.basket.payment.stripe.api-key}") String apiKey) {
        return StripeClient.builder()
                .setApiKey(apiKey)
                .setApiBase(apiBase)
                .setConnectTimeout(2_000)
                .setReadTimeout(10_000)
                .setMaxNetworkRetries(2)
                .build();
    }

    @Bean("basketPaymentGateway")
    public PaymentGateway basketPaymentGateway(@Qualifier("basketStripeClient") StripeClient stripeClient,
                                               @Value("${eshop.basket.payment.currency}") String currency) {
        return new StripePaymentGateway(stripeClient, currency);
    }

}
