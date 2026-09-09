# Known Issues

Defects and testing gaps that are known, deliberately not fixed yet, and recorded here with the reasoning so they are neither rediscovered nor re-argued. The focus is business and behaviour logic, not line coverage.

The two sections are kept apart because *broken* and *unverified* call for different remedies.

**Relationship to [BACKLOG.md](BACKLOG.md):** this file is for code that is **wrong or unverified right now**; the backlog is for code that is fine as far as it goes but missing a capability or a decision. Route by nature, not by origin — something surfaced while deferring a plan step is usually a defect and belongs here.

**Relationship to plans:** a plan's *Deferred* section records what **that increment** chose not to do, and is frozen once the plan merges. These two files are the live lists. Anything a plan defers must land in one of them, or be consciously dropped — so something can legitimately appear in both a plan and here.

## 🐛 Known Bugs

### 2. Outbox Dispatchers Have No Retry Limit or Dead-Letter State for a Poisoned Event
- **Location:** `OutboxEventProcessor.dispatchPendingEvents` (`shared.infrastructure`), used by both `BasketOutboxDispatcher` and `CatalogOutboxDispatcher`.
- **Issue:** [outbox-dispatcher-per-event-isolation](../ai-coding-tools/plans/outbox-dispatcher-per-event-isolation/outbox-dispatcher-per-event-isolation.md) fixed the original defect: each event's publish-then-`markAsProcessed` is now wrapped in a try/catch, so a failure is logged and the loop moves on to the next event instead of propagating out of the scheduled method. What's still open is what happens to that one poisoned event afterward: it stays unprocessed and is retried every 5 seconds forever, with no attempt counter and no dead-letter state — the only trace of it is a log line.
- **Impact:** No longer halts unrelated events — a bad event no longer blocks its neighbors or the rest of the batch. But a permanently-poisoned event (e.g. a listener that will always throw for that payload) retries silently and indefinitely, invisible to the HTTP caller and to anyone not reading logs.
- **Action Required:** Decide retry semantics for the poisoned event itself — an attempt counter with a terminal dead-letter state, or some other bound. This requires an `OutboxEventStore` port change (both the Mongo and JPA adapters currently only support "unprocessed" vs. "processed") and is a decision, not a mechanical fix.

### 3. Outbox Payloads Have No Schema Versioning
- **Location:** `OutboxDocumentMapper`, `DomainEventRegistry`, every persisted `DomainEvent`
- **Issue:** A domain event's JSON payload is persisted in the outbox and deserialized later by a registry keyed on a bare type name (`"BasketCheckedOut"`). Changing the event's shape is therefore a backward-incompatible change to data already at rest: documents written before the change deserialize with the new fields null, and consumers built for the new shape reject them. Adding address and payment to `BasketCheckedOut` had exactly this effect — pre-existing unprocessed documents would deserialize with nulls and be rejected by `Order.create` with an `IllegalArgumentException`. It was handled by manually clearing the local `outbox` collection, which is adequate for local development and not for anything deployed.
- **Compounds Known Bug 2:** without a dead-letter path, a single undeserializable or rejected payload does not merely fail — it wedges the whole outbox behind it. Versioning without dead-lettering only narrows the window.
- **Action Required:** Decide a compatibility policy before the next event-shape change. Options: a versioned type key (`BasketCheckedOutV2`) with the old key kept readable; tolerant deserialization plus an explicit policy for events that cannot be satisfied; or a documented drain-before-deploy step. Any of the three is fine — having none is the issue.

### 4. Raw Card Data Is Stored and Transmitted in Clear
- **Locations:** `orders.contract.PaymentDTO`, `basket.contract.PaymentDTO`, `orders.domain.valueobjects.Payment`, `basket.domain.valueobjects.Payment`, `PaymentDB`, the basket `outbox` collection
- **Issue:** The full card number, expiry and CVV travel through the API, the domain, the integration event, and both databases as plain text. CVV must never be stored at all under PCI DSS, and a PAN should be tokenized or reduced to brand plus last four digits. Order storage predates the checkout work; capturing payment at checkout widened the exposure by adding a second copy at rest, as JSON in the basket outbox.
- **Impact:** Acceptable only because this is a study project against test data with no real cardholder information. It would be a blocking compliance defect in any deployed system.
- **Mitigated so far:** `InMemoryMessageBus` no longer logs message payloads, so the data does not reach application logs.
- **Action Required:** Out of reach of a refactor — it needs a payment provider integration, so that checkout carries a provider token instead of card details and no module ever sees a PAN. Until then, do not add further copies, and keep payloads out of logs.

### 6. No Consumer-Side Idempotency
- **Locations:** `BasketCheckoutIntegrationEvent.eventId`, `BasketCheckoutIntegrationEventListener.handle`
- **Issue:** The integration event carries an `eventId`, but nothing consults it. The outbox is at-least-once by construction — `BasketOutboxDispatcher` publishes and only then calls `markAsProcessed`, so a crash or failure between the two republishes an event that was already handled. The listener creates an order unconditionally.
- **Impact:** A redelivered checkout event creates a duplicate order. The `eventId` reads as if this were handled; it is not.
- **Action Required:** Dedup on `eventId` at the consumer — a processed-message table or collection checked before handling. This is the counterpart to Known Bug 2: that one decides what happens to an event that never succeeds, this one what happens to an event that succeeds twice.

### 7. Order Sort Has No Tie-Breaker
- **Location:** `GetOrdersHandler.handle`
- **Issue:** The read path sorts on `placedAt` DESC alone. Orders sharing an instant compare equal, so their relative order is unspecified — and because the sort is applied per page, a row can appear on two pages or on none while paginating.
- **Impact:** Non-deterministic pagination. Rare with millisecond precision and organic traffic; reliable in tests and bulk inserts, where it presents as flakiness rather than as a read-model bug.
- **Action Required:** Add a secondary key — `placedAt` DESC, `id` ASC. More clock precision is not a fix; it only makes collisions rarer.

### 8. A Fresh Clone Cannot Start the Application
- **Locations:** `starter/src/main/resources/application-local.yaml` (untracked), `starter/src/main/resources/application.yaml`, `MongoConfiguration.mongoClient`, `docker/sb-eshop/compose.yaml`, [SETUP.md](SETUP.md)
- **Issue:** Three independent defects that only show up on a clean checkout, because a developer machine already carries the missing pieces:
    1. `MongoConfiguration.mongoClient` injects `@Value("${spring.mongodb.uri}")` with no default, and that property is set **only** in `application-local.yaml`, which is not committed. Context startup therefore fails on every profile from a fresh clone. (`spring.mongodb.uri` is also not the Spring Boot property name — the framework's is `spring.data.mongodb.uri` — so nothing supplies it by accident.)
    2. The committed `application.yaml` declares `spring.datasource.password: password`, while `compose.yaml` starts Postgres with `POSTGRES_PASSWORD=postgres`; and it carries no Mongo URI at all, so nothing points at compose's mapped port `27020`.
    3. [SETUP.md](SETUP.md) instructs the reader to create the `eshop` realm, the `eshop-app` client and an audience mapper by hand. `compose.yaml` already imports `keycloak-init/eshop-realm.json` via `--import-realm`, so the documented steps are both unnecessary and misleading about what the running realm contains.
- **Impact:** Nobody but the author can run the application. Local development is unaffected — the untracked file and the pre-existing containers hide all three — which is exactly why this survived. It is a blocking defect for any second developer, for a CI job that starts the app, and for anyone evaluating the repository.
- **Action Required:** Decide what the committed default configuration is, and make one documented command sequence work end to end on a clean clone. That means: a value for the Mongo URI that is present without an untracked file (rename the property to `spring.data.mongodb.uri` or give the `@Value` a default), datasource credentials and Mongo port that agree with `compose.yaml`, and a rewritten [SETUP.md](SETUP.md) whose steps have actually been run against a fresh checkout. Keep genuinely machine-specific overrides in a profile, but the default must boot.

### 9. Actuator Health Probes Are Rejected by the Security Filter Chain
- **Locations:** `SecurityFilterChainConfig.filterChain`, `Kubernetes/simple-deployment/backend/deployment.yaml`, `starter/pom.xml`
- **Issue:** `spring-boot-starter-actuator` is a dependency, and the Kubernetes deployment's liveness and readiness probes both `httpGet` `/actuator/health`. The security chain permits only `/v3/api-docs/**`, `/swagger-ui/**` and `/swagger-ui.html`, and requires authentication for `anyRequest()` — so both probes receive 401. There is additionally no `management.endpoints.web.exposure` configuration anywhere, so which endpoints are even exposed is left to defaults rather than stated.
- **Impact:** The manifests as written would never pass readiness, and the pod would be killed by the liveness probe on a loop. Invisible today because nothing deploys them.
- **Action Required:** Permit the health endpoint explicitly (`/actuator/health/**`, and `/actuator/info` if wanted) in `SecurityFilterChainConfig`, and state the exposed management endpoints in configuration rather than relying on defaults. Decide deliberately whether liveness and readiness should use the separate `health/liveness` and `health/readiness` groups; anything beyond health should stay authenticated.

## 🧪 Testing Considerations & Gaps

### 2. Missing Failure Path in `UpdateProductHandler`
The `UpdateProductHandler` handles the "happy path" (product exists and is updated) but the failure path is untested.
- **Location:** `UpdateProductHandler.java`
- **Issue:** The `ProductNotFound` exception thrown inside `.ifPresentOrElse(...)` is never reached in tests.
- **Improvement:** Add a test scenario in `UpdateProductHandlerTest` ensuring that attempting to update a non-existent product properly aborts and triggers a `ProductNotFound` exception.

### 3. Untested Application Domain Event Listeners
- **Locations:** `BasketCheckedOutHandler`, `ProductCreatedHandler`
- **Issue:** These classes currently report 0 lines covered. Although they might only be logging right now, missing tests mean there is no guarantee that a `ProductCreated` or `BasketCheckedOut` domain event is actually being picked up and routed by the Spring context.
- **Improvement:** Add integration/unit tests verifying that the `ApplicationEventPublisher` successfully triggers these event handlers.

### 4. Empty / Unimplemented Error Handler
- **Location:** `CreateOrderErrorHandler`
- **Issue:** The class is completely empty and misses test coverage.
- **Improvement:** Implement the error handler to provide a proper Contract DTO error response (instead of an unhandled 500 Server Error) if order creation fails, and add corresponding tests.

### 5. No Test Exercises an Update Through the JPA Adapter
- **Locations:** `JpaProductRepository.save`, `JpaAuditMetadata`
- **Issue:** `CreateProductHandlerTransactionalTest` covers creation only, and `UpdateProductHandlerTest` fakes at the port — so nothing runs an update through real JPA. That hid a real defect: `save` reconstructs `ProductDB` from the domain object, whose fresh `JpaAuditMetadata` is all nulls, and `merge` copies those over the stored row. `created_at` failed loudly against its NOT NULL constraint; `created_by` is nullable, so it was silently wiped on every update and survived for a further increment before being noticed by reading the class rather than by a failure. Both now carry `@Column(updatable = false)` — **the defect is fixed, the gap is not.** No test would catch its reintroduction.
- **Related:** the deeper cause is that `save` blind-overwrites a whole row from a projection that does not carry every column. The structural fix is load-and-mutate — fetch the managed entity and copy the mutable fields onto it. Ordering's Mongo path will meet the same shape when a `MongoAuditMetadata` counterpart arrives ([BACKLOG.md](BACKLOG.md) item 3).
- **Improvement:** Add an update case to `JpaProductRepositoryTest` asserting that `createdAt` and `createdBy` survive, and extend Gap 2 above beyond its failure-path scope.

### 6. Transactional Semantics of Order Creation Are Unverified
- **Locations:** `CreateOrderHandler`, `BasketCheckoutIntegrationEventListener`
- **Issue:** `CreateOrderHandler` carries no `@Transactional` on either of its two entry points — the HTTP controller and the integration-event listener, which runs on the outbox dispatcher's scheduler thread. One aggregate and one save, so this is very likely harmless today, and nothing proves it or would notice if a second write joined the handler.
- **Improvement:** Recorded so it is not rediscovered as a surprise rather than because a test is owed right now. It becomes a real gap the moment order creation writes more than once, which Backlog item 1 (`totalPrice`) and item 5 (`OrderCreated` outbox) both approach.

### 7. Slice Test Configs Can Still Drift From Production
- **Locations:** every `@DataJpaTest`/`@DataMongoTest`-based test's local `@Configuration`, versus the module configs in `main`
- **Issue:** [assembled-context-wiring-test](../ai-coding-tools/plans/assembled-context-wiring-test/assembled-context-wiring-test.md) closed the gap in one direction — nothing previously loaded the real assembled context — but it cannot see drift in the other direction. A slice test's hand-built `@Configuration` declaring a bean under the wrong name, missing a bean production has, or configuring one differently is invisible to a test of production config, by construction: production is what it loads, so it has nothing to compare against. This was one of the three original failures the new test set out to close (catalog's slice-declared transaction-manager names had drifted from production's) and it remains open.
- **Improvement:** No single test closes this; it needs slice test configuration to stop being reinvented per test class, or a comparison that isn't itself another hand-built config. Not scoped here.

### 8. The Coverage Badge's Number Has Never Been Checked Against What Sonar Actually Measures
- **Location:** the coverage badge in [README.md](../README.md), fed by the SonarCloud project `TomMante_SpringBoot-EshopModularMonolith`
- **Issue:** Two separate configuration defects have now been fixed — `outbox-mongo` missing from the `sonar` module's dependency list, and four modules pointing `sonar.coverage.jacoco.xmlReportPaths` one directory above the Maven root. Both were found by reading configuration, not by anything failing, and both had the same effect: a module's code silently absent from the reported figure. The local aggregate is now verified complete (ten directories, one per code-bearing module, and every module resolves the report path to a file that exists). What has never been checked is the other end — whether SonarCloud's own number covers all ten.
- **Impact:** The badge is a public claim about this repository, and nobody has confirmed what it counts. That it was wrong twice, undetected, is the reason to check rather than assume.
- **Improvement:** After the next CI run, confirm on SonarCloud that all ten code-bearing modules report coverage and that the project figure is close to the local aggregate (96.9% instructions as of this writing). Neither was checkable from here: the Sonar project is private, so its API answers "Project doesn't exist" anonymously, and the Actions logs were not readable with the available credentials.
