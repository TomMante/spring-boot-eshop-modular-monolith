package com.github.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GetOrderByIdHandlerTest {

    private FakeOrderQueryRepository repository;
    private GetOrderByIdHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeOrderQueryRepository();
        handler = new GetOrderByIdHandler(repository);
    }

    @Test
    @DisplayName("Given order in database, when query by id is called, then correct order is retrieved")
    void givenOrderInDatabase_whenQueryByIdIsCalled_thenCorrectOrderIsRetrieved(){
        var orderDTO = DTOMapper.toDto(OrderMother.order());
        var query = new GetOrderByIdQuery(orderDTO.id());
        assertThat(repository.findById(orderDTO.id())).isEmpty();
        repository.addOrder(orderDTO);
        var order = handler.handle(query);
        var expectedResult = new GetOrderByIdResult(orderDTO);
        assertThat(order).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("Given GetOrderByIdQuery, when specified id is not present, then should throw an exception")
    void givenGetProductByIdQuery_whenSpecifiedIdIsNotPresent_thenShouldThrowAnException() {
        var query = new GetOrderByIdQuery(UUID.randomUUID());
        assertThatExceptionOfType(OrderNotFound.class).isThrownBy(() -> handler.handle(query));
    }
}