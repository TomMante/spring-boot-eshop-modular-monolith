package io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions;

import java.util.UUID;

public class OrderNotFound extends RuntimeException {

    public OrderNotFound(UUID orderId) {
        super("Order with id [" + orderId + "] not found.");
    }
}
