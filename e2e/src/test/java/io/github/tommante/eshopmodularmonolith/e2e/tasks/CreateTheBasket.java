package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;

public class CreateTheBasket implements Task {

    public static CreateTheBasket empty() {
        return Tasks.instrumented(CreateTheBasket.class);
    }

    @Override
    @Step("{0} creates a basket")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Api.post("/basket"));
    }
}
