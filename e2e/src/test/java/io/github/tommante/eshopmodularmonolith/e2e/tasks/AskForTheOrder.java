package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;

public class AskForTheOrder implements Task {

    private final String orderId;
    private final boolean underTest;

    public AskForTheOrder(String orderId, boolean underTest) {
        this.orderId = orderId;
        this.underTest = underTest;
    }

    public static AskForTheOrder withId(String orderId) {
        return Tasks.instrumented(AskForTheOrder.class, orderId, false);
    }

    public AskForTheOrder asTheRequestUnderTest() {
        return Tasks.instrumented(AskForTheOrder.class, orderId, true);
    }

    @Override
    @Step("{0} asks for the order #orderId")
    public <T extends Actor> void performAs(T actor) {
        var request = Api.get("/order/{id}", specification -> specification.pathParam("id", orderId));
        actor.attemptsTo(underTest ? request.asTheRequestUnderTest() : request);
    }
}
