package com.github.eshopmodularmonolith;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConfiguration {

    // Spring Boot's own MongoAutoConfiguration backs off once any MongoDatabaseFactory bean
    // exists (each module defines its own), so it never creates a MongoClient. Provide it here instead.
    @Bean
    public MongoClient mongoClient(@Value("${spring.mongodb.uri:mongodb://localhost/test}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean
    public MongoMappingContext mongoMappingContext(){
        return new MongoMappingContext();
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoMappingContext mongoMappingContext){
        var converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE, mongoMappingContext
        );
        converter.afterPropertiesSet();
        return converter;
    }
}
