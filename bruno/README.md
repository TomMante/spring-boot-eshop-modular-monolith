# Bruno collection — EShop Modular Monolith

Manual test collection covering every REST endpoint currently exposed by the app (Basket, Catalog, Ordering), plus a request to obtain a Keycloak access token.

The collection and its two environments are committed, so a fresh clone can authenticate and call every endpoint without any setup step.

## Prerequisites

1. Start infra: `docker compose -f SpringBoot-EshopModularMonolith/docker/sb-eshop/compose.yaml up -d` (Postgres, MongoDB, Keycloak, and `payment-provider`, the WireMock container standing in for Stripe).
2. Run the app on port `8080`: `./mvnw -pl starter spring-boot:run` from `SpringBoot-EshopModularMonolith/`, or your IDE. No profile is needed — the committed `application.yaml` points at the compose stack.
3. Open this folder as a collection in Bruno and select the **default - john** environment.

## Credentials

Both environments point at the compose stack and differ only in the user:

| Environment | User |
| --- | --- |
| **default - john** | `john` / `password` |
| **default - jane** | `jane` / `password` |

Both use the `eshop-app` client secret `local-demo-secret`. These credentials are deliberately committed. They are local-only demo values that exist solely in `docker/sb-eshop/keycloak-init/eshop-realm.json`, which likewise commits `admin`/`admin` for the Keycloak container itself. They authenticate against nothing but a container on your own machine.

Two users exist so that per-user behaviour can be checked by hand: switch environment, re-run **Get Token**, and the same requests act as the other user. If `jane` fails to authenticate, your Keycloak predates her — see *Keycloak administration* in [SETUP.md](../docs/SETUP.md) for re-importing the realm.

`access_token` is declared as a secret var so Bruno keeps it out of the file — the token request writes to it on every run, and a plain var would leave the committed environment permanently dirty in `git status`. Bruno keeps secret values per environment, so each environment needs its own **Get Token** run.

To point the collection somewhere else, create an additional environment in Bruno and select that instead. Leave the two **default** environments as the ones that work against the compose stack out of the box.

## Authenticating

Run **Auth → Get Token (Password Grant)** first. It calls Keycloak's token endpoint with the `password` grant using `username`/`password`, authenticated as the confidential `eshop-app` client (`client_id`/`client_secret`). A post-response script stores the result in the `access_token` environment variable. Every other request inherits a Bearer auth header from the collection root, so nothing else needs to be configured. Re-run this request whenever the token expires (access tokens on this realm live 300s).

`CurrentUser` on the Basket and Ordering endpoints is resolved from the JWT's `preferred_username` claim ([CurrentUserArgumentResolver.java](../SpringBoot-EshopModularMonolith/security/src/main/java/io/github/tommante/eshopmodularmonolith/security/CurrentUserArgumentResolver.java)), so authenticating as `john` means basket requests act as that user and order requests see only his orders — another user's order answers `404`.

## Structure

- **Auth** — token acquisition.
- **Basket** — create/get/delete basket, add/remove items, checkout, and a declined checkout. All scoped to the current JWT user.
- **Catalog** — create/get/update/delete products, paginated product listing.
- **Ordering** — get/delete orders, paginated order listing. All scoped to the current JWT user. Orders are placed by checking out a basket, not through Ordering.

## Working with ids

The catalog is seeded at startup with three products carrying fixed ids, so requests that need a product id work immediately:

| Product | Id |
| --- | --- |
| Galaxy Smartphone | `a1000000-0000-4000-8000-000000000001` |
| Aurora Laptop | `a1000000-0000-4000-8000-000000000002` |
| Pulse Headphones | `a1000000-0000-4000-8000-000000000003` |

**Add Item Into Basket** and **Remove Item From Basket** already point at the Galaxy Smartphone. The seed is controlled by `eshop.catalog.seed-demo-data` in `application.yaml`; with it turned off, create a product via **Catalog → Create Product** and use the id from the response instead.

The remaining requests still take placeholder values you need to fill in yourself: `Get Product By Id`, `Update Product`, `Delete Product`, `Get Order By Id` and `Delete Order` take a UUID path param. Fill those from a prior `Create` response or a `Get` listing.

## Checking out

**Checkout Basket** and **Checkout Basket (Declined)** send `payment.paymentMethodId`, a token from the payment provider, never card details. In a browser, Stripe's card field would create it; here they use Stripe's test ids, which the `payment-provider` container answers like Stripe does:

| Request | `paymentMethodId` | Expected |
| --- | --- | --- |
| **Checkout Basket** | `pm_card_visa` (or `pm_card_mastercard`) | `202`; the order shows the card summary |
| **Checkout Basket (Declined)** | `pm_card_visa_chargeDeclined` | `402`, `basket.checkout.payment-declined`; the basket is kept |

Both requests assert their status, and each one's *Docs* tab lists the other test ids it accepts. Check out with a basket that has at least one item, or the answer is `400` (`basket.checkout.empty-cart`) before any payment is attempted.
