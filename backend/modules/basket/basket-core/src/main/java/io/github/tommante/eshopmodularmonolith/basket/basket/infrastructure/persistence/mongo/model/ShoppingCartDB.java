package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = ShoppingCartDB.COLLECTION_NAME)
@TypeAlias(value = ShoppingCartDB.TYPE_ALIAS)
public record ShoppingCartDB(
        @Id String userName,
        List<ItemDB> items) {

    public static final String COLLECTION_NAME = "shopping-cart";
    public static final String TYPE_ALIAS = "ShoppingCart";
}
