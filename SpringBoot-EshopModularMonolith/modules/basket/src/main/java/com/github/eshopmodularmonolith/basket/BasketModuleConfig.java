package com.github.eshopmodularmonolith.basket;

import com.mongodb.client.MongoClient;
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

@Configuration
@EnableMongoRepositories(
        basePackages = {"com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo"},
        mongoTemplateRef = "basketMongoTemplate"
)
@ComponentScan(basePackages = "com.github.eshopmodularmonolith.basket.basket")
@EnableScheduling
public class BasketModuleConfig {

    @Bean("basketMongoDatabaseFactory")
    public MongoDatabaseFactory basketMongoDatabaseFactory(MongoClient client) {
        return new SimpleMongoClientDatabaseFactory(client, "eshop-basket-db");
    }

    @Bean("basketTransactionManager")
    public MongoTransactionManager basketTransactionManager(@Qualifier("basketMongoDatabaseFactory") MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean("basketMongoTemplate")
    public MongoTemplate mongoTemplate(MongoClient client){
        return new MongoTemplate(client, "eshop-basket-db");
    }

}
