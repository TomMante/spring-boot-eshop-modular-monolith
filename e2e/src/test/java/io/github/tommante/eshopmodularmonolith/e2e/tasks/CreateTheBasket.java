package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

public class CreateTheBasket implements Task {

    public static CreateTheBasket empty() {
        return Tasks.instrumented(CreateTheBasket.class);
    }

    @Override
    @Step("{0} creates a basket")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Post.to("/basket").with(request -> UseABearerToken.authorize(actor, request)));
    }
}
