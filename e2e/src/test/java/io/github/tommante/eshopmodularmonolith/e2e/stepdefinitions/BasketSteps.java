package io.github.tommante.eshopmodularmonolith.e2e.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import io.github.tommante.eshopmodularmonolith.e2e.questions.BasketLine;
import io.github.tommante.eshopmodularmonolith.e2e.questions.TheBasket;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.AddToTheBasket;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.CreateTheBasket;
import net.serenitybdd.screenplay.Actor;

import java.util.List;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static io.github.tommante.eshopmodularmonolith.e2e.api.ResponseUnderTest.seeThatTheResponseUnderTest;
import static org.hamcrest.Matchers.equalTo;

public class BasketSteps {

    @Given("{actor} has {int} {string} in the basket")
    public void hasInTheBasket(Actor actor, int quantity, String productName) {
        actor.attemptsTo(CreateTheBasket.empty(), AddToTheBasket.quantityOf(quantity, productName));
    }

    @Then("{actor} still has {int} {string} in the basket")
    public void stillHasInTheBasket(Actor actor, int quantity, String productName) {
        actor.should(seeThat(TheBasket.lines(), equalTo(List.of(new BasketLine(productName, quantity)))));
    }

    @Then("{actor} no longer has a basket")
    public void noLongerHasABasket(Actor actor) {
        actor.attemptsTo(Api.get("/basket").asTheRequestUnderTest());
        actor.should(seeThatTheResponseUnderTest("there is no basket",
                response -> response.statusCode(404).body("code", equalTo("basket.not-found"))));
    }
}
