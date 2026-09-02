# Basket Checkout Creates Order

## ⚠️ Known limitation shipped by this increment — MUST be fixed next

**`POST /basket/checkout` returns a 500 after this merges.** Deliberate, accepted, and tracked — but do not lose it.

The listener creates orders with `null` shipping address, billing address, and payment (the "pending order" decision below). `DBMapper.toDb(Order)` and `DTOMapper.toDto(Order)` dereference those fields unconditionally, so `MongoOrderRepository.save` throws a `NullPointerException` inside the listener, in the publisher's transaction.

- This is a **regression**: `POST /basket/checkout` works on `main` today (it empties the basket and publishes an event nobody consumes). After this merge it fails.
- The full test suite is green regardless: the listener test uses `FakeOrderRepository`, and every Mongo test builds orders via `OrderMother`, which always populates address and payment. No test exercises the shape production actually produces.
- **Step 6's manual smoke check cannot pass** and is deferred with it.

Fixed by [basket-checkout-captures-address-and-payment](../basket-checkout-captures-address-and-payment/basket-checkout-captures-address-and-payment.md), which puts the missing data on the integration event so the nulls stop existing. Decision recorded: accepted knowingly to keep this PR small on a personal project.

## Context

The basket module already publishes a `BasketCheckoutIntegrationEvent` when a customer checks out, and the ordering module already has a listener stub for it (`BasketCheckoutIntegrationEventListener`, added in commit `5240dea`) — but the listener only logs the event, it doesn't create anything. This feature implements the missing piece: turning a received checkout event into a persisted `Order`.

Investigation surfaced three real design gaps, discussed and resolved before writing this todo list:

1. **Identity mismatch** — `Order.customerId` is a `UUID`, but the only identity anywhere in the system is `CurrentUser.username` (`String`, from `shared.contract.identity`). No username→UUID mapping exists.
   → **Decision: change `Order.customerId` to `String`.** Simplest, matches what the system actually has; no synthetic ID, no new identity module.

2. **Missing address/payment** — `BasketCheckoutIntegrationEvent` carries only `userName`, `items`, `totalPrice`. `Order.create(...)` currently takes required `Address`/`Payment` args, but its constructor performs no null-checks itself (validation of those fields today lives only at the `CreateOrderRequest` DTO layer via `@NotNull`).
   → **Decision: orders created from checkout are "pending"** — created with no shipping/billing address and no payment, to be filled in by a later step (out of scope here). This is a deliberate intermediate state: the next increment extends the basket checkout flow to collect address and payment, after which the nulls disappear.

3. **No domain events / outbox on `Order`** — `Order` never calls `addDomainEvent`, and `OrderingModuleConfig` has no outbox wiring.
   → **Decision: out of scope.** Don't add a speculative `OrderCreated` event/outbox until something needs to consume it.

Also confirmed out of scope: the known `ShoppingCart.checkout` bug (ignores quantity in total price, NPEs on missing price — tracked in `docs/KNOWN_ISSUES.md`). It does **not** reach ordering in this increment: `Order` has no total-price field, so the event's `totalPrice` is discarded by the listener, and ordering `Item.price` is mapped from basket's `priceWhenAdded` rather than from the recomputed total. Storing the total is a follow-up (see Deferred), and deliberately not done here — persisting a number already known to be wrong is worse than not persisting it yet.

### Revised approach: reuse `CreateOrderHandler`, no new feature slice

An earlier draft of this plan added a `createorderfromcheckout` feature slice (new command + handler + handler test) and an `Order.createFromCheckout` factory. Both were dropped after review:

- **Vertical slices are cut by capability, not by trigger.** "Create an order" is the capability; the HTTP `POST /order` and the checkout integration event are two transports into it. Slicing by transport leads to `createorderfromcheckout`, `createorderfromadmin`, `createorderfromimport` — several copies of one behaviour.
- **The two paths converge.** Once the basket checkout flow collects address and payment (gap 2 above, next increment), the event carries exactly `CreateOrderCommand`'s shape. A separate slice built now would be built to be deleted.
- **Provenance is metadata, not a use case.** Why an order was created is recoverable from the correlation id and the outbox/message log; it does not justify a duplicated slice.

So the listener becomes a genuine adapter: map basket `ItemDTO`s → ordering `Item`s, build a `CreateOrderCommand` with `null` address/payment/name, and invoke the existing `CreateOrderHandler`. `Order.createFromCheckout` becomes dead code and is deleted.

### Testing rigor

Each step below is marked with its rigor per the project's "TDD where it earns its cost" rule ([rules-of-engagement.md](../../processes/rules-of-engagement.md)) — mechanical refactors get tests updated alongside them; new deterministic logic gets red→green→refactor.

No isolated `OrderTest` is written here. An earlier draft called for one on the strength of the "Domain Isolation" *MUST* in [getting-started.md](../../processes/getting-started.md), but `Order` gains no calculation, invariant, state machine, or merge logic in this increment — a domain test would pin nothing that the listener test does not already prove. (That blanket *MUST* is due a rewording to the narrower criterion; tracked separately, not part of this increment.)

**Open call:** whether a checkout event with an empty item list should be rejected. Nothing enforces it today — the checkout path has no DTO boundary, so `@NotNull`-style validation does not reach it. If it is to be rejected, that rule is a genuine `Order` invariant and *would* earn a small isolated domain test.

---

## Todo List

### 1. Migrate `Order.customerId`: `UUID` → `String`
*Mechanical refactor, no behavior change — update tests alongside, no TDD ceremony needed.*

*Implemented slightly beyond plan: the field was also **renamed** `customerId` → `customer`, everywhere including the message key (`ordering.order.customer.notBlank`). Gap 1 in the Context section above still describes it as `customerId`; the code is the source of truth.*

- [x] `domain/Order.java` — field, `create`, `reconstitute`, accessor
- [x] `infrastructure/persistence/mongo/model/OrderDB.java` + `DBMapper.java`
- [x] `contract/OrderDTO.java` + `DTOMapper.java`
- [x] `application/features/createorder/{CreateOrderCommand,CreateOrderRequest,CreateOrderController,CreateOrderHandler}.java` (existing manual `POST /order` path) — swapped `@NotNull` for `@NotBlank` on `CreateOrderRequest.customer`, plus a blank-string test case
- [x] `orders/domain/OrderMother.java` test builder
- [x] Sweep remaining test usages: `grep -rn "customerId" modules/ordering/src/test` — at minimum `CreateOrderHandlerTest`, `CreateOrderControllerTest`, `MongoOrderRepositoryTest`, `MongoOrderQueryRepositoryTest`, `GetOrderByIdHandlerTest`/`ControllerTest`, `GetOrdersHandlerTest`/`ControllerTest`

*(all paths under `modules/ordering/src/main/java/com/github/eshopmodularmonolith/ordering/orders/` unless noted)*

### 2. Remove the `createFromCheckout` scaffolding

- [x] Delete `Order.createFromCheckout(String, List<Item>)` from `domain/Order.java` — superseded by the listener building a `CreateOrderCommand`
- [x] Leave `Order.create(...)` and `Order.reconstitute(...)` untouched

### 3. Add a test-only accessor to `FakeOrderRepository`
*Test infrastructure, no production behavior.*

- [x] `orders/infrastructure/persistence/fake/FakeOrderRepository.java` — expose the stored orders (`List<Order> orders()`), so tests can assert persistence without knowing the generated id. Mirrors `FakeBasketRepository.domainEvents()`
- [ ] Add a `clear()` for test isolation — Spring caches the test context across methods, so the fake is a single bean shared by every test in the class. Harmless with one test; silently breaks the second one added. Call it from a `@BeforeEach`. Seeding/reset methods on fakes are already house style (`FakeOrderQueryRepository.addOrder`)

### 4. Implement the listener as an adapter
*Glue code following the established pattern — test alongside implementation, not strict TDD.*

- [x] Replace the log-only body of `infrastructure/messaging/BasketCheckoutIntegrationEventListener.java`:
  - [x] inject `CreateOrderHandler` via constructor
  - [x] map basket `ItemDTO(id, productId, quantity, color, priceWhenAdded, productName)` → ordering `Item(id, productId, quantity, price)` — drop `color`/`productName`, `price` ← `priceWhenAdded`
  - [x] build `CreateOrderCommand(event.userName(), null /* name */, null, null, null, mappedItems)` and invoke the handler
  - [x] the event's `totalPrice` has no home on `Order` and is dropped — deliberate for now, see Deferred

### 5. Rework `BasketCheckoutIntegrationEventListenerTest`
*This is the boundary test for the checkout→order path — it carries the behavioural weight, since there is no new slice.*

- [x] Wire the **real** `CreateOrderHandler` over a `FakeOrderRepository` in the Spring test context (a static `@Configuration` inner class registering fake repo + handler + listener). Do **not** mock or spy the handler — `verify(handler).handle(cmd)` asserts an interaction rather than an outcome, breaks on command-shape changes that alter no behaviour, and proves no order exists
- [x] Replace the existing `@MockitoSpyBean` + `verify(listener).handle(event)` test — it only asserted that Spring calls the method it was told to call
- [x] Single test, GWT-in-method-name per [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md): publish a `BasketCheckoutIntegrationEvent`, then assert on the persisted order — customer matches `userName`, items mapped correctly (including `priceWhenAdded` → `price`), and `shippingAddress`/`billingAddress`/`payment`/`name` all null. The four null assertions are what replaces the domain test that was dropped; without them nothing pins what a pending order is
- [x] Use **distinct** values in the test data — quantity `2`, price `12.50`, separate `id`/`productId` UUIDs. Quantity `1` with price `BigDecimal.ONE` cannot detect a field swap in the mapping, which is the exact class of bug this test exists to catch
- [x] Compare `BigDecimal` with `isEqualByComparingTo`, not `isEqualTo` — scale differences (`12.50` vs `12.5`) fail equality but not comparison, and will bite once a Mongo round-trip is involved
- [x] Keeping the Spring context is deliberate: proving the `@EventListener` wiring actually fires is the one thing only a context test can do
- [x] Note: this does **not** close `docs/KNOWN_ISSUES.md` gap 3 ("Untested Application Domain Event Listeners"). That item is scoped to `BasketCheckedOutHandler` and `ProductCreatedHandler`, both still untested. Earlier drafts of this plan cited it as "item 4" and claimed it was closed — both wrong

*Note: the item mapping is covered through this test. If the mapping later grows beyond a straight field copy, extract it to a small static mapper beside the listener with its own container-free unit test.*

### 6. Verification

- [x] `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green
- [ ] ~~Manual smoke check: `POST /basket/checkout` → `GET /order`~~ — **deferred, cannot pass.** See the known limitation at the top of this document: the checkout endpoint 500s on the null-hostile mappers. Runs for the first time in [basket-checkout-captures-address-and-payment](../basket-checkout-captures-address-and-payment/basket-checkout-captures-address-and-payment.md)
- [ ] Note while smoke-checking: `GetOrdersHandler` sorts by `name` ASC, so checkout-created orders (`name = null`) will cluster at the top of every page. Not a defect in this increment; symptom of the `name` → `createdAt` follow-up below
- [x] Check [README.md](../../../README.md) per the README-maintenance rule — **verified, no change needed**: the README does not mention orders, order creation, or the checkout flow at all
- [x] `docs/KNOWN_ISSUES.md` reviewed — gap 3 stays open (see step 5); gap 1 was rewritten during this session to match the new [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) criteria, and Known Bug 1 gained the duplicate-product `Collectors.toMap` defect

---

## Explicitly Deferred (not part of this increment)

- **Null-hostile mappers — the known limitation at the top of this document.** `DBMapper` (`toDb`/`toDomain`/`toDto` for `Address` and `Payment`) and `DTOMapper` (same four) dereference their argument immediately, so a pending order cannot be saved or read. Two ways out, both deferred to the next plan, which removes the nulls entirely and makes the choice moot:
    - null-guard the ten mapper methods (`if (x == null) return null;`), or
    - keep them strict and let the invariant live in `Order.create` with a real error message once address/payment become mandatory.
  Rejected during discussion: substituting empty `Address`/`Payment` objects — `Payment.paymentMethod` is an `int` so an "empty" payment must invent a value, and an all-null address is indistinguishable from a genuinely blank one, which destroys the "absent" signal both the API and the follow-up increment need.
- **Pre-existing NPE on the manual path.** `CreateOrderRequest.shippingAddress` has no `@NotNull` (unlike `billingAddress` and `payment`), and `CreateOrderController` calls `DTOMapper.toDomain(request.shippingAddress())` unconditionally. `POST /order` without a shipping address is a 500 **today**, independent of checkout. Not introduced here, but it shares a root cause with the item above and should be fixed with it.
- **Shipping address should default to billing when absent.** Decided during this session, not implemented: billing is mandatory, so an omitted shipping address should copy billing rather than be null or rejected. This is a real domain rule and belongs in the address-capture increment, where billing actually gets collected. Note it does **not** help the checkout path — there is no billing address there either to copy from.
- **`Order.create` has no invariants.** Once address and payment become mandatory, the null check belongs there — one place, at the layer that owns the rule, with an error message naming the missing field — not in a persistence mapper, which should translate rather than validate. Also unresolved: whether an order with an empty item list should be rejected (the checkout path has no DTO boundary, so nothing enforces it today).
- **No idempotency/dedup.** `BasketCheckoutIntegrationEvent` now carries an `eventId`, but no consumer-side dedup mechanism exists anywhere in this codebase; a redelivered event (e.g. from outbox dispatcher retry, which is at-least-once by construction) would still create a duplicate order. Worth a backlog note, not fixed here.
- **Transaction boundary.** `CreateOrderHandler` has no `@Transactional`, and a synchronous `@EventListener` runs inside the publisher's transaction — so the HTTP path and the event path do not share transactional semantics. One aggregate, one save, so likely harmless; noted so it isn't discovered later by surprise.
- **No "pending" status field.** Nothing in the read model distinguishes a checkout-originated pending order from a fully detailed one yet. Fine while nothing queries on it; likely needed once a "complete your order" step exists.
- `OrderCreated` domain/integration event + outbox wiring for `Order` — add when another module actually needs to react to order creation.

---

## Follow-ups (own increments, surfaced by this work)

### A. Pricing correctness + `Order` total price

`Order` should keep the total price — decided, but deliberately not done in this increment, because the number is currently wrong and the two available price sources disagree by design:

| Value | Source | Meaning |
| --- | --- | --- |
| ordering `Item.price` | basket's `priceWhenAdded` | price when the item was added to the cart |
| event `totalPrice` | `ShoppingCart.checkout(currentPrices)`, refetched from the catalog | price at checkout |

Storing both without a decision persists a contradiction: `Σ(item.price × quantity)` will not equal `totalPrice` whenever a catalog price moved between add and checkout, and nothing marks which is authoritative.

**Decide the pricing basis first:**

- *Checkout-time pricing* (conventional for e-commerce — price confirmed at purchase): checkout overwrites item prices with current ones; total is `Σ(current price × quantity)`. Gives the basket's refetch an actual purpose.
- *Add-time pricing* (customer keeps the price they saw): basket stops refetching; total is `Σ(priceWhenAdded × quantity)`, derivable from items — storing it is then an audit snapshot rather than new information.

Then, in one piece of work (all in `ShoppingCart.checkout`, plus ordering plumbing):

- Fix the three known `ShoppingCart.checkout` defects — quantity ignored in the total, NPE when a product is missing from `currentPrices`, and `IllegalStateException` from `Collectors.toMap` in `CheckoutBasketHandler` when the same product appears as two items (currently tracked in `docs/KNOWN_ISSUES.md`)
- Add `totalPrice` to `Order` + `OrderDB` + `OrderDTO` + both mappers + `CreateOrderCommand`/`Request`/`Controller`
- This is the calculation that *does* earn an isolated domain test — multiple items, quantity > 1, missing price, duplicate product

### B. `Order.name` → `placedAt` — done

Implemented in [order-name-to-placed-at](../order-name-to-placed-at/order-name-to-placed-at.md).

`name` was carried through every layer (`Order`, `OrderDB`, `OrderDTO`, `CreateOrderCommand`, `CreateOrderRequest`, both mappers) with a `@NotNull` and a message key, but nothing read it for a decision — inherited from the eShop reference implementation's `OrderName` value object rather than from any requirement here. Its one real consumer was `GetOrdersHandler`, which used it as the default sort key (`SortOrder("name", ASC)`).

What shipped, and where it departed from this sketch:

- `name` deleted from all layers, including the `ordering.order.name.notBlank` message key
- **Named `placedAt`, not `createdAt`.** `createdAt` reads as persistence bookkeeping, which is most of what made the ownership question feel ambiguous. When an order was placed is a business fact — invoices, return windows, "Your Orders" — so it takes the domain's name and lives on the aggregate. The audit-metadata vs domain-temporal-fact reasoning is recorded in that plan's Context
- `GetOrdersHandler` now sorts by `placedAt` DESC
- The `Clock` did earn its keep as anticipated, but the domain **receives** the instant rather than generating it — `Order.create(customer, placedAt, ...)`, with `CreateOrderHandler` holding an injected `Clock`. Registered as `@Bean("orderClock")` (one application context, so an unqualified `clock` would clash) using `Clock.tickMillis(ZoneOffset.UTC)`, since Mongo stores `Instant` as a millisecond-precision BSON date
- Fixed along the way: `FakeOrderQueryRepository` silently ignored `sortOrders`, so nothing could pin the handler's sort choice — the fake now honours it and rejects unknown properties
- Known gap left open deliberately: the `placedAt` sort has no tie-breaker, so orders sharing an instant have unspecified relative order. Fix is a secondary key (`placedAt` DESC, `id` ASC); more clock precision would not help
- Surfaced but not done: a `MongoAuditMetadata` counterpart to `shared.infrastructure.JpaAuditMetadata`. Needs `@Version` on the document to work at all — Mongo auditing decides created-vs-modified purely from `isNew`, which is id-based, and every document here assigns its id up front. Scoped to its own increment
