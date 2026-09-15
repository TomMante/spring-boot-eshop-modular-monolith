# One Order per Checkout

## Context

[Known Bug 6](../../../docs/KNOWN_ISSUES.md) (ordering): a redelivered checkout event creates a duplicate order.

The outbox is at-least-once by construction. [OutboxEventProcessor.dispatchPendingEvents](../../../SpringBoot-EshopModularMonolith/shared/src/main/java/com/github/eshopmodularmonolith/shared/infrastructure/OutboxEventProcessor.java) publishes an event and only then calls `markAsProcessed`, inside one try/catch. Publication is synchronous (`InMemoryMessageBus` → `ApplicationEventPublisher`), so by the time `markAsProcessed` runs the order is already committed in `eshop-order-db`. If `markAsProcessed` fails or the pod dies in between, the event stays unprocessed and is published again 5 seconds later. The two writes live in different databases, so no transaction can span them — this is structural, not removable; the consumer has to absorb it.

Two ways in, one fix:

- **A single instance** retrying after a failed `markAsProcessed` (the documented case).
- **Two instances** polling concurrently — `fetchUnprocessedEvents` claims nothing, so both can publish the same event.

The consumer then creates a second order unconditionally: [BasketCheckoutIntegrationEventListener](../../../SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/com/github/eshopmodularmonolith/ordering/orders/infrastructure/messaging/BasketCheckoutIntegrationEventListener.java) builds a command, and `Order.create` mints `UUID.randomUUID()`, so nothing collides. `BasketCheckoutIntegrationEvent.eventId` is stable across redeliveries (it is the outbox document's `_id`, copied through by `BasketOutboxDispatcher`, and pinned by `BasketOutboxDispatcherTest`) — but nothing reads it.

The goal is not to stop the event arriving twice — that cannot be prevented — but to stop a second arrival from having an effect.

Also found during planning: `POST /order` ([CreateOrderController](../../../SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/com/github/eshopmodularmonolith/ordering/orders/application/features/createorder/CreateOrderController.java)) predates checkout by six months (first ordering commit, 2026-02-27; checkout listener 2026-08-20). It takes `customer` and per-item `price` from the request body with no `@AuthenticatedUser`, so any authenticated caller can place an order in anyone's name at prices of their choosing, for products that need not exist. In this shop, checkout is the only legitimate way to get an order. Removing it is step 1, because it lets `checkoutId` be required, which simplifies the dedup design.

## Design decisions

Reached through several rounds with the user; recorded here because the rejected shapes are as informative as the chosen one.

- **Dedup is a single atomic write, never check-then-act.** "Check whether an order exists, then create" races: two pods both check, both find nothing, both insert. And a separate processed-messages marker plus the order is two writes with the same atomicity gap one layer down — marker-first risks a silently lost order, order-first leaves the duplicate window open — closeable only with a Mongo multi-document transaction, which needs the replica-set container [Backlog 7](../../../docs/BACKLOG.md) deliberately did not pay for. A unique index on a field of the order document makes the storage engine reject the second insert on its own.
- **The field is `checkoutId` on the `Order` aggregate, filled from `eventId` by the listener.** Considered and rejected along the way:
    - *Deriving the order id from the event id* (a name-based UUID): atomic, but hides the cause in a one-way hash.
    - *A generic `causationId`*: right concept for dedup (correlation id is not — one flow legitimately causes many effects), but it is infrastructure metadata. On the aggregate it contradicts DDD; beside the aggregate (`save(order, causationId)`) it still lands in a domain-package port signature and is lost on `reconstitute`, so the first update path would wipe it; as ambient context (a thread-scoped holder) it fails silently in both directions — lost context disables dedup, leaked context rejects a different checkout's order.
    - *A checkout identity minted by basket*: the most correct shape, but nothing would read it today — `CheckoutBasketHandler` deletes the cart right after checkout and `POST /basket/checkout` returns a bodyless `202` — and it changes `BasketCheckedOut`'s shape, which outbox payloads at rest do not have. Recorded in [DESIGN_NOTES.md](../../../docs/DESIGN_NOTES.md) ("A Checkout Identity Owned by Basket") with the triggers that would justify it.

  "One checkout places at most one order" is a business rule, so `checkoutId` is business language and belongs on the aggregate. The decision "a checkout is identified by its event" is made in exactly one place, the listener; if basket ever mints a real checkout id, only that line changes.
- **`checkoutId` is required.** `Order.create` becomes `Order.placeFromCheckout(UUID checkoutId, ...)` and rejects a null. With `POST /order` gone there is no legitimate order without a checkout, so the index is plain unique — not sparse — and the "Spring Data omits null fields" dependency disappears.
- **`OrderAlreadyExists`** in `ordering.orders.domain.exceptions`, next to `OrderNotFound`. Carries the `checkoutId`; message says the checkout was already placed as an order and must not be placed again.
- **The adapter keeps `save` and translates the duplicate.** `MongoOrderRepository.save` keeps `springRepository.save(...)` and translates `DuplicateKeyException` into `OrderAlreadyExists`; the unique index rejects a second `checkoutId` whether the write is an insert or an upsert. Planned first as `insert`, to stop the port's only write from silently replacing a stored order by `_id`; changed during implementation on 2026-09-15, because the two differ only when the `_id` already exists — which random order ids make possible only through an update path, and none exists — and in that case `insert` is worse: the translation cannot tell an `_id` collision from a `checkoutId` collision, so an ordinary update would fail as "checkout already placed", and a port method named `save` would behave as insert-only. The wholesale replace `save` does is already recorded in [Backlog 3](../../../docs/BACKLOG.md); an update path, when one arrives, needs its own method that `$set`s domain-owned fields.
- **The annotation declares the index; the adapter creates it.** `OrderDB.checkoutId` carries `@Indexed(unique = true, name = "orders_checkoutId_unique")` as the single definition. Auto-index creation stays off, so the annotation creates nothing by itself: both production (`new MongoTemplate(dbFactory)` in `OrderingModuleConfig`) and the adapter test (`new MongoTemplate(client, ...)`) build their templates by hand, each gets a fresh `MongoMappingContext` with `autoIndexCreation = false`, and `spring.data.mongodb.auto-index-creation` never reaches them (verified in the spring-data-mongodb 5.0.2 and Boot 4.0.2 sources). `MongoOrderRepository` gets the `orderMongoTemplate` injected and a package-private `@PostConstruct createIndexes()` that resolves the annotated indexes with `IndexResolver.create(mappingContext).resolveIndexFor(OrderDB.class)` and creates each one. Living in the adapter means the adapter test, which `@Import`s it, runs the same code production does. Considered and rejected on 2026-09-15:
    - *Auto-index through a per-module mapping context* — works, but adds converter boilerplate to every module; Spring Data has defaulted it off since 3.0 and recommends managing indexes explicitly; and it would not help the test, because the index creator runs once per entity type while `OrderingDatabaseTest` drops the database before every test.
    - *Auto-index through one shared mapping context* (e.g. the `MongoMappingContext` bean in starter's `MongoConfiguration`, which no module template uses) — `MongoTemplate`'s constructor sets the context's event publisher through a setter, so the last template constructed wins and its index creator writes to its own database: one module's indexes could land in another module's database.
- **"Already handled" is a success, not a failure.** The listener catches `OrderAlreadyExists`, logs at `info` with the `eventId`, and returns normally. If it let the exception escape, it would propagate through `InMemoryMessageBus` into `OutboxEventProcessor`'s catch, the event would stay unprocessed, and a duplicate-order bug would become an infinite retry loop.
- **The fake enforces the same rule as the adapter.** `FakeOrderRepository.save` throws `OrderAlreadyExists` when an order with the same `checkoutId` is already stored. The adapter test pins the real behavior; the fake only mirrors it.

Sketch of the shapes (guidance, not a diff):

```java
// listener — the one place a checkout is identified by its event
var cmd = new CreateOrderCommand(event.eventId(), event.userName(), ...);
try {
    createOrderHandler.handle(cmd);
    log.info("Placed order for checkout {} (user {}).", event.eventId(), event.userName());
} catch (OrderAlreadyExists alreadyPlaced) {
    log.info("Checkout {} was already placed as an order; skipping redelivery.", event.eventId());
}
```

```java
// OrderDB — the single definition of the index
@Indexed(unique = true, name = "orders_checkoutId_unique") UUID checkoutId,
```

```java
// MongoOrderRepository
@PostConstruct
void createIndexes() {
    var indexOps = mongoTemplate.indexOps(OrderDB.class);
    IndexResolver.create(mongoTemplate.getConverter().getMappingContext())
            .resolveIndexFor(OrderDB.class)
            .forEach(indexOps::createIndex);
}

@Override
public void save(Order order) {
    try {
        springRepository.save(DBMapper.toDb(order));
    } catch (DuplicateKeyException duplicate) {
        throw new OrderAlreadyExists(order.checkoutId(), duplicate);
    }
}
```

## Testing rigor

- **Step 1 (remove `POST /order`)**: no red-first — a deletion. The suite compiling and staying green without the files is the check.
- **Step 2 (`checkoutId` required)**: strict red → green for the null guard only, at `CreateOrderHandlerTest`, where `Order`'s other guards are already tested. The rename and plumbing are tests-alongside — mechanical.
- **Step 3 (Mongo enforces uniqueness)**: strict red → green in `MongoOrderRepositoryTest`. Red is real: `save` upserts and no index exists, so a second order with the same `checkoutId` is stored silently. This is the step the whole design rests on, and the fake can only imitate it.
- **Step 4 (listener skips redelivery)**: strict red → green in `BasketCheckoutIntegrationEventListenerTest`. Red is real: once the fake enforces uniqueness, the second publish throws out of `publishEvent`.

## Todo List

### 1. Remove `POST /order`
- [x] Delete `CreateOrderController`, `CreateOrderRequest`, `CreateOrderErrorHandler` and `CreateOrderControllerTest`. `CreateOrderHandler` and `CreateOrderCommand` stay — the listener uses them.
- [x] Delete ordering's `src/main/resources/messages.properties`: its five `ordering.order.*` keys are referenced only by `CreateOrderRequest`. Caution: basket-core and catalog-core each also ship a `messages.properties` at the classpath root, and a plain resource-bundle lookup resolves only the first one found. Unverified whether all three resolve in the assembled app today — check a basket and a catalog validation error message before and after this deletion, so a change in which file wins is noticed rather than shipped. *(2026-09-15 review: first emptied rather than deleted — an empty file still takes part in the lookup, and resolves nothing if it is found first — then deleted on a second pass. Checked the same day by reproducing the lookup instead of the before/after message check: the files are not merged, only the first on the classpath is used, and in the starter's order catalog-core's wins, so basket's messages already do not resolve. Recorded as Known Bug 11. Deleting ordering's file does not change which file wins in that order.)*
- [x] Delete `bruno/Ordering/Create Order.bru`.
- [x] Full suite green (confirmed by the user, again after the second pass).

### 2. An order requires a checkout
- [x] `CreateOrderCommand` gains `UUID checkoutId` as its first component; the listener passes `event.eventId()`.
- [x] `Order`: `checkoutId` field and accessor; `create` renamed to `placeFromCheckout(UUID checkoutId, String customer, Instant placedAt, ...)`; `reconstitute` gains `checkoutId`. `CreateOrderHandler` passes it through.
- [x] `OrderMother.Builder` gets a `checkoutId` defaulting to `UUID.randomUUID()` (distinct per order, so the uniqueness rule added in steps 3–4 does not break the existing arrange steps) and a `withCheckoutId(UUID)`. *(2026-09-15: random default done; `withCheckoutId` moved into step 3, where it is first needed.)*
- [x] Existing `CreateOrderHandlerTest` cases get a checkout id; the happy path also asserts `order.checkoutId()` equals the command's. *(2026-09-15 review: the happy-path assertion was missing on the first pass, added on the second.)*
- [x] Red: new `CreateOrderHandlerTest` case — a command with a null `checkoutId` is rejected with `IllegalArgumentException`. Watch it fail (accepted silently), then add the guard to `placeFromCheckout` with a message stating an order must be placed from a checkout. *(Red-first confirmed by the user.)*
- [x] `BasketCheckoutIntegrationEventListenerTest` happy path also asserts `order.checkoutId()` equals `event.eventId()` — this pins the one place the equivalence is decided. *(2026-09-15 review: missing on the first pass, added on the second.)*
- [x] Full suite green (confirmed by the user, after the second pass).

### 3. Mongo rejects a second order for the same checkout
- [x] `OrderMother.Builder.withCheckoutId(UUID)` — carried over from step 2.
- [x] `OrderDB` gains `checkoutId`; `DBMapper.toDb`/`toDomain` carry it both ways. The existing round-trip test (`usingRecursiveComparison`) catches a mapper that drops it, now that `OrderMother` sets one. *(Done early, alongside step 2. `@Indexed(unique = true)` was also added to `OrderDB`; with auto-index creation off it created nothing until the green step below.)*
- [x] Add `OrderAlreadyExists` in `ordering.orders.domain.exceptions`.
- [x] `MongoOrderRepositoryTest`: add a `@BeforeEach` that calls `repository.createIndexes()` — `OrderingDatabaseTest.setUp` drops the whole database, indexes included, before each test, and a parent class's `@BeforeEach` runs before the subclass's. *(2026-09-15: implemented by overriding `setUp()` and calling `super.setUp()` before `repository.createIndexes()` — same order, made explicit. An override that dropped the `super` call would silently stop the drop.)*
- [x] Red: saving a second order with the same `checkoutId` (different order id) throws `OrderAlreadyExists`, and exactly one document is stored. *(The test also asserts the exception carries the `checkoutId`.)*
- [x] Green: name the index on the annotation (`@Indexed(unique = true, name = "orders_checkoutId_unique")`). `MongoOrderRepository` takes `@Qualifier("orderMongoTemplate") MongoTemplate`, gains `@PostConstruct createIndexes()` that creates every index resolved from `OrderDB`'s annotations, and `save` translates `DuplicateKeyException` into `OrderAlreadyExists`. *(2026-09-15: planned with `insert`; kept `springRepository.save` instead — see the design decision.)*
- [x] Full suite green — including `ApplicationWiringTest`, which proves the new `orderMongoTemplate` qualifier resolves in the assembled context and the index is created at startup. *(Confirmed by the user.)*

### 4. The listener skips a redelivered checkout
- [x] `FakeOrderRepository.save` throws `OrderAlreadyExists` when a stored order already has the same `checkoutId`.
- [x] Red: publish the same `BasketCheckoutIntegrationEvent` twice → the second publish does not throw, and exactly one order exists, with `checkoutId` equal to the event's `eventId`. Fails today because `OrderAlreadyExists` escapes `publishEvent`.
- [x] Add a case with test data that can fail: two events with **different** `eventId`s and the **same** user → two orders. Guards against a dedup keyed on something coarser than the checkout.
- [x] Green: catch `OrderAlreadyExists` in the listener, log at `info` with the `eventId`, return normally. *(2026-09-15: only `OrderAlreadyExists` is caught; the old "Received … for user" log was replaced by a success log inside the `try`. The redelivery log takes the id from the exception's `checkoutId()` — the same value.)*
- [x] Full suite green. *(Confirmed by the user.)*

### 5. Local data and manual pass
- [x] Drop the local `orders` collection in `eshop-order-db` before starting the app. Existing documents have no `checkoutId`, so with two or more of them the unique index creation fails at startup (a missing field indexes as `null`), and `reconstitute` would receive a null. This failure is loud, not silent — but expected, not a bug. *(2026-09-15: not needed — the user has no local order data.)*
- [x] Bruno: check out a basket, then `Get Orders` shows exactly one order for it. *(Confirmed by the user, 2026-09-15.)*

## Verification
- [x] `./mvnw clean verify` — full suite green. *(Full suite green, confirmed by the user after each step; not re-run by the assistant, whose shell forked the tests on the wrong JDK.)*
- [x] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): remove Known Bug 6. Remove Testing Gap 4 (`CreateOrderErrorHandler` is deleted, not implemented). Reword Testing Gap 6: order creation now has one entry point (the listener), and still a single write. *(Also: Known Bug 3's mention of `Order.create` now notes the rename; Known Bug 11's locations drop ordering's deleted `messages.properties`.)*
- [x] [BACKLOG.md](../../../docs/BACKLOG.md): narrow item 2 to `CheckoutBasketRequest` only; drop `Request`/`Controller` from item 1's list of touched types; item 3's note about `MongoOrderRepository.save` replacing the stored document wholesale stays accurate — this plan kept `save` (see the design decision) — so leave it; add to item 5 (`OrderCreated` outbox) that the dedup here covers exactly one write, so a second effect in the listener reopens the atomicity question. *(Also: item 1's parenthetical was inverted — it said the command would need the total if the event's value is **not** trusted; a re-derived total needs no pass-through, so it now says the command and listener change only if the value **is** trusted. Item 2's "why deferred" no longer rests on parity with a request that no longer exists. Item 4 renamed to `Order.placeFromCheckout`. Item 5's note also flags that transactional inserts may surface a concurrent duplicate as a write conflict the listener does not catch.)*
- [x] [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md): the `CreateOrderHandler` row no longer describes a `POST /order` path — reword it around `Order`'s guards; add a row for checkout dedup (enforcement at `MongoOrderRepositoryTest`, skip behavior at the listener).
- [x] [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md): the `placedAt` row names `Order.create` — rename to `Order.placeFromCheckout`.
- [x] [README.md](../../../README.md): checked during planning — no mention of order creation or endpoints; recheck at close-out. *(Rechecked 2026-09-15: still none.)*
- [x] Every *Explicitly deferred* entry routed *(2026-09-15)*:
    - **A checkout identity owned by basket** → [DESIGN_NOTES.md](../../../docs/DESIGN_NOTES.md), "A Checkout Identity Owned by Basket".
    - **Dedup beyond a single write** → [BACKLOG.md](../../../docs/BACKLOG.md) item 5, "When the trigger fires".
    - **`DELETE /order/{id}`** → [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) Known Bug 12. Looking at it showed the two read endpoints have the same gap — no user scoping, and they return card data — so one entry covers all three.
    - **Classpath-root `messages.properties` collision** → confirmed (only the first file resolves) and recorded as Known Bug 11.

## Explicitly deferred
- **A checkout identity owned by basket** — recorded in [DESIGN_NOTES.md](../../../docs/DESIGN_NOTES.md) with its triggers.
- **Dedup beyond a single write** — if the listener ever does a second thing (`OrderCreated` outbox, a second persisted effect), the unique index no longer covers the whole handling and a transaction is needed. Recorded against Backlog item 5 at close-out (see Verification), not as its own entry.
- **`DELETE /order/{id}`** — likely the same early-scaffolding history as `POST /order`, but not examined in this plan. Look at it at close-out and route it: Known Issues if it is wrong (e.g. a customer can delete anyone's order), otherwise drop.
- **Classpath-root `messages.properties` collision** — if the check in step 1 shows only one module's messages resolve, record it in Known Issues; if all three resolve, drop.
