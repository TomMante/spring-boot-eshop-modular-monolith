package io.github.tommante.eshopmodularmonolith.ordering.orders.contract;

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
