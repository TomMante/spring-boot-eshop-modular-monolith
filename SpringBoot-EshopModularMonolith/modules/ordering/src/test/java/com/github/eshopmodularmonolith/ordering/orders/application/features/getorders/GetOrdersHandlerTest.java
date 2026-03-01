package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetOrdersHandlerTest {


    private FakeOrderQueryRepository repository;
    private GetOrdersHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeOrderQueryRepository();
        handler = new GetOrdersHandler(repository);
    }

    @Test
    @DisplayName("Given get orders query, when handler is called, then get orders result is return")
    void givenGetOrdersQuery_whenHandlerIsCalled_thenGetOrdersResultIsReturned() {
        var order1 = DTOMapper.toDto(OrderMother.order());
        repository.addOrder(order1);
        var order2 = DTOMapper.toDto(OrderMother.order());
        repository.addOrder(order2);
        var order3 = DTOMapper.toDto(OrderMother.order());
        repository.addOrder(order3);

        var page1 = handler.handle(new GetOrdersQuery(0, 2));
        assertThat(page1.orders()).containsAnyElementsOf(List.of(order1, order2));
        assertThat(page1.pageIndex()).isZero();
        assertThat(page1.pageSize()).isEqualTo(2);
        assertThat(page1.totalItems()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);

        var page2 = handler.handle(new GetOrdersQuery(1, 2));
        assertThat(page2.orders()).containsAnyElementsOf(List.of(order3));
        assertThat(page2.pageIndex()).isOne();
        assertThat(page2.pageSize()).isEqualTo(2);
        assertThat(page2.totalItems()).isEqualTo(3);
        assertThat(page2.totalPages()).isEqualTo(2);

    }
}