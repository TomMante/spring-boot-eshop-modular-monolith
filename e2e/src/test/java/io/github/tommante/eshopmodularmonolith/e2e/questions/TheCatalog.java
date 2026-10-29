package io.github.tommante.eshopmodularmonolith.e2e.questions;

import io.github.tommante.eshopmodularmonolith.e2e.tasks.ListTheProducts;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Question;

public final class TheCatalog {

    private TheCatalog() {
    }

    public static Question<String> idOfTheProductNamed(String productName) {
        return Question.about("the id of " + productName).answeredBy(actor -> {
            actor.attemptsTo(ListTheProducts.inTheCatalog());
            String productId = SerenityRest.lastResponse().jsonPath()
                    .getString("pagedProducts.items.find { it.name == '%s' }.id".formatted(productName));
            if (productId == null) {
                throw new IllegalStateException("No product named '" + productName
                        + "' in the catalog. Scenarios may only use the seeded products by name.");
            }
            return productId;
        });
    }
}
