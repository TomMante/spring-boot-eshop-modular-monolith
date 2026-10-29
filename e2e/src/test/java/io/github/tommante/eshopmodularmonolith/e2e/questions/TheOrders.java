package io.github.tommante.eshopmodularmonolith.e2e.questions;

import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.rest.interactions.Get;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class TheOrders {

    private TheOrders() {
    }

    public static Question<List<OrderSummary>> placed() {
        return Question.about("the orders").answeredBy(actor -> {
            listTheOrders(actor);
            var orders = SerenityRest.lastResponse().jsonPath(
                    JsonPathConfig.jsonPathConfig().numberReturnType(NumberReturnType.BIG_DECIMAL));
            return IntStream.range(0, orders.getList("pagedOrders.items").size())
                    .mapToObj(index -> summaryOf(orders, "pagedOrders.items[%d].".formatted(index)))
                    .toList();
        });
    }

    public static Question<List<String>> ids() {
        return Question.about("the order ids").answeredBy(actor -> {
            listTheOrders(actor);
            return SerenityRest.lastResponse().jsonPath().getList("pagedOrders.items.id", String.class);
        });
    }

    private static void listTheOrders(Actor actor) {
        actor.attemptsTo(Get.resource("/orders").with(request ->
                UseABearerToken.authorize(actor, request).queryParam("pageIndex", 0).queryParam("pageSize", 10)));
    }

    private static OrderSummary summaryOf(JsonPath orders, String order) {
        List<Map<String, Object>> lines = orders.getList(order + "items");
        return new OrderSummary(
                lines.stream().map(TheOrders::lineOf).toList(),
                orders.get(order + "totalPrice"),
                orders.getString(order + "payment.card.brand"),
                orders.getString(order + "payment.card.last4"));
    }

    private static OrderLine lineOf(Map<String, Object> line) {
        return new OrderLine((String) line.get("productId"), ((Number) line.get("quantity")).intValue());
    }
}
