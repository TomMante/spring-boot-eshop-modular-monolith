package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias(AddressDB.TYPE_ALIAS)
public record AddressDB(
        String firstName,
        String lastName,
        String emailAddress,
        String addressLine,
        String country,
        String state,
        String zipCode
) {

    public static final String TYPE_ALIAS = "Address";
}
