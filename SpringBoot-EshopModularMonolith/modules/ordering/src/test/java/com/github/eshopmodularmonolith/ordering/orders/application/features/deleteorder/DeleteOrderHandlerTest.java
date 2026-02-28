package com.github.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        var cmd = new DeleteOrderCommand(existingOrder.id());
        handler.handle(cmd);
        assertThat(repository.findById(existingOrder.id())).isEmpty();
    }
}