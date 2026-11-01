package io.github.tommante.eshopmodularmonolith.e2e.tasks;

import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import io.restassured.http.ContentType;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;

import java.util.Map;

public class CheckOut implements Task {

    private static final Map<String, String> BILLING_ADDRESS = Map.of(
            "firstName", "End-to-end",
            "lastName", "Customer",
            "emailAddress", "customer@e2e.test",
            "addressLine", "1 Test Street",
            "country", "USA",
            "state", "Test State",
            "zipCode", "10001");

    private final String paymentMethodId;
    private final boolean underTest;

    public CheckOut(String paymentMethodId, boolean underTest) {
        this.paymentMethodId = paymentMethodId;
        this.underTest = underTest;
    }

    public static CheckOut payingWith(String paymentMethodId) {
        return Tasks.instrumented(CheckOut.class, paymentMethodId, false);
    }

    public CheckOut asTheRequestUnderTest() {
        return Tasks.instrumented(CheckOut.class, paymentMethodId, true);
    }

    @Override
    @Step("{0} checks out paying with #paymentMethodId")
    public <T extends Actor> void performAs(T actor) {
        var checkout = Api.post("/basket/checkout", request -> request
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "billingAddress", BILLING_ADDRESS,
                        "payment", Map.of("paymentMethodId", paymentMethodId))));
        actor.attemptsTo(underTest ? checkout.asTheRequestUnderTest() : checkout);
    }
}
