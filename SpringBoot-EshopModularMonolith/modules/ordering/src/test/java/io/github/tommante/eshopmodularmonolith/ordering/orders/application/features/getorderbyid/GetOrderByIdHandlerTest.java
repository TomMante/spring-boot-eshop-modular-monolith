package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import io.github.tommante.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderQueryRepository;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
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
        var query = new GetOrderByIdQuery(new CurrentUser("john_doe"), orderDTO.id());
        assertThat(repository.findVisibleTo(new CurrentUser("john_doe"), orderDTO.id())).isEmpty();
        repository.addOrder(orderDTO);
        var order = handler.handle(query);
        var expectedResult = new GetOrderByIdResult(orderDTO);
        assertThat(order).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("Given GetOrderByIdQuery, when specified id is not present, then should throw an exception")
    void givenGetProductByIdQuery_whenSpecifiedIdIsNotPresent_thenShouldThrowAnException() {
        var query = new GetOrderByIdQuery(new CurrentUser("john_doe"), UUID.randomUUID());
        assertThatExceptionOfType(OrderNotFound.class).isThrownBy(() -> handler.handle(query));
    }

    @Test
    @DisplayName("Given order placed by another customer, when query by id is called, then should throw an exception")
    void givenOrderPlacedByAnotherCustomer_whenQueryByIdIsCalled_thenShouldThrowAnException() {
        var janesOrder = DTOMapper.toDto(OrderMother.instance().withCustomer("jane_roe").build());
        repository.addOrder(janesOrder);

        var query = new GetOrderByIdQuery(new CurrentUser("john_doe"), janesOrder.id());

        assertThatExceptionOfType(OrderNotFound.class).isThrownBy(() -> handler.handle(query));
    }
}