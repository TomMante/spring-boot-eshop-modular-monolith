package io.github.tommante.eshopmodularmonolith.e2e.abilities;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.screenplay.Ability;
import net.serenitybdd.screenplay.Actor;

public record UseABearerToken(String token) implements Ability {

    public static UseABearerToken of(String token) {
        return new UseABearerToken(token);
    }

    public static RequestSpecification authorize(Actor actor, RequestSpecification request) {
        var bearerToken = actor.abilityTo(UseABearerToken.class);
        return bearerToken == null ? request : request.auth().oauth2(bearerToken.token());
    }
}
