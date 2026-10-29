package io.github.tommante.eshopmodularmonolith.e2e.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.CheckOut;
import net.serenitybdd.screenplay.Actor;

import static net.serenitybdd.screenplay.rest.questions.ResponseConsequence.seeThatResponse;
import static org.hamcrest.Matchers.equalTo;

public class CheckoutSteps {

    @When("{actor} checks out paying with {string}")
    public void checksOut(Actor actor, String paymentMethodId) {
        actor.attemptsTo(CheckOut.payingWith(paymentMethodId));
    }

    @Then("{actor}'s checkout is accepted")
    public void checkoutIsAccepted(Actor actor) {
        actor.should(seeThatResponse("the checkout is accepted", response -> response.statusCode(202)));
    }

    @Then("{actor}'s payment is declined")
    public void paymentIsDeclined(Actor actor) {
        actor.should(seeThatResponse("the payment is declined",
                response -> response.statusCode(402).body("code", equalTo("basket.checkout.payment-declined"))));
    }
}
