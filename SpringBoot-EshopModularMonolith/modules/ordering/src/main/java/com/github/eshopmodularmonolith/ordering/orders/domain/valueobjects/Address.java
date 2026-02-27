package com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects;


public record Address(
        String firstName,
        String lastName,
        String emailAddress,
        String addressLine,
        String country,
        String state,
        String zipCode
) {
}
