package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;

public class ListTheProducts implements Task {

    private final boolean underTest;

    public ListTheProducts(boolean underTest) {
        this.underTest = underTest;
    }

    public static ListTheProducts inTheCatalog() {
        return Tasks.instrumented(ListTheProducts.class, false);
    }

    public ListTheProducts asTheRequestUnderTest() {
        return Tasks.instrumented(ListTheProducts.class, true);
    }

    @Override
    @Step("{0} lists the products")
    public <T extends Actor> void performAs(T actor) {
        var listing = Api.get("/products", request -> request.queryParam("pageIndex", 0).queryParam("pageSize", 10));
        actor.attemptsTo(underTest ? listing.asTheRequestUnderTest() : listing);
    }
}
