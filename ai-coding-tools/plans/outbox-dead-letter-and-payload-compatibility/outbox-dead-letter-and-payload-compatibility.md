# Outbox Dead-Letter and Payload Compatibility

## Context

Closes [Known Bug 2](../../../docs/KNOWN_ISSUES.md) (a poisoned outbox event retries forever, with no attempt limit or dead-letter state) and [Known Bug 3](../../../docs/KNOWN_ISSUES.md) (outbox payloads have no schema versioning), and folds in [Backlog 11](../../../docs/BACKLOG.md) (`FakeOutboxEventStore` exists three times).

**The assessment found Bug 2 understated.** [outbox-dispatcher-per-event-isolation](../outbox-dispatcher-per-event-isolation/outbox-dispatcher-per-event-isolation.md) isolated *publish* failures per event, but `OutboxEventProcessor.dispatchPendingEvents` calls `outboxEventStore.fetchUnprocessedEvents()` outside its `try`, and both adapters deserialize every pending row inside that call (`.map(eventMapper::toDomainEvent).toList()`). One row whose payload no longer fits its record, or whose type is not registered, throws `IllegalStateException` out of the fetch: nothing is dispatched — not that event, not any other — and the scheduled method fails again every 5 seconds, indefinitely. Bug 3's "wedges the whole outbox" is therefore still literally true for unreadable payloads. This is the link between the two bugs: **an unreadable payload must become a failed event, not a failed dispatch run.**

Other facts that shape the design:
- `OutboxEventMapper` (outbox-jpa) and `OutboxDocumentMapper` (outbox-mongo) are identical in logic: registry lookup plus Jackson.
- The port change forces a rewrite of all three `FakeOutboxEventStore` copies, which is the natural moment to consolidate them.
- Consumers run synchronously in-process (`InMemoryMessageBus` → `ApplicationEventPublisher`). Permanent failures are unreadable payloads and listeners that always throw for a given payload; transient failures are mostly a consumer's database being briefly unavailable.
- `DomainEventRegistry` lives in `shared.domain`, which `ArchitectureTest` forbids from depending on infrastructure or Jackson. Schema versions and upcasters are serialization concerns, so they cannot live there.
- This is not a production system. Existing local databases are dropped rather than migrated (decided with the user).

## Design decisions

Decided with the user, one question at a time:

1. **Payload reading moves to `outbox-core`; adapters store raw rows.** (Originally `shared`; see decision 6.) Chosen over keeping mappers in each adapter, which would duplicate unreadable-row isolation and retry policy across two databases.
2. **Retry policy: attempt limit plus exponential backoff.** Chosen over a fixed limit (a consumer outage longer than the limit would dead-letter everything in flight) and over an age limit.
3. **Compatibility policy: schema version per row plus JSON upcasters, enforced by golden payload fixtures.** Chosen over "additive in place, new type key when breaking" (consumers would handle two classes until drained, and every change needs an additive-vs-breaking judgment) and over typed legacy records (every old shape stays as a class forever).
4. **Backlog 11 is included:** one flag-and-retain fake, published from `shared`'s test-jar in increment 1, and moved to `outbox-core`'s test-jar in increment 2.
5. **Visibility and replay: ERROR log plus stored state, and a documented manual replay.** A dead-letter metric and a replay operation are recorded as Backlog improvements, not built here.
6. **The outbox's shared code lives in a new `outbox-core` Maven module, not in `shared`** (decided with the user on 2026-10-06, after increment 1). `outbox-jpa` and `outbox-mongo` depend on it. Chosen over an `outbox` package inside `shared.infrastructure`: a package fixes the layout but not the dependencies. With a module, only modules that choose an outbox adapter see outbox classes (`ordering` deliberately has none, see Backlog 5), and `shared` keeps to what every module needs rather than collecting optional capabilities. The three outbox modules are grouped under `outbox/`. `outbox/pom.xml` (artifact `outbox`, packaging `pom`) is an **aggregator only**: it lists `outbox-core`, `outbox-jpa` and `outbox-mongo` in its `<modules>`, and the root lists only `outbox`. The three modules keep the root as their `<parent>`, because no configuration is shared at group level (decided with the user). This is the target layout. `modules/basket/` and `modules/catalog/` still use the older pattern, where the grouping pom is neither parent nor aggregator and the root lists every module; converting them is [Backlog 15](../../../docs/BACKLOG.md). The artifactIds of the two adapters don't change, so the modules that depend on them don't change either. Package: `io.github.tommante.eshopmodularmonolith.outboxcore`, matching `outboxjpa` and `outboxmongo`. A module gets `outbox-core` **transitively** by depending on an adapter, and does not declare it itself, although its config and dispatcher use `outbox-core` classes directly. That is deliberate: choosing an adapter should be all it takes to get the outbox (decided with the user). Only the test-jar has to be declared, because Maven never passes on test-scoped dependencies. `DomainEventRegistry` stays in `shared.domain` (decided with the user): it registers domain events, and the outbox is only one of its users, so conceptually it is not outbox code. Every new outbox class below lives there, and every outbox bean (serializer, retry policy, schema registry) is declared in an `OutboxCoreConfig` in that package. The clock is not an outbox bean (decision 9). The starter's component scan picks it up, as it does `SharedModuleConfig`.
7. **Each increment adds only the fields and signatures it uses** (decided with the user). `StoredOutboxEvent` starts as `(eventId, occurredOn, eventType, payload)`, and increment 5 adds `schemaVersion`. Increment 4 adds no field to it (decision 8). Likewise, increment 3's port has `markAsProcessed(UUID)` and the adapters keep stamping `Instant.now()`; increment 4 adds the `Instant` parameter together with the `Clock` that supplies it (step 4.2, moved from 4.4 with the user's agreement, so the port changes once). The shapes below are the end state.
8. **Delivery state stays off `StoredOutboxEvent`** (decided with the user on 2026-10-15, during increment 4; it replaces this plan's original flat record with `failedAttempts` and `nextAttemptAt`). `StoredOutboxEvent` is what the serializer writes once: identity, type and payload. `fetchDue` returns `DueOutboxEvent(StoredOutboxEvent event, int failedAttempts)`, a read view and not a persisted type. `nextAttemptAt`, `lastError`, `deadLetteredOn` and `processedOn` are storage-only columns of the one entity or document per row. The storage model doesn't change: one table, one collection. Reasons:
   - `save` can't receive delivery state, so the port doesn't leave it to each adapter whether to trust it, and requeueing has to become an explicit port method.
   - The serializer doesn't invent scheduling values.
   - It is consistent with `processedOn`, which was already kept off the record in increment 3.
   - It is the smaller diff: no `new StoredOutboxEvent(…)` call site changes.
   - Delivery state is the part expected to grow (Backlog: dead-letter metric, replay), and it can do so in read views without touching the row the serializer writes.
   The cost is one extra record. The store owns the attempt count: `recordFailedAttempt` and `markAsDeadLettered` increment it, and on `save` the adapter sets `nextAttemptAt = occurredOn`, because a new event is due immediately.
9. **One application `Clock`, defined in `starter`** (decided with the user on 2026-10-16, during increment 4; it replaces this plan's original `outboxClock` bean in `OutboxCoreConfig`).
   - **The bean:** a `ClockConfiguration` in `starter`, next to `AuditingConfiguration`, declares `Clock.tickMillis(ZoneOffset.UTC)`. Millisecond ticks match what Mongo stores and what `ordering` already used.
   - **Consumers:** modules inject `Clock` by type, with no qualifier: the outbox dispatchers, and `ordering`'s `CreateOrderHandler`.
   - **`ordering`'s `orderClock` bean is removed.** It was guarded by `@ConditionalOnMissingBean`, which is only reliable on auto-configuration. Next to a second `Clock` bean, it would have been skipped or kept depending on the order in which Spring processed the configurations.
   - **Why one clock:** several clocks need qualifiers everywhere, and a test that fixes one of them leaves the modules running on different times.
   - **Why the starter and not `shared`:** the starter is the composition root, so modules declare the need and the application decides what time is.
   - **The cost:** a test context that builds module beans without the starter must declare its own `Clock`.
10. **`OutboxSchemaRegistry` registers one version per call, and only the next one** (decided with the user on 2026-10-17, during increment 5; it replaces this plan's original upcaster interface, which carried `eventType()` and `fromVersion()`). `registerEvent(type)` adds a type at version 1. `registerVersion(type, version, upcaster)` adds exactly the next version, together with the upcaster that reaches it from the previous one. Anything other than the current version + 1 is rejected, and so is a missing upcaster. Each line in a module's registration helper states its version, so the current version is visible without counting. A gap, a duplicate, an overwritten version or a version without an upcaster fails at startup, inside the call that made the mistake. Chosen over:
    - two independent methods (`registerCurrentVersion`, `registerUpcaster`): an upcaster can be forgotten, and gaps are only found when an old row is read;
    - one call with a list of upcasters, where the version is the list's length + 1: the version is nowhere on the page, and it has to be counted;
    - the same call with the version stated as well: complete, but one call that grows with every version;
    - typed version classes with annotations, as in some event-sourcing libraries: these are the typed legacy records decision 3 rejected, and annotations on domain events would put a storage concern in the domain.

    The upcaster no longer carries its type or source version, because `registerVersion` supplies both.

11. **Decimal amounts stay exact through the upcaster path** (decided with the user on 2026-10-18, during increment 5). Since 5.2, `read` parses the payload into a tree for the upcasters, and by default Jackson stores decimal numbers in a tree as `double`: `550.00` came back as `550.0`, and `12345678901234567890.12` lost digits. The basket fixture test caught it. The fix is applied at two levels:
    - **In the serializer:** `read` parses with `mapper.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)`. This keeps exact decimals an invariant of `outbox-core`, whatever mapper the host application passes in, and it is what the unit tests, built with `new ObjectMapper()`, exercise.
    - **In the application:** `spring.jackson.deserialization.use-big-decimal-for-floats: true` in `starter`'s `application.yaml`, so every untyped read in the eshop keeps exact amounts.

    Money as strings in the HTTP API, which protects clients that parse JSON numbers as `double`, is an API contract decision and went to [Backlog 18](../../../docs/BACKLOG.md).

### Shapes

**`StoredOutboxEvent`** (`outboxcore`), the row the serializer writes and `save` takes:
```java
public record StoredOutboxEvent(UUID eventId, Instant occurredOn, String eventType, int schemaVersion, String payload) {}
```

**`DueOutboxEvent`** (`outboxcore`), what `fetchDue` returns (decision 8):
```java
public record DueOutboxEvent(StoredOutboxEvent event, int failedAttempts) {}
```

**`OutboxEventStore` port**: no `DomainEvent` on it any more. All timestamps come from the processor's `Clock`, never `Instant.now()` inside an adapter.
```java
public interface OutboxEventStore {
    void save(StoredOutboxEvent event);
    List<DueOutboxEvent> fetchDue(Instant now);      // not processed, not dead-lettered, nextAttemptAt <= now, in occurredOn order
    void markAsProcessed(UUID eventId, Instant processedOn);
    void recordFailedAttempt(UUID eventId, String error, Instant nextAttemptAt);   // increments failedAttempts
    void markAsDeadLettered(UUID eventId, String error, Instant deadLetteredOn);   // increments failedAttempts
}
```

**`OutboxSchemaRegistry`** (`outboxcore`): current schema version per event type, plus upcasters (decision 10). One bean, contributed to by each module, following the same contributor pattern as `DomainEventRegistry`.
```java
public interface OutboxPayloadUpcaster {
    ObjectNode upcast(ObjectNode payload);   // previous version → the version it was registered with
}

public class OutboxSchemaRegistry {
    public void registerEvent(String eventType);                                          // version 1
    public void registerVersion(String eventType, int version, OutboxPayloadUpcaster upcaster);  // current + 1 only
    public int currentVersionOf(String eventType);
}
```
Rejected with a message that names the type and what the caller must change:
- `currentVersionOf` or `registerVersion` for a type that was never registered;
- `registerEvent` for a type that is already registered;
- `registerVersion` with any version other than the current one + 1 (the message names the expected version);
- `registerVersion` with a `null` upcaster.

A module helper at version 3 reads:
```java
schemaRegistry.registerEvent("BasketCheckedOut");
schemaRegistry.registerVersion("BasketCheckedOut", 2, new BasketCheckedOutV1ToV2());
schemaRegistry.registerVersion("BasketCheckedOut", 3, new BasketCheckedOutV2ToV3());
```
A `null` upcaster is rejected with `Objects.requireNonNull`, so it throws `NullPointerException` (the JDK convention for null arguments, decided with the user); the other rejections throw `IllegalArgumentException`.

Step 5.2 adds the lookup the serializer's upcaster loop needs: `List<OutboxPayloadUpcaster> upcastersFrom(String eventType, int storedVersion)`, the upcasters that take a payload from `storedVersion` to the current version, in order. It is empty at the current version. A stored version below 1 or above the current one is rejected, because the application can't read it: a version above the current one usually means a newer deployment wrote the row and the application was then rolled back.

**`OutboxPayloadSerializer`** (`outboxcore`) replaces both mappers:
- `toStored(DomainEvent)`: event id, occurrence time, registry key, current schema version and JSON. No delivery state (decision 8).
- `read(StoredOutboxEvent)`: parse to a tree, apply upcasters from the stored version up to the current one, then `treeToValue` into the registered class. It throws a descriptive exception for an unknown type, a stored version outside 1 to the current version, or a payload that doesn't fit. Decision 10 makes a gap in the chain impossible, so there is no "missing upcaster" case.

**`OutboxEventSubscriber`** serializes before saving: `outbox.save(serializer.toStored(event))`.

**`OutboxRetryPolicy`** (`outboxcore`): `record OutboxRetryPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay)`, with `Instant nextAttemptAfter(int failedAttempts, Instant now)` = `now + min(initialDelay × 2^(failedAttempts − 1), maxDelay)` and `boolean isExhausted(int failedAttempts)` = `failedAttempts >= maxAttempts`. `failedAttempts` includes the failure that just happened. Defaults for both modules: **8 attempts, 5s initial, 5 min cap**. The delays are 5s, 10s, 20s, 40s, 80s, 160s and 300s, so an event is dead-lettered on its 8th failure, about 10 minutes after the first.
- **It is tuning, so it is configuration** (decided with the user on 2026-10-11). The record itself is `@ConfigurationProperties("eshop.outbox.retry")`, registered with `@EnableConfigurationProperties` on `OutboxCoreConfig`. The defaults live once, in `@DefaultValue` on the components, not in `application.yaml`, so contexts that set nothing still get them. A compact constructor rejects invalid values (`maxAttempts < 1`, non-positive delays, `initialDelay > maxDelay`), with a message naming the property key, so a bad value fails at startup.
- **The exhaustion boundary lives in the policy** (decided with the user), not in the processor, so the "8th failure dead-letters" boundary is tested once as a pure function.
- The calculation must not overflow for large counts: it stops doubling once it reaches `maxDelay`.

**`OutboxEventProcessor`** owns all policy:
```java
public void dispatchPendingEvents(OutboxEventPublisher publisher) {
    var now = clock.instant();
    for (var due : outboxEventStore.fetchDue(now)) {
        try {
            publisher.publish(serializer.read(due.event()));
            outboxEventStore.markAsProcessed(due.event().eventId(), now);
        } catch (Exception failure) {
            recordFailure(due, failure, now);   // retryPolicy.isExhausted(due.failedAttempts() + 1) → dead-letter + ERROR log; else recordFailedAttempt with backoff
        }
    }
}
```
A `markAsProcessed` failure after a successful publish counts as a failed attempt and the event is redelivered, as today. One-order-per-checkout deduplication already covers that.

**Dispatchers** construct `new OutboxEventProcessor(store, serializer, retryPolicy, clock)`. The `Clock` is the application's single `Clock` bean from `starter` (decision 9).

**Registration per module**: a small static registration helper per module (`BasketOutboxEvents.registerIn(DomainEventRegistry, OutboxSchemaRegistry)`, and the catalog equivalent). The module config's `@PostConstruct` and that module's fixture test both call it, so the test can never drift from production registration.

### Storage
- **Mongo `outbox` documents** gain `schemaVersion`, `failedAttempts`, `nextAttemptAt`, `lastError` and `deadLetteredOn`. The fetch query is `processedOn == null && deadLetteredOn == null && nextAttemptAt <= now`. Updates use `updateFirst` with `$inc`/`$set` rather than load-and-save.
- **Catalog `outbox_events`**: updates are `@Modifying` JPQL update queries (`set e.failedAttempts = e.failedAttempts + 1, …`), the counterpart of Mongo's `$inc`/`$set`, rather than load-change-save. Decided with the user on 2026-10-16, so both adapters change only the fields they name and count atomically. With one scheduler per module the concurrency gain is small today; the user chose it to keep the adapters symmetric. The entity has no methods that change it. An update that matches no row throws `IllegalStateException("Unknown outbox event: …")` in both adapters.
- **Catalog `outbox_events`**: the same columns are added **directly to `V1__init.sql`** (`schema_version int not null`, `failed_attempts int not null default 0`, `next_attempt_at timestamp not null`, `last_error text`, `dead_lettered_on timestamp`). There is no V2 migration. An existing local database fails Flyway's checksum validation and must be dropped (`docker compose down -v`).
- **No fallback for old rows:** `schemaVersion` is required, because rows without it will not exist.

### Compatibility policy (to be written into ARCHITECTURE.md)
> Every change to a persisted domain event's JSON shape bumps its schema version, registers an upcaster from the previous version, and adds a fixture for the new version. Existing fixtures are never edited.

- **Fixtures:** `src/test/resources/outbox-payloads/<EventType>/v<N>.json` in the module that owns the event. Each is captured once from a real serialized payload.
- **Fixture test per module:** reads every fixture through a real `OutboxPayloadSerializer` built with that module's registration helper, and asserts the result **equals the record built from the fixture's captured values** (decided with the user on 2026-10-18, during increment 5). A type check alone would miss the most likely breaking change: a field added to the record without an upcaster reads as `null` from an old payload, and Jackson doesn't fail. Comparing the whole record makes that fixture's test fail and points at the missing upcaster.
- **Starting point:** every event is at version 1 with no upcasters. The first real upcaster arrives with Backlog 8.

### Manual replay (to be written into ARCHITECTURE.md)
To requeue a dead-lettered event, clear `deadLetteredOn`, set `failedAttempts` to 0 and `nextAttemptAt` to now. The plan's close-out adds one Mongo `updateOne` and one SQL `UPDATE`.

## Test seams

| Behaviour | Seam | Why |
|---|---|---|
| Unreadable row doesn't block others; retry counting, backoff, cap, dead-lettering, due-filtering | `OutboxEventProcessorTest` with the shared fake store and a fixed `Clock` | A state machine with more cases than an adapter test would enumerate, and all policy lives in the processor. The fake is behind the port, so no container is needed. |
| Backoff arithmetic | `OutboxRetryPolicyTest` | A pure calculation with edge cases (first attempt, doubling, cap). |
| Version registration: unknown type, duplicate type, only the next version, upcaster required | `OutboxSchemaRegistryTest` (`outbox-core`) | Invariants that must hold whichever module registers; each mistake must fail at startup. |
| Upcaster chain, unknown type, stored version outside the known range | `OutboxPayloadSerializerTest` (`outbox-core`) with test events at v1→v2→v3 | Deterministic logic with several failure modes. |
| Rows persist and filter correctly: due, not processed, not dead-lettered; counters and timestamps written | `MongoOutboxEventStoreTest`, `JpaOutboxEventStoreTest` (Testcontainers) | Null handling and time comparisons in real queries only real databases prove. |
| Decimal amounts keep their digits and scale through the upcaster path | `OutboxPayloadSerializerTest` with a `BigDecimal` test event | The serializer's own invariant, independent of the host's mapper configuration. |
| Stored payloads of every version still read | Fixture test in `basket-core` and `catalog-core` | Compatibility is a property of the module's real registration and records. |
| Dispatchers translate events and wire the processor | `BasketOutboxDispatcherTest`, `CatalogOutboxDispatcherTest` (existing, adapted) | Wiring only; policy is proven once, in `outbox-core`. |
| Assembled context still routes events into both outboxes | `ApplicationWiringTest` (existing) | Unchanged behaviour; it must stay green. |

## Delivery: five increments

The design above is delivered as five increments. Each one is its own branch and PR, merges on its own, and leaves `./mvnw clean verify` green. The cut was chosen with the user to avoid one large change. Increment 2 was inserted after increment 1 had been opened as a PR (decision 6).

| # | Increment | Closes | Depends on |
|---|---|---|---|
| 1 | One `FakeOutboxEventStore`, in `shared`'s test-jar, on the **current** port | Backlog 11 | — |
| 2 | New `outbox-core` module; the outbox's shared classes and the fake move into it | — (restructure) | 1 |
| 3 | The port returns rows, reading moves to `outbox-core`, and an unreadable row no longer stops the batch | The "whole outbox stops" part of Known Bug 2 | 2 |
| 4 | Retry, backoff and dead-letter | Known Bug 2 | 3 |
| 5 | Schema version, upcasters and payload fixtures | Known Bug 3 | 3 |

Why this order:
- **Fakes first.** Every port change has to rewrite every fake. With one fake left, increments 3 and 4 each change one fake instead of three. It also settles Backlog 11's real question, delete vs. flag-and-retain, by itself (decision 4: flag-and-retain).
- **The module before the fix.** Increment 2 only moves code and changes poms and imports across five modules. Kept separate, it doesn't bury increment 3's behaviour change in a rename diff, and increment 3 creates its new classes in their final place.
- **Retry before versioning.** This swaps the original Phase 2 and Phase 3. Once increment 3 has merged, Known Bug 3 only matters at the next change to an event's shape (Backlog 8). Known Bug 2's endless retry exists today. Increments 4 and 5 depend only on 3, so if Backlog 8 becomes urgent, 5 can go first.
- **Retry and backoff stay together.** A fixed limit without backoff would dead-letter every in-flight event after a consumer outage of about 40 seconds (8 × 5s). That's the reason decision 2 rejected it.
- **Each increment closes its own live-list entries and its own docs.** A plan that merges in pieces has no single close-out to defer them to.
- **Increments 4 and 5 each edit `V1__init.sql`**, so each one needs `docker compose down -v` locally.

**Branches.** [feature-workflow.md](../../processes/feature-workflow.md) assumes one branch per plan. This plan has one branch per increment: `<prefix>/outbox-dead-letter-and-payload-compatibility-<n>`, where the prefix depends on what that increment closes. Increment 1 closes a Backlog item, so it uses `feature/`. Increment 2 closes nothing, so it uses `refactor/`. Increments 3–5 close (part of) Known Bugs, so they use `fix/`. Each branch is cut from `main` after the previous increment has merged, or stacked on the previous increment's branch while that one is still in review and rebased onto `main` once it merges. Increment 2 is stacked on increment 1.

## Testing rigor

- **Strict red → green:** unreadable-row isolation (3.3), backoff arithmetic (4.1), the retry and dead-letter state machine (4.2), the adapters' `fetchDue` filter (4.3), the schema registry's validation (5.1), and the upcaster chain (5.2). This is new control flow or query logic with real regression risk, and a failing test first pins the interface down.
- **Tests alongside:**
  - fake consolidation (increment 1) and the module move (increment 2): no behaviour change, and the existing tests are the safety net, with only their package and import lines changing;
  - the serializer round-trip and the port refactor (3.1–3.2): behaviour-preserving, existing tests are the safety net;
  - dispatcher wiring (4.4);
  - schema-version persistence and fixture capture (5.3).
- **Refactor steps** (3.1–3.2, and 5.1's `schemaVersion` stamping) follow [refactoring.md](../../processes/refactoring.md). Increments 1 and 2 do not: that process is for production code and forbids touching tests, and both increments necessarily move or change test code.

## Todo List

### Increment 1: One fake outbox store (closes Backlog 11)
No production code changes. The port stays `save(DomainEvent)` / `fetchUnprocessedEvents()` / `markAsProcessed(DomainEvent)`.
- [x] 1.1. **Rewrite `shared`'s `FakeOutboxEventStore`** (`shared.infrastructure.fake`) as flag-and-retain: a `LinkedHashMap` of stored events plus a processed flag per event id. `fetchUnprocessedEvents()` returns the unflagged events **in save order**. Basket's copy uses a `HashMap`, which doesn't keep that order; the red test in 3.3 ("unreadable row saved *before* a valid one") depends on it. Don't add query methods that no current test needs.
- [x] 1.2. **Publish and consume it.**
  - `shared/pom.xml` gains the `maven-jar-plugin` `test-jar` execution. Copy the four lines from `security/pom.xml`.
  - `basket-core` and `catalog-core` each add a `shared` dependency with `<type>test-jar</type>` and `<scope>test</scope>`, as `basket-core` already does for `security`.
  - Delete `basket.infrastructure.messaging.fake.FakeOutboxEventStore` and `products.infrastructure.messaging.fake.FakeOutboxEventStore`. Point `BasketOutboxDispatcherTest` and `CatalogOutboxDispatcherTest` at the shared one.
- [x] 1.3. **`./mvnw clean verify`** is green. The full build matters here, not `-Dtest`: the test-jar only resolves through the reactor.
- [x] 1.4. **Live lists:** remove Backlog 11 from [BACKLOG.md](../../../docs/BACKLOG.md). Move its "Do not fix the other duplicates SonarCloud reports" paragraph into [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) (decided with the user). It explains duplication that the architecture requires, so it belongs with the architecture, not with speculation.

### Increment 2: An `outbox-core` module (restructure, closes nothing)
No behaviour changes. Only packages, imports and poms change.
- [x] 2.1. **Group the outbox modules under `outbox/`, as an aggregator** (decision 6).
  - `outbox/pom.xml`: artifact `outbox`, packaging `pom`, parent the root with `<relativePath>../pom.xml</relativePath>`, and `<modules>` listing `outbox-core`, `outbox-jpa` and `outbox-mongo`.
  - `git mv outbox-jpa outbox/outbox-jpa` and `git mv outbox-mongo outbox/outbox-mongo`. In both poms, set `<relativePath>../../pom.xml</relativePath>`. Their parent stays the root and their artifactIds stay the same.
  - New `outbox/outbox-core/pom.xml`: same parent and `relativePath`, depending on `shared` at `compile` and `spring-boot-starter-test` at `test`, plus the `maven-jar-plugin` `test-jar` execution.
  - Root `pom.xml`: replace the `outbox-jpa` and `outbox-mongo` entries with a single `outbox` entry. Each module may be listed only once in the reactor, so the root must not list the three children too. In `sonar/pom.xml`, add an `outbox-core` dependency so the JaCoCo aggregate covers it.
  - `./mvnw clean verify` before moving any classes, so a red build later points at 2.2 or 2.3, not at the restructure.
- [x] 2.2. **Move the classes** from `shared.infrastructure` to `io.github.tommante.eshopmodularmonolith.outboxcore` with the IDE's *Move*, keeping their names: `OutboxEventStore`, `OutboxEventProcessor` and `OutboxEventSubscriber`. `OutboxEventPublisher` also moves, renamed to **`OutboxEventPublisher`** (decided with the user). It isn't a generic publication interface: it's the processor's callback for each event read from the outbox. It receives a domain event, not an integration event (publishing those is `MessageBus`'s job), and a throw from it is what keeps the event pending, which increment 4's retry and dead-letter policy builds on. Tests: `OutboxEventProcessorTest` and `OutboxEventSubscriberTest` to `outboxcore`, and `FakeOutboxEventStore` to `outboxcore.fake`. `MessageBus`, `InMemoryMessageBus`, `SpringDomainEventPublisher`, `JpaAuditMetadata` and `error/` stay in `shared`: they're not outbox code.
- [x] 2.3. **Rewire the dependencies.**
  - `outbox-jpa` and `outbox-mongo` depend on `outbox-core`. They keep `shared`, because their mappers use `DomainEvent` and `DomainEventRegistry` directly until increment 3 deletes the mappers.
  - `basket-core` and `catalog-core` do **not** declare `outbox-core`: they get it through `outbox-mongo` and `outbox-jpa` (decision 6). Their `shared` test-jar dependency becomes an `outbox-core` test-jar dependency, which they must declare.
  - `shared/pom.xml` loses the `test-jar` execution added in increment 1, because nothing else consumes its test classes.
- [x] 2.4. **Keep `ArchitectureTest`'s protection.** Its domain and contract rules forbid `..infrastructure..`. `shared.infrastructure.OutboxEventStore` matched that; `outboxcore` doesn't, and neither do `outboxjpa` or `outboxmongo`. Add `"..outboxcore..", "..outboxjpa..", "..outboxmongo.."` to the forbidden packages of both rules, so the move doesn't silently let domain code reach the outbox.
- [x] 2.5. **`./mvnw clean verify`:** green, with no assertion changed.
- [x] 2.6. **Docs:** [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) gets the fake's new path, the outbox modules and how they depend on each other. The project structure in [README.md](../../../README.md) gets the new module. README's highlight that puts `OutboxEventStore` in `shared.infrastructure` is corrected. [Known Bug 2](../../../docs/KNOWN_ISSUES.md)'s location becomes `outbox-core`. [Backlog 12](../../../docs/BACKLOG.md)'s note on `analyze` output gets one line saying that `outbox-core` showing up as "used undeclared" in `basket-core` and `catalog-core` is deliberate. Live lists: nothing closes.

### Increment 3: Reading moves to `outbox-core`; an unreadable row doesn't stop the batch
- [x] 3.1. **`OutboxPayloadSerializer` and `StoredOutboxEvent`** in `outboxcore`. The record has only `(eventId, occurredOn, eventType, payload)` (decision 7). `OutboxPayloadSerializerTest`: a round trip, the row carrying the event's id, time and type, and two distinct failures (decided with the user): an unregistered type fails with the registry's own `IllegalArgumentException`, which `read` lets through unwrapped, and a payload that doesn't fit the registered class fails as a deserialization error that keeps Jackson's exception as its cause. The processor attaches the event's identity when it logs, so the exceptions don't need to carry it.
- [x] 3.2. **The port returns rows.** `OutboxEventStore` becomes `save(StoredOutboxEvent)`, `fetchPending()` returning rows, and `markAsProcessed(UUID)` (decision 7: the `Instant` parameter arrives with the `Clock` in increment 4).
  - `OutboxEventSubscriber` serializes.
  - Both adapters store and return raw rows. `OutboxEventMapper` and `OutboxDocumentMapper` are deleted, and a single `OutboxPayloadSerializer` bean is declared in a new `OutboxCoreConfig`.
  - The processor still reads **all rows before the loop**. This keeps today's behaviour on purpose, so 3.3 can go red.
  - The fake moves to the new port.
  - Adapter, subscriber, dispatcher and transactional tests are adjusted, and the full suite is green.
- [x] 3.3. **Red:** `OutboxEventProcessorTest`: a row with an unreadable payload is saved before a valid one, and the valid one is still published. It fails, because reading happens outside the `try`. **Green:** move `serializer.read` inside the per-event `try`.
- [x] 3.4. **Docs:** add a responsibilities table (serializer, store, processor, subscriber, publisher) to the *📮 Outbox modules* section of [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md), explaining why the port speaks `StoredOutboxEvent` rows rather than `DomainEvent`s: the store only persists rows, translation sits once in the serializer, and policy for unreadable rows sits once in the processor instead of in each adapter (decided with the user). Update the module table's `outbox-core` contents (`StoredOutboxEvent`, `OutboxPayloadSerializer`, `OutboxCoreConfig`, `FakeDomainEvent`) and drop the adapters' dependency on `shared` and Jackson. **Live lists:** rewrite [Known Bug 2](../../../docs/KNOWN_ISSUES.md) to say that a row that can't be read now only blocks itself, and that the endless retry is what remains. In Known Bug 3, drop the "wedges the whole outbox" sentence.

### Increment 4: Retry, backoff and dead-letter (closes Known Bug 2)
- [x] 4.1. **Red → green:** `OutboxRetryPolicy` (a `@ConfigurationProperties` record) and `OutboxRetryPolicyTest`: first retry after `initialDelay`, doubling, capped at `maxDelay`, no overflow for a count far beyond the cap, `isExhausted` at `maxAttempts − 1` and `maxAttempts`, and invalid values rejected with a message naming the property.
- [x] 4.2. **Red → green**, `OutboxEventProcessorTest` with a fixed `Clock`, one test at a time:
  - **Scaffolding first (no behaviour change):**
    - `DueOutboxEvent` (decision 8);
    - the port gains `fetchDue(Instant)` (replacing `fetchPending()`), `recordFailedAttempt` and `markAsDeadLettered`, and `markAsProcessed` gains its `Instant processedOn` parameter;
    - the fake implements them and exposes `isDeadLettered(UUID)`, because "never fetched again" alone can't tell dead-lettered from processed;
    - the processor takes `(store, serializer, OutboxRetryPolicy, Clock)`.

    The two existing processor tests stay green. Their events use `occurredOn = NOW`, because a real-time `occurredOn` is after the fixed clock and would never be due.
  - **Red:** a failure records an attempt, and the event is due again only after the policy's delay.
  - **Red:** the `maxAttempts`-th failure dead-letters the event; it is never fetched again, and the fake reports it as dead-lettered.
  - **Guards** (green when written, kept as regression tests):
    - a row that isn't due yet is not dispatched; the filter's real red tests are the adapters' in 4.3;
    - a success after earlier failures marks the event processed.

  Tests reach attempt state through the port, by calling `recordFailedAttempt`, rather than building it by hand. Until 4.3 and 4.4, only `outbox-core` compiles and is tested.
- [x] 4.3. **Adapters:** the entity and the document gain the delivery columns and a `toDue()`, `save` sets `nextAttemptAt = occurredOn`, and `markAsProcessed` stamps the `Instant` it receives instead of calling `Instant.now()`; Mongo fields and `$inc`/`$set` updates; JPA columns in `V1__init.sql`, entity fields, a `findDue` query and `@Modifying` update queries. **Red → green** adapter tests for `fetchDue`, covering due, not due, processed and dead-lettered rows. Tests alongside for the counters, `lastError` and timestamps being written.
- [x] 4.4. **Wiring:**
  - `@EnableConfigurationProperties(OutboxRetryPolicy.class)` in `OutboxCoreConfig`;
  - one application `Clock` in a new `starter` `ClockConfiguration`, while `ordering`'s `orderClock` bean is removed and `CreateOrderHandler` injects `Clock` without a qualifier (decision 9);
  - the dispatchers receive the `OutboxRetryPolicy` and the `Clock` and build the processor with serializer, policy and clock;
  - the dispatcher tests and `CheckoutBasketHandlerTransactionalTest` move to `fetchDue`.
- [x] 4.5. **`./mvnw clean verify`:** green, including `ArchitectureTest` and `ApplicationWiringTest`.
- [x] 4.6. **Docs:** [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) gets the retry and dead-letter behaviour and the manual replay commands. [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) gets a worked-example row for the processor policy seam. [SETUP.md](../../../docs/SETUP.md) gets a note that `V1__init.sql` changed, so existing local volumes need `docker compose down -v`.
- [x] 4.7. **Live lists:** remove Known Bug 2. Route the Deferred entries below (all of them relate to dead-lettering). Check [README.md](../../../README.md).

### Increment 5: Schema version and upcasters (closes Known Bug 3)
- [x] 5.1. **`OutboxSchemaRegistry`** in `outboxcore`, with `registerEvent`, `registerVersion` and `currentVersionOf` (decision 10), and `OutboxPayloadUpcaster`. A bean in `OutboxCoreConfig`. `StoredOutboxEvent` gains `schemaVersion`. The serializer takes the registry and stamps the current version on `toStored`.
  - **Red → green, `OutboxSchemaRegistryTest`:**
    - a registered type is at version 1, and at version 3 after `registerVersion` for 2 and 3;
    - `currentVersionOf` for an unregistered type is rejected;
    - `registerEvent` twice for the same type is rejected;
    - `registerVersion` for an unregistered type is rejected;
    - `registerVersion` with a version other than the next one (skipped, or repeated) is rejected, naming the expected version;
    - `registerVersion` with a `null` upcaster is rejected.
  - **Tests alongside:** the existing `outbox-core` tests register `FakeDomainEvent` at version 1 and build the serializer with the registry, and the "row carries its id, time and type" test also asserts `schemaVersion` is 1.

  Until 5.3, only `outbox-core` compiles and is tested: the adapters and the module tests still use the old constructors.
- [x] 5.2. **Red:** `OutboxPayloadSerializerTest`, with `FakeDomainEvent` registered at version 3 and upcasters 1→2 and 2→3:
  - a version 1 payload reads into the current record, through both upcasters in order;
  - a version 2 payload goes through the 2→3 upcaster only;
  - a stored version above the current one, or below 1, is rejected with a message naming the type, the stored version and the readable range.

  The original "missing upcaster" test is dropped: decision 10 makes a gap in the chain impossible. **Green:** `upcastersFrom` in `OutboxSchemaRegistry`, and the upcaster loop in `read`. The behaviour seam is reading a row, so the range check is tested through the serializer, not in `OutboxSchemaRegistryTest`.
- [x] 5.3. **Adapters persist `schemaVersion`:** Mongo field, `V1__init.sql` column, adapter tests alongside.
  - Per-module registration helpers (`BasketOutboxEvents`, and the catalog equivalent) are called from the module configs.
  - Capture `BasketCheckedOut/v1.json` and `ProductCreated/v1.json`, and add a fixture test in `basket-core` and `catalog-core` that asserts equality with the captured values.
  - **Exact decimals (decision 11):** `read` parses with `USE_BIG_DECIMAL_FOR_FLOATS`, and `starter`'s `application.yaml` enables the same feature for the application's mapper. Regression test in `OutboxPayloadSerializerTest`: a `FakePricedDomainEvent` with a `BigDecimal` reads `550.00` and `12345678901234567890.12` back with their exact digits and scale.
- [x] 5.4. **`./mvnw clean verify`:** green.
- [x] 5.5. **Docs:** the compatibility policy in [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md). A worked-example row for the payload fixtures in [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md). Make sure SETUP.md's `down -v` note covers this change too.
- [x] 5.6. **Live lists:** remove Known Bug 3. Update [Backlog 1](../../../docs/BACKLOG.md)'s "Realistic order", which names Known Bug 3 as the first step. Check [README.md](../../../README.md).

## Deferred
- **Dead-letter metric** (a Micrometer gauge such as `eshop.outbox.dead_lettered{module=…}`, needing a count query on the port). Decided with the user as an improvement → **Backlog**. It must not affect health status.
- **Replay operation** (an endpoint or admin command that requeues dead-lettered events). It needs authorization, which depends on Backlog 13's roles decision → **Backlog**.
- **Two instances polling the same outbox**: unchanged by this plan; already covered by [Known Bug 13](../../../docs/KNOWN_ISSUES.md)'s redelivery window. Nothing new to route.
- **Retry jitter**: not needed with a single scheduler per module. Dropped.
