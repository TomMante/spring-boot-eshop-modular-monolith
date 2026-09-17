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
- **Issue:** A domain event's JSON payload is persisted in the outbox and deserialized later by a registry keyed on a bare type name (`"BasketCheckedOut"`). Changing the event's shape is therefore a backward-incompatible change to data already at rest: documents written before the change deserialize with the new fields null, and consumers built for the new shape reject them. Adding address and payment to `BasketCheckedOut` had exactly this effect — pre-existing unprocessed documents would deserialize with nulls and be rejected by `Order.create` (since renamed `Order.placeFromCheckout`) with an `IllegalArgumentException`. It was handled by manually clearing the local `outbox` collection, which is adequate for local development and not for anything deployed.
- **Compounds Known Bug 2:** without a dead-letter path, a single undeserializable or rejected payload does not merely fail — it wedges the whole outbox behind it. Versioning without dead-lettering only narrows the window.
- **Action Required:** Decide a compatibility policy before the next event-shape change. Options: a versioned type key (`BasketCheckedOutV2`) with the old key kept readable; tolerant deserialization plus an explicit policy for events that cannot be satisfied; or a documented drain-before-deploy step. Any of the three is fine — having none is the issue.

### 4. Raw Card Data Is Stored and Transmitted in Clear
- **Locations:** `CheckoutPayment` (basket's checkout request), `orders.contract.PaymentDTO`, `basket.contract.PaymentDTO`, `orders.domain.valueobjects.Payment`, `basket.domain.valueobjects.Payment`, `PaymentDB`, the basket `outbox` collection
- **Issue:** The full card number, expiry and CVV travel through the API, the domain, the integration event, and both databases as plain text. CVV must never be stored at all under PCI DSS, and a PAN should be tokenized or reduced to brand plus last four digits. Order storage predates the checkout work; capturing payment at checkout widened the exposure by adding a second copy at rest, as JSON in the basket outbox.
- **Impact:** Acceptable only because this is a study project against test data with no real cardholder information. It would be a blocking compliance defect in any deployed system.
- **Mitigated so far:** `InMemoryMessageBus` no longer logs message payloads, so the data does not reach application logs.
- **Action Required:** Out of reach of a refactor — it needs a payment provider integration, so that checkout carries a provider token instead of card details and no module ever sees a PAN. Until then, do not add further copies, and keep payloads out of logs.

### 13. Deleting an Order Lets Its Checkout Be Placed Again
- **Locations:** `DeleteOrderHandler` → `MongoOrderRepository.delete`; the `orders_checkoutId_unique` index on `OrderDB.checkoutId`; `BasketCheckoutIntegrationEventListener`
- **Issue:** A redelivered checkout event is deduplicated by the unique index on `checkoutId`: the second insert collides, and the listener catches `OrderAlreadyExists` and skips it. `DELETE /order/{id}` is a hard delete, so it removes the document and its index entry together. A redelivery of that checkout arriving after the delete no longer collides with anything, and the order is placed again.
- **Impact:** Narrow window, silent result. Redelivery happens only inside the outbox retry window — `markAsProcessed` failing after a successful publish (retried 5 seconds later), or two application instances polling the same unprocessed event — so a customer would have to delete the order inside it. When it does happen, the deleted order simply reappears; nothing logs it as a duplicate, because it no longer is one.
- **Accepted on 2026-09-16** in [order-endpoints-scoped-to-customer](../ai-coding-tools/plans/order-endpoints-scoped-to-customer/order-endpoints-scoped-to-customer.md), which scoped delete to the caller's own orders but kept it a hard delete rather than widening that increment.
- **Action Required:** Replace the hard delete with a state change — a cancelled or deleted state on `Order` that the read queries exclude — so the document and its index entry survive. That adds state to the aggregate and the first update path to `OrderRepository`. Note that `MongoOrderRepository.save` replaces the stored document wholesale, so the update path needs its own method that sets only the fields the domain owns.

## 🧪 Testing Considerations & Gaps

### 6. Transactional Semantics of Order Creation Are Unverified
- **Locations:** `CreateOrderHandler`, `BasketCheckoutIntegrationEventListener`
- **Issue:** `CreateOrderHandler` carries no `@Transactional`, and its only entry point is the integration-event listener, which runs on the outbox dispatcher's scheduler thread (the HTTP entry point, `POST /order`, was removed on 2026-09-15). One aggregate and one save, so this is very likely harmless today, and nothing proves it or would notice if a second write joined the handler.
- **Improvement:** Recorded so it is not rediscovered as a surprise rather than because a test is owed right now. It becomes a real gap the moment order creation writes more than once, which Backlog item 1 (`totalPrice`) and item 5 (`OrderCreated` outbox) both approach.

### 7. Slice Test Configs Can Still Drift From Production
- **Locations:** every `@DataJpaTest`/`@DataMongoTest`-based test's local `@Configuration`, versus the module configs in `main`
- **Issue:** [assembled-context-wiring-test](../ai-coding-tools/plans/assembled-context-wiring-test/assembled-context-wiring-test.md) closed the gap in one direction — nothing previously loaded the real assembled context — but it cannot see drift in the other direction. A slice test's hand-built `@Configuration` declaring a bean under the wrong name, missing a bean production has, or configuring one differently is invisible to a test of production config, by construction: production is what it loads, so it has nothing to compare against. This was one of the three original failures the new test set out to close (catalog's slice-declared transaction-manager names had drifted from production's) and it remains open.
- **Improvement:** No single test closes this; it needs slice test configuration to stop being reinvented per test class, or a comparison that isn't itself another hand-built config. Not scoped here.
