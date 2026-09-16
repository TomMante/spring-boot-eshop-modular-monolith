package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(OrderDB.COLLECTION_NAME)
@TypeAlias(OrderDB.TYPE_ALIAS) //Better to provide a type alias, this makes the collection not dependent on a class.
@CompoundIndex(name = "orders_customer_placedAt_id", def = "{'customer': 1, 'placedAt': -1, '_id': 1}")
public record OrderDB(
        @Id UUID id,
        @Indexed(unique = true, name = "orders_checkoutId_unique") UUID checkoutId,
        List<ItemDB> items,
        String customer,
        Instant placedAt,
        AddressDB shippingAddress,
        AddressDB billingAddress,
        PaymentDB payment
) {
    public static final String COLLECTION_NAME = "orders";
    public static final String TYPE_ALIAS = "Order";
}
