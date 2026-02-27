package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.shared.domain.Entity;

import java.math.BigDecimal;
import java.util.UUID;

//This was renamed to Item, as within the Bounded context of ordering, item  means order item (ubiquitous language of the bounded context).
public record Item(UUID id, UUID productId, int quantity, BigDecimal price) implements Entity<UUID> {

}
