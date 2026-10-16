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

### 1. `Order.totalPrice` Doesn't Exist
- **Resolved by [basket-quantity-fix](../ai-coding-tools/plans/basket-quantity-fix/basket-quantity-fix.md):** the pricing-basis question this item used to gate on is settled — checkout-time pricing, `Σ(current catalog price × quantity)`, fetched via `ProductService` at checkout. That plan also fixed `ShoppingCart.checkout`'s defects (closed Known Bug 1) and added `ShoppingCartTest` (closed Testing Gap 1).
- **What's still missing:** `CreateOrderCommand`/`Order.placeFromCheckout` never receive or compute a total at all. Basket now computes a correct, current-price `totalPrice` on `BasketCheckedOut`, but `BasketCheckoutIntegrationEventListener` still never reads it — the value is computed and still never used downstream.
- **Decision needed:** should `Order` trust the value carried on the integration event, or re-derive its own from the items it received? Trusting a caller-computed number repeats the shape of the bug this item used to describe — an aggregate should generally own its own invariant rather than believe one handed to it. Settle this before adding `totalPrice` to `Order`, `OrderDB`, `OrderDTO` and both mappers — and, only if the event's value is trusted, to `CreateOrderCommand` and `BasketCheckoutIntegrationEventListener`, which would have to carry it; a re-derived total needs neither.
- **Neither option is independent of other open work (found 2026-09-17).** Ordering builds each order `Item` from `ItemDTO.priceWhenAdded` (`BasketCheckoutIntegrationEventListener.toDomain(ItemDTO)`), while `BasketCheckedOut.totalPrice` uses *current* catalog prices. Re-deriving the total in `Order` today would therefore sum add-time prices and contradict the checkout-time pricing decision above; it only becomes correct once item lines carry the current price (Backlog 8). Trusting the event's value means reading a field ordering ignores today. Either way the integration event's consumed shape changes, and events already at rest in the basket outbox would deserialize with the new field null — Known Bug 3. Realistic order: Known Bug 3's compatibility policy, then Backlog 8, then this.
- **Why still deferred:** demonstrating the current-price-at-checkout mechanism was that plan's priority; wiring the result into `Order` is separate, smaller work once the trust-vs-derive question is answered.

### 3. `MongoAuditMetadata`
- Ordering has no counterpart to `shared.infrastructure.JpaAuditMetadata`, so Mongo-backed documents carry no created/modified audit fields.
- **Blocked on:** adding `@Version` to the documents. Mongo auditing decides created-vs-modified purely from `isNew`, which is id-based, and every document in this codebase assigns its id up front — so without a version field auditing cannot distinguish the two cases at all.
- **Note:** whatever shape this takes must not write the audit fields from the domain projection. Catalog's `JpaProductRepository.save` originally rebuilt a whole `ProductDB` from the domain object and merged it over the stored row, which silently wiped `created_by` on every update; it now loads the stored row and mutates only the fields the domain owns. `MongoBasketRepository.save` and `MongoOrderRepository.save` have the same shape — they hand `repository.save` a document rebuilt from the domain, which replaces the stored one wholesale. Harmless only while `ShoppingCartDB`/`OrderDB` are total projections of what is stored; the audit fields added here would be wiped on the next basket update. Both are records, so the fix is not JPA-style mutation: either `$set` the domain-owned fields, or carry the stored audit values forward into the new record.

### 8. `BasketCheckedOut` Items Don't Reflect Current Price
- [basket-quantity-fix](../ai-coding-tools/plans/basket-quantity-fix/basket-quantity-fix.md) made `BasketCheckedOut.totalPrice` correct — `Σ(current catalog price × quantity)` — but each `Item` on that same event still only carries `priceWhenAdded`, the add-time price. Summing the event's own item lines does not reconcile against its `totalPrice`.
- **Why it matters:** there's no way for a consumer of this event to show a customer the add-time-vs-current-price delta ("price dropped since you added it"), and the event is internally inconsistent for anyone reading it later, e.g. as an audit trail.
- **Action:** needs a dedicated shape carrying both prices per item (e.g. `CheckedOutItem(id, productId, quantity, priceWhenAdded, currentPrice, productName)`) — deliberately not a change to `Item` itself, which is persisted cart state and shouldn't carry a checkout-only field.
- **Why deferred:** proving the current-price-at-checkout mechanism exists at all was the priority; representing both prices together, and any delta view built on it, is separate follow-up work.

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
- **Decision needed first:** where roles live — Keycloak realm or client roles carried in the token, or roles owned by the application. This is also on the README roadmap, unchecked ("Consider Roles in Keycloak vs Roles in application"). Then what a back-office actually needs across customers: listing and reading are clear; deleting is doubtful, since cancelling an order is a state change rather than a removal (see Known Bug 13).
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
