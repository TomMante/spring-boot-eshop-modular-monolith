package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class DeleteOrderHandler {

    private final OrderRepository repository;

    public DeleteOrderHandler(OrderRepository repository) {
        this.repository = repository;
    }

    public void handle(DeleteOrderCommand cmd){
        repository.findById(cmd.id())
                .filter(order -> cmd.requester().username().equals(order.customer()))
                .orElseThrow(() -> new OrderNotFound(cmd.id()));
        repository.delete(cmd.id());
    }

}
