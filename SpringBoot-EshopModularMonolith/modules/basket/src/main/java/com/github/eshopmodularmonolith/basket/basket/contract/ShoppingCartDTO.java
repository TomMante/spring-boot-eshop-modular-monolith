package com.github.eshopmodularmonolith.basket.basket.contract;

import java.util.List;

public record ShoppingCartDTO(String userName, List<ItemDTO>items) {
}
