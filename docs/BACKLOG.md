# Backlog

Work this project has decided to do later, recorded with the reasoning that put it here — so a future session can pick something up without re-deriving why it was left.

**What belongs here vs. [KNOWN_ISSUES.md](KNOWN_ISSUES.md).** Ask one question: *is the code wrong or unverified right now?*

| The code is… | Goes in |
| --- | --- |
| broken, or behaving correctly with nothing proving it | [KNOWN_ISSUES.md](KNOWN_ISSUES.md) |
| fine as far as it goes, but a capability is missing or a decision is unmade | here |

Route by **nature, not by origin**. Something surfaced while deferring a plan step can still be a defect — most of this project's deferrals have been. "A plan deferred it" is not a category.

**Entry bar:** work actually decided on and deliberately postponed. Not ideas, not "would be nice". A backlog that accepts those stops being read, which is the only way a backlog fails.

**Lifecycle:** an entry leaves when it is done, when it is promoted into a plan under `ai-coding-tools/plans/`, or when it is consciously dropped. Entries here never duplicate a fix already written as a Known Issue's *Action Required* — two copies of one decision drift apart.

Entries are self-contained on purpose. Plan documents are frozen history and may not be reachable; nothing here should require reading one.

## 🎯 Ready to pick up

### 3. `MongoAuditMetadata`
- Ordering has no counterpart to `shared.infrastructure.JpaAuditMetadata`, so Mongo-backed documents carry no created/modified audit fields.
- **Blocked on:** adding `@Version` to the documents. Mongo auditing decides created-vs-modified purely from `isNew`, which is id-based, and every document in this codebase assigns its id up front — so without a version field auditing cannot distinguish the two cases at all.
- **Note:** whatever shape this takes must not write the audit fields from the domain projection. Catalog's `JpaProductRepository.save` originally rebuilt a whole `ProductDB` from the domain object and merged it over the stored row, which silently wiped `created_by` on every update; it now loads the stored row and mutates only the fields the domain owns. `MongoBasketRepository.save` and `MongoOrderRepository.save` have the same shape — they hand `repository.save` a document rebuilt from the domain, which replaces the stored one wholesale. Harmless only while `ShoppingCartDB`/`OrderDB` are total projections of what is stored; the audit fields added here would be wiped on the next basket update. Both are records, so the fix is not JPA-style mutation: either `$set` the domain-owned fields, or carry the stored audit values forward into the new record.

### 12. Nothing Stops a Test Dependency Being Declared at Compile Scope

- Until 2026-09-10, `ordering` and `basket-core` each declared `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-mongodb-test` with no `<scope>`, so Maven resolved all six at `compile`. Test infrastructure sat on those modules' production classpath, propagated transitively, and ~1.3 MB of it was physically packaged into `starter/target/starter.jar`. The scopes are now correct and the jar is clean; nothing prevents the next declaration from repeating it.
- **Why it matters:** the failure is silent. The build stays green, the suite stays green, and the only symptom is test machinery — `@TestConfiguration`, `@AutoConfigure*`, `TestPropertySource` — on the production classpath where component scanning can reach it. It was found by running a plugin by hand, not by anything failing.
- **The detector exists.** `maven-dependency-plugin`'s `analyze` goal reports exactly this as `Non-test scoped test only dependencies found`.
- **Two things must be settled before it can gate the build:**
  1. **The plugin has to be pinned to 3.9.0.** The version inherited from the Spring Boot BOM chain is 3.7.0, whose bundled ASM cannot read Java 25 bytecode — it fails with `Unsupported class file major version 69` and cannot run on this project at all. 3.9.0 works and currently has to be invoked explicitly: `./mvnw org.apache.maven.plugins:maven-dependency-plugin:3.9.0:analyze`.
  2. **`failOnWarning` cannot be switched on as-is.** Measured on 2026-09-10 with the scopes already fixed, the `Non-test scoped test only` warning still fires for **six modules** — `outbox-jpa`, `catalog-core`, `ordering`, `outbox-mongo`, `basket-core`, `starter` — across fourteen entries, and **not one of them is a declared test library**. They are all transitive compile dependencies that happen to be referenced only from test classes (`spring-boot-persistence`, `spring-beans`, `spring-context`, `spring-boot-autoconfigure`, `mongodb-driver-sync`, `jackson-databind`, `spring-core`, and `starter`'s own sibling modules `shared`/`basket-core`/`catalog-core`). The plugin's heuristic is "referenced only by this module's test classes", which is not "is a test library". A guard has to distinguish the two — by matching on the declaration rather than the resolved graph, or by an explicit ignore list — or it will fail every build.
- **Do not** treat `analyze`'s other output as a to-do list. It reports every Spring Boot starter as `Unused declared` and the starter's contents as `Used undeclared`, because a starter POM has no classes of its own. Declaring the aggregator and using what it brings in is deliberate here. The same goes for `outbox-core` showing up as `Used undeclared` in `basket-core` and `catalog-core`: they reach it through their outbox adapter on purpose, so that choosing an adapter is all it takes to get the outbox (see [ARCHITECTURE.md](ARCHITECTURE.md#-outbox-modules)).
- **Structurally the same shape as the coverage-aggregator question**, now a note in [DESIGN_NOTES.md](DESIGN_NOTES.md): a hand-maintained fact about the build with no check behind it, where the failure is silent and flatters. That one was demoted because it only protects a reported number; this one keeps test infrastructure off the production classpath, so it stays committed work.
- **Why deferred:** the six scopes are correct as of 2026-09-10, so this buys protection against the next declaration rather than fixing a current defect. The `failOnWarning` noise above is its own configuration decision, which is why it was not bundled into the scope fix.

### 13. Back-Office Access to Orders
- Every ordering endpoint acts only on the calling user's own orders since 2026-09-16: `GET /orders`, `GET /order/{id}` and `DELETE /order/{id}`, with another customer's order answering `404`. Nothing can see across customers — no role exists in the realm or the application, and there is no back-office endpoint.
- **Decision needed first:** where roles live — Keycloak realm or client roles carried in the token, or roles owned by the application. Then what a back-office actually needs across customers: listing and reading are clear; deleting is doubtful, since cancelling an order is a state change rather than a removal (see Known Bug 13).
- **The seam that already exists:** `OrderQueryRepository` takes the viewer — `ordersVisibleTo(CurrentUser viewer, PageRequest)` and `findVisibleTo(CurrentUser viewer, UUID id)` — and `CurrentUser` is built by `CurrentUserArgumentResolver` from the JWT. The work is roles on `CurrentUser` (read from `realm_access.roles`, if they come from Keycloak), then one branch in `MongoOrderQueryRepository` that drops the `customer` criteria for a back-office viewer, and the same branch in `FakeOrderQueryRepository`.
- **Watch for:** that branch is authorization policy living in infrastructure, implemented twice. Two copies of the same logic drift apart: the three copies of `FakeOutboxEventStore` did, until they were merged on 2026-10-05. The alternative, if it starts to hurt: the handler turns the viewer into a visibility value (e.g. a sealed `OrderVisibility` with `OnlyPlacedBy(customer)` and `AllOrders`) and the adapters only translate it into criteria, keeping the decision in the application layer.
- **Why deferred:** closing the cross-customer exposure was the priority, and doing it did not require choosing a role model.

### 14. Start the Compose Stack With the Application
- **Priority: low.** A local-development convenience: running `docker compose up -d` by hand, as [SETUP.md](SETUP.md) describes, works today.
- **Goal:** starting the application brings the compose stack up only as far as needed — running stack: nothing happens; stopped: started; down: created and started. That is `docker compose up`'s own behaviour.
- **Shape:** Spring Boot's Docker Compose support. Add `spring-boot-docker-compose` to `starter/pom.xml` as `<optional>true</optional>`, which keeps it out of the repackaged jar; it is skipped in tests by default, so `ApplicationWiringTest` keeps its Testcontainers. Configure `spring.docker.compose.file` to point at `docker/sb-eshop/compose.yaml`, and `spring.docker.compose.lifecycle-management: start-only` — the default, `start-and-stop`, stops the stack every time the application exits. A stack that was already running before the application started is never stopped either way.
- **Keep `application.yaml` authoritative.** For images it recognizes — Postgres and MongoDB here — Boot creates connection details from the running containers, and those take precedence over `spring.datasource.url` and `spring.mongodb.uri`, silently. There is no setting that keeps the connection details but lets the properties win; the opt-out is the `org.springframework.boot.ignore: true` label, per service. MongoDB is the one that matters: the replica set is initiated with the internal host name `mongodb:27017`, and the current `mongodb://localhost:27020` works because a single-host URI connects directly. A Boot-derived URI may trigger member discovery, which cannot resolve `mongodb:27017` from the host. Label `mongo-init` as ignored regardless — it is a one-shot job with nothing to wire. Keycloak is not a recognized image, so `issuer-uri` is never rewired.
- **Verify before relying on it:**
    - whether readiness checks also skip ignored services. If they do, the application can start before Postgres or MongoDB accept connections; `docker compose up --wait` waits on the healthchecks both services already declare, passed through what should be `spring.docker.compose.start.arguments` — confirm the property exists in Boot 4.0.
    - whether `spring.docker.compose.file` resolves from both `./mvnw spring-boot:run` and an IntelliJ run configuration, which can use different working directories.
- **Alternative considered:** an IntelliJ Docker Compose run configuration under the application's *Before launch*. It works, but only in IntelliJ and only for whoever has the configuration, unless it is committed under `.run/`. Boot's support also covers `spring-boot:run` and a fresh clone.
- **When done:** update [SETUP.md](SETUP.md)'s start-infrastructure step, and the Prerequisites in [bruno/README.md](../bruno/README.md).
- **Why deferred:** convenience, not a defect. Raised on 2026-09-16 while working on order visibility, and kept out of that change.

### 15. Make the Grouping Poms Aggregators
- **Priority: low.** Nothing is broken. The build is correct, but its poms describe a structure that isn't there.
- **Today:** `modules/pom.xml`, `modules/basket/pom.xml` and `modules/catalog/pom.xml` are standalone `packaging pom` projects. None of them has `<modules>`, and no module uses them as `<parent>`. The root pom is both the parent and the aggregator of every module and lists each one by path (`modules/basket/basket-core`, …). The folder nesting suggests a Maven hierarchy that doesn't exist, and each of the three poms only produces an empty artifact. No other pom references the `modules`, `basket` or `catalog` artifacts (checked 2026-10-06).
- **Target:** the layout `outbox/` got in [outbox-dead-letter-and-payload-compatibility](../ai-coding-tools/plans/outbox-dead-letter-and-payload-compatibility/outbox-dead-letter-and-payload-compatibility.md) increment 2. Each grouping pom is an **aggregator only**: `modules/pom.xml` lists `basket`, `catalog` and `ordering`; `basket/pom.xml` lists `basket-core` and `basket-contract`; `catalog/pom.xml` lists `catalog-core` and `catalog-contract`. The root lists only top-level areas (`starter`, `shared`, `security`, `sonar`, `modules`, `outbox`).
- **Keep the parent where it is.** Every leaf module keeps the root as its `<parent>`. Making the grouping poms parents too only pays off when a group shares configuration, and all configuration lives in the root. An extra parent level would add a `relativePath` chain and a second place to look for settings, with nothing in it.
- **Watch for:**
    - each module may be listed only once in the reactor, so the root's `<modules>` must shrink in the same change;
    - `sonar/pom.xml` collects modules through `<dependencies>`, not `<modules>`, and doesn't change;
    - building one group alone (`./mvnw -f modules/basket/pom.xml verify`) only includes that group's modules in the reactor, so anything they depend on (`shared`, `catalog-contract`, `outbox-mongo`) must already be installed by a root `install`.
- **Shape:** poms only, behaviour unchanged, checked by a green `./mvnw clean verify`. Fits the [quick-fix](../ai-coding-tools/processes/quick-fix.md) workflow.
- **Why deferred:** raised on 2026-10-06 while grouping the outbox modules, and kept out of that increment because it's unrelated to the outbox.

### 16. Dead-Letter Metric
- **Priority: low.** Dead-lettering is visible today only as an ERROR log line and a stored state (see [ARCHITECTURE.md](ARCHITECTURE.md), *Retry and dead-letter*).
- **Goal:** a Micrometer gauge such as `eshop.outbox.dead_lettered{module=…}`, so a dashboard or alert can see dead-lettered events without reading logs.
- **Shape:** the gauge needs a count, so the `OutboxEventStore` port gains a count query (rows with `deadLetteredOn` set), implemented in both adapters and the fake. Register one gauge per module dispatcher.
- **Constraint:** it must not affect health status. A dead-lettered event is a data problem for an operator, not a reason to take the application out of rotation.
- **Why deferred:** decided with the user in [outbox-dead-letter-and-payload-compatibility](../ai-coding-tools/plans/outbox-dead-letter-and-payload-compatibility/outbox-dead-letter-and-payload-compatibility.md) as an improvement on top of the dead-letter state, not part of closing the endless retry.

### 17. Replay Operation for Dead-Lettered Events
- **Priority: low.** Replay works today by hand: one Mongo `updateOne` or one SQL `UPDATE`, documented in [ARCHITECTURE.md](ARCHITECTURE.md), *Retry and dead-letter*.
- **Goal:** an endpoint or admin command that requeues a dead-lettered event: clear its dead-letter time, reset its attempt count, make it due now.
- **Shape:** an explicit port method (for example `requeue(UUID eventId, Instant now)`) rather than reusing `save`, which accepts no delivery state on purpose. One implementation per adapter, tested in the adapter tests like the other updates.
- **Blocked on:** authorization. Requeueing is an operator action, so it needs a role, which depends on Backlog 13's decision about where roles live.
- **Why deferred:** decided with the user in the same plan as Backlog 16.

### 18. Money Amounts as Strings in the HTTP API
- **Priority: low.** No precision has been lost so far, but nothing in the API contract prevents it.
- **Problem:** amounts travel as JSON numbers: `ProductDTO.price`, `ItemDTO`, and `CreateProductRequest`/`UpdateProductRequest`. The server side is exact: a typed `BigDecimal` field reads the number's text directly, and `spring.jackson.deserialization.use-big-decimal-for-floats` (in `starter`'s `application.yaml`) covers untyped reads. A JavaScript client, however, parses every JSON number as a `double`. It can drop the scale (`550.00` becomes `550`) and, for amounts with more significant digits than a `double` holds, change the value. A client that loads a product and sends its price back in an update request would then overwrite the stored price.
- **Decision to make:** whether amounts become JSON strings in the API, through `@JsonFormat(shape = JsonFormat.Shape.STRING)` on the fields or one `BigDecimal`-as-string serializer for the whole API. It changes the contract for every client, including the Bruno collection, so it is a decision, not a fix.
- **Related, separate question:** an update request that carries a price nobody meant to change. A partial update, or a dedicated price change, would remove the round trip altogether.
- **Why deferred:** raised by the user on 2026-10-18, while fixing the outbox's decimal precision in [outbox-dead-letter-and-payload-compatibility](../ai-coding-tools/plans/outbox-dead-letter-and-payload-compatibility/outbox-dead-letter-and-payload-compatibility.md) increment 5, which only covers the server side.

### 19. Show the Checkout Price Before Paying
- `POST /basket/checkout` returns `202` with no body. The only prices a customer ever sees are the cart's (`priceWhenAdded`), while checkout charges the current catalog price, which can differ. The customer pays a price they were never shown.
- **What exists since 2026-10-19:** the checkout price is carried on each checked-out line and recorded on the order (each line's `price`, and `Order.totalPrice` derived from them), so the order shows afterwards what was paid. Showing it *before* paying is what's missing.
- **Shape options:** a quote step, e.g. `GET /basket/checkout-preview`, pricing the cart through the same `ShoppingCart` logic checkout uses; or refreshing the cart's prices whenever it is read.
- **Decision needed:** whether a price shown in a preview is binding (held for a while at checkout, which needs a quote with an expiry) or informational only (checkout may still charge a newer price).
- **Why deferred:** raised on 2026-10-19 in [checkout-price-on-order](../ai-coding-tools/plans/checkout-price-on-order/checkout-price-on-order.md), which placed the price at checkout and on the order; a preview is separate work.

### 20. 3-D Secure (Bank Authentication) at Checkout
- Checkout uses Stripe's *card payments without bank authentication* flow: the server confirms the PaymentIntent with `error_on_requires_action=true`, and a card that needs 3-D Secure is declined (`authentication_required` / `authentication_not_handled`). Stripe documents that flow for US and Canadian cards only. In a European shop, where banks regularly ask for authentication, many legitimate cards would be declined; this is the largest gap between the project and a production integration.
- **Shape:** checkout becomes two steps. The server creates the PaymentIntent and returns its `client_secret`; the browser confirms it with Stripe's Payment Element, which runs 3-D Secure; the server learns the outcome from a webhook (`payment_intent.succeeded` / `payment_intent.payment_failed`) and only then records `BasketCheckedOut`. That needs a pending-checkout state, webhook handling with signature verification, and a client, none of which exist today.
- **Decision needed:** whether the basket is held while a payment is pending, and what a pending checkout looks like to the customer.
- **Sources:** [stripe-integration.md](../ai-coding-tools/context/stripe-integration.md) (section 2).
- **Why deferred:** the increment that introduced payments, [tokenized-card-payment](../ai-coding-tools/plans/tokenized-card-payment/tokenized-card-payment.md), was scoped to getting card data out of the system; 3-D Secure needs a client this project doesn't have.

### 21. Saved Cards (Card on File)
- A returning customer enters their card every time. Saved cards would let them pick one.
- **Shape:** the provider keeps the card. The shop creates a Stripe Customer for each user, attaches the payment method to it (`setup_future_usage` on the PaymentIntent), and stores only the customer reference, the payment method id and the card summary for display. A later checkout sends the saved payment method id with the customer reference. No card data is stored by the shop either way.
- **Depends on:** item 20 in practice: a saved card used later usually needs 3-D Secure on first use, and off-session charges have their own authentication rules.
- **Why deferred:** raised on 2026-10-21 while designing [tokenized-card-payment](../ai-coding-tools/plans/tokenized-card-payment/tokenized-card-payment.md); not needed to remove card data from the system.

### 22. Running the End-to-End Suite in GitHub Actions
- The [end-to-end suite](../e2e/README.md) in `e2e/` runs locally, through compose's `e2e` profile or from the host. No workflow runs it, so a change that breaks a flow across modules, Keycloak or the payment provider is caught only if someone runs it by hand.
- **What CI needs:**
  - a workflow of its own on PRs, uploading `e2e/target/site/serenity/` as an artifact;
  - caching for the application's image build and the suite's `~/.m2`;
  - a checkout that includes `e2e/`: both existing workflows check out only `SpringBoot-EshopModularMonolith/` (`sparse-checkout`), and their `paths` filters don't list `e2e/` or `docker/`;
  - `user:` on the `e2e` service, or an equivalent: the container writes the report as root, which on Docker Desktop for Mac is invisible but on Linux leaves root-owned files in the mounted folder.
- **Decision needed:** whether the job gates PRs or only runs on `main`.

### 23. The Full Stack on Minikube
- `Kubernetes/simple-deployment` deploys the application only, and has drifted: its ConfigMap sets only the Postgres datasource (no MongoDB URI, issuer or payment provider), so the application can't start from it, and `imagePullPolicy: Always` on the local `sb-eshop:test` image fails on Minikube unless the image is in a registry.
- **Goal:** everything the application needs runs in the cluster, with a README that takes a reader from `minikube start` to a placed order. It shows Deployments, Services, ConfigMaps, Secrets, StatefulSets with persistent volumes for Postgres and MongoDB, a Job for the replica-set initiation, and probes.
- **Shape (settled with the user, 2026-10-26):** plain manifests with kustomize, split into infrastructure and application. GitOps (Argo CD) is out of scope for this repository; it belongs to a separate project.
- **Depends on:** an image built from source and the settings-per-environment table from [compose-runs-the-application](../ai-coding-tools/plans/compose-runs-the-application/compose-runs-the-application.md). The fixed-issuer approach carries over: Keycloak's public hostname fixed, the application fetching keys at the in-cluster address through `jwk-set-uri`.
- **Decisions needed:** how a host-side client reaches the application and Keycloak (port-forward, `minikube service`, or an Ingress), which also decides what Keycloak's fixed hostname is in the cluster; whether the end-to-end suite (item 22) also runs against the cluster.

## ⏳ Gated on a trigger

Nothing to do until the stated condition holds. Deliberate, not neglected.

### 4. Distinguishing an Incomplete Order
- Nothing in the read model marks an order as pending or incomplete.
- **Trigger:** a "complete your order" step, or any flow that can produce a partially specified order. Checkout currently produces complete orders and `Order.placeFromCheckout` rejects missing address or payment, so there is no incomplete state to represent.

### 5. `OrderCreated` Domain Event and Outbox Wiring
- `Order` never calls `addDomainEvent`, and `OrderingModuleConfig` has no outbox wiring — unlike basket and catalog.
- **Trigger:** another module actually needing to react to order creation. Deliberate YAGNI; a speculative event plus outbox is real infrastructure to maintain in exchange for nothing.
- **When the trigger fires:** checkout deduplication relies on order creation being a single write — a redelivered checkout is rejected by the unique index on `checkoutId` ([one-order-per-checkout](../ai-coding-tools/plans/one-order-per-checkout/one-order-per-checkout.md)). An outbox write added to the same handling is not covered by that index: it has to commit or roll back together with the order insert, as basket's and catalog's do through `OutboxEventSubscriber` writing inside the module's transaction. `CreateOrderHandler` opens no transaction today (Testing Gap 6), and Mongo transactions need a replica set. Also check what the listener catches once inserts are transactional: two concurrent inserts for the same checkout may fail with a write conflict rather than a duplicate key, which the listener would not recognise as "already placed".

### 6. Test Naming Pass — GWT as Structure
- Move the suite from `givenX_whenY_thenZ` method names to GWT-as-structure (`@Nested` for a shared given, method name = outcome only). Full reasoning and steps in [gwt-test-naming-later-refactor](../ai-coding-tools/plans/gwt-test-naming-later-refactor/gwt-test-naming-later-refactor.md), and the target style in [TESTING_CONVENTIONS.md](TESTING_CONVENTIONS.md).
- **Trigger:** the owner declaring the blueprint finished. Not during feature work — mixing styles mid-project is worse than a consistent noisy one.
