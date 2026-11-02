package io.github.tommante.eshopmodularmonolith.e2e.api;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.BaseConsequence;

import java.util.function.Consumer;

public class ResponseUnderTest extends BaseConsequence<Response> {

    private final String description;
    private final Consumer<ValidatableResponse> expectation;

    private ResponseUnderTest(String description, Consumer<ValidatableResponse> expectation) {
        this.description = description;
        this.expectation = expectation;
    }

    public static ResponseUnderTest seeThatTheResponseUnderTest(String description, Consumer<ValidatableResponse> expectation) {
        return new ResponseUnderTest(description, expectation);
    }

    @Override
    public void evaluateFor(Actor actor) {
        performSetupActionsAs(actor);
        Response response = actor.recall(Send.RESPONSE_UNDER_TEST);
        if (response == null) {
            throw new AssertionError("Can't check that " + description + ": " + actor.getName()
                    + " sent no request under test. Send it in an earlier When step, with asTheRequestUnderTest().");
        }
        expectation.accept(response.then());
    }

    @Override
    public String toString() {
        return description;
    }
}
