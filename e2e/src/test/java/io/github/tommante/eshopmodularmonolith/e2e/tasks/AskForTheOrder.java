package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Get;

public class AskForTheOrder implements Task {

    private final String orderId;

    public AskForTheOrder(String orderId) {
        this.orderId = orderId;
    }

    public static AskForTheOrder withId(String orderId) {
        return Tasks.instrumented(AskForTheOrder.class, orderId);
    }

    @Override
    @Step("{0} asks for the order #orderId")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Get.resource("/order/{id}").with(request ->
                UseABearerToken.authorize(actor, request).pathParam("id", orderId)));
    }
}
