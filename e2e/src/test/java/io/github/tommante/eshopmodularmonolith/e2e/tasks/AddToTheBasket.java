package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import io.github.tommante.eshopmodularmonolith.e2e.questions.TheCatalog;
import io.restassured.http.ContentType;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

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
        actor.attemptsTo(Post.to("/basket/items").with(request -> UseABearerToken.authorize(actor, request)
                .contentType(ContentType.JSON)
                .body(Map.of("productId", productId, "quantity", quantity))));
    }
}
