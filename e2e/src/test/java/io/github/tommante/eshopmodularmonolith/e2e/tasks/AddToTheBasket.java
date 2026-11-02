package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import io.github.tommante.eshopmodularmonolith.e2e.questions.TheCatalog;
import io.restassured.http.ContentType;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;

import java.util.Map;

public class AddToTheBasket implements Task {

    private final int quantity;
    private final String productName;

    public AddToTheBasket(int quantity, String productName) {
        this.quantity = quantity;
        this.productName = productName;
    }

    public static AddToTheBasket quantityOf(int quantity, String productName) {
        return Tasks.instrumented(AddToTheBasket.class, quantity, productName);
    }

    @Override
    @Step("{0} adds #quantity #productName to the basket")
    public <T extends Actor> void performAs(T actor) {
        var productId = actor.asksFor(TheCatalog.idOfTheProductNamed(productName));
        actor.attemptsTo(Api.post("/basket/items", request -> request
                .contentType(ContentType.JSON)
                .body(Map.of("productId", productId, "quantity", quantity))));
    }
}
