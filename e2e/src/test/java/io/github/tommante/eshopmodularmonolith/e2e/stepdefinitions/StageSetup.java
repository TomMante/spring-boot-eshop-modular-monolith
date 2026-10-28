package io.github.tommante.eshopmodularmonolith.e2e.stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.github.tommante.eshopmodularmonolith.e2e.EndToEndEnvironment;
import io.github.tommante.eshopmodularmonolith.e2e.keycloak.CustomerAccount;
import io.github.tommante.eshopmodularmonolith.e2e.keycloak.CustomerAccounts;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.Cast;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StageSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageSetup.class);

    // Cucumber creates this class once per scenario, and Stage doesn't expose its cast.
    private final Cast cast = Cast.ofStandardActors();

    @Before
    public void setTheStage() {
        OnStage.setTheStage(cast);
    }

    @After
    public void deleteTheScenarioCustomers() {
        for (Actor actor : cast.getActors()) {
            deleteBestEffort(actor.recall(CustomerSteps.CUSTOMER_ACCOUNT));
        }
    }

    @ParameterType("[A-Z][a-z]+")
    public Actor actor(String name) {
        return OnStage.theActorCalled(name).whoCan(CallAnApi.at(EndToEndEnvironment.appUrl()));
    }

    private static void deleteBestEffort(CustomerAccount account) {
        if (account == null) {
            return;
        }
        try {
            CustomerAccounts.delete(account);
        } catch (RuntimeException | AssertionError failure) {
            LOGGER.warn("Could not delete the Keycloak user {}; a fresh stack drops it: {}",
                    account.username(), failure.getMessage());
        }
    }
}
