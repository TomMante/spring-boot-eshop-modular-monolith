package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Order;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderRepository;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DeleteOrderHandlerTest {

    private OrderRepository repository;
    private DeleteOrderHandler handler;


    @BeforeEach
    void setUp() {
        repository = new FakeOrderRepository();
        handler = new DeleteOrderHandler(repository);
    }

    @Test
    @DisplayName("Given delete order command, when handler is called, then order is deleted")
    void givenDeleteOrderCommand_whenHandlerIsCalled_thenOrderIsDeleted() {
        var existingOrder = OrderMother.order();
        repository.save(existingOrder);
        assertThat(repository.findById(existingOrder.id())).isPresent().get().extracting(Order::id).isEqualTo(existingOrder.id());
        var cmd = new DeleteOrderCommand(new CurrentUser("john_doe"), existingOrder.id());
        handler.handle(cmd);
        assertThat(repository.findById(existingOrder.id())).isEmpty();
    }

    @Test
    @DisplayName("Given order placed by another customer, when handler is called, then order is not deleted")
    void givenOrderPlacedByAnotherCustomer_whenHandlerIsCalled_thenOrderIsNotDeleted() {
        var janesOrder = OrderMother.instance().withCustomer("jane_roe").build();
        repository.save(janesOrder);

        var cmd = new DeleteOrderCommand(new CurrentUser("john_doe"), janesOrder.id());

        assertThatExceptionOfType(OrderNotFound.class).isThrownBy(() -> handler.handle(cmd));
        assertThat(repository.findById(janesOrder.id())).isPresent();
    }

    @Test
    @DisplayName("Given no order with the given id, when handler is called, then order not found is thrown")
    void givenNoOrderWithGivenId_whenHandlerIsCalled_thenOrderNotFoundIsThrown() {
        var cmd = new DeleteOrderCommand(new CurrentUser("john_doe"), UUID.randomUUID());

        assertThatExceptionOfType(OrderNotFound.class).isThrownBy(() -> handler.handle(cmd));
    }

}