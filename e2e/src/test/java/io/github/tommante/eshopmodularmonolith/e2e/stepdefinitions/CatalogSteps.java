package io.github.tommante.eshopmodularmonolith.e2e.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.tommante.eshopmodularmonolith.e2e.tasks.ListTheProducts;
import net.serenitybdd.screenplay.Actor;

import static io.github.tommante.eshopmodularmonolith.e2e.api.ResponseUnderTest.seeThatTheResponseUnderTest;
import static org.hamcrest.Matchers.hasItems;

public class CatalogSteps {

    @When("{actor} lists the products")
    public void listsTheProducts(Actor actor) {
        actor.attemptsTo(ListTheProducts.inTheCatalog().asTheRequestUnderTest());
    }

    @Then("{actor} sees the products {string}, {string} and {string}")
    public void seesTheProducts(Actor actor, String first, String second, String third) {
        actor.should(seeThatTheResponseUnderTest("the catalog lists the seeded products",
                response -> response.statusCode(200).body("pagedProducts.items.name", hasItems(first, second, third))));
    }
}
