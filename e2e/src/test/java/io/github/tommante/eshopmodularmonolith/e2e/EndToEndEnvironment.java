package io.github.tommante.eshopmodularmonolith.e2e;

public final class EndToEndEnvironment {

    private EndToEndEnvironment() {
    }

    public static String appUrl() {
        return valueOf("E2E_APP_URL", "http://localhost:8080");
    }

    public static String keycloakUrl() {
        return valueOf("E2E_KEYCLOAK_URL", "http://localhost:9090");
    }

    public static String keycloakAdminUser() {
        return valueOf("E2E_KEYCLOAK_ADMIN_USER", "admin");
    }

    public static String keycloakAdminPassword() {
        return valueOf("E2E_KEYCLOAK_ADMIN_PASSWORD", "admin");
    }

    private static String valueOf(String variable, String fallback) {
        var value = System.getenv(variable);
        return value == null || value.isBlank() ? fallback : value;
    }
}
