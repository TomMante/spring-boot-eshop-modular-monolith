package com.github.eshopmodularmonolith.catalog.products.contract.integrationevents;

import java.util.UUID;

public record ProductCreatedMessage(UUID productId) {
}
