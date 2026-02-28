package com.github.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class DeleteOrderHandler {

    private final OrderRepository repository;

    public DeleteOrderHandler(OrderRepository repository) {
        this.repository = repository;
    }

    public void handle(DeleteOrderCommand cmd){
        repository.delete(cmd.id());
    }

}
