package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class CreateOrderHandler {

    private final OrderRepository repository;
    private final Clock clock;

    public CreateOrderHandler(OrderRepository repository, @Qualifier("orderClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public UUID handle(CreateOrderCommand cmd){
        var order = Order.create(cmd.customer(), clock.instant(), cmd.billingAddress(), cmd.payment(), cmd.shippingAddress(), cmd.items());
        repository.save(order);
        return order.id();
    }
}
