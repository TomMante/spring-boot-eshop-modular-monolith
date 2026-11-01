package io.github.tommante.eshopmodularmonolith.e2e.questions;

import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Question;

import java.util.List;
import java.util.Map;

public final class TheBasket {

    private TheBasket() {
    }

    public static Question<List<BasketLine>> lines() {
        return Question.about("the basket lines").answeredBy(actor -> {
            actor.attemptsTo(Api.get("/basket"));
            return SerenityRest.lastResponse().jsonPath()
                    .getList("shoppingCart.items", Map.class).stream()
                    .map(line -> new BasketLine((String) line.get("productName"), (Integer) line.get("quantity")))
                    .toList();
        });
    }
}
