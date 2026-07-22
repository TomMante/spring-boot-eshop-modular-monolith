# Setup Instructions

## Prerequisites
- Java 21+
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