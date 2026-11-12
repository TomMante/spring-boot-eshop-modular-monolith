package io.github.tommante.eshopmodularmonolith.ordering.orders.contract.dto;

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
