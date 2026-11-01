package io.github.tommante.eshopmodularmonolith.e2e.questions;

import io.github.tommante.eshopmodularmonolith.e2e.api.Api;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

import java.util.HashMap;
import java.util.Map;

public final class TheCatalog {

    private static final int PAGE_SIZE = 10;

    // The seeded catalog is read-only, so one read serves every scenario in the run.
    private static Map<String, String> seededProductIds;

    private TheCatalog() {
    }

    public static Question<String> idOfTheProductNamed(String productName) {
        return Question.about("the id of " + productName).answeredBy(actor -> {
            String productId = seededProductIds(actor).get(productName);
            if (productId == null) {
                throw new IllegalStateException("No product named '" + productName + "' in the seeded catalog."
                        + " Scenarios may only use the seeded products by name; a product a scenario creates"
                        + " is known by the id its create request returned.");
            }
            return productId;
        });
    }

    private static synchronized Map<String, String> seededProductIds(Actor actor) {
        if (seededProductIds == null) {
            seededProductIds = readEveryPage(actor);
        }
        return seededProductIds;
    }

    private static Map<String, String> readEveryPage(Actor actor) {
        var productIds = new HashMap<String, String>();
        int totalPages = 1;
        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            int page = pageIndex;
            actor.attemptsTo(Api.get("/products", request -> request.queryParam("pageIndex", page).queryParam("pageSize", PAGE_SIZE)));
            var response = SerenityRest.lastResponse().jsonPath();
            response.getList("pagedProducts.items", Map.class)
                    .forEach(product -> productIds.put((String) product.get("name"), (String) product.get("id")));
            totalPages = response.getInt("pagedProducts.totalPages");
        }
        return Map.copyOf(productIds);
    }
}
