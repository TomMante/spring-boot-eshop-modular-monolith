package com.github.eshopmodularmonolith;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConfiguration {

    // Spring Boot's own MongoAutoConfiguration backs off once any MongoDatabaseFactory bean
    // exists (each module defines its own), so it never creates a MongoClient. Provide it here instead.
    @Bean
    public MongoClient mongoClient(@Value("${spring.mongodb.uri}") String uri) {
        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(cfg -> {});
    }

    @Bean
    public MongoMappingContext mongoMappingContext(MongoCustomConversions mongoCustomConversions){
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(mongoCustomConversions.getSimpleTypeHolder());
        return context;
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoMappingContext mongoMappingContext, MongoCustomConversions mongoCustomConversions){
        var converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE, mongoMappingContext
        );
        converter.setCustomConversions(mongoCustomConversions);
        converter.afterPropertiesSet();
        return converter;
    }
}
