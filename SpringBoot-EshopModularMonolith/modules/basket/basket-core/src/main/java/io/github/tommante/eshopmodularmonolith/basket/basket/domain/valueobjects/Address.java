package io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects;


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
