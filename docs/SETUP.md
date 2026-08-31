# Setup Instructions

## Prerequisites
- Java 25+
- Maven
- Docker & Docker Compose (for running dependencies like databases and Keycloak)

## Running the Application
1. Ensure your local infrastructure (e.g., PostgreSQL, MongoDB, Keycloak) is running.
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Keycloak Setup
1. Setup a realm called `eshop`
2. Setup a client called `eshop-app`
3. Add Audience mapper in Client scope of `eshop-app`

## API Documentation
Swagger UI is available at `http://localhost:8080/swagger-ui.html` (raw OpenAPI JSON at `/v3/api-docs`) once the application is running. These paths are exempt from authentication, but calling the documented endpoints still requires a bearer token — obtain one (see `bruno/Auth/Get Token (Password Grant).bru`) and paste it into Swagger UI's "Authorize" button.

A ready-to-use [Bruno](https://www.usebruno.com/) collection covering all endpoints also lives in the `bruno/` directory at the repo root.