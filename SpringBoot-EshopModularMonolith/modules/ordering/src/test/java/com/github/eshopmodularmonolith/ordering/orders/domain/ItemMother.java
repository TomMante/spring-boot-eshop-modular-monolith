package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.ordering.orders.domain.Item;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemMother {

    public static Item item() {
        return new Builder().build();
    }

    public static class Builder {

        private UUID id = UUID.randomUUID();
        private UUID productId = UUID.randomUUID();
        private int quantity = 2;
        private BigDecimal price = BigDecimal.valueOf(1234);

        public Item build(){
            return new Item(id, productId, quantity, price);
        }
    }
}
