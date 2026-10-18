# Architecture

## Architecture Highlights

- Modules are isolated as if they were microservices-in-waiting
- Each module can own its own database rather than sharing a schema (JPA/Postgres for some, MongoDB for others)
- Modules never share tables. State changes cross a module boundary as Spring ApplicationEvents, sent through an outbox; a module that needs another module's current data calls a query interface in that module's `contract` package (see [Cross-module communication](#-cross-module-communication))
- Domain is fully decoupled from infrastructure via ports/adapters

## Architectural Trade-offs

This project deliberately pushes module isolation further than most production systems would need:
- Each module can own its own database (JPA/Postgres for some, MongoDB for others) rather than sharing a schema
- State changes cross a module boundary only as events through an outbox, never as a call into another module's write side

This buys near-mechanical extraction into microservices later, at the cost of losing cross-module transactions and taking on real distributed-systems problems (e.g. dual writes, eventual consistency) inside what is still a single deployable.

In a real production system, I'd default to a shared-schema modular monolith with enforced module boundaries, and only isolate a module's storage when there's a concrete reason — e.g. a genuinely different data shape (graph/document data that doesn't fit relational), a compliance requirement for data isolation, or clear evidence a module needs independent scaling. Here, I isolated storage across the board on purpose, specifically to explore what that costs.

### Domain event publication
In this repository we can see how the repositories (JPA/Mongo) are in charge of broadcasting the event. This is not the ideal solution, as it breaks the Single Responsibility Principle.
Better solutions: 
1. Leverage the @DomainEvents annotation from Spring boot: this would require the outbox logic to be moved to the domain event lister.
2. Create a new AOP interceptor that deals with it: this would allow to have both the event and outbox to be published in the same place.
3. Explicit per-module wiring of the outbox to avoid cross-module bean conflicts. A future enhancement could be a `@EnableMongoOutbox` (or similar) custom annotation backed by an `ImportBeanDefinitionRegistrar` to automatically register module-prefixed outbox beans, avoiding conflicts by construction. Until several modules of the same type need an outbox, explicit per-module wiring is preferred for clarity.

### Duplication the architecture requires
SonarCloud reports some code as duplicated that is duplicated on purpose. Do not merge these copies:
- **`OutboxEventEntity` and `OutboxEventDocument`**: two adapters over two database engines. Showing that is the point of this project.
- **`Product` and `ProductDB`**: a domain model and its persistence projection. The ports/adapters split requires both.
- **`PaymentMother`/`AddressMother` in basket and in ordering**: they mirror the `Payment` and `Address` value objects, which each module owns separately on purpose. Sharing the test builders would couple the two modules.

Test fakes for a shared port are different: there is only one, published from the test-jar of the module that owns the port (e.g. `outboxcore.fake.FakeOutboxEventStore` from `outbox-core`), because two copies of a fake drift apart.

## 🔀 Cross-module communication

Modules cross their boundaries in two ways, depending on what they need.

| Need | Channel | Example |
|---|---|---|
| Tell another module that something happened | Domain event written to the module's outbox in the same transaction as the change, dispatched later as a Spring ApplicationEvent. Asynchronous and eventually consistent. | Basket's checkout writes `BasketCheckedOut`; Ordering's `BasketCheckoutIntegrationEventListener` places the order. |
| Read another module's current data | Synchronous in-process call to a query interface in the owning module's `contract` package, which returns contract DTOs. Read-only. | Basket's `AddItemIntoBasketHandler` and `CheckoutBasketHandler` look up a product's name and current price through catalog's `ProductQueryService`. |

The read path follows the same ports-and-adapters split as persistence. Basket's domain declares the port it needs (`ProductService`, returning basket's own `ProductInfo`); `CatalogProductACL` in basket's infrastructure is the adapter, and translates catalog's `ProductDTO` into basket's type. That keeps catalog's contract out of basket's domain and application layers, and makes the adapter the only class that changes if the lookup changes.

**Why a synchronous read here.** Basket needs the price *now*: add-to-basket records it, and checkout totals at the current catalog price. A local copy of the catalog, fed by catalog's events, would give basket a price that may be stale, and would need a projection, a backfill and a catch-up story to keep it correct. A direct query gives the current value for none of that.

**What it costs.** Basket depends on catalog at runtime: in-process today, so the call cannot fail independently, but after extraction into separate services it becomes a network call. At that point basket's availability and latency would depend on catalog's, and the choice would be revisited — an HTTP client behind the same `ProductService` port, or a local read model fed by catalog's events.

**Rules.**
- Only query interfaces go in a `contract` package. A module never changes another module's state by calling it; that goes through an event.
- The caller reaches the query through its own domain port and an adapter in its infrastructure, never by injecting the other module's interface into a handler.

`ArchitectureTest` enforces that a module reaches another module only through its `contract` package. It does not check that what it calls there is read-only; that rule is upheld by review.

## 💬 Validation messages — one bundle per module

Each module owns the texts for its own validation messages (`@NotNull(message = "{basket.checkout.payment.notNull}")`), but the assembled application has exactly one `MessageSource`. Spring does not merge bundles that share a name: with several `messages.properties` on the classpath, only the first is read and every other module's keys come back to the caller as the raw `{…}` placeholder.

**Rule:**
> Every module ships its own uniquely named bundle, and `starter` lists them all.

When a module adds validation messages:

1. Name the bundle after the module, at its resource root: `<module>-messages.properties` (e.g. `basket-messages.properties`). Never `messages.properties`.
2. Add it to `spring.messages.basename` in `starter/src/main/resources/application.yaml`, as a **comma-separated string** — `basename: basket-messages, catalog-messages`. Do not write it as a YAML list: Boot's check that decides whether to create the `MessageSource` at all reads the property as one string, doesn't see a list, falls back to `messages`, finds nothing and creates no `MessageSource` — so no module's messages resolve, silently.
3. Give the module's own tests the bundle name in `src/test/resources/application.yaml` (`spring.messages.basename: <module>-messages`). Slice tests such as `@WebMvcTest` don't see `starter`'s configuration, and without it they fall back to `messages` too.
4. Add one of the module's requests to `ApplicationWiringTest`'s message-resolution test. Only the assembled context can see a missing or colliding bundle; module tests, each with just its own file on the classpath, pass either way.

---

## ⚖️ Controller, handler, aggregate — who owns what

"Domain logic belongs in the aggregate" is only useful once you can decide what
counts as domain logic. The criterion used in this repository:

> **An aggregate owns a decision if it can make it from the state it already holds
> plus its arguments, and the decision must hold regardless of who asked.**
>
> **A handler owns a step if it needs something the aggregate must not reach for** —
> a port, a repository, a clock, a transaction, or coordination across more than one
> thing.

Short version: **handlers supply, aggregates decide.**

The controller is an adapter by role even though it lives under
`application/features/…`: it translates transport into a command and does nothing
else. Defaults, validation beyond bean-validation annotations, and any business
decision belong past it.

| Concern | Owner | Why |
|---|---|---|
| Total price from items × prices | `ShoppingCart.checkout` | Function of the cart's own items |
| Fetching current prices | `CheckoutBasketHandler` | Needs `ProductService` — a port |
| Shipping defaults to billing when absent | `ShoppingCart.checkout` | Pure function of its arguments |
| Loading the cart / `BasketNotFound` | handler | Repository lookup |
| Deleting the basket after checkout | handler | A lifecycle decision *about* the aggregate, not *by* it |
| `placedAt` on an order | handler holds the `Clock`, `Order.placeFromCheckout` receives the `Instant` | An aggregate must not read ambient time |

That last row is the pattern at its purest: the handler owns the *dependency*, the
aggregate owns the *fact*.

**A shortcut that decides most borderline cases:** ask whether you could unit-test the
step with no fake at all. If yes, it is domain logic. If it needs a fake, it is
orchestration. That line falls almost exactly on the boundary — which is the
"fake only at ports" rule from [TESTING_CONVENTIONS.md](TESTING_CONVENTIONS.md) seen
from the other side.

**Do not put policy in a domain event's constructor.** Domain events here are
registered in `DomainEventRegistry`, serialized to JSON, persisted in an outbox and
mirrored by an integration event — they are the most contract-like types a module
owns. A rule such as "shipping defaults to billing" changes on a different clock than
the event's shape, so it belongs in the aggregate method that builds the event, not in
the record itself. (If one ever must go there, it has to be a *compact canonical*
constructor: Jackson deserializes records through the canonical constructor, so a rule
on a convenience overload is silently skipped for every event read back from the
outbox.)

**The failure mode to watch for:** handlers accumulating conditionals that each look
like harmless orchestration and collectively become the domain model — the anemic
domain model, arrived at one ternary at a time.

---

## 📨 Request shapes vs. module contracts

A module's `-contract` artifact and an endpoint's request are both "contracts", but with
different parties, and they change for different reasons.

| Type | Lives in | Party | Example |
|---|---|---|---|
| Module contract | `<module>-contract` | other modules, which compile against it | `BasketCheckoutIntegrationEvent`, its `AddressDTO`/`PaymentDTO` |
| Request shape | `<module>-core/…/application/features/<feature>/` | an HTTP client, which only ever sees JSON | `CheckoutBasketRequest`, `CheckoutAddress`, `CheckoutPayment` |

**Rule:**
> An endpoint accepts its own request records, even when their fields coincide with a contract DTO. Bean-validation annotations go on those records, never on `-contract` types.

Why the duplication is not duplicated knowledge:
- **Different audiences.** No module calls another module's endpoint, so a request type in `-contract` would be visible to exactly the modules that must never use it.
- **Different change clocks.** Integration events are persisted in an outbox; changing their shape changes data already at rest (see Known Issues). A request can gain or lose a field without touching that.
- **They already have reasons to diverge.** Checkout must accept card details from the client, while the published event should eventually carry a provider token instead (see Known Issues, raw card data).
- **Annotations would lie on the event.** `@NotBlank` on an event field reads as a guarantee to the consuming module, and nothing on its side enforces it.

The controller maps the request records into domain value objects (`CheckoutAddress.toDomain()`); the domain value object stays the pivot between the request and the published event, so neither knows about the other.

---

## 🧩 Shared module structure

The `shared` module contains reusable building blocks that are used across bounded contexts and vertical slices.  
It is divided into clear conceptual packages that reflect architectural responsibilities.

---

## 📦 shared.domain

**Purpose:**  
Contains **Domain-Driven Design building blocks** that are shared across all bounded contexts and vertical slices.

These classes:
- Represent **pure domain concepts**, not technical concerns
- Provide base types for implementing domain models consistently
- Are **framework-agnostic** (no Spring, JPA, or infrastructure dependencies)
- Help enforce correct domain modeling patterns

This package exists to:
- Standardize how aggregates, entities, and domain events are modeled
- Avoid duplication of core DDD abstractions
- Keep domain logic independent from infrastructure and API layers

**Examples:**
- `AggregateRoot`
- `Entity`
- `DomainEvent`
- `Aggregate` marker interfaces

**Rule:**
> Code in `shared.domain` must not depend on infrastructure or contract code.

It may only depend on:
- The Java standard library
- Other domain-level abstractions

---

## 📦 shared.contract

**Purpose:**  
Defines the **public data contract of the application**: the shapes of data exchanged between the system and the outside world (controllers, clients, queries).

These classes:
- Represent **what the system exposes**, not how it is implemented
- Are **framework-agnostic** (no Spring, JPA, or database dependencies)
- Are safe to use in:
    - Controllers
    - Query handlers
    - API request/response models
    - Application services

**Examples:**
- `ProductDto`
- `GetProductsResponse`
- `PaginationRequest`
- `ApiError`, the error response shape. The Spring code that builds and sends it — `ApiErrors` and `ValidationExceptionHandler` — is infrastructure and lives in `shared.infrastructure.error`.

**Rule:**
> Classes in `shared.contract` must not depend on domain entities or infrastructure concerns.

---

## 📦 shared.contract.pagination

**Purpose:**  
Contains reusable **pagination-related contract models** used across API endpoints and queries.

These classes:
- Describe **how paginated data is represented externally**
- Are independent of persistence and frameworks
- Can be reused by multiple features or vertical slices

**Examples:**
- `PaginatedResult<T>`
- `PaginationRequest`

**Rule:**
> Pagination models are part of the API contract and must remain stable and framework-agnostic.

---

## 📦 shared.infrastructure

**Purpose:**  
Contains **technical and framework-related concerns** that support the application but are not part of the business model or API contract.

These classes:
- Are tied to **specific technologies** (Spring, JPA, Hibernate, database, messaging, etc.)
- Are **not meaningful to the domain**
- Exist to support persistence, auditing, logging, or integrations
- May change if the technical stack changes

**Examples:**
- `AuditMetadata` (createdAt, updatedAt, version, etc.)
- Base JPA entities
- ORM mappings
- Technical adapters
- Framework configuration helpers

**Rule:**
> Code in `shared.infrastructure` must NOT be depended on by domain or contract code.

A module's **application** layer may depend on `shared.infrastructure`, even though it must never depend on its *own* infrastructure. Controllers and their `@RestControllerAdvice` error handlers live under `application/features/…` but are adapters by role (see *Controller, handler, aggregate*), so shared web plumbing such as `ApiErrors` is theirs to use.

The outbox is **not** in `shared`: not every module has one, so it lives in its own modules (next section).

---

## 📮 Outbox modules

The outbox lives under `outbox/`, separate from `shared`, because it's an optional capability: `ordering` has none.

| Module | Package | Contains | Depends on |
|---|---|---|---|
| `outbox-core` | `outboxcore` | The `OutboxEventStore` port with its row type `StoredOutboxEvent` and its read view `DueOutboxEvent`, `OutboxPayloadSerializer`, `OutboxSchemaRegistry` and `OutboxPayloadUpcaster`, `OutboxEventProcessor`, `OutboxRetryPolicy`, `OutboxEventSubscriber`, `OutboxEventPublisher`, and `OutboxCoreConfig` (declares the schema registry and serializer beans and binds `OutboxRetryPolicy` from `eshop.outbox.retry.*`). Test-jar: `outboxcore.fake.FakeOutboxEventStore`, `outboxcore.fake.FakeDomainEvent` and `outboxcore.fake.FakePricedDomainEvent`. | `shared`, Jackson |
| `outbox-jpa` | `outboxjpa` | JPA adapter for the port (used by Catalog) | `outbox-core` |
| `outbox-mongo` | `outboxmongo` | MongoDB adapter for the port (used by Basket) | `outbox-core` |

The adapters depend only on `outbox-core`: they never look inside a payload, so they need neither `shared` nor Jackson.

### Who does what

| Component | Responsibility | Knows about |
|---|---|---|
| **Serializer** (`OutboxPayloadSerializer`) | Translation: `DomainEvent` ↔ `StoredOutboxEvent`. Stamps the type's current schema version on write; on read, runs the payload through the upcasters from the stored version to the current one (see *Payload versioning* below). Fails when a row can't be translated: an unregistered type or a stored version it can't read surfaces as the registry's `IllegalArgumentException`, a payload that doesn't fit its class (or an upcaster that throws) as an `IllegalStateException` with the original exception as the cause. | `DomainEventRegistry`, `OutboxSchemaRegistry`, Jackson. Not storage, not policy. |
| Schema registry (`OutboxSchemaRegistry`) | Versions: each event type's current schema version and the upcasters that lead to it. Registration mistakes fail at startup. | Upcasters. Not Jackson's mapping, not storage. |
| **Store** (`OutboxEventStore`, implemented by the JPA and Mongo adapters) | Persistence: save rows, return the rows that are due, record a failed attempt, mark a row processed or dead-lettered. Owns the attempt count. Never interprets the payload. | Its database. Not event types, not Jackson, not the retry rules. |
| **Processor** (`OutboxEventProcessor`) | Policy: for each due row, read it, publish it, mark it processed. A failure at any of those steps (including reading) is caught per row, so it costs only that row: it is retried after a backoff, or dead-lettered once its attempts are exhausted (see *Retry and dead-letter* below). | Serializer, store, publisher, retry policy, `Clock`. The only place where decisions are made. |
| Retry policy (`OutboxRetryPolicy`) | Arithmetic: when the next attempt is due after the *n*th failure, and whether *n* failures exhaust the event. Bound from configuration. | Nothing else: a pure calculation. |
| Subscriber (`OutboxEventSubscriber`) | Write entry point: domain event → serializer → store, inside the module's transaction (`BEFORE_COMMIT`). | Serializer, store. |
| Publisher (`OutboxEventPublisher`) | Exit point: what to do with each event read back. Each module's dispatcher implements it by translating to an integration event and handing it to `MessageBus`. | Implemented per module. |

**Why the port speaks rows (`StoredOutboxEvent`), not `DomainEvent`s.** An adapter could return domain events and still isolate failures by catching per row. But then each adapter would decide what happens to a row it can't read, and that policy would be written twice, once per database, and drift the way three copies of the test fake once did. With rows on the port, the store only persists, translation sits once in the serializer, and policy sits once in the processor. Each changes for one reason: a new database touches only a store, a payload schema change only the owning module's registration and upcasters, a retry rule only the processor. It also keeps the adapter tests about persistence alone (any type and payload string will do), and lets every policy case run against the fake store without containers. The port does speak a storage shape (`payload` as a `String`); that is deliberate, since the outbox *is* the store of serialized events. Domain events still enter and leave the outbox as `DomainEvent`.

**What the row carries is what the serializer writes; delivery state stays in storage.** `StoredOutboxEvent` is identity, type, schema version and payload, written once, and it is all `save` accepts. `fetchDue` returns `DueOutboxEvent(event, failedAttempts)`, a read view rather than a persisted type: the attempt count is the only delivery state the processor needs. `nextAttemptAt`, `lastError`, `deadLetteredOn` and `processedOn` are storage-only columns of the one entity or document per row, written by the adapters and checked in their tests by reading the entity or document directly. So `save` cannot receive delivery state, the serializer never invents any, and state the store owns grows in read views without touching the row.

### Retry and dead-letter

A row that fails (unreadable, publish throws, or marking it processed throws) is not retried on the next poll. The processor asks `OutboxRetryPolicy` when it is next due, with exponential backoff, and dead-letters it once it has failed `max-attempts` times:

| Property | Default | Meaning |
|---|---|---|
| `eshop.outbox.retry.max-attempts` | `8` | The failure that reaches this count dead-letters the row. |
| `eshop.outbox.retry.initial-delay` | `5s` | Delay after the first failure; it doubles after each further one. |
| `eshop.outbox.retry.max-delay` | `5m` | Cap on the delay. |

With the defaults the delays are 5 s, 10 s, 20 s, 40 s, 80 s, 160 s and 300 s, so a row is dead-lettered on its 8th failure, about 10 minutes after the first. That is long enough to ride out a consumer's database being briefly down, and short enough that a payload that can never be read stops retrying. The defaults live once, in `@DefaultValue` on the record; invalid values (`max-attempts` below 1, a non-positive delay, an initial delay longer than the cap) fail at startup with a message naming the property.

- **Each retry logs a WARN** with the attempt number and the next attempt time. **Dead-lettering logs an ERROR**, and the row keeps its last error in storage. A dead-lettered row is never fetched again until it is requeued by hand (below).
- **Time comes from the application's single `Clock`**, declared in `starter` and injected into both dispatchers (and `ordering`'s `CreateOrderHandler`). The adapters never call `Instant.now()`; every timestamp they write is one the processor passes in. Tests fix the clock instead of sleeping.
- **Both adapters update only the fields they name, and count in the database.** Mongo uses `updateFirst` with `$inc`/`$set`; JPA uses `@Modifying` update queries (`set e.failedAttempts = e.failedAttempts + 1, …`), each `@Transactional` because nothing around them opens a transaction. Neither reads the row first, so a concurrent write to another field is not overwritten and no increment is lost. With one scheduler per module that is cheap insurance rather than a present need. An update that matches no row fails with `IllegalStateException("Unknown outbox event: …")` in both.
- **A failure while recording the failure is not caught.** If the store itself is down, the exception ends that dispatch run; the row is unchanged and the next poll tries again.

**Manual replay.** To requeue a dead-lettered row, clear its dead-letter time, reset its count, and make it due. Its `lastError` is kept until the next failure overwrites it.

Basket (MongoDB, database `eshop-basket-db`, collection `outbox`):
```javascript
db.outbox.updateOne(
  { _id: UUID("<event id>") },
  { $set: { failedAttempts: 0, nextAttemptAt: new Date() }, $unset: { deadLetteredOn: "" } }
)
```

Catalog (Postgres, table `catalog.outbox_events`). Setting `next_attempt_at` to `occurred_on` makes the row due without any time-zone conversion:
```sql
update catalog.outbox_events
set dead_lettered_on = null, failed_attempts = 0, next_attempt_at = occurred_on
where event_id = '<event id>';
```

- **A module gets the outbox by depending on one adapter.** `basket-core` and `catalog-core` declare only `outbox-mongo` / `outbox-jpa` and reach `outbox-core` transitively, even though their configs and dispatchers use its classes directly. That's deliberate: choosing an adapter is all it takes. Only `outbox-core`'s test-jar has to be declared, because Maven never passes on test-scoped dependencies.
- **`outbox/pom.xml` is an aggregator only.** It lists the three modules; each of them keeps the root pom as its `<parent>`, because no configuration is shared at group level.
- **`OutboxEventPublisher` isn't an integration-event publisher.** It receives a *domain* event; translating it into an integration event and handing it to `MessageBus` is the dispatcher's job. If it throws, the event stays pending and is retried after a backoff, until it is dead-lettered.
- **`DomainEventRegistry` stays in `shared.domain`.** The outbox uses it to deserialize payloads, but it registers domain events, which isn't an outbox concern.

### Payload versioning

A domain event's JSON is stored in the outbox and read later, possibly by a newer version of the application. So a change to an event's shape is a change to data already at rest. Every row carries the `schemaVersion` it was written with, and the compatibility policy is:

> Every change to a persisted domain event's JSON shape bumps its schema version, registers an upcaster from the previous version, and adds a fixture for the new version. Existing fixtures are never edited.

**Registration.** Each module registers the events it owns in one static helper, `BasketOutboxEvents.registerIn(DomainEventRegistry, OutboxSchemaRegistry)` and `CatalogOutboxEvents.registerIn(…)`, called from the module config's `@PostConstruct` and from the module's tests, so tests register exactly what production does. A type starts at version 1, and each later version is one line that states its number:
```java
schemaRegistry.registerEvent("BasketCheckedOut");                                    // version 1
schemaRegistry.registerVersion("BasketCheckedOut", 2, new BasketCheckedOutV1ToV2());  // future
```
`registerVersion` accepts only the current version + 1, and only with an upcaster (`OutboxPayloadUpcaster`: `ObjectNode upcast(ObjectNode payload)`, from the previous version to this one). So a gap, a duplicate, an overwritten version or a version without its upcaster fails at startup, in the call that made the mistake. A missing upcaster is a `NullPointerException`; every other mistake is an `IllegalArgumentException` that says what to call instead. Storing or reading a type that was never registered fails the same way, so an event can't reach the outbox unversioned. Every event is at version 1 today; the first real upcaster is expected with Backlog 8.

**Reading.** The serializer parses the payload into a tree, applies the upcasters from the stored version up to the current one in order, then maps the tree onto the registered class. A stored version below 1 or above the current one is rejected. The case that matters is a rollback: a row written by a newer deployment fails on every attempt, so it is dead-lettered after about 10 minutes like any other failure, and is replayed by hand (above) once a version that knows it is deployed again.

**Decimal amounts stay exact.** By default Jackson holds decimals in a tree as `double`, which would turn `550.00` into `550.0` and drop digits a `double` can't hold. So the serializer parses with `DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS`, which keeps that guarantee inside `outbox-core` whatever mapper it is given, and `starter`'s `application.yaml` enables the same feature for the application's mapper (`spring.jackson.deserialization.use-big-decimal-for-floats`). Typed `BigDecimal` fields were already exact; the setting covers untyped reads. Clients that parse JSON numbers as `double` are a separate, open question ([Backlog 18](BACKLOG.md)).

**Fixtures.** Each module keeps one captured payload per event version under `src/test/resources/outbox-payloads/<EventType>/v<N>.json`, taken once from a payload the running application wrote. A fixture test per module (`BasketOutboxPayloadFixturesTest`, `CatalogOutboxPayloadFixturesTest`) reads each fixture through a real serializer built with the module's `registerIn`, and asserts that the result **equals** the record built from the captured values. Equality, not a type check, because the likeliest breaking change is silent: a field added to the record without an upcaster reads as `null` from an old payload, and Jackson doesn't fail.

**Changing an event's shape**, in order:
1. Write the upcaster from the current version to the next, and register the new version in the module's `registerIn`.
2. Run the fixture tests: the old fixtures must still read into the new record. Update the expected records in the test, never the fixture files.
3. Capture a fixture for the new version from the running application, and add a test for it.

No database migration or `down -v` is needed: rows written before the change are upcast when they are read.

---

## 🧭 Architectural intent

This separation enforces:

- **Dependency direction:**  
  Infrastructure → Application → Domain, and outer layers may use Contract;  
  Domain and Contract never depend on each other, on outer layers, or on Spring/Jakarta

- **Framework isolation:**  
  Spring and JPA are implementation details, not part of the model

- **API stability:**  
  Changing persistence technology does not affect API contracts

- **DDD correctness:**  
  Domain logic is expressed using explicit DDD building blocks

- **CQRS correctness:**  
  Queries return contract DTOs, not infrastructure or domain types

---

## 🛡️ Enforced by `ArchitectureTest`

The dependency rules above are checked on every build by `starter/src/test/…/ArchitectureTest.java` (ArchUnit, production classes only). It lives in `starter` because that is the only module whose classpath holds every module.

| Rule | Why Maven alone doesn't enforce it |
|---|---|
| `..domain..` depends on no Spring, Jakarta, application, infrastructure, contract or outbox code | Same Maven module as the layers it must not reach |
| `..contract..` depends on no Spring, Jakarta, domain, application, infrastructure or outbox code | Same — and `ordering` and `shared` have no separate contract artifact |
| A module's application layer never depends on its own infrastructure | Same Maven module |
| A module reaches another module only through that module's `contract` package | Maven only limits *which* artifacts are visible; a wrong pom dependency would open a module's internals |

Maven's module split already guarantees the rest: a `-core` module cannot import anything its pom does not declare. When a rule fails, fix the dependency or change the rule *and this document* together — a rule that no longer matches the prose is the drift this test exists to catch.

---

## 🧠 Mental model

| Layer | Think of it as | Example |
|-------|----------------|---------|
| shared.domain | Domain primitives | `AggregateRoot`, `DomainEvent` |
| Domain | Business rules | `Product`, `Order`, `Money` |
| Contract | System boundary | `ProductDto`, `PaginatedResult`, `GetProductsResponse` |
| Infrastructure | Technical plumbing | `ProductEntity`, `AuditMetadata`, `JpaRepository` |