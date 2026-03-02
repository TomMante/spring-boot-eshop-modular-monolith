package com.github.eshopmodularmonolith.basket;

import com.mongodb.client.MongoClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

@Configuration
@EnableMongoRepositories(
        basePackages = {"com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo"}
)
@EnableMongoAuditing
@ComponentScan(basePackages = "com.github.eshopmodularmonolith.basket.basket")
@EnableScheduling
public class BasketModuleConfig {

    @Bean
    @ConditionalOnBean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient client){
        return new MongoTemplate(client, "eshop-basket-db");
    }
}
