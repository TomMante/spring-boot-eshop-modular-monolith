package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.OrderDB;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

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
    private OrderRepository writeRepository;

    @Test
    @DisplayName("Given existing orders, when querying by id, then return correct order")
    void givenExistingOrders_whenQueryingById_thenReturnCorrectOrder(){
        var order1 = OrderMother.order("First Order");
        var order2 = OrderMother.order("Second Order");
        writeRepository.save(order1);
        writeRepository.save(order2);
        assertThat(queryRepository.findById(order1.id())).isPresent()
                .get().usingRecursiveComparison()
                .isEqualTo(DTOMapper.toDto(order1));
        assertThat(queryRepository.findById(order2.id())).isPresent()
                .get().usingRecursiveComparison()
                .isEqualTo(DTOMapper.toDto(order2));
    }

    @Test
    @DisplayName("Given database with orders, when paginated orders are requested, then paginated orders are returned")
    void givenDatabaseWithOrders_whenPaginatedOrdersAreRequested_thenPaginatedOrdersAreReturned() {
        var order1 = OrderMother.order("Order 1");
        writeRepository.save(order1);
        var order2 = OrderMother.order("Order 2");
        writeRepository.save(order2);
        var order3 = OrderMother.order("Order 3");
        writeRepository.save(order3);

        var expected = List.of(DTOMapper.toDto(order1), DTOMapper.toDto(order2));

        var page1 = queryRepository.paginatedOrders(new PageRequest(0, 2, List.of(new SortOrder("name", SortOrder.Direction.ASC))));
        Assertions.assertThat(page1.items().stream().toList())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expected);
        Assertions.assertThat(page1.pageIndex()).isZero();
        Assertions.assertThat(page1.pageSize()).isEqualTo(2);
        Assertions.assertThat(page1.totalItems()).isEqualTo(3);
        Assertions.assertThat(page1.totalPages()).isEqualTo(2);

        var page2 = queryRepository.paginatedOrders(new PageRequest(1, 2, List.of(new SortOrder("name", SortOrder.Direction.ASC))));
        Assertions.assertThat(page2.items())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(List.of(DTOMapper.toDto(order3)));
        Assertions.assertThat(page2.pageIndex()).isOne();
        Assertions.assertThat(page2.pageSize()).isEqualTo(2);
        Assertions.assertThat(page2.totalItems()).isEqualTo(3);
        Assertions.assertThat(page2.totalPages()).isEqualTo(2);

    }
}