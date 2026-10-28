package io.github.tommante.eshopmodularmonolith.e2e.keycloak;

import io.github.tommante.eshopmodularmonolith.e2e.EndToEndEnvironment;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * Plain RestAssured rather than SerenityRest, so token requests stay out of the report.
 */
public final class Tokens {

    private static final String CLIENT_ID = "eshop-app";
    private static final String CLIENT_SECRET = "local-demo-secret";

    private Tokens() {
    }

    public static String forCustomer(CustomerAccount account) {
        return passwordGrant("eshop", RestAssured.given()
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .formParam("username", account.username())
                .formParam("password", account.password()));
    }

    public static String forTheAdministrator() {
        return passwordGrant("master", RestAssured.given()
                .formParam("client_id", "admin-cli")
                .formParam("username", EndToEndEnvironment.keycloakAdminUser())
                .formParam("password", EndToEndEnvironment.keycloakAdminPassword()));
    }

    private static String passwordGrant(String realm, RequestSpecification request) {
        return request
                .baseUri(EndToEndEnvironment.keycloakUrl())
                .formParam("grant_type", "password")
                .post("/realms/{realm}/protocol/openid-connect/token", realm)
                .then().statusCode(200)
                .extract().path("access_token");
    }
}
