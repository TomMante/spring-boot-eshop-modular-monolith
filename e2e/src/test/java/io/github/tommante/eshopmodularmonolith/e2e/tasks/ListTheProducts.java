package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Get;

public class ListTheProducts implements Task {

    public static ListTheProducts inTheCatalog() {
        return Tasks.instrumented(ListTheProducts.class);
    }

    @Override
    @Step("{0} lists the products")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Get.resource("/products").with(request ->
                UseABearerToken.authorize(actor, request).queryParam("pageIndex", 0).queryParam("pageSize", 10)));
    }
}
