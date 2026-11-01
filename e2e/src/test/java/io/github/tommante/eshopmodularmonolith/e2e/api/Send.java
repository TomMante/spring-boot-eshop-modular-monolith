package io.github.tommante.eshopmodularmonolith.e2e.api;

import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Get;
import net.serenitybdd.screenplay.rest.interactions.Post;
import net.serenitybdd.screenplay.rest.interactions.RestInteraction;
import net.serenitybdd.screenplay.rest.questions.RestQueryFunction;

import java.util.List;

public class Send implements Interaction {

    static final String RESPONSE_UNDER_TEST = "response under test";

    enum Method { GET, POST }

    private final Method method;
    private final String path;
    private final List<RestQueryFunction> customisations;
    private final boolean underTest;

    Send(Method method, String path, List<RestQueryFunction> customisations, boolean underTest) {
        this.method = method;
        this.path = path;
        this.customisations = customisations;
        this.underTest = underTest;
    }

    public Send asTheRequestUnderTest() {
        return new Send(method, path, customisations, true);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(restInteraction()
                .with(request -> withTheTokenOf(actor, request))
                .with(customisations));
        // Read straight after this actor's own request, before anything else can send one on this thread.
        var response = SerenityRest.lastResponse();
        if (underTest) {
            actor.remember(RESPONSE_UNDER_TEST, response);
        } else if (!isSuccessful(response)) {
            throw new AssertionError("%s %s answered %d: %s. This request sets up the scenario or answers a question, so it must succeed; only a When step's request, sent with asTheRequestUnderTest(), may fail."
                    .formatted(method, path, response.statusCode(), response.asString()));
        }
    }

    private RestInteraction restInteraction() {
        return switch (method) {
            case GET -> Get.resource(path);
            case POST -> Post.to(path);
        };
    }

    private static RequestSpecification withTheTokenOf(Actor actor, RequestSpecification request) {
        var bearerToken = actor.abilityTo(UseABearerToken.class);
        return bearerToken == null ? request : request.auth().oauth2(bearerToken.token());
    }

    private static boolean isSuccessful(Response response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }
}
