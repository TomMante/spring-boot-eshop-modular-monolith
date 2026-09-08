# Bruno collection — EShop Modular Monolith

Manual test collection covering every REST endpoint currently exposed by the app (Basket, Catalog, Ordering), plus a request to obtain a Keycloak access token.

The collection itself is committed; `environments/local.bru` is not, because it holds the Keycloak client secret and a user password. Bruno creates it for you when you select the **local** environment — fill it in following *One-time setup* below.

## Prerequisites

1. Start infra: `docker compose -f SpringBoot-EshopModularMonolith/docker/sb-eshop/compose.yaml up -d` (Postgres, MongoDB, Keycloak).
2. Run the app (default port `8080`, e.g. via the `starter` module or your IDE).
3. Open this folder as a collection in Bruno, and select the **local** environment.

## One-time setup: Keycloak client secret and user password

The `eshop-app` client (defined in `docker/sb-eshop/keycloak-init/eshop-realm.json`) is confidential, so its secret isn't checked into the repo (Keycloak masks it on export). The `password` env var is likewise left blank on purpose.

1. Open the Keycloak admin console: http://localhost:9090 (`admin` / `admin`).
2. Realm **eshop** → Clients → **eshop-app** → Credentials tab → copy the **Client secret**.
3. In Bruno, open the **local** environment and paste it into `client_secret` (it's marked as a secret var — it's never written into a `.bru` file, so this step must be repeated whenever you re-import the realm or use a different Keycloak instance).
4. Also set `password` to the password of the `john` user (`username` is already set to `john`). Change `username` in the environment if you want to authenticate as a different user.

## Authenticating

Run **Auth → Get Token (Password Grant)** first. It calls Keycloak's token endpoint with the `password` grant using `username`/`password`, authenticated as the confidential `eshop-app` client (`client_id`/`client_secret`). A post-response script stores the result in the `access_token` environment variable. Every other request inherits a Bearer auth header from the collection root, so nothing else needs to be configured. Re-run this request whenever the token expires (access tokens on this realm live 300s).

`CurrentUser` on the Basket endpoints is resolved from the JWT's `preferred_username` claim ([CurrentUserArgumentResolver.java](../SpringBoot-EshopModularMonolith/security/src/main/java/com/github/eshopmodularmonolith/security/CurrentUserArgumentResolver.java)), so authenticating as `john` means basket requests act as that user — swap `username`/`password` in the environment to test as a different person.

## Structure

- **Auth** — token acquisition.
- **Basket** — create/get/delete basket, add/remove items, checkout. All scoped to the current JWT user.
- **Catalog** — create/get/update/delete products, paginated product listing.
- **Ordering** — create/get/delete orders, paginated order listing.

`Get Product By Id`, `Update Product`, `Delete Product`, `Get Order By Id`, and `Delete Order` take a UUID path param (`id`) — fill it in from a prior `Create` response or a `Get` listing. Request bodies use placeholder UUIDs (`00000000-0000-0000-0000-000000000000`) and sample data — replace with real IDs as you go, e.g. copy a product ID returned by **Create Product** into **Add Item Into Basket**.
