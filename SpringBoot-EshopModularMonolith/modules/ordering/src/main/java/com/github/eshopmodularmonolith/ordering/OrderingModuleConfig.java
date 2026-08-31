package com.github.eshopmodularmonolith.ordering;

import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
@EnableMongoRepositories(
        basePackages = {"com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo"},
        mongoTemplateRef = "orderMongoTemplate"
)
@ComponentScan(basePackages = "com.github.eshopmodularmonolith.ordering.orders")
@EnableScheduling
public class OrderingModuleConfig {


    @Bean("orderMongoDatabaseFactory")
    public MongoDatabaseFactory orderMongoDatabaseFactory(MongoClient client) {
        return new SimpleMongoClientDatabaseFactory(client, "eshop-order-db");
    }

    @Bean("orderTransactionManager")
    public MongoTransactionManager orderTransactionManager(@Qualifier("orderMongoDatabaseFactory") MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean("orderMongoTemplate")
    public MongoTemplate mongoTemplate(@Qualifier("orderMongoDatabaseFactory") MongoDatabaseFactory dbFactory){
        return new MongoTemplate(dbFactory);
    }

    @Bean("orderClock")
    @ConditionalOnMissingBean
    public Clock orderClock() {
        return Clock.tickMillis(ZoneOffset.UTC);
    }
}
