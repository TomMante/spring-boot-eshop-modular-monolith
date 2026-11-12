package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

public record CardSummaryDB(String brand, String last4, int expiryMonth, int expiryYear) {
}
