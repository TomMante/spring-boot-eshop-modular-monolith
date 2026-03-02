package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.OrderDB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;


@ContextConfiguration(classes = MongoOrderRepositoryTest.Config.class)
@Import(MongoOrderRepository.class)
class MongoOrderRepositoryTest extends OrderingDatabaseTest {


    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {OrderDB.class})
    @EnableMongoRepositories(basePackageClasses = MongoOrderRepository.class)
    static class Config {

    }

    @Autowired
    private MongoOrderRepository repository;


    @Test
    @DisplayName("Given existing database, when saving order, then order is saved")
    void givenExistingDatabase_whenSavingOrder_thenOrderIsSaved(){
        var order = OrderMother.order();
        assertThat(repository.findById(order.id())).isEmpty();
        repository.save(order);
        assertThat(repository.findById(order.id())).isPresent().get()
                .usingRecursiveComparison().isEqualTo(order);
    }

    @Test
    @DisplayName("Given existing database, when deleting order, then order is deleted")
    void givenExistingDatabase_whenDeletingOrder_thenOrderIsDeleted() {
        var order = OrderMother.order();
        repository.save(order);
        assertThat(repository.findById(order.id())).isPresent();
        repository.delete(order.id());
        assertThat(repository.findById(order.id())).isEmpty();
    }

}