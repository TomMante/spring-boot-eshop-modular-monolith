package com.github.eshopmodularmonolith.ordering.models;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class OrderItemTest {


    @Test
    @DisplayName("Given id, when order item is created, then id is set")
    void giveId_whenOrderItemIsCreated_thenIdIsSet(){
        var id = UUID.randomUUID();
        var orderItem = new OrderItem(id);

        Assertions.assertThat(orderItem).extracting(OrderItem::id).isEqualTo(id);
    }
}