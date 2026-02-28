package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.OrderingDatabaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ContextConfiguration(classes = MongoOrderQueryRepositoryTest.Config.class)
@Import(value = {MongoOrderRepository.class, MongoOrderQueryRepository.class})
class MongoOrderQueryRepositoryTest extends OrderingDatabaseTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {OrderDB.class})
    @EnableMongoRepositories(basePackageClasses = {MongoOrderRepository.class, MongoOrderQueryRepository.class})
    static class Config {
    }

    @Autowired
    private OrderQueryRepository queryRepository;
    @Autowired
    private OrderRepository repository;

    @Test
    @DisplayName("Given existing orders, when querying by id, then return correct order")
    void givenExistingOrders_whenQueryingById_thenReturnCorrectOrder(){
        var order1 = OrderMother.order("First Order");
        var order2 = OrderMother.order("Second Order");
        repository.save(order1);
        repository.save(order2);
        assertThat(queryRepository.findById(order1.id())).isPresent()
                .get().usingRecursiveComparison()
                .isEqualTo(DTOMapper.toDto(order1));
        assertThat(queryRepository.findById(order2.id())).isPresent()
                .get().usingRecursiveComparison()
                .isEqualTo(DTOMapper.toDto(order2));
    }
}