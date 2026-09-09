# Bruno collection — EShop Modular Monolith

Manual test collection covering every REST endpoint currently exposed by the app (Basket, Catalog, Ordering), plus a request to obtain a Keycloak access token.

The collection and its **default** environment are both committed, so a fresh clone can authenticate and call every endpoint without any setup step.

## Prerequisites

1. Start infra: `docker compose -f SpringBoot-EshopModularMonolith/docker/sb-eshop/compose.yaml up -d` (Postgres, MongoDB, Keycloak).
2. Run the app on port `8080`: `./mvnw -pl starter spring-boot:run` from `SpringBoot-EshopModularMonolith/`, or your IDE. No profile is needed — the committed `application.yaml` points at the compose stack.
3. Open this folder as a collection in Bruno and select the **default** environment.

## Credentials

The **default** environment carries working credentials: user `john` / `password`, and the `eshop-app` client secret `local-demo-secret`. These are deliberately committed. They are local-only demo values that exist solely in `docker/sb-eshop/keycloak-init/eshop-realm.json`, which likewise commits `admin`/`admin` for the Keycloak container itself. They authenticate against nothing but a container on your own machine.

`access_token` is declared as a secret var so Bruno keeps it out of the file — the token request writes to it on every run, and a plain var would leave the committed environment permanently dirty in `git status`.

To point the collection somewhere else, or authenticate as a different user, create an additional environment in Bruno and select that instead. Leave **default** as the one that works against the compose stack out of the box.

## Authenticating

Run **Auth → Get Token (Password Grant)** first. It calls Keycloak's token endpoint with the `password` grant using `username`/`password`, authenticated as the confidential `eshop-app` client (`client_id`/`client_secret`). A post-response script stores the result in the `access_token` environment variable. Every other request inherits a Bearer auth header from the collection root, so nothing else needs to be configured. Re-run this request whenever the token expires (access tokens on this realm live 300s).

`CurrentUser` on the Basket endpoints is resolved from the JWT's `preferred_username` claim ([CurrentUserArgumentResolver.java](../SpringBoot-EshopModularMonolith/security/src/main/java/com/github/eshopmodularmonolith/security/CurrentUserArgumentResolver.java)), so authenticating as `john` means basket requests act as that user.

## Structure

- **Auth** — token acquisition.
- **Basket** — create/get/delete basket, add/remove items, checkout. All scoped to the current JWT user.
- **Catalog** — create/get/update/delete products, paginated product listing.
- **Ordering** — create/get/delete orders, paginated order listing.

## Working with ids

The catalog is seeded at startup with three products carrying fixed ids, so requests that need a product id work immediately:

| Product | Id |
| --- | --- |
| Galaxy Smartphone | `a1000000-0000-4000-8000-000000000001` |
| Aurora Laptop | `a1000000-0000-4000-8000-000000000002` |
| Pulse Headphones | `a1000000-0000-4000-8000-000000000003` |

**Add Item Into Basket** and **Remove Item From Basket** already point at the Galaxy Smartphone. The seed is controlled by `eshop.catalog.seed-demo-data` in `application.yaml`; with it turned off, create a product via **Catalog → Create Product** and use the id from the response instead.

The remaining requests still take placeholder values you need to fill in yourself: `Get Product By Id`, `Update Product`, `Delete Product`, `Get Order By Id` and `Delete Order` take a UUID path param, and **Create Order** carries `00000000-0000-0000-0000-000000000000` for its customer and item ids. Fill those from a prior `Create` response or a `Get` listing.
