package io.github.tommante.eshopmodularmonolith.e2e.stepdefinitions;

import io.cucumber.java.en.Given;
import io.github.tommante.eshopmodularmonolith.e2e.abilities.UseABearerToken;
import io.github.tommante.eshopmodularmonolith.e2e.keycloak.CustomerAccounts;
import io.github.tommante.eshopmodularmonolith.e2e.keycloak.Tokens;
import net.serenitybdd.screenplay.Actor;

public class CustomerSteps {

    static final String CUSTOMER_ACCOUNT = "customer account";

    @Given("{actor} is a customer")
    public void isACustomer(Actor actor) {
        var account = CustomerAccounts.create();
        actor.remember(CUSTOMER_ACCOUNT, account);
        actor.whoCan(UseABearerToken.of(Tokens.forCustomer(account)));
    }
}
