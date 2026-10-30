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

## 🧪 Testing Considerations & Gaps

### 6. Transactional Semantics of Order Creation Are Unverified
- **Locations:** `CreateOrderHandler`, `BasketCheckoutIntegrationEventListener`
- **Issue:** `CreateOrderHandler` carries no `@Transactional`, and its only entry point is the integration-event listener, which runs on the outbox dispatcher's scheduler thread (the HTTP entry point, `POST /order`, was removed on 2026-09-15). One aggregate and one save, so this is very likely harmless today, and nothing proves it or would notice if a second write joined the handler.
- **Improvement:** Recorded so it is not rediscovered as a surprise rather than because a test is owed right now. It becomes a real gap the moment order creation writes more than once, which Backlog item 5 (`OrderCreated` outbox) approaches. The order total added on 2026-10-19 is a field of the same document, so it did not.

### 7. Slice Test Configs Can Still Drift From Production
- **Locations:** every `@DataJpaTest`/`@DataMongoTest`-based test's local `@Configuration`, versus the module configs in `main`
- **Issue:** [assembled-context-wiring-test](../ai-coding-tools/plans/assembled-context-wiring-test/assembled-context-wiring-test.md) closed the gap in one direction — nothing previously loaded the real assembled context — but it cannot see drift in the other direction. A slice test's hand-built `@Configuration` declaring a bean under the wrong name, missing a bean production has, or configuring one differently is invisible to a test of production config, by construction: production is what it loads, so it has nothing to compare against. This was one of the three original failures the new test set out to close (catalog's slice-declared transaction-manager names had drifted from production's) and it remains open.
- **Improvement:** No single test closes this; it needs slice test configuration to stop being reinvented per test class, or a comparison that isn't itself another hand-built config. Not scoped here.

### 9. End-to-End Response Steps Check the Thread's Last Response, Not the Named Actor's
- **Locations:** the `seeThatResponse` checks in a *Then* step that checks a request sent by an earlier step, under `e2e/`: `AuthenticationSteps`, `CatalogSteps`, `CheckoutSteps`, and `OrderSteps.receivesThatOrder` and `isRefusedAsNotFound`
- **Issue:** `seeThatResponse` reads `SerenityRest.lastResponse()`, which is the last response on the scenario's thread, whichever actor received it. A step worded "Bob's request is refused" checks Bob's response only because Bob made the last request. Every current scenario checks right after the same actor's own request, so all of them check the right response. A scenario that interleaves requests (Bob asks, then Alice asks, then Bob's request is checked) would check Alice's response, and could pass or fail for the wrong reason. Steps that send their own request and check it at once (`BasketSteps.noLongerHasABasket`, the `202` check in `OrderSteps.hasPlacedAnOrder`) and the questions (`TheBasket`, `TheCatalog`, `TheOrders`) aren't affected, because each reads the response to the request it just sent.
- **The same applies within one actor.** A question or a task that sends its own request moves the last response too, even though it reads its own answer correctly. `AddToTheBasket` and `OrderSteps.eventuallyHasExactlyOneOrder` both ask `TheCatalog`, which sends `GET /products`. A response step placed after any step that asks a question checks that question's response, not the request the step names, and nothing in the feature file shows it.
- **Improvement:** each task that sends a request remembers its response in the actor (`actor.remember(...)`), and the response steps assert on the response the named actor recalls. Until then, write a response step straight after that actor's own request, with no question asked in between.

### 16. End-to-End Setup Tasks Don't Check Their Own Response
- **Locations:** `CreateTheBasket`, `AddToTheBasket` and `CheckOut` under `e2e/`, as used by the *Given* steps `BasketSteps.hasInTheBasket` and `OrderSteps.hasPlacedAnOrder`
- **Issue:** the tasks send their request and never look at the status. A *Given* whose request fails (a basket that wasn't created, an add-to-basket answering `400`) doesn't fail its own step. The scenario carries on and fails later, on a *Then* that has nothing to do with the cause: a refused checkout, or `eventually` waiting 30 seconds for an order that was never placed. `hasPlacedAnOrder` checks the checkout's `202`, but not the basket requests before it.
- **Improvement:** a task used for setup checks that its response is 2xx and fails with the status and body. Where a *When* sends the request under test (`CheckOut` in `checkout.feature`, whose status the *Then* asserts), it uses a variant that doesn't check.

### 17. A New End-to-End Request Without the Token Goes Out Unauthenticated, Silently
- **Locations:** `UseABearerToken.authorize`, and every task and question under `e2e/` that calls it
- **Issue:** each request opts in to authentication by calling `UseABearerToken.authorize(actor, request)`, and `authorize` returns the request unchanged when the actor holds no token, which is how Visitor sends a request without one. A new task that forgets the call sends no token, whichever actor performs it. A scenario expecting a refusal then passes for the wrong reason, and one expecting success fails with a `401` that points away from the cause. Every current task and question makes the call.
- **Improvement:** one way to build a request, which always applies the actor's token when it has one: for example a small factory (`Api.get(path)`, `Api.post(path)`) that the tasks and questions use instead of Serenity's `Get` and `Post`. A request without a token then happens only because the actor has none.

### 18. The Declined-Checkout "No Order" Check Depends on the Outbox Poll Interval
- **Locations:** `OrderSteps.hasNoOrders` and its `PUBLISHING_WINDOW`, used by "Alice has no orders once the outbox has had time to publish" in `checkout.feature`
- **Issue:** the step sleeps 10 seconds, two cycles of the outbox dispatcher's 5-second poll, and then checks that the order list is empty. The window is a constant in the suite, not read from the application. If the poll interval grows past it, or the dispatcher backs off, the check runs before anything could have been published and passes whatever the application does. It also adds 10 seconds to every run.
- **Improvement:** show that the outbox has drained instead of waiting a fixed time. After the declined checkout the same customer checks out successfully, and the step waits for exactly one order, the paid one. The alternative is to keep only "the basket is unchanged" at end-to-end level, and check in basket's integration tests that a declined checkout writes no outbox event.

### 19. End-to-End Product Lookup Reads Only the First Page of the Catalog
- **Locations:** `TheCatalog.idOfTheProductNamed`, through `ListTheProducts` (`pageIndex=0`, `pageSize=10`)
- **Issue:** a product's id is found by its name in the first page of `GET /products`, which has no name filter. With the 3 seeded products this always works. The [e2e README](../e2e/README.md#parallelism) tells a scenario that writes catalog data to create its own product, though. Once the catalog holds more than 10, a product past the first page fails with "No product named …" although it exists. Under parallel runs, which page a product lands on can depend on what other scenarios have created, so the failure may come and go.
- **Improvement:** the seeded catalog is read-only, so read every page once per run and keep the name-to-id map. A product a scenario creates is known by the id its create request returned, not looked up by name.

### 20. End-to-End Steps Worded Generically Check One Endpoint's Answer
- **Locations:** `OrderSteps.isRefusedAsNotFound` ("{actor}'s request is refused as not found") and `AuthenticationSteps.isAccepted` ("{actor}'s request is accepted")
- **Issue:** both read as reusable after any request, and neither is. "Refused as not found" asserts the error code `order.not-found`, so reused after a basket request it fails on a code that a reader of the feature can't explain. "Accepted" asserts `200`, so it fails for a request answering `201` or `202`.
- **Improvement:** either word each step for its domain ("Bob is told the order doesn't exist"), or make it generic in fact, checking the status only, with the domain's error code in a separate step.
