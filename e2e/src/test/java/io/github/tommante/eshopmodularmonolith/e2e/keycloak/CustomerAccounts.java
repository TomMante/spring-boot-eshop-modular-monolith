package io.github.tommante.eshopmodularmonolith.e2e.keycloak;

import io.github.tommante.eshopmodularmonolith.e2e.EndToEndEnvironment;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates and deletes eshop realm users through Keycloak's admin API, so every scenario has
 * customers with no basket and no orders.
 */
public final class CustomerAccounts {

    private static final String USERS = "/admin/realms/eshop/users";

    private CustomerAccounts() {
    }

    public static CustomerAccount create() {
        var username = "e2e-" + UUID.randomUUID();
        var password = UUID.randomUUID().toString();
        // A complete profile, so Keycloak attaches no required action and the password grant works.
        var user = Map.of(
                "username", username,
                "enabled", true,
                "firstName", "End-to-end",
                "lastName", "Customer",
                "email", username + "@e2e.test",
                "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));
        String location = asAdministrator()
                .contentType(ContentType.JSON)
                .body(user)
                .post(USERS)
                .then().statusCode(201)
                .extract().header("Location");
        return new CustomerAccount(location.substring(location.lastIndexOf('/') + 1), username, password);
    }

    public static void delete(CustomerAccount account) {
        asAdministrator()
                .delete(USERS + "/{id}", account.id())
                .then().statusCode(204);
    }

    private static RequestSpecification asAdministrator() {
        return RestAssured.given()
                .baseUri(EndToEndEnvironment.keycloakUrl())
                .auth().oauth2(Tokens.forTheAdministrator());
    }
}
