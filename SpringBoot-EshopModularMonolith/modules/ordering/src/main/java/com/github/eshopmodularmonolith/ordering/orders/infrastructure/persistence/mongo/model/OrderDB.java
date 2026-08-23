package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(OrderDB.COLLECTION_NAME)
@TypeAlias(OrderDB.TYPE_ALIAS) //Better to provide a type alias, this makes the collection not dependent on a class.
public record OrderDB(
        @Id UUID id,
        List<ItemDB> items,
        String customer,
        String name,
        AddressDB shippingAddress,
        AddressDB billingAddress,
        PaymentDB payment
) {
    public static final String COLLECTION_NAME = "orders";
    public static final String TYPE_ALIAS = "Order";
}
