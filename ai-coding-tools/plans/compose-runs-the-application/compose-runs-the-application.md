# Compose Runs the Application

Approved by the user on 2026-10-27.

## Status
- [x] Step 1 — The image builds from source, with no JDK on the host. Started alone, it runs as
  `apiuser` on Java 25 and fails only on the missing Postgres. Extraction keeps the source jar's
  name, so the build stage copies `starter.jar` to `application.jar` before extracting, and the
  entrypoint is unchanged.
- [x] Step 2 — Keycloak issues tokens under one fixed issuer, whoever asks. A token requested on
  `keycloak:8080` from a container on the compose network carries
  `iss=http://localhost:9090/realms/eshop`, and the composed application accepts it.
- [x] Step 3 — An `app` compose profile runs the built image against the stack. Smoke run on
  2026-10-27 from `down -v`, in both modes (application in Docker; application on the host via
  `./mvnw`): token, catalog, basket, a declined checkout (`402`, basket kept), checkout (`202`),
  and the order listed. `down -v` without `--profile app` leaves the application container
  running, so SETUP.md says to add the profile.
- [x] Step 4 — Docs.

## Context

Today the application runs only from a JDK: `docker compose up -d` starts Postgres, MongoDB,
Keycloak and the Stripe stand-in, and the application itself runs on the host with
`./mvnw -pl starter spring-boot:run`. Trying the project needs JDK 25 installed.

There is a `Dockerfile`, but it can't produce an image from a clean clone either. It copies a
prebuilt jar (`ARG JAR_FILE=./starter/target/starter.jar`), so `./mvnw package` has to run on the
host first. Nothing builds or runs the image: not compose, not CI.

This is the first of three increments that build on each other:
1. **This plan:** compose builds and runs the application.
2. Automated end-to-end tests (Serenity BDD) that compose runs against the composed application,
   locally and in GitHub Actions.
3. The full stack on Minikube, replacing `Kubernetes/simple-deployment`.

Increments 2 and 3 both need what this plan settles: an image built from source, and the set of
settings that change when the application runs in a container rather than on the host.

### Decisions (settled with the user, 2026-10-26)

- **Compose `build:`**, not an image published from CI. A published image is useful later (a
  cluster that pulls from a registry). It isn't needed to run the stack locally.
- **One compose file with profiles**, not override files:
  - `docker compose up -d` starts the infrastructure only, as today. That mode is for running the
    application from the IDE or `./mvnw`.
  - `docker compose --profile app up -d --build` also builds and starts the application.
  - Increment 2 adds an `e2e` profile to the same file.

  The two modes differ only in whether the `app` service exists. Its container-specific settings
  live on that one service, so separate override files would add a file per mode without
  anything to override.
- **No change to `application.yaml`.** The committed defaults keep pointing at the host ports of
  the compose stack (`DefaultConfigurationTest` guards that they are complete). The container
  overrides them through environment variables on the `app` service only.
- **CI doesn't build the image in this increment.** A `docker build` step would only check that
  the `Dockerfile` still builds, then discard the image. Increment 2 runs compose with `--build`
  in GitHub Actions, which makes the same check as a side effect. Until then, CI doesn't check
  the `Dockerfile`.

## Design

### Settings that change in a container

On the host, the application reaches every service through a published port on `localhost`.
In the compose network, `localhost` is the application's own container, so it reaches each
service by its compose name instead:

| Setting | Host (`application.yaml`) | `app` container (environment) |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/EShopDb` | `SPRING_DATASOURCE_URL=jdbc:postgresql://eshopdb:5432/EShopDb` |
| `spring.mongodb.uri` | `mongodb://localhost:27020` | `SPRING_MONGODB_URI=mongodb://mongodb:27017/?replicaSet=rs0` |
| `eshop.basket.payment.stripe.api-base` | `http://localhost:12111` | `ESHOP_BASKET_PAYMENT_STRIPE_APIBASE=http://payment-provider:8080` |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | unset (discovered from the issuer) | `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI=http://keycloak:8080/realms/eshop/protocol/openid-connect/certs` |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://localhost:9090/realms/eshop` | unchanged |

- **MongoDB.** On the host, the URI has a single host and no `replicaSet`, so the driver connects
  directly and never tries to resolve the replica set member `mongodb:27017`, which the host can't
  reach. Inside the network that name resolves, so the container can use the replica-set URI.
- **This table is the seed of a "settings per environment" reference.** Increment 3 adds a
  Minikube column. Every new setting must then land in `application.yaml`, compose and the
  manifests. Where the table finally lives (SETUP.md or ARCHITECTURE.md) is decided in step 4.

### The issuer problem (step 2)

The application checks every token's `iss` claim against `issuer-uri`. Keycloak writes `iss`
from the URL the token was requested on:
- a client on the host requests `http://localhost:9090/...` and gets
  `iss=http://localhost:9090/realms/eshop`;
- the application container can reach Keycloak only at `http://keycloak:8080`, and with
  `issuer-uri` alone, Spring calls `<issuer-uri>/.well-known/openid-configuration` at the first
  request to find the keys. `localhost:9090` doesn't resolve to Keycloak from inside the
  container.

Fix, in two halves:
- **Keycloak:** `KC_HOSTNAME=http://localhost:9090` on the `keycloak` service. Under hostname v2,
  a full URL fixes the scheme, host and port of every frontend URL Keycloak generates, `iss`
  included, whatever host the request came in on. Backchannel URLs follow the frontend ones
  unless `hostname-backchannel-dynamic` is enabled, and it stays off. So a token requested on
  `keycloak:8080` from inside the network (the Serenity runner in increment 2) carries the same
  `iss` as one requested on `localhost:9090`.
- **Application container:** set `jwk-set-uri` to the internal address and keep `issuer-uri`.
  Per Spring Security's reference (*Specifying the Authorization Server JWK Set Uri Directly*),
  with both set, the resource server fetches keys from `jwk-set-uri`, never calls the issuer's
  discovery endpoint, and still validates `iss` against `issuer-uri`. The configured `audiences`
  check is unaffected.

The host mode is unchanged by this. Its tokens were already issued on `localhost:9090`.

Considered and not chosen:
- `extra_hosts` mapping `localhost` tricks, or `network_mode: host`: platform-dependent, and
  `network_mode: host` doesn't work the same on Docker Desktop for Mac.
- Pointing clients at `keycloak:8080` from the host through `/etc/hosts`: puts a manual step on
  every visitor.

### The image (step 1)

- **A Maven build stage** on a JDK 25 Alpine image (the same Temurin/Alpine line as the runtime
  stage), with the wrapper and
  `./mvnw -B -pl starter -am package -DskipTests`.
  - `-DskipTests`: the suite needs Docker for Testcontainers, which isn't available inside an
    image build. The tests run in CI, not here.
  - `-pl starter -am` builds only what the jar needs, leaving out `sonar`.
  - `~/.m2` as a BuildKit cache mount (`RUN --mount=type=cache,target=/root/.m2`), so a rebuild
    doesn't download every dependency again. Compose builds with BuildKit by default.
  - The wrapper is the `only-script` type and downloads Maven with `wget` or `curl`. Check that
    the chosen JDK image has one of them.
- **The layer extraction moves into the build stage**
  (`java -Djarmode=tools -jar starter/target/starter.jar extract --layers`), and the separate
  JRE builder stage goes, together with its `gcompat` package and `LD_PRELOAD` (settled with the
  user, 2026-10-27). They were added with the first, untested Kubernetes files, with no reason
  recorded, and the extraction is plain Java. Two stages remain: build on the JDK, run on the JRE.
- **The `JAR_FILE` build argument goes**, along with copying a host-built jar. One way to build
  the image, from source.
- **Build context:** `SpringBoot-EshopModularMonolith/`, the directory holding `mvnw` and the root
  pom. A new `.dockerignore` there keeps `**/target/`, `.idea/`, `*.iml`, `.DS_Store` and
  `docker/` out of the context. `docker/` holds compose's mounted files, which the image doesn't
  need.
- **The runtime stage is unchanged:** layered extraction, non-root `apiuser` (uid 1002),
  `java -jar application.jar`.

### The `app` service (step 3)

- `build: { context: ../.., dockerfile: Dockerfile }`, relative to `docker/sb-eshop/`.
- `profiles: [app]`.
- `ports: "8080:8080"`. The host reaches the application at the same address in both modes, so
  `SETUP.md`'s requests and both Bruno environments work unchanged.
- The environment from the table above.
- `depends_on`:
  - `eshopdb: service_healthy`;
  - `mongo-init: service_completed_successfully`, since the replica set must be initiated before
    the first transaction;
  - `keycloak: service_started`. With `jwk-set-uri`, startup doesn't contact Keycloak: keys are
    fetched on the first authenticated request;
  - `payment-provider: service_started`.
- A `healthcheck` on `/actuator/health/readiness` (exposed and probe-enabled already), using
  BusyBox `wget` from the Alpine runtime image. That lets `docker compose --profile app up -d
  --wait` return only once the application is ready. Increment 2 needs exactly that before it
  starts the tests.

## Steps and verification

Nothing under `src/` changes, so there is no red/green cycle in this plan. The seam is the
composed stack itself, and the behaviour is "a fresh clone with only Docker places an order".
Increment 2 automates that check. Here it is a manual smoke run at the end of steps 2 and 3,
recorded in this plan.

1. **Image builds from source.** `Dockerfile` build stage, `.dockerignore`.
   - Verify: `docker build SpringBoot-EshopModularMonolith` succeeds on a clone with no
     `target/` directories, and the image starts (it fails on the missing datastores, which is
     expected; it must get that far). Without `gcompat`, a failure would show here, at JVM
     start, before any connection attempt.
2. **Fixed issuer.** `KC_HOSTNAME` on `keycloak`.
   - Verify the host mode didn't change: infrastructure only, application via `./mvnw`, and the
     SETUP.md walk-through through to a placed order.
   - Verify a token requested on `keycloak:8080` from inside the network carries
     `iss=http://localhost:9090/realms/eshop`, e.g. with a throwaway `curl` container on the
     compose network.
3. **`app` profile.** The `app` service.
   - Verify: `docker compose down -v`, then `docker compose --profile app up -d --build --wait`,
     then SETUP.md's walk-through from *Get a token* through to a placed order, from the host,
     with no JDK involved. Include one declined checkout, to cover the Stripe stand-in's address.
4. **Docs.**
   - `README.md`: a second quickstart line, *without a JDK*, using the `app` profile. Update the
     `SpringBoot-EshopModularMonolith/` row of *Repository Structure* (the Dockerfile now builds
     from source).
   - `docs/SETUP.md`: Docker-only as an alternative to steps 2 and 3, and the settings table.
   - `bruno/README.md` *Prerequisites*: the second way to run the application.

## Deferred

- A published image (e.g. GHCR from CI): needed when a cluster pulls from a registry. Not needed
  for compose, or for Minikube, which can load or build a local image.
- Backlog 14 (*Start the Compose Stack With the Application*) is the opposite direction: the
  application on the host starting the stack through Spring Boot's Docker Compose support. This
  plan doesn't close it and doesn't conflict with it.
