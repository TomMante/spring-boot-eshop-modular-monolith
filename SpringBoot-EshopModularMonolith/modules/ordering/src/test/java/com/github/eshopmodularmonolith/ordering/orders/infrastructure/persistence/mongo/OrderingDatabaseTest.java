package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@DataMongoTest
@Testcontainers
@EnableMongoAuditing
@TestPropertySource(properties = {
        "spring.mongodb.representation.uuid=java_legacy",
        "spring.data.mongodb.representation.big-decimal=decimal128"
})
public abstract class OrderingDatabaseTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDb = new MongoDBContainer("mongo:7.0.6");

    @Autowired
    protected MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
    }

}
