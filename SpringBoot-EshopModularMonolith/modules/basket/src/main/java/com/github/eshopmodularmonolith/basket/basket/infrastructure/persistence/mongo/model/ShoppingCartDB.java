package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartItem;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = ShoppingCartDB.COLLECTION_NAME)
@TypeAlias(value = ShoppingCartDB.TYPE_ALIAS)
public record ShoppingCartDB(
        @Id UUID id,
        String userName,
        List<ShoppingCartItemDB> items) {

    public static final String COLLECTION_NAME = "shopping-cart";
    public static final String TYPE_ALIAS = "ShoppingCart";
}
