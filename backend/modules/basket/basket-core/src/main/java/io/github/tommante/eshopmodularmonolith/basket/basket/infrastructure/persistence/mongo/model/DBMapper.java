package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.ItemDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.ShoppingCartDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;

import java.util.stream.Collectors;

public class DBMapper {

    private DBMapper() {
    }

    public static ItemDB toDb(Item cartItem){
        return new ItemDB(cartItem.id(), cartItem.productId(), cartItem.quantity(), cartItem.priceWhenAdded(), cartItem.productName());
    }

    public static Item toDomain(ItemDB cartItem){
        return new Item(cartItem.id(), cartItem.productId(), cartItem.quantity(), cartItem.price(), cartItem.productName());
    }

    public static ItemDTO toDTO(ItemDB cartItem){
        return new ItemDTO(cartItem.id(), cartItem.productId(), cartItem.quantity(), cartItem.price(), cartItem.productName());
    }

    public static ShoppingCartDB toDb(ShoppingCart cart){
        return new ShoppingCartDB(cart.userName(), cart.items().stream().map(DBMapper::toDb).toList());
    }

    public static ShoppingCart toDomain(ShoppingCartDB cart){
        return ShoppingCart.reconstitute(cart.userName(), cart.items().stream().map(DBMapper::toDomain).collect(Collectors.toCollection(java.util.ArrayList::new)));
    }

    public static ShoppingCartDTO toDTO(ShoppingCartDB cart){
        return new ShoppingCartDTO(cart.userName(), cart.items().stream().map(DBMapper::toDTO).toList());
    }

}
