package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartItem;

public class DBMapper {


    public static ShoppingCartItemDB toDb(ShoppingCartItem cartItem){
        return new ShoppingCartItemDB(cartItem.id(), cartItem.productId(), cartItem.quantity(), cartItem.color(), cartItem.priceWhenAdded(), cartItem.productName());
    }

    public static ShoppingCartItem toDomain(ShoppingCartItemDB cartItem){
        return new ShoppingCartItem(cartItem.id(), cartItem.productId(), cartItem.quantity(), cartItem.color(), cartItem.price(), cartItem.productName());
    }

    public static ShoppingCartDB toDb(ShoppingCart cart){
        return new ShoppingCartDB(cart.userName(), cart.items().stream().map(DBMapper::toDb).toList());
    }

    public static ShoppingCart toDomain(ShoppingCartDB cart){
        return ShoppingCart.reconstitute(cart.userName(), cart.items().stream().map(DBMapper::toDomain).toList());
    }

}
