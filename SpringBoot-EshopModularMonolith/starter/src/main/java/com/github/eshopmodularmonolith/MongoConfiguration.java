package com.github.eshopmodularmonolith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConfiguration {

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
