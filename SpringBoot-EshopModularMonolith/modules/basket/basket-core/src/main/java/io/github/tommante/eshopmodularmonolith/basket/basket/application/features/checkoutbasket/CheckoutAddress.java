package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckoutAddress(
        @NotBlank(message = "{basket.checkout.address.firstName.notBlank}") String firstName,
        @NotBlank(message = "{basket.checkout.address.lastName.notBlank}") String lastName,
        @NotBlank(message = "{basket.checkout.address.emailAddress.notBlank}")
        @Email(message = "{basket.checkout.address.emailAddress.email}") String emailAddress,
        @NotBlank(message = "{basket.checkout.address.addressLine.notBlank}") String addressLine,
        @NotBlank(message = "{basket.checkout.address.country.notBlank}") String country,
        String state,
        @NotBlank(message = "{basket.checkout.address.zipCode.notBlank}") String zipCode) {

    Address toDomain() {
        return new Address(firstName, lastName, emailAddress, addressLine, country, state, zipCode);
    }
}
