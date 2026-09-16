package io.github.tommante.eshopmodularmonolith.basket.basket.contract;

public record AddressDTO(
        String firstName,
        String lastName,
        String emailAddress,
        String addressLine,
        String country,
        String state,
        String zipCode
) {
}
