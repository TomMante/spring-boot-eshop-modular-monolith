# Order Name To Placed At

Implements follow-up B of [basket-checkout-creates-order](../basket-checkout-creates-order/basket-checkout-creates-order.md): remove `Order.name` and give orders a real timestamp to sort and display by.

**Status: complete.** Full suite green (57 tests). The single open item is the manual smoke check, which stays blocked on [basket-checkout-captures-address-and-payment](../basket-checkout-captures-address-and-payment/basket-checkout-captures-address-and-payment.md) for reasons unrelated to this work. Two things surfaced here and deliberately left for later: the missing sort tie-breaker (Gotchas) and `MongoAuditMetadata` (below).

## Context

`name` was carried through every layer (`Order`, `OrderDB`, `OrderDTO`, `CreateOrderCommand`, `CreateOrderRequest`, both mappers) with a `@NotNull` and a message key, but nothing read it for a decision. It appears to be inherited from the eShop reference implementation's `OrderName` value object rather than from any requirement here — nobody names an order when it already has an identifier.

Its one real consumer was `GetOrdersHandler`, which used it as the default sort key.

### Why this is not just a deletion

`name` is the **only** sort key in the ordering read path. Deleting it orphans three references:

- `getorders/GetOrdersHandler.java` — `new SortOrder("name", ASC)` (production)
- `MongoOrderQueryRepositoryTest` — two occurrences, used to make pagination deterministic

`SortOrder` takes a `String` property, so none of these fail to compile. Mongo does not error on a sort over a missing field either — every document compares equal, so page boundaries become arbitrary. The failure mode is a silently non-deterministic read model plus a flaky pagination test, not a red build. **Verified: `mvn test-compile` passes with all three references dangling.**

So the timestamp is not scope creep bolted onto a deletion; it is what makes the deletion safe.

### Decisions

**1. `placedAt` is a domain field on `Order`, not audit metadata on `OrderDB`.**

The distinction that matters is *audit metadata* ("when was this row written") versus *domain temporal fact* ("when did the business thing happen"). Two tests separate them:

- **Migration test** — if we moved database tomorrow, must the value survive unchanged? For `Product.createdAt`, no; regenerating it as "migration date" loses nothing. For an order's placement date, yes; regenerating it is data corruption. *A value that must survive a change of persistence technology is not a persistence concern.*
- **Ubiquitous language test** — "order date" / "placed on" is core commerce vocabulary; it goes on invoices, in "Your Orders", in return windows. "Product created at" is not something a domain expert says.

An `Order` is also **event-shaped**: unlike a `Product` (a thing that exists continuously and gets edited), it is a record of something that happened at a moment, and that moment is constitutive of it. This already matters here — order creation is event-driven, so the checkout instant and the document-write instant are already two different times, and deferred/retried delivery will widen the gap.

Note "no domain logic uses it yet" is a statement about this increment, not about ownership: `Order.customer` drives no invariant today either.

**2. Named `placedAt`, not `createdAt`.** `createdAt` reads as bookkeeping, which is most of what made the ownership question feel ambiguous. `placedAt` names the business fact.

**3. The application layer supplies the instant; the domain receives it.** `Order.create(customer, placedAt, ...)`. Putting `Instant.now()` inside the aggregate would be a non-deterministic system call in framework-agnostic domain code. Passing it in keeps `Order` pure and fully deterministic in tests, and models the fact anyway.

**4. An injected `Clock`, not `Instant.now()` in the handler.** `java.time.Clock` is a JDK abstract class with ready-made implementations, so tests pin an exact instant with `Clock.fixed(...)` and need no mocking framework — consistent with this project's fakes-over-mocks convention. `Instant.now()` in the handler was considered and would also keep the domain pure, but the strongest honest assertion it allows is `isNotNull()` or a time window. Both entry points (`POST /order` and the checkout listener) funnel through `CreateOrderHandler`, so the clock lives in exactly one place.

**5. Sort becomes `placedAt` DESC.** Sorting orders alphabetically was never the intent; `name` was standing in for a timestamp. This also resolves the note carried in the previous plan that checkout-created orders (`name = null`) would cluster at the top of every page.

**6. `placedAt` is server-generated, so it is not on `CreateOrderRequest`.** The `ordering.order.name.notBlank` message key and the "name is missing" validation test disappear with no replacement.

### Separate increment, not this one: `MongoAuditMetadata`

A Mongo counterpart to `shared.infrastructure.JpaAuditMetadata` is wanted as a blueprint exemplar, and is genuinely separable — `Order.placedAt` (domain fact) and `OrderDB` audit metadata (infra bookkeeping) can coexist, and showing both side by side is instructive. It is deferred to its own increment because it needs real design attention, not a bolt-on:

- **`isNew` is id-based.** `save()` decides INSERT vs UPDATE via `EntityInformation.isNew`: new ⟺ non-primitive `@Version` is null, else new ⟺ `@Id` is null. Every document here assigns its id up front, so Spring Data always concludes "not new". Mongo's `IsNewAwareAuditingHandler` then calls `markModified` rather than `markCreated`, and **`@CreatedDate` is never written**. The fix is a `@Version Long` field on the document (which also buys optimistic locking).
- JPA does not have this problem: `AuditingEntityListener` hooks `@PrePersist`, and `merge()` on a missing row still issues an INSERT, so the callback fires regardless of `isNew`. That is why `ProductDB` works today and why the pattern does not transfer.
- **`save` is a full document replace and the mappers rebuild from scratch.** `DBMapper.toDb(order)` constructs a fresh document every time, so on any *update* a stored `createdAt` is overwritten with null. Orders are insert-only today (create/delete/read, no update feature), so the trap is dormant on `OrderDB` — and would fire silently the day an order-update feature lands.
- Whether auditing populates an `@Unwrapped` nested record at all, and whether Spring Data re-instantiates a Java record to inject the values, both need proving with the Testcontainers tests rather than assuming.
- **That increment does not inherit testable time from the `orderClock` bean.** Spring Data auditing gets its timestamps from a `DateTimeProvider` (`@EnableMongoAuditing(dateTimeProviderRef = "…")`, defaulting to `CurrentDateTimeProvider`), which is a separate seam. It needs its own determinism story.

### Work already done before this plan was written

`name` has already been stripped from 16 files in the working tree — `Order`, `OrderDB`, `OrderDTO`, `DBMapper`, `DTOMapper`, the whole `createorder` slice, `messages.properties`, `OrderMother`, and the affected tests. That work is complete, coherent, and compiles. This plan covers what remains.

---

## Todo List

### 1. `Order.placedAt`
*Mechanical — no logic, only a signature change. Tests updated alongside, no red-first ceremony.*

- [x] `domain/Order.java` — add `private Instant placedAt`, add the parameter to the private constructor, `create(...)` and `reconstitute(...)`, add the `placedAt()` accessor
- [x] Parameter position: directly after `customer` in both factories, matching where `name` sat

### 2. Clock seam + `CreateOrderHandler`
*New behaviour with a value worth pinning — strict red → green → refactor.*

- [x] **Red:** `CreateOrderHandlerTest` — construct the handler with `Clock.fixed(PLACED_AT, ZoneOffset.UTC)` and assert the saved order's `placedAt` equals `PLACED_AT` exactly
- [x] **Green:** `OrderingModuleConfig` — add `@Bean("orderClock")`; `CreateOrderHandler` — take `Clock` in the constructor (`@Qualifier("orderClock")`) and pass `clock.instant()` into `Order.create`
- [x] Name it `orderClock`, **not** `clock`. A modular monolith is one application context, so an unqualified `clock` claims that name application-wide and a second module declaring its own would fail startup (bean-definition overriding is disabled by default). The prefix matches the beans already beside it — `orderMongoTemplate`, `orderTransactionManager`, `orderMongoDatabaseFactory`
- [x] Use `Clock.tickMillis(ZoneOffset.UTC)` for the production bean, **not** `Clock.systemUTC()` — see Gotchas
- [x] Spring Boot auto-configures **no** `java.time.Clock` bean, so this is the only definition. Beware the IDE auto-importing `io.micrometer.core.instrument.Clock`, which *is* auto-configured whenever Actuator is on the classpath
- [x] `BasketCheckoutIntegrationEventListenerTest.Config` — the handler bean now needs a `Clock`; register a fixed one so the listener test can assert `placedAt` too

### 3. Persistence and contract
*Mechanical field propagation — tests alongside.*

- [x] `mongo/model/OrderDB.java` — add `Instant placedAt`
- [x] `contract/OrderDTO.java` — add `Instant placedAt`
- [x] `mongo/model/DBMapper.java` — carry it through `toDb(Order)`, `toDomain(OrderDB)`, `toDto(OrderDB)`
- [x] `contract/DTOMapper.java` — carry it through `toDto(Order)`
- [x] No `GetOrderByIdResponse` / `GetOrdersResponse` change needed — both wrap `OrderDTO`

### 4. `OrderMother`
*Test infrastructure.*

- [x] Builder gains `private Instant placedAt` defaulting to a **fixed whole-second** instant, never `Instant.now()`
- [x] Add `withPlacedAt(Instant)` — the pagination test needs orders that are distinguishable by sort key, which is what `withName` used to provide

### 5. Repoint the sort
*Behaviour change with a real assertion available — red → green.*

- [x] **Red:** `MongoOrderQueryRepositoryTest` — give the three orders distinct `placedAt` values, sort on `placedAt` DESC, and assert page contents **in order**
- [x] Switch both pagination assertions from `containsExactlyInAnyOrderElementsOf` to an order-sensitive matcher — the current form never verified sorting at all, which is part of why the orphaned key would have gone unnoticed
- [x] **Green:** `getorders/GetOrdersHandler.java` — `new SortOrder("placedAt", SortOrder.Direction.DESC)`
- [x] Confirm no `SortOrder("name", ...)` references remain in the ordering module

### 6. Pin the handler's sort choice
*New deterministic logic in the fake, then a real outcome assertion — red → green → refactor.*

- [x] **Red:** `GetOrdersHandlerTest` — seed the fake with orders in a non-sorted insertion order and assert the handler returns them newest-first
- [x] **Green:** `FakeOrderQueryRepository.paginatedOrders` — honour `pageRequest.sortOrders()`, sorting by the named property before paginating. Closes the standing `LinkedHashMap` TODO
- [x] Throw with a message naming the unsupported property and listing the supported ones, rather than silently ignoring an unknown sort key — silently ignoring it is the same failure mode this whole increment exists to fix

### 7. Bruno collection
*Static content — no test.*

- [x] `bruno/Ordering/Create Order.bru` — remove `"name"`
- [x] Same file — `"customerId"` is stale from the earlier `customerId` → `customer` rename and is wrong on `main` today; fix it in the same edit

### 8. Verification

- [x] `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green
- [x] Confirm `Instant` serialises as ISO-8601 in the controller tests' JSON, not as an epoch number
- [x] Check [README.md](../../../README.md) per the README-maintenance rule — expected to need no change (it does not mention orders at all), but verify rather than assume
- [x] Update follow-up B in [basket-checkout-creates-order](../basket-checkout-creates-order/basket-checkout-creates-order.md) to point here and record that `createdAt` became `placedAt`
- [ ] Manual smoke check is **still deferred** — `POST /basket/checkout` continues to 500 on the null-hostile mappers until [basket-checkout-captures-address-and-payment](../basket-checkout-captures-address-and-payment/basket-checkout-captures-address-and-payment.md) lands. Unchanged by this increment

---

## Gotchas

**Mongo stores `Instant` as a BSON date — millisecond precision.** `Clock.systemUTC()` produces microsecond-or-finer instants, so a saved order and its read-back copy would differ in a field-by-field comparison, and `MongoOrderRepositoryTest` compares recursively. Using `Clock.tickMillis(ZoneOffset.UTC)` for the production bean makes the domain's precision match the store's, removing the whole class of problem rather than papering over it at the assertion. Fixed test instants should be whole seconds for the same reason.

**The `placedAt` sort has no tie-breaker — known gap, deliberately not fixed here.** Two orders sharing a `placedAt` have an unspecified relative order, so a row can appear on two pages or none as a client pages through. `Clock.tickMillis` does not cause this and finer precision would not fix it — ties are possible at any resolution, and rarer ties are worse because they surface only under production load. The fix is a secondary key (`placedAt` DESC, `id` ASC) making the ordering total; `PageRequest.sortOrders()` is already a `List` and `MongoOrderQueryRepository.toSpringSortOrder` already maps all of it, so it is a small addition whenever it is picked up. Decided out of scope to keep this increment to follow-up B.

**String-typed sort keys are unchecked.** Nothing — not the compiler, not Mongo — objects to sorting on a property that does not exist. This increment leaves the same hazard in place for the next field rename; a typed sort key is worth considering if it bites again.

**`FakeOrderQueryRepository` ignored `sortOrders` entirely** (a `LinkedHashMap` with a standing TODO), so `GetOrdersHandler`'s choice of key and direction was pinned by nothing — `MongoOrderQueryRepositoryTest` proves the repository honours whatever key it is handed, not that the handler hands it the right one. Step 6 closes this. Asserting instead that the handler *passed* a particular `SortOrder` would test an interaction rather than an outcome, which [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) steers away from.
