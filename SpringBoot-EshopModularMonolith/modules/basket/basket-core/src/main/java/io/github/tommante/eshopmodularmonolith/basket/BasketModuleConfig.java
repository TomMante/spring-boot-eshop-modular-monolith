package io.github.tommante.eshopmodularmonolith.basket;

import com.mongodb.client.MongoClient;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.outboxmongo.MongoOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxmongo.OutboxDocumentMapper;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.OutboxEventSubscriber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMongoRepositories(
        basePackages = {"io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo"},
        mongoTemplateRef = "basketMongoTemplate"
)
@ComponentScan(basePackages = "io.github.tommante.eshopmodularmonolith.basket.basket")
@EnableScheduling
public class BasketModuleConfig {

    private final DomainEventRegistry registry;

    public BasketModuleConfig(DomainEventRegistry registry) {
        this.registry = registry;
    }

    //Contributor pattern: the module registers the domain events it owns so the outbox can (de)serialize them.
    @PostConstruct
    private void postConstruct() {
        registry.registerDomainEvent("BasketCheckedOut", BasketCheckedOut.class);
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

    @Bean("basketOutboxDocumentMapper")
    public OutboxDocumentMapper outboxDocumentMapper(DomainEventRegistry domainEventRegistry, ObjectMapper mapper) {
        return new OutboxDocumentMapper(domainEventRegistry, mapper);
    }

    @Bean("basketOutboxEventStore")
    public OutboxEventStore mongoOutboxEventStore(@Qualifier("basketMongoTemplate") MongoTemplate mongoTemplate,
                                                  @Qualifier("basketOutboxDocumentMapper") OutboxDocumentMapper outboxDocumentMapper) {
        return new MongoOutboxEventStore(mongoTemplate, outboxDocumentMapper);
    }

    @Bean("basketOutboxEventSubscriber")
    public OutboxEventSubscriber outboxEventSubscriber(@Qualifier("basketOutboxEventStore") OutboxEventStore outboxEventStore) {
        return new OutboxEventSubscriber(outboxEventStore);
    }

}
