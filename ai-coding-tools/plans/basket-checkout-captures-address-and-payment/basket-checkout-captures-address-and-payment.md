# Basket Checkout Captures Address and Payment

## Status

**Implemented and verified.** Follow-up to [basket-checkout-creates-order](../basket-checkout-creates-order/basket-checkout-creates-order.md), whose deliberately-shipped regression this closes.

Full suite green. Smoke-tested end to end both ways — with a shipping address, and with it omitted so the step 2 default applies — producing complete orders with address and payment populated, and outbox documents marked `processedOn`. This is the first time the checkout → order path has ever worked.

## Context

The previous increment knowingly shipped a broken checkout path. `BasketCheckoutIntegrationEventListener` builds a `CreateOrderCommand` with `null` shipping address, billing address and payment, and ordering's `DBMapper.toDb(Order)` dereferences all three unconditionally.

This increment fixes it at the source: the checkout endpoint collects address and payment, the data rides the integration event, and the listener builds a complete order. `Order` then gains the invariants that make a null address a loud, named failure instead of an NPE inside a mapper.

The test suite is green today because the listener test uses `FakeOrderRepository` and every Mongo test builds fully populated orders via `OrderMother` — no test exercises the shape production actually produces.

### The failure is not a 500 — correcting the earlier plan documents

Earlier drafts of both plans describe this as `POST /basket/checkout` returning 500. Tracing the wiring, it isn't:

1. `CheckoutBasketHandler` (`@Transactional`) → `MongoBasketRepository.delete(cart)` publishes the domain events → `OutboxEventSubscriber` (`BEFORE_COMMIT`) writes to the `outbox` collection in `eshop-basket-db` → commit → **HTTP returns 202**.
2. On a separate thread, `BasketOutboxDispatcher.dispatch()` (`@Scheduled(fixedDelay = 5000)`) deserializes the event, publishes the integration event via `InMemoryMessageBus`, the ordering listener runs synchronously — and the NPE is thrown there.
3. The NPE propagates out of `dispatch()` **before** `markAsProcessed(event)` runs.

So it is a **poison-message loop**: the document stays unprocessed and is retried every 5 seconds indefinitely. Because `dispatch()` has no per-event error isolation, that one document also blocks every later basket outbox event behind it.

Two consequences that shape the work below: "confirm the 500 is gone" is not a valid check (the real check is `processedOn` being set and the scheduler log going quiet), and **the local Mongo almost certainly holds poison documents right now** carrying the *old* `BasketCheckedOut` JSON — which must be cleared before smoke testing, or they will re-poison against the new invariants.

### Design decisions

- **Defaulting is checkout's job; rejecting is ordering's.** Checkout owns the entry point and validates its input, so it also normalizes it — an absent shipping address is filled from billing *there*. `Order` does not invent a shipping address it wasn't given; it only refuses to be built without one. The pre-existing `POST /order` null-shipping 500 is fixed by the missing `@NotNull`, not by moving a rule into the aggregate.
- **`ShoppingCart` does not validate address or payment.** It carries them through to the event as plain data.
- **Plain `AddressDTO` / `PaymentDTO` on `basket-contract`.** Same names as ordering's, different package — exactly what `ItemDTO` already does in both modules. Each module owns its contract shapes; no `shared.contract` sharing, which would couple two modules that are meant to be independently deployable.
- **`BasketCheckedOut` carries basket domain value objects**, not contract DTOs — plain records in `basket.basket.domain.valueobjects` beside the existing `ProductInfo`, mapped to DTOs by `BasketOutboxDispatcher` exactly as `Item` → `ItemDTO` is today. No validation in them; they carry data.
- **One DTO shape; mapping split by layer, not by direction.** `CheckoutBasketRequest` and `BasketCheckoutIntegrationEvent` both use the `basket-contract` DTOs — duplicating the shape now would be overkill. The *mapping* is split by the layer that owns it, and the two must never be the same class even where the field copies coincide:
    - `application/mapper/DTOMapper` — between this module's REST contract and its domain, in whichever direction the application layer needs.
    - `BasketOutboxDispatcher` — between the domain and the published integration event, private to infrastructure, beside its existing `toDto(Item)`.

  The domain value object is the pivot, so neither side knows about the other. That keeps the escape hatch cheap: if the request shape later needs to diverge from the published event, introduce a request-specific DTO and only the application mapper changes — the integration contract is untouched.
- **Parameter order is `billingAddress, payment, shippingAddress`, at every hop.** Mandatory first, optional last — but the real reason is that it stops the two `AddressDTO` slots being adjacent, and an adjacent transposition of two same-typed arguments is the easiest slip to make and the hardest to see. It is a mitigation, not a guarantee: both remain `AddressDTO`, so only distinct test values actually catch a swap. Carry this one order through the whole chain — `CheckoutBasketRequest` → `CheckoutBasketCommand` → `ShoppingCart.checkout(...)` → `BasketCheckedOut` → `BasketCheckoutIntegrationEvent` → `CreateOrderCommand` → `Order.create(...)`. Two adjacent structures disagreeing about the order is what produced the bug this rule exists to prevent. Ordering's `CreateOrderCommand` and `CreateOrderController` were brought onto the same order during step 3, so the chain is uniform end to end; `Order.create` is the last hop still on the old shipping-first order and is corrected in step 4.
- **Empty-item-list invariant is deferred** — it only lands safely with a matching basket-side guard, which is scope creep here. See Deferred.
- **Card data**: fix the log line, record the rest. See step 5.

### Testing rigor per step

Marked inline, per the "TDD where it earns its cost" rule in [rules-of-engagement.md](../../processes/rules-of-engagement.md). Steps are sequenced so the suite is green at every commit.

---

## Todo List

### 1. Basket: accept address and payment at checkout
*Mechanical plumbing following an established pattern — tests alongside implementation.*

**Status:** complete, with two clean-ups noted below. All `fail("REVIEW")` markers resolved.

- [x] `basket-contract`: add `AddressDTO` and `PaymentDTO` records mirroring ordering's field lists, beside the existing `ItemDTO`
- [x] `basket-core` `domain/valueobjects/`: add plain `Address` and `Payment` records (no validation), beside `ProductInfo`
- [x] `CheckoutBasketRequest` (new, in `application/features/checkoutbasket/`): `@NotNull billingAddress`, `@NotNull payment`, `shippingAddress` using the contract DTOs. Message keys `basket.checkout.billingAddress.notNull` / `basket.checkout.payment.notNull` added. *Reorder to mandatory-first per the design decision above*
- [x] `CheckoutBasketController` — takes `@RequestBody @Validated CheckoutBasketRequest`; username still from `@AuthenticatedUser`
- [x] Absent shipping address no longer NPEs in the controller — ternary at the call site passes `null` through, `DTOMapper` stays strict, and `whenShippingAddressIsMissing_thenRequestGoesThrough` asserts 202.

  *This is plumbing, not the step 2 rule, and the two do not overlap:* the controller passes absence through, the handler turns absence into billing. `CheckoutBasketCommand.shippingAddress` is therefore nullable by design — the accepted cost of keeping the rule at a seam that produces an observable outcome (the `BasketCheckedOut` event) rather than a captured command argument, and of keeping every controller in this codebase a thin adapter.
- [x] `application/mapper/DTOMapper` — application-layer translation, both directions. Settled: the layer boundary is what matters, not the direction. `toDto` currently has no production caller (only `CheckoutBasketControllerTest`); left in place deliberately as not worth the churn
- [x] `CheckoutBasketCommand` carries **domain value objects**, not DTOs — the DTO stops at the controller boundary, as `CreateOrderCommand` does in ordering. *Two fixes outstanding: rename its `paymentMethod` component to `payment` (the type is already `Payment` and `Payment` has its own `paymentMethod` int, so today it reads `cmd.paymentMethod().paymentMethod()`), and reorder to `userName, billingAddress, payment, shippingAddress`*
- [x] Threaded on through `CheckoutBasketHandler` → `ShoppingCart.checkout(currentPrices, billing, payment, shipping)` → `BasketCheckedOut`, mandatory-first order held at every hop
- [x] `BasketOutboxDispatcher.toIntegrationEvent(...)` and `BasketCheckoutIntegrationEvent` carry the DTOs
- [x] The dispatcher's `toDto(Address)` / `toDto(Payment)` construct the DTOs directly, beside `toDto(Item)` — no dependency on the application `DTOMapper`, so the layer boundary is real rather than nominal
- [x] `CheckoutBasketControllerTest` — order fixed and billing/shipping now genuinely differ via `AddressMother.address("Midgard")`. *Minor left: `whenBillingAddressIsMissing…` still passes `null` for shipping too, so it omits more than its name claims*
- [x] `CheckoutBasketHandlerTest` — markers resolved; asserts billing, payment and shipping on the emitted `BasketCheckedOut`
- [x] `CheckoutBasketHandlerTransactionalTest` — markers resolved with real values, no new assertions. It exercises *serialization* of the new event shape but only counts documents, so it does not replace the round-trip test in Verification
- [x] Unused `fail` imports removed from `CheckoutBasketHandlerTest` and `CheckoutBasketHandlerTransactionalTest`

### 2. Shipping defaults to billing when absent
*New deterministic rule — strict red → green → refactor.*

- [x] Lives in **`ShoppingCart.checkout`** — revised from "the handler" while implementing. The aggregate already derives `totalPrice` for the same event, so defaulting shipping is the same kind of work in the same place; the handler derives nothing and stays pure orchestration. Rejected alternatives: the controller (would make `CheckoutBasketCommand.shippingAddress` non-nullable, but the rule is not HTTP-specific and every controller here is a thin command-building adapter), and the `BasketCheckedOut` constructor (the event is basket's most contract-like type — registered, serialized, mirrored by the integration event — and a defaulting *policy* changes on a different clock than the record's shape). The criterion behind all three calls is now written up in [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md#️-controller-handler-aggregate--who-owns-what).

  Seam is unchanged either way: `CheckoutBasketHandlerTest` asserts on `repository.domainEvents()`, which is pinned to the outcome rather than to where the logic sits
- [x] Red first: absent shipping address → the resulting `BasketCheckedOut` carries billing as the shipping address. Written and confirmed failing for the right reason (`expected: Address[...] but was: null`); the present-shipping case uses a differing address so the two cannot pass for the same reason
- No refactor step. The two handler tests share cart setup and assertions, but extracting either costs more than it saves at two cases — a two-line setup becomes a four-line helper that would need parameterizing for the next variation, and an extracted assertion states the second test's point less loudly than the inline one. Revisit only if a third case with the same shape appears.

### 3. Ordering: build a complete order
*Extends existing behavior — test alongside.*

- [x] `BasketCheckoutIntegrationEventListener` — maps the basket DTOs into ordering's `Address` / `Payment` and passes them into `CreateOrderCommand`. No new handler or slice. *Beyond plan: `CreateOrderCommand` and `CreateOrderController` were reordered to the same mandatory-first order as basket, removing the cross-module divergence*
- [x] `FakeOrderRepository.clear()` — added and called from `@BeforeEach`, closing the unfinished item carried over from the previous plan
- [x] `BasketCheckoutIntegrationEventListenerTest` — the four null assertions are now assertions on real values, with billing and shipping made distinct via `AddressMother.address("Midgard")`
- [x] Listener test renamed off "pending order" — `…thenOrderIsCreated`, `@DisplayName` updated
- [x] Listener's three `toDomain` helpers are consistently `private`. *Minor left: the test's own `toDto` helpers are still split — `private AddressDTO toDto(Address)` beside `public static PaymentDTO toDto(Payment)`*

**Step 3 verified:** `mvn test -pl modules/ordering` — 23 tests, BUILD SUCCESS.

### 4. Ordering: invariants and the manual path
*New deterministic logic — strict red → green → refactor. Tested at the `CreateOrderHandler` seam rather than in an isolated domain test; see the reasoning on the box below.*

- [x] `Order.create` rejects null shipping address, billing address and payment, with an error message naming the missing field (per the error-message rule in [rules-of-engagement.md](../../processes/rules-of-engagement.md)). Decide the exception type — `IllegalArgumentException` is the low-ceremony choice; there is no invariant-violation precedent in the codebase, only not-found types
- [x] **Tested at `CreateOrderHandlerTest`, not in an isolated `OrderTest`** — revised while implementing. The "narrow the seam for an invariant that must hold regardless of caller" bullet in [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) looks like it applies, but both transports (the HTTP controller and `BasketCheckoutIntegrationEventListener`) funnel through `CreateOrderHandler`, so the handler is the genuine common choke point — nothing reaches `Order.create` without passing it. Three cases, one per missing field, each asserting the **message names the field**, not only the exception type; without that, all three pass on one generic message. The code still lives in `Order.create` per [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md#️-controller-handler-aggregate--who-owns-what): the architecture rule decides where the check goes, the seam decides where it is tested. Accepted residual risk: `Order.create` stays publicly callable, so a future path bypassing the handler would be uncovered
- [x] `Order.reconstitute` reordered to match `create`, along with `DBMapper.toDomain(OrderDB)` and `OrderMother.build()` — all three agree
- [x] `OrderMother`'s billing and shipping addresses are now distinct, via new intent-named `AddressMother.billing()` / `AddressMother.shipping()` factories. This makes `MongoOrderQueryRepositoryTest` able to fail on a swap — it compares two independent mapping paths (`domain → toDb → toDto` versus `DTOMapper.toDto`). It does not help `MongoOrderRepositoryTest`, whose pure round-trip cancels out a symmetric swap in `toDb`/`toDomain` regardless of the values
- [x] `CreateOrderRequest.shippingAddress` gets `@NotNull` + an `ordering.order.shippingAddress.notNull` message key — this is the pre-existing `POST /order` 500, independent of checkout. Add the `CreateOrderControllerTest` case
- [x] Leave `DBMapper` / `DTOMapper` strict. With the invariants above, a null address is a bug and should fail loudly; a mapper translates, it does not validate

### 5. Stop logging event payloads
*One-line change — no seam worth a test.*

- [x] **Removed the log line from `InMemoryMessageBus.publish`** (logger field and slf4j imports with it). It logs the full message at INFO, which after step 1 means a card number and CVV in plaintext. Removal rather than reformatting: the bus takes an `Object` and so has no business describing a payload whose shape it does not own, it sits between a dispatcher and a listener that both know more, and you cannot leak what you do not log. If the observability is wanted, the line belongs in `BasketOutboxDispatcher.dispatch()`, which already has `eventType()` and `eventId()`

---

## Verification

- [x] `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green
- [x] **Outbox round-trip test for `BasketCheckedOut`.** Its JSON payload schema changes in step 1, and nothing covers that seam today — `BasketOutboxDispatcherTest` uses `FakeOutboxEventStore`, and `MongoOutboxEventStoreTest` doesn't use this event. Add a test in `basket-core/src/test/.../infrastructure/messaging/` wiring a real `OutboxDocumentMapper` (real `ObjectMapper` + a `DomainEventRegistry` with `BasketCheckedOut` registered) and asserting a serialize → deserialize round-trip preserves address and payment
- [x] **Cleared the poison documents first**: `db.outbox.deleteMany({})` against `eshop-basket-db`. Old documents carry the pre-change `BasketCheckedOut` shape and will deserialize with null address/payment, hitting the new step-4 invariants and re-poisoning the dispatcher
- [x] **The smoke check deferred from the previous plan — passed.** First end-to-end run this flow has ever had. A complete order is created from a checkout, with address and payment populated. Second run with `shippingAddress` omitted produces an order whose shipping address equals billing, proving the step 2 rule across all five hops including the outbox JSON round-trip. *Note: item prices are add-time (`priceWhenAdded`), so a catalog price change between add and checkout is not reflected — by design, pending the pricing-basis decision in Follow-up A of the previous plan*
- [x] Confirmed the outbox document has `processedOn` set and the scheduler log is not repeating an error every 5 seconds — this, not an HTTP status, is what proves the regression is gone
- [x] `bruno/Basket/Checkout Basket.bru` now sends a full body, with billing and shipping deliberately different so the smoke test can reveal a swap. Omit `shippingAddress` for a second run to exercise the step 2 default
- [x] Checked [README.md](../../../README.md) per the README-maintenance rule. It currently doesn't mention orders or checkout at all, so this is a genuine question rather than a formality
- [x] `docs/KNOWN_ISSUES.md`: no tracked gap closes (gap 1 `ShoppingCart.checkout`, gap 2 `UpdateProductHandler`, gap 3 `BasketCheckedOutHandler`/`ProductCreatedHandler`, gap 4 `CreateOrderErrorHandler` all stay open). **Add** the dispatcher error-isolation defect below

*Not needed, contrary to the earlier draft: an extra `MongoOrderRepositoryTest` case "covering whatever shape orders now actually take". After step 4 the domain cannot produce a partial order, so production shape and `OrderMother` shape converge — there is no new shape to cover.*

---

## Explicitly deferred (recorded, not fixed here)

- **`BasketOutboxDispatcher` has no per-event error isolation.** One failing event is retried forever and blocks every later event in the basket outbox. Surfaced by this work, a real defect, and its own increment — the fix needs a retry/dead-letter decision, not a try/catch.
- **Empty item list.** `Order.create` rejecting it needs a matching `CheckoutBasketHandler` guard (empty basket → 400) or an empty checkout just becomes the next poison event. Two rules, a domain exception and a controller error path — its own small increment.
- **Consumer-side idempotency.** `BasketCheckoutIntegrationEvent` carries an `eventId` but nothing dedups on it; the outbox is at-least-once by construction, so a redelivery creates a duplicate order.
- **Card data at rest.** After this change the PAN and CVV sit as plaintext JSON in the basket `outbox` collection, and already sit in the orders collection via `POST /order`. Accepted property of a study project; step 5 fixes only the part this change introduces. Real systems never store CVV — its removal is its own increment.
- **Nested `@Valid` on address/payment contents.** Neither `CreateOrderRequest` nor the new `CheckoutBasketRequest` validates the fields *inside* an address. Kept at parity deliberately.
- **`CreateOrderErrorHandler` is empty** (`KNOWN_ISSUES.md` gap 4), so a step-4 invariant violation surfaces as an unhandled 500 on `POST /order`. Validation catches it at the controller first, so the invariant is a backstop only.
- **Pricing and `Order.totalPrice`** — Follow-up A in [basket-checkout-creates-order](../basket-checkout-creates-order/basket-checkout-creates-order.md). The pricing basis (add-time vs checkout-time) must be settled first, and `ShoppingCart.checkout`'s three known defects fixed with it.
