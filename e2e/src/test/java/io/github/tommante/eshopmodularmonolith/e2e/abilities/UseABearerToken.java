package io.github.tommante.eshopmodularmonolith.e2e.abilities;

import net.serenitybdd.screenplay.Ability;

public record UseABearerToken(String token) implements Ability {

    public static UseABearerToken of(String token) {
        return new UseABearerToken(token);
    }
}
