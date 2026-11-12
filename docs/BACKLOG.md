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

### 24. Organise the End-to-End Suite by Capability
- **Priority: before the suite grows.** With 4 features and 8 scenarios each change below is small. Every feature added in the current shape makes it larger.
- **Packages by capability, not by Screenplay type.** Today `e2e/` has flat `tasks/`, `questions/` and `stepdefinitions/` packages. Group them as `basket/`, `ordering/`, `catalog/` and `identity/`, each holding its tasks, questions and models. `BasketLine`, `OrderLine` and `OrderSummary` are models and leave `questions/`. `keycloak/` becomes `identity/`, named for its role rather than the vendor.
- **Feature folders by capability.** Serenity builds the report's requirements tree from the folders under `src/test/resources/features/`, which are flat today. Folders such as `features/ordering/checkout.feature` and `features/identity/authentication.feature` turn the report into documentation grouped by capability.
- **Step definitions only translate Gherkin into Screenplay calls.** `BasketSteps.noLongerHasABasket` sends `GET /basket` itself, and status codes and error codes (`basket.not-found`, `basket.checkout.payment-declined`) are asserted in step classes, so an API change touches both layers. Move these into questions (e.g. `TheBasket.exists()`). Assert through questions throughout: catalog steps assert on the raw response, basket and order steps through questions. Keep `seeThatTheResponseUnderTest` (in `e2e/api/`) for steps that check the status of the request under test.
- **The customer account as an ability.** The account is stored in actor memory under `CustomerSteps.CUSTOMER_ACCOUNT`, a string key that `StageSetup` also reads, and `ORDER_ID` is a second shared key. An ability that holds the account and its token and implements Serenity's `HasTeardown` to delete the user would remove the key, the `@After` loop and the `cast` field in `StageSetup`. **Verify before relying on it:** whether ability teardown runs on its own in this Cucumber setup, or needs `OnStage.drawTheCurtain()` in an `@After` hook.
- **Placing an order as one task.** `OrderSteps.hasPlacedAnOrder` builds create, add and checkout by hand, then waits for the order. A composite `PlaceAnOrder` task makes it reusable in other scenarios and shows it as one step in the report.
- **Why deferred:** raised on 2026-10-29 in a review of the suite added by [end-to-end-tests-with-serenity](../ai-coding-tools/plans/end-to-end-tests-with-serenity/end-to-end-tests-with-serenity.md). The suite works as it is; this is structure for growth.

### 25. Business-Readable End-to-End Scenarios
- **Priority: low.** Best done feature by feature, whenever one is touched anyway.
- **Implementation details in the Gherkin.** `checkout.feature` names Stripe test tokens (`"pm_card_visa"`, `"pm_card_visa_chargeDeclined"`), and its description mentions the outbox. Name the cards in business terms ("checks out with a card that is declined") and map them in one place, e.g. a `PaymentCard` enum carrying the token, brand and last four digits. "Paid with a visa card ending in 4242" then comes from the same entry, not from what the stub happens to return.
- **Steps with a fixed number of arguments.** `"{actor} sees the products {string}, {string} and {string}"` fits exactly three products. The order step packs line, total and card into one step with six parameters and has to be copied for an order with two lines. Use data tables for lists of products and order lines, and separate steps for the total and the card. `"{actor} has {int} {string} in the basket"` creates a new basket each time, so it can't describe a basket with two products either.
- **Expected values that depend on seed data the reader can't see.** The 1100.00 total is right only because a Galaxy Smartphone is seeded at 550.00. A step that checks the price ("Given the Galaxy Smartphone costs 550.00"), without setting it, lets a reader confirm the total from the feature alone.
- **Why deferred:** raised on 2026-10-29 in the same review as item 24. Wording, not correctness: every scenario checks what it says.

### 26. End-to-End Suite Settings and Scenario Selection
- **Priority: low.**
- **Settings outside `EndToEndEnvironment`.** `Tokens` hard-codes the `eshop` realm, the `eshop-app` client and its secret `local-demo-secret`, while the URLs and admin credentials come from environment variables. Running the suite against any other environment needs all of them configurable.
- **One admin token per call.** `CustomerAccounts.create` and `delete` each request a new admin token, so each actor costs Keycloak two extra token requests. A short-lived cached token halves that traffic under parallel runs.
- **No tags.** Add `@wip`, excluded by default, and a smoke subset, so that selecting scenarios is a setting rather than something bolted on later.
- **Why deferred:** raised on 2026-10-29 in the same review as item 24. Nothing needs it with one environment and 8 scenarios.

### 27. Observability: Metrics, Traces and Dashboards in the Compose Stack
- Today the application exposes only `/actuator/health`. Nothing records metrics or traces, so the asynchronous path (checkout → outbox → dispatcher → order) and the payment calls can be followed only through logs.
- **Goal:** a visitor starts the stack with an extra compose profile and opens Grafana on committed dashboards showing the application's own behaviour, and a tracing UI showing one checkout from HTTP request to created order, across both modules.
- **Shape:**
    - Micrometer for metrics, and Micrometer Tracing bridged to OpenTelemetry, exporting over OTLP so the tracing backend is a compose detail rather than a code dependency. **Verify before relying on it:** what Spring Boot 4 provides out of the box here (starters, auto-configuration, property names), since Boot 4 changed its OpenTelemetry support.
    - Compose services under their own profile (e.g. `observability`), so the default stack and the end-to-end run stay as light as today: Prometheus, Grafana, and a tracing backend.
    - Grafana datasources and dashboards committed through provisioning, so a fresh clone opens with them ready.
    - Trace and span ids in the log pattern, so a log line leads to its trace.
- **Trace context across the outbox, the part that is specific to this project.** HTTP, the database calls and the Stripe call are traced out of the box. The outbox breaks the trace: the dispatcher publishes on a scheduler thread with no context, so ordering's consumer starts an unrelated trace. Store the W3C trace context on the outbox record when the event is written, and restore it when the dispatcher publishes. Keep it in the record's metadata, not in the event payload: the payload is schema-versioned with upcasting and captured fixtures, and the trace context isn't part of the event. Both adapters (JPA and Mongo) and the fake change.
- **Metrics that say something about this design**, beyond the built-in JVM and HTTP ones, each tagged by module:
    - outbox: pending events, age of the oldest pending event, dispatch attempts and failures, dead-lettered events (item 16's gauge, which this item includes);
    - payments: charges, declines by reason, refunds, and failed refunds, which makes Known Issue 14 visible until it is fixed;
    - the delay from checkout to order created, the eventual-consistency window measured.
- **Decisions needed:**
    - parent span or span link across the outbox. A child span gives one connected trace whose duration includes the poll wait; a link models the asynchronous hand-off as it is. Record the choice and why;
    - tracing backend: Zipkin, Jaeger or Grafana Tempo. Tempo lets Grafana show traces next to metrics;
    - separate Prometheus, Grafana and tracing containers, or the single `grafana/otel-lgtm` image. Separate services show the wiring; one image is less to maintain;
    - whether logs go to Loki as well, or stay in the container output.
- **Why deferred:** decided with the user on 2026-10-29, when running the stack on Kubernetes was dropped from this repository ([kubernetes-sample-scope](../ai-coding-tools/plans/kubernetes-sample-scope/kubernetes-sample-scope.md)) in favour of observability, which shows the modular monolith's asynchronous design rather than its deployment.

### 28. Project Layout Names: the Inner Folder, `starter`, and a `config` Package
Three renames, done as one change and before the repository goes public. Doing it later breaks existing clones, bookmarks and links into the tree.
- **`starter` says the wrong thing.** In Spring, a "starter" is a dependency you add (`spring-boot-starter-*`), so the name reads as a library rather than the module that assembles and launches the application. Rename the module, its artifact and the main class `EshopModularMonolithStarter` to match.
- **Configuration classes sit in the root package.** `OpenApiConfig`, `MongoConfiguration`, `ClockConfiguration` and `AuditingConfiguration` are next to the main class. Move them into a `config` subpackage, and use one suffix for all four (`…Config` or `…Configuration`, not both). Component scanning still finds them below the main class's package. Check `ApplicationWiringTest` and `ArchitectureTest` for package names.
- **The inner `SpringBoot-EshopModularMonolith/` folder repeats the repository's name**, in mixed case, so every path reads `SpringBoot-EshopModularMonolith/SpringBoot-EshopModularMonolith/…`. Rename it to a lowercase name that says what it holds. Upper-case documentation files (`README.md`, `ARCHITECTURE.md`) are the usual convention and stay.
- **What the renames touch** (measured on 2026-11-04 with `git grep`, plan documents excluded since they're frozen):
    - module: the parent `pom.xml` and every module pom that names it, `sonar/pom.xml` (the coverage aggregator), the `Dockerfile` (`-pl starter`, `starter/target/starter.jar`), README, SETUP, ARCHITECTURE, COVERAGE, DESIGN_NOTES, BACKLOG, `bruno/README.md`, and `ai-coding-tools/processes/getting-started.md`;
    - folder: the three workflows under `.github/workflows/` (paths, `-f` arguments, compose file), README, SETUP, DESIGN_NOTES, `e2e/README.md`, `bruno/README.md`, `k8s-samples/` (README and the comment in `deployment.yaml`), `ai-coding-tools/processes/getting-started.md`, `ai-coding-tools/scripts/README.md`, and `REPORT_DIR` in `ai-coding-tools/scripts/coverage-summary.py`.
- **Decisions needed:** the module's new name (`application`, `app` or `bootstrap`), and the folder's (e.g. `backend`).
- **Why deferred:** raised on 2026-11-03 in an outside review of the repository's organisation. Each rename is mechanical, but together they touch the build, CI and most docs, so they belong in one change of their own.

### 29. One Naming Rule for Contract Types
- **Today `contract` packages mix two conventions.** Read and payload shapes carry a `DTO` suffix (`ProductDTO`, `ShoppingCartDTO`, `ItemDTO`, `OrderDTO`, `PaymentDTO`, `AddressDTO`, `CardSummaryDTO`, `ChargeDTO`, `CheckedOutItemDTO`). Integration events are named two ways: catalog's `ProductCreatedMessage` and basket's `BasketCheckoutIntegrationEvent`. A module's `contract` is its public API, and an outside review on 2026-11-03 read the suffix as dated. The inconsistency between the two events is the stronger reason to change.
- **Goal:** one rule, written in ARCHITECTURE's *Request shapes vs. module contracts*, applied to every contract type.
- **Constraints on the rule:**
    - These are not HTTP-only shapes. `ProductQueryService` returns `ProductDTO` to basket in-process, so a `…Response` suffix would describe one of its two uses.
    - A bare name clashes with the domain model where one exists. Catalog has both `Product` and `ProductDTO`, and a file that needs both would have to use a fully qualified name.
    - Options: a role suffix (`ProductView`, `OrderSummary`), or the bare name where no domain type of that name exists. For events, one suffix (`…IntegrationEvent`).
- **What it does not touch:** stored data. The outbox persists domain events (`BasketCheckedOut`, `ProductCreated`) under the `eventType()` string each declares, and builds the integration event only when it dispatches, so renaming contract types changes no payload at rest. **Check before relying on it:** that no read-side Mongo query or test fixture depends on a contract class's name.
- **What it does touch:** every module's contract package and its consumers; the OpenAPI schema names in Swagger UI, which are the class names; and the docs that say "contract DTOs" (README, ARCHITECTURE, the getting-started guide).
- **Decision needed:** the rule itself.
- **Why deferred:** a naming choice with no defect behind it, and broad enough to deserve its own change.

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
