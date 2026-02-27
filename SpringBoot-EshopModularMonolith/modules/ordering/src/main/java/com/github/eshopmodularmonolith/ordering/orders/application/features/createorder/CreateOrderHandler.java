package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateOrderHandler {

    private final OrderRepository repository;

    public CreateOrderHandler(OrderRepository repository) {
        this.repository = repository;
    }

    public UUID handle(CreateOrderCommand cmd){
        var order = Order.create(cmd.customerId(), cmd.name(), cmd.shippingAddress(), cmd.billingAddress(), cmd.payment(), cmd.items());
        repository.save(order);
        return order.id();
    }
}
