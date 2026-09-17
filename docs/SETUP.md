# Setup Instructions

From a clean clone to an order placed through the application. Every command below was run in
this order against a fresh checkout with empty database volumes.

## Prerequisites

- **JDK 25.** The build targets release 25; an older JDK fails with
  `invalid target release: 25`, which does not mention Java. Check with `java -version` before
  starting.
- **Docker and Docker Compose**, running.

Nothing else needs installing — the Maven wrapper pins its own Maven version.

## 1. Start the infrastructure

From the repository root:

```bash
docker compose -f SpringBoot-EshopModularMonolith/docker/sb-eshop/compose.yaml up -d
```

This starts Postgres (`5432`), MongoDB (`27020`, as a single-node replica set) and Keycloak
(`9090`), and imports the `eshop` realm. Keycloak waits for Postgres to be healthy before
starting, so the command returns when everything is up in the right order.

Keycloak takes a few seconds longer than the databases. It is ready when this returns `200`:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9090/realms/eshop/.well-known/openid-configuration
```

## 2. Build

```bash
cd SpringBoot-EshopModularMonolith
./mvnw clean install
```

The test suite runs Testcontainers and needs Docker; add `-DskipTests` to skip it if you only
want to run the application.

## 3. Run the application

```bash
./mvnw -pl starter spring-boot:run
```

`-pl starter` matters: without it Maven runs the goal for every module in the reactor and fails on
the aggregator with *"Unable to find a suitable main class"*.

No profile is needed. The committed `application.yaml` already points at the compose stack. The
application listens on `8080` and logs `Started EshopModularMonolithStarter` when it is up.

## 4. Get a token

The realm ships with two demo users. These credentials are committed on purpose — they are
local-only values that exist in `docker/sb-eshop/keycloak-init/eshop-realm.json`, which likewise
commits `admin`/`admin` for the Keycloak container itself.

| | |
| --- | --- |
| Users | `john` / `password`, `jane` / `password` |
| Client | `eshop-app` / `local-demo-secret` |

```bash
TOKEN=$(curl -s -X POST http://localhost:9090/realms/eshop/protocol/openid-connect/token \
  -d grant_type=password -d client_id=eshop-app -d client_secret=local-demo-secret \
  -d username=john -d password=password | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
```

Access tokens live 300 seconds; re-run this when requests start returning `401`.

`-d username=` picks the user. Baskets and orders belong to whoever the token names — each user
sees only their own — so a second token is what you need to check that isolation by hand:

```bash
TOKEN_JANE=$(curl -s -X POST http://localhost:9090/realms/eshop/protocol/openid-connect/token \
  -d grant_type=password -d client_id=eshop-app -d client_secret=local-demo-secret \
  -d username=jane -d password=password | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
```

## 5. Walk the checkout flow

The catalog is seeded at startup with three products carrying fixed ids, so there is something to
buy immediately.

```bash
# The seeded catalog
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/products?pageIndex=0&pageSize=10"

# A basket for the authenticated user — 201
curl -s -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/basket

# Add the seeded Galaxy Smartphone — 202
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"productId":"a1000000-0000-4000-8000-000000000001","quantity":2}' \
  http://localhost:8080/basket/items

# The basket now carries the product's name and current price, resolved from catalog
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/basket

# Check out — 202
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "billingAddress":{"firstName":"John","lastName":"Doe","emailAddress":"john.doe@example.com","addressLine":"1 Billing Street","country":"USA","state":"Billing State","zipCode":"10001"},
    "payment":{"cardName":"John Doe","cardNumber":"4111111111111111","expiration":"12/29","cvv":"123"},
    "shippingAddress":{"firstName":"Jane","lastName":"Roe","emailAddress":"jane.roe@example.com","addressLine":"2 Shipping Avenue","country":"USA","state":"Shipping State","zipCode":"90210"}
  }' http://localhost:8080/basket/checkout

# The order appears a few seconds later
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/orders?pageIndex=0&pageSize=10"
```

**Checkout returns `202`, not the order.** Basket writes a `BasketCheckedOut` event to its outbox
and returns; a dispatcher polls every 5 seconds, publishes the integration event, and ordering
creates the order in its own database. `GET /orders` is empty until that completes — usually within
about 10 seconds. That delay is the architecture working, not a fault: the two modules share no
database and communicate only through events.

Orders belong to the user who checked out. `GET /orders` lists only the caller's own, and another
user's order id answers `404` on both `GET /order/{id}` and `DELETE /order/{id}` — the same
`TOKEN_JANE` from step 4 sees an empty list until jane checks out herself.

## Seed data

Seeding is controlled by `eshop.catalog.seed-demo-data` in `application.yaml`. It is applied by a
Flyway instance separate from the schema one, keeping its own `flyway_seed_history` table, so
turning it off does not break schema validation.

| Product | Id |
| --- | --- |
| Galaxy Smartphone | `a1000000-0000-4000-8000-000000000001` |
| Aurora Laptop | `a1000000-0000-4000-8000-000000000002` |
| Pulse Headphones | `a1000000-0000-4000-8000-000000000003` |

Turning the flag off stops the seed running; it does not delete rows already inserted. For an empty
catalog, start from fresh volumes (see below).

## Starting over

```bash
docker compose -f SpringBoot-EshopModularMonolith/docker/sb-eshop/compose.yaml down -v
```

`-v` discards the volumes, so the next `up` gives you empty databases and a freshly imported realm.

## API documentation and tooling

Swagger UI is at `http://localhost:8080/swagger-ui.html` (raw OpenAPI JSON at `/v3/api-docs`).
Those paths are exempt from authentication, but the documented endpoints still require a bearer
token — obtain one as in step 4 and paste it into Swagger UI's **Authorize** button.

A [Bruno](https://www.usebruno.com/) collection covering every endpoint lives in `bruno/` at the
repository root. Open that folder as a collection, select the committed **default - john** (or
**default - jane**) environment, and run **Auth → Get Token (Password Grant)** first; every other
request inherits the bearer token from it. See [bruno/README.md](../bruno/README.md).

## Keycloak administration

The admin console is at `http://localhost:9090` (`admin` / `admin`). The `eshop` realm, the
`eshop-app` client, its audience mapper and the `john` and `jane` users are all created by
`--import-realm` from `docker/sb-eshop/keycloak-init/eshop-realm.json` — there is no manual realm
setup to do.

**Editing that file only takes effect on a fresh realm.** `--import-realm` skips a realm that
already exists, and Keycloak stores its data in Postgres' `identity` schema. No named volumes are
declared in `compose.yaml`, so the way to re-import is:

```bash
docker compose down && docker compose up -d
```

That drops the databases along with the realm — catalog re-seeds itself on the next application
start, while baskets and orders start empty.
