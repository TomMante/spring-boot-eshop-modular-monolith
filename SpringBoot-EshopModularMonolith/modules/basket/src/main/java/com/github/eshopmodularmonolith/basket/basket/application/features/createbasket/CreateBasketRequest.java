package com.github.eshopmodularmonolith.basket.basket.application.features.createbasket;

import jakarta.validation.constraints.NotBlank;

public record CreateBasketRequest(
        @NotBlank(message = "{basket.basket.userName.notBlank}") String userName) {
}
