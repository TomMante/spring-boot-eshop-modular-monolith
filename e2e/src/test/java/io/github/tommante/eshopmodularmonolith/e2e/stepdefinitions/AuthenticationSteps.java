package io.github.tommante.eshopmodularmonolith.e2e.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import io.github.tommante.eshopmodularmonolith.e2e.keycloak.Tokens;
import net.serenitybdd.screenplay.Actor;

import static net.serenitybdd.screenplay.rest.questions.ResponseConsequence.seeThatResponse;

public class AuthenticationSteps {

    @Given("{actor} holds a token from another realm")
    public void holdsATokenFromAnotherRealm(Actor actor) {
        actor.whoCan(UseABearerToken.of(Tokens.forTheAdministrator()));
    }

    @Then("{actor}'s request is refused as unauthenticated")
    public void isRefusedAsUnauthenticated(Actor actor) {
        actor.should(seeThatResponse("the request is refused", response -> response.statusCode(401)));
    }

    @Then("{actor}'s request is accepted")
    public void isAccepted(Actor actor) {
        actor.should(seeThatResponse("the request is accepted", response -> response.statusCode(200)));
    }
}
