# Known Issues

Defects and testing gaps that are known, deliberately not fixed yet, and recorded here with the reasoning so they are neither rediscovered nor re-argued. The focus is business and behaviour logic, not line coverage.

The two sections are kept apart because *broken* and *unverified* call for different remedies.

**Relationship to [BACKLOG.md](BACKLOG.md):** this file is for code that is **wrong or unverified right now**; the backlog is for code that is fine as far as it goes but missing a capability or a decision. Route by nature, not by origin — something surfaced while deferring a plan step is usually a defect and belongs here.

**Relationship to plans:** a plan's *Deferred* section records what **that increment** chose not to do, and is frozen once the plan merges. These two files are the live lists. Anything a plan defers must land in one of them, or be consciously dropped — so something can legitimately appear in both a plan and here.

## 🐛 Known Bugs

### 4. Raw Card Data Is Stored and Transmitted in Clear
- **Locations:** `CheckoutPayment` (basket's checkout request), `orders.contract.PaymentDTO`, `basket.contract.PaymentDTO`, `orders.domain.valueobjects.Payment`, `basket.domain.valueobjects.Payment`, `PaymentDB`, the basket `outbox` collection
- **Issue:** The full card number, expiry and CVV travel through the API, the domain, the integration event, and both databases as plain text. CVV must never be stored at all under PCI DSS, and a PAN should be tokenized or reduced to brand plus last four digits. Order storage predates the checkout work; capturing payment at checkout widened the exposure by adding a second copy at rest, as JSON in the basket outbox. What PCI DSS allows, and why encrypting the data would not be enough: [card-data-and-pci-dss.md](../ai-coding-tools/context/card-data-and-pci-dss.md).
- **Impact:** Acceptable only because this is a study project against test data with no real cardholder information. It would be a blocking compliance defect in any deployed system.
- **Mitigated so far:** `InMemoryMessageBus` no longer logs message payloads, so the data does not reach application logs.
- **Action Required:** Out of reach of a refactor — it needs a payment provider integration, so that checkout carries a provider token instead of card details and no module ever sees a PAN. Until then, do not add further copies, and keep payloads out of logs.

### 13. Deleting an Order Lets Its Checkout Be Placed Again
- **Locations:** `DeleteOrderHandler` → `MongoOrderRepository.delete`; the `orders_checkoutId_unique` index on `OrderDB.checkoutId`; `BasketCheckoutIntegrationEventListener`
- **Issue:** A redelivered checkout event is deduplicated by the unique index on `checkoutId`: the second insert collides, and the listener catches `OrderAlreadyExists` and skips it. `DELETE /order/{id}` is a hard delete, so it removes the document and its index entry together. A redelivery of that checkout arriving after the delete no longer collides with anything, and the order is placed again.
- **Impact:** Narrow window, silent result. Redelivery happens only inside the outbox retry window — `markAsProcessed` failing after a successful publish (retried after the outbox backoff, 5 seconds after the first failure), or two application instances polling the same unprocessed event — so a customer would have to delete the order inside it. When it does happen, the deleted order simply reappears; nothing logs it as a duplicate, because it no longer is one.
- **Accepted on 2026-09-16** in [order-endpoints-scoped-to-customer](../ai-coding-tools/plans/order-endpoints-scoped-to-customer/order-endpoints-scoped-to-customer.md), which scoped delete to the caller's own orders but kept it a hard delete rather than widening that increment.
- **Action Required:** Replace the hard delete with a state change — a cancelled or deleted state on `Order` that the read queries exclude — so the document and its index entry survive. That adds state to the aggregate and the first update path to `OrderRepository`. Note that `MongoOrderRepository.save` replaces the stored document wholesale, so the update path needs its own method that sets only the fields the domain owns.

## 🧪 Testing Considerations & Gaps

### 6. Transactional Semantics of Order Creation Are Unverified
- **Locations:** `CreateOrderHandler`, `BasketCheckoutIntegrationEventListener`
- **Issue:** `CreateOrderHandler` carries no `@Transactional`, and its only entry point is the integration-event listener, which runs on the outbox dispatcher's scheduler thread (the HTTP entry point, `POST /order`, was removed on 2026-09-15). One aggregate and one save, so this is very likely harmless today, and nothing proves it or would notice if a second write joined the handler.
- **Improvement:** Recorded so it is not rediscovered as a surprise rather than because a test is owed right now. It becomes a real gap the moment order creation writes more than once, which Backlog item 5 (`OrderCreated` outbox) approaches. The order total added on 2026-10-19 is a field of the same document, so it did not.

### 7. Slice Test Configs Can Still Drift From Production
- **Locations:** every `@DataJpaTest`/`@DataMongoTest`-based test's local `@Configuration`, versus the module configs in `main`
- **Issue:** [assembled-context-wiring-test](../ai-coding-tools/plans/assembled-context-wiring-test/assembled-context-wiring-test.md) closed the gap in one direction — nothing previously loaded the real assembled context — but it cannot see drift in the other direction. A slice test's hand-built `@Configuration` declaring a bean under the wrong name, missing a bean production has, or configuring one differently is invisible to a test of production config, by construction: production is what it loads, so it has nothing to compare against. This was one of the three original failures the new test set out to close (catalog's slice-declared transaction-manager names had drifted from production's) and it remains open.
- **Improvement:** No single test closes this; it needs slice test configuration to stop being reinvented per test class, or a comparison that isn't itself another hand-built config. Not scoped here.
