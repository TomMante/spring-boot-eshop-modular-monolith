# Known Issues

Defects and testing gaps that are known, deliberately not fixed yet, and recorded here with the reasoning so they are neither rediscovered nor re-argued. The focus is business and behaviour logic, not line coverage.

The two sections are kept apart because *broken* and *unverified* call for different remedies.

**Relationship to [BACKLOG.md](BACKLOG.md):** this file is for code that is **wrong or unverified right now**; the backlog is for code that is fine as far as it goes but missing a capability or a decision. Route by nature, not by origin — something surfaced while deferring a plan step is usually a defect and belongs here.

**Relationship to plans:** a plan's *Deferred* section records what **that increment** chose not to do, and is frozen once the plan merges. These two files are the live lists. Anything a plan defers must land in one of them, or be consciously dropped — so something can legitimately appear in both a plan and here.

## 🐛 Known Bugs

### 1. `ShoppingCart` Quantity Ignored in Checkout
- **Location:** `ShoppingCart.checkout(Map<UUID, BigDecimal> currentPrices)`
- **Issue:** The total price calculation completely ignores the `quantity` of the items in the basket. If a user adds 3 units of the same product, they are currently only charged for 1. Additionally, if the product ID is missing from `currentPrices`, `.get()` returns null, which throws a `NullPointerException` during the `BigDecimal::add` reduction.
- **Related, in the caller:** `CheckoutBasketHandler` builds `currentPrices` with `Collectors.toMap(ProductInfo::id, ProductInfo::price)`. `ShoppingCart.addItem` never merges duplicates, so adding the same product twice produces two `Item`s with the same `productId` and the collector throws `IllegalStateException` on the duplicate key — checkout fails outright for an ordinary user action.
- **Impact:** Critical business logic failure affecting order totals, and a hard failure on duplicate products.
- **Action Required:** Fix the calculation to multiply price by `quantity`, handle missing prices gracefully, and decide whether duplicate products merge into one item or the price map tolerates duplicate keys. Add isolated domain tests for `ShoppingCart` (see Gap 1). The pricing basis — add-time vs checkout-time price — must be settled first.

### 2. `BasketOutboxDispatcher` Has No Per-Event Error Isolation
- **Location:** `BasketOutboxDispatcher.dispatch()`
- **Issue:** The loop publishes an event and then calls `markAsProcessed`. If a consumer throws, the exception propagates out of the scheduled method *before* the event is marked processed, so it is republished every 5 seconds indefinitely — and because the loop never resumes, that one document also blocks every later event in the basket outbox. There is no dead-letter path and no retry limit.
- **Impact:** A single bad event silently halts all cross-module messaging from basket. The HTTP caller sees a 202 and never learns.
- **Action Required:** Decide retry semantics — attempt counter with a dead-letter state, or isolate each event so one failure does not stop the batch. Not a `try/catch`; the point is deciding what happens to the poisoned event.

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

### 5. `createdBy` Is Silently Wiped on Every Update
- **Location:** `JpaAuditMetadata.createdBy`
- **Issue:** `createdAt` carries `@Column(nullable = false, updatable = false)`; `createdBy` carries neither. `JpaProductRepository.save` reconstructs `ProductDB` from the domain object, whose fresh `JpaAuditMetadata` is all nulls, and `merge` copies those over the stored row. `createdAt` is protected by `updatable = false`; `createdBy` is nullable and updatable, so it is overwritten with null on every update without any error.
- **Impact:** Silent audit-data loss. Loud enough to notice only because its sibling column failed first — the same defect on `createdAt` threw a NOT NULL violation and was fixed; this one has no such backstop.
- **Action Required:** Add `@Column(updatable = false)` to `createdBy`. One annotation in `shared.infrastructure` fixes it for every entity. Testing Gap 6 is the reason it survived — nothing exercises an update through the real JPA adapter.

### 6. Checkout Accepts an Empty Basket
- **Locations:** `CheckoutBasketHandler.handle`, `ShoppingCart.checkout`, `Order.create`
- **Issue:** No layer rejects a checkout with no items. The handler does not guard, `ShoppingCart.checkout` reduces an empty stream to a `totalPrice` of zero, and `Order.create` validates address and payment but not the item list. The result is a persisted order with no items and a zero total, from a request that is meaningless.
- **Impact:** A junk order is created and is indistinguishable in the read model from a real one.
- **Action Required:** Two rules, and both are needed — an empty basket should be rejected at the checkout entry point with a 400, and `Order.create` should refuse an empty item list as an invariant. Fixing only the aggregate turns a bad request into a poison outbox event instead of an HTTP error (see Known Bug 2).

### 7. No Consumer-Side Idempotency
- **Locations:** `BasketCheckoutIntegrationEvent.eventId`, `BasketCheckoutIntegrationEventListener.handle`
- **Issue:** The integration event carries an `eventId`, but nothing consults it. The outbox is at-least-once by construction — `BasketOutboxDispatcher` publishes and only then calls `markAsProcessed`, so a crash or failure between the two republishes an event that was already handled. The listener creates an order unconditionally.
- **Impact:** A redelivered checkout event creates a duplicate order. The `eventId` reads as if this were handled; it is not.
- **Action Required:** Dedup on `eventId` at the consumer — a processed-message table or collection checked before handling. This is the counterpart to Known Bug 2: that one decides what happens to an event that never succeeds, this one what happens to an event that succeeds twice.

### 8. Order Sort Has No Tie-Breaker
- **Location:** `GetOrdersHandler.handle`
- **Issue:** The read path sorts on `placedAt` DESC alone. Orders sharing an instant compare equal, so their relative order is unspecified — and because the sort is applied per page, a row can appear on two pages or on none while paginating.
- **Impact:** Non-deterministic pagination. Rare with millisecond precision and organic traffic; reliable in tests and bulk inserts, where it presents as flakiness rather than as a read-model bug.
- **Action Required:** Add a secondary key — `placedAt` DESC, `id` ASC. More clock precision is not a fix; it only makes collisions rarer.

## 🧪 Testing Considerations & Gaps

### 1. Missing Domain Test for `ShoppingCart.checkout`
Domain classes here do not get isolated tests by default — they get one when they carry logic whose cases outgrow the seam above them, per the criteria in [TESTING_CONVENTIONS.md](TESTING_CONVENTIONS.md). Most of this codebase's domain classes (`Order`, `Product`, `Address`, `Payment`, `Item`) are constructors, getters, and factories that shuffle arguments; a test on them would assert that assignment works.
- **Issue:** `ShoppingCart.checkout` is the one method that does qualify — a calculation over a collection — and it has no isolated test. `CheckoutBasketHandlerTest` covers it with a single item at quantity 1, which is precisely the case where Known Bug 1 is invisible.
- **Improvement:** Create `ShoppingCartTest.java` covering multiple items, quantity greater than 1, a product missing from `currentPrices`, and the same product present as two separate items. Note that 100% JaCoCo coverage on these classes is a side effect of the Application layer tests and says nothing about whether these cases are exercised.

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

### 5. Nothing Loads the Real Module Configurations
Every test builds its own `@Configuration`, so the wiring that the running application actually uses is never exercised. Three separate failures reached a manual run rather than a test:
- A bare `@Transactional` in `JpaProductRepository` could not resolve among three `TransactionManager` beans. Each module's slice test sees only its own, so by-type resolution succeeds there and fails only in the assembled context.
- The `DomainEventRegistry` contribution in `BasketModuleConfig.postConstruct()` is untested. Removing or misspelling it still compiles and leaves the suite green, while checkout poisons the outbox at runtime with `Unknown event type`.
- The catalog test slices declare transaction-manager beans under names that drifted from production's.
- **Improvement:** A single `@SpringBootTest` that only asserts the context loads would catch this entire class. Note it needs the same infrastructure the app does (Postgres, Mongo, Keycloak), so it belongs with the Testcontainers-based tests rather than the plain unit ones.

### 6. No Test Exercises an Update Through the JPA Adapter
- **Locations:** `JpaProductRepository.save`, `JpaAuditMetadata`
- **Issue:** `CreateProductHandlerTransactionalTest` covers creation only, and `UpdateProductHandlerTest` fakes at the port — so nothing runs an update through real JPA. That hid a real defect: `save` reconstructs `ProductDB` from the domain object, whose fresh `JpaAuditMetadata` is all nulls, and `merge` copies those over the stored row. `created_at` failed loudly against its NOT NULL constraint and was fixed with `updatable = false`; `created_by` is nullable, so it is **still** silently wiped on every update — see Known Bug 5.
- **Related:** the deeper cause is that `save` blind-overwrites a whole row from a projection that does not carry every column. The structural fix is load-and-mutate — fetch the managed entity and copy the mutable fields onto it. Ordering's Mongo path will meet the same shape when a `MongoAuditMetadata` counterpart arrives ([BACKLOG.md](BACKLOG.md) item 3).
- **Improvement:** Add an update case to `JpaProductRepositoryTest` asserting that `createdAt` and `createdBy` survive, and extend Gap 2 above beyond its failure-path scope.

### 7. Transactional Semantics of Order Creation Are Unverified
- **Locations:** `CreateOrderHandler`, `BasketCheckoutIntegrationEventListener`
- **Issue:** `CreateOrderHandler` carries no `@Transactional` on either of its two entry points — the HTTP controller and the integration-event listener, which runs on the outbox dispatcher's scheduler thread. One aggregate and one save, so this is very likely harmless today, and nothing proves it or would notice if a second write joined the handler.
- **Improvement:** Recorded so it is not rediscovered as a surprise rather than because a test is owed right now. It becomes a real gap the moment order creation writes more than once, which Backlog item 1 (`totalPrice`) and item 5 (`OrderCreated` outbox) both approach.