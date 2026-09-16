package io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions;

import java.util.UUID;

public class OrderAlreadyExists extends RuntimeException {

    private final UUID checkoutId;

    public OrderAlreadyExists(UUID checkoutId, Throwable cause) {
        super("Checkout " + checkoutId + " has already been placed as an order; the same checkout cannot be placed twice.", cause);
        this.checkoutId = checkoutId;
    }

    public UUID checkoutId() {
        return checkoutId;
    }
}
