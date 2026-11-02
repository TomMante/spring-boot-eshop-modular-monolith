# Known Issues

Defects and testing gaps that are known, deliberately not fixed yet, and recorded here with the reasoning so they are neither rediscovered nor re-argued. The focus is business and behaviour logic, not line coverage.

The two sections are kept apart because *broken* and *unverified* call for different remedies.

**Relationship to [BACKLOG.md](BACKLOG.md):** this file is for code that is **wrong or unverified right now**; the backlog is for code that is fine as far as it goes but missing a capability or a decision. Route by nature, not by origin — something surfaced while deferring a plan step is usually a defect and belongs here.

**Relationship to plans:** a plan's *Deferred* section records what **that increment** chose not to do, and is frozen once the plan merges. These two files are the live lists. Anything a plan defers must land in one of them, or be consciously dropped — so something can legitimately appear in both a plan and here.

## 🐛 Known Bugs

### 13. Deleting an Order Lets Its Checkout Be Placed Again
- **Locations:** `DeleteOrderHandler` → `MongoOrderRepository.delete`; the `orders_checkoutId_unique` index on `OrderDB.checkoutId`; `BasketCheckoutIntegrationEventListener`
- **Issue:** A redelivered checkout event is deduplicated by the unique index on `checkoutId`: the second insert collides, and the listener catches `OrderAlreadyExists` and skips it. `DELETE /order/{id}` is a hard delete, so it removes the document and its index entry together. A redelivery of that checkout arriving after the delete no longer collides with anything, and the order is placed again.
- **Impact:** Narrow window, silent result. Redelivery happens only inside the outbox retry window — `markAsProcessed` failing after a successful publish (retried after the outbox backoff, 5 seconds after the first failure), or two application instances polling the same unprocessed event — so a customer would have to delete the order inside it. When it does happen, the deleted order simply reappears; nothing logs it as a duplicate, because it no longer is one.
- **Accepted on 2026-09-16** in [order-endpoints-scoped-to-customer](../ai-coding-tools/plans/order-endpoints-scoped-to-customer/order-endpoints-scoped-to-customer.md), which scoped delete to the caller's own orders but kept it a hard delete rather than widening that increment.
- **Action Required:** Replace the hard delete with a state change — a cancelled or deleted state on `Order` that the read queries exclude — so the document and its index entry survive. That adds state to the aggregate and the first update path to `OrderRepository`. Note that `MongoOrderRepository.save` replaces the stored document wholesale, so the update path needs its own method that sets only the fields the domain owns.

### 14. A Failed Refund Leaves the Customer Charged With No Order
- **Locations:** `CheckoutBasketHandler.refundAfterFailedCheckout`, `StripePaymentGateway.refund`
- **Issue:** checkout charges the customer before the basket transaction opens, and refunds the charge if that transaction fails. If the refund also fails (the provider unreachable, a timeout), nothing retries it: the handler logs an ERROR with the checkout id and the charge reference, and rethrows the original failure. The customer has paid, no order exists, and the basket is still there.
- **Impact:** needs two failures in a row: the basket transaction (Mongo, the outbox write) and then the provider. Rare, but the outcome is money taken for nothing, and the only trace is a log line. The charge can be found in Stripe from the checkout id, through the PaymentIntent's `metadata[checkout_id]`.
- **Accepted on 2026-10-26** in [tokenized-card-payment](../ai-coding-tools/plans/tokenized-card-payment/tokenized-card-payment.md), which introduced the charge and kept the compensation synchronous and best-effort.
- **Action Required:** make the refund durable instead of best-effort, e.g. write a pending-refund record that a scheduled job retries until the provider confirms, the way the outbox retries events. The refund call is already idempotent (`refund-<charge reference>`), so retrying it is safe.

### 21. The Basket Accepts a Quantity of Zero or Less
- **Locations:** `AddItemIntoBasketRequest.quantity`, `ShoppingCart.addItem`, `Item.increaseQuantity`
- **Issue:** nothing validates the quantity of a basket line. Found on 2026-10-30 by the end-to-end suite: `POST /basket/items` with quantity 0 answered 2xx, and checkout placed an order whose only line has quantity 0 and whose total is 0.00. A negative quantity takes the same path (not reproduced yet): as a new line it gives a negative line total, and merged into an existing line it lowers that line's quantity, possibly to zero or below.
- **Impact:** one ordinary request with a bad value produces a silently wrong order.
- **Action Required:** reject a quantity below 1 at the request (`@Positive`, with a message key like the existing `basket.basketItem.productId.notNull`), and make it an invariant of `ShoppingCart`/`Item`, so that no line can hold less than 1 whatever the caller. Decide what a merge that would end below 1 means; rejecting it is the simplest answer.

### 22. Basket Add/Remove Errors Answer 500
- **Locations:** `AddItemIntoBasketHandler`, `RemoveItemFromBasketHandler`; the basket's `@RestControllerAdvice` classes, `GetBasketErrorHandler` and `CheckoutBasketErrorHandler`, each scoped by `basePackages` to its own feature package
- **Issue:** `BasketNotFound` (thrown by add and remove when the caller has no basket) and `CatalogProductNotFound` (thrown by add for an unknown product id) have no handler in the `additemintobasket` and `removeitemfrombasket` packages. They reach the servlet, and the caller gets a bare 500 with no error code instead of a 404 saying what is missing. Seen on 2026-10-30 for add-item without a basket; the remove path and the unknown-product path are inferred from the code.
- **Action Required:** error handlers for `additemintobasket` and `removeitemfrombasket`, mapping `BasketNotFound` to `404 basket.not-found` and `CatalogProductNotFound` to a 404 with its own code, like the other features' handlers.

### 23. Concurrent First Checkouts on a Fresh Database Fail With 500
- **Locations:** `MongoOutboxEventStore` and the `outbox` collection in `eshop-basket-db`; `CheckoutBasketHandler`'s basket transaction
- **Issue:** nothing creates the `outbox` collection up front, so the first checkouts on a fresh database create it implicitly inside their Mongo transactions. When two run at once, one fails with MongoDB error 112 `WriteConflict` ("Collection namespace 'eshop-basket-db.outbox' is already in use", labelled `TransientTransactionError`), surfacing as a `TransactionSystemException` and a 500. Nothing retries a transient transaction error. Seen on 2026-11-01: a stack whose Mongo container was created at 14:36:01 failed a setup checkout at 14:36:38, on the end-to-end suite's first parallel checkouts.
- **Impact:** the customer is charged before the transaction opens, so every occurrence also takes Known Bug 14's refund path. The end-to-end workflow in CI starts from a fresh stack on every run, so the suite can fail there for this reason alone.
- **Action Required:** create the collection and its indexes at startup (in `mongo-init`, or when the application starts), or retry the transaction on `TransientTransactionError`. Creating it up front removes the cause; retrying also covers other transient conflicts.

## 🧪 Testing Considerations & Gaps

### 6. Transactional Semantics of Order Creation Are Unverified
- **Locations:** `CreateOrderHandler`, `BasketCheckoutIntegrationEventListener`
- **Issue:** `CreateOrderHandler` carries no `@Transactional`, and its only entry point is the integration-event listener, which runs on the outbox dispatcher's scheduler thread (the HTTP entry point, `POST /order`, was removed on 2026-09-15). One aggregate and one save, so this is very likely harmless today, and nothing proves it or would notice if a second write joined the handler.
- **Improvement:** Recorded so it is not rediscovered as a surprise rather than because a test is owed right now. It becomes a real gap the moment order creation writes more than once, which Backlog item 5 (`OrderCreated` outbox) approaches. The order total added on 2026-10-19 is a field of the same document, so it did not.

### 7. Slice Test Configs Can Still Drift From Production
- **Locations:** every `@DataJpaTest`/`@DataMongoTest`-based test's local `@Configuration`, versus the module configs in `main`
- **Issue:** [assembled-context-wiring-test](../ai-coding-tools/plans/assembled-context-wiring-test/assembled-context-wiring-test.md) closed the gap in one direction — nothing previously loaded the real assembled context — but it cannot see drift in the other direction. A slice test's hand-built `@Configuration` declaring a bean under the wrong name, missing a bean production has, or configuring one differently is invisible to a test of production config, by construction: production is what it loads, so it has nothing to compare against. This was one of the three original failures the new test set out to close (catalog's slice-declared transaction-manager names had drifted from production's) and it remains open.
- **Improvement:** No single test closes this; it needs slice test configuration to stop being reinvented per test class, or a comparison that isn't itself another hand-built config. Not scoped here.

### 24. The Declined-Checkout Check Infers the Outbox Instead of Reading It
- **Locations:** "A declined checkout keeps the basket and places no order" in `e2e/src/test/resources/features/checkout.feature`
- **Issue:** after the decline, the same customer checks out with a valid card, and the scenario waits for exactly one order, the paid one. Basket's outbox hands out due events oldest first (`MongoOutboxEventStore` sorts by `occurredOn`), so an event written by the decline would become an order no later than the paid one. The check relies on that ordering: if `MongoOutboxEventStore` stopped handing out events oldest first, the scenario would stay green but prove less, and nothing would say so. The suite only talks HTTP, though, and nothing in the API shows the outbox, so this is inferred, not read. A decline event whose first publish *failed* is retried after the outbox backoff, possibly after the paid order arrived, and the check would miss it.
- **Improvement:** check the outbox through a public surface once one exists, e.g. the pending-events gauge in Backlog 27.
