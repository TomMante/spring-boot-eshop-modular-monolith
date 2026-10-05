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

1. **Payload reading moves to `shared`; adapters store raw rows.** Chosen over keeping mappers in each adapter, which would duplicate unreadable-row isolation and retry policy across two databases.
2. **Retry policy: attempt limit plus exponential backoff.** Chosen over a fixed limit (a consumer outage longer than the limit would dead-letter everything in flight) and over an age limit.
3. **Compatibility policy: schema version per row plus JSON upcasters, enforced by golden payload fixtures.** Chosen over "additive in place, new type key when breaking" (consumers would handle two classes until drained, and every change needs an additive-vs-breaking judgment) and over typed legacy records (every old shape stays as a class forever).
4. **Backlog 11 is included:** one flag-and-retain fake, published from `shared`'s test-jar.
5. **Visibility and replay: ERROR log plus stored state, and a documented manual replay.** A dead-letter metric and a replay operation are recorded as Backlog improvements, not built here.

### Shapes

**`StoredOutboxEvent`** (`shared.infrastructure`), the row the port speaks:
```java
public record StoredOutboxEvent(UUID eventId, Instant occurredOn, String eventType, int schemaVersion,
                                String payload, int failedAttempts, Instant nextAttemptAt) {}
```

**`OutboxEventStore` port**: no `DomainEvent` on it any more. All timestamps come from the processor's `Clock`, never `Instant.now()` inside an adapter.
```java
public interface OutboxEventStore {
    void save(StoredOutboxEvent event);
    List<StoredOutboxEvent> fetchDue(Instant now);   // not processed, not dead-lettered, nextAttemptAt <= now
    void markAsProcessed(UUID eventId, Instant processedOn);
    void recordFailedAttempt(UUID eventId, String error, Instant nextAttemptAt);
    void markAsDeadLettered(UUID eventId, String error, Instant deadLetteredOn);
}
```

**`OutboxSchemaRegistry`** (`shared.infrastructure`): current schema version per event type, plus upcasters. One bean, contributed to by each module, following the same contributor pattern as `DomainEventRegistry`.
```java
public interface OutboxPayloadUpcaster {
    String eventType();
    int fromVersion();                     // upgrades fromVersion → fromVersion + 1
    ObjectNode upcast(ObjectNode payload);
}
```

**`OutboxPayloadSerializer`** (`shared.infrastructure`) replaces both mappers:
- `toStored(DomainEvent)`: registry key, current schema version, JSON, `failedAttempts` 0, `nextAttemptAt = occurredOn`.
- `read(StoredOutboxEvent)`: parse to a tree, apply upcasters from the stored version up to the current one, then `treeToValue` into the registered class. It throws a descriptive exception for an unknown type, a missing upcaster in the chain, or a payload that doesn't fit.

**`OutboxEventSubscriber`** serializes before saving: `outbox.save(serializer.toStored(event))`.

**`OutboxRetryPolicy`** (`shared.infrastructure`): `record OutboxRetryPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay)`, with `Instant nextAttemptAfter(int failedAttempts, Instant now)` = `now + min(initialDelay × 2^(failedAttempts − 1), maxDelay)`. Defaults for both modules: **8 attempts, 5s initial, 5 min cap**. The delays are 5s, 10s, 20s, 40s, 80s, 160s and 300s, so an event is dead-lettered on its 8th failure, about 10 minutes after the first.

**`OutboxEventProcessor`** owns all policy:
```java
public void dispatchPendingEvents(EventPublisher publisher) {
    var now = clock.instant();
    for (var stored : outboxEventStore.fetchDue(now)) {
        try {
            publisher.publish(serializer.read(stored));
            outboxEventStore.markAsProcessed(stored.eventId(), now);
        } catch (Exception failure) {
            recordFailure(stored, failure, now);   // failedAttempts + 1 >= maxAttempts → dead-letter + ERROR log; else recordFailedAttempt with backoff
        }
    }
}
```
A `markAsProcessed` failure after a successful publish counts as a failed attempt and the event is redelivered, as today. One-order-per-checkout deduplication already covers that.

**Dispatchers** construct `new OutboxEventProcessor(store, serializer, retryPolicy, clock)`. The `Clock` is an `outboxClock` bean (`Clock.systemUTC()`) in `SharedModuleConfig`.

**Registration per module**: a small static registration helper per module (`BasketOutboxEvents.registerIn(DomainEventRegistry, OutboxSchemaRegistry)`, and the catalog equivalent). The module config's `@PostConstruct` and that module's fixture test both call it, so the test can never drift from production registration.

### Storage
- **Mongo `outbox` documents** gain `schemaVersion`, `failedAttempts`, `nextAttemptAt`, `lastError` and `deadLetteredOn`. The fetch query is `processedOn == null && deadLetteredOn == null && nextAttemptAt <= now`. Updates use `updateFirst` with `$inc`/`$set` rather than load-and-save.
- **Catalog `outbox_events`**: the same columns are added **directly to `V1__init.sql`** (`schema_version int not null`, `failed_attempts int not null default 0`, `next_attempt_at timestamp not null`, `last_error text`, `dead_lettered_on timestamp`). There is no V2 migration. An existing local database fails Flyway's checksum validation and must be dropped (`docker compose down -v`).
- **No fallback for old rows:** `schemaVersion` is required, because rows without it will not exist.

### Compatibility policy (to be written into ARCHITECTURE.md)
> Every change to a persisted domain event's JSON shape bumps its schema version, registers an upcaster from the previous version, and adds a fixture for the new version. Existing fixtures are never edited.

- **Fixtures:** `src/test/resources/outbox-payloads/<EventType>/v<N>.json` in the module that owns the event. Each is captured once from a real serialized payload.
- **Fixture test per module:** reads every fixture through a real `OutboxPayloadSerializer` built with that module's registration helper, and asserts the result is the registered class. Where an upcaster's output matters, it also asserts the upcast fields.
- **Starting point:** every event is at version 1 with no upcasters. The first real upcaster arrives with Backlog 8.

### Manual replay (to be written into ARCHITECTURE.md)
To requeue a dead-lettered event, clear `deadLetteredOn`, set `failedAttempts` to 0 and `nextAttemptAt` to now. The plan's close-out adds one Mongo `updateOne` and one SQL `UPDATE`.

## Test seams

| Behaviour | Seam | Why |
|---|---|---|
| Unreadable row doesn't block others; retry counting, backoff, cap, dead-lettering, due-filtering | `OutboxEventProcessorTest` with the shared fake store and a fixed `Clock` | A state machine with more cases than an adapter test would enumerate, and all policy lives in the processor. The fake is behind the port, so no container is needed. |
| Backoff arithmetic | `OutboxRetryPolicyTest` | A pure calculation with edge cases (first attempt, doubling, cap). |
| Upcaster chain, unknown type, missing upcaster | `OutboxPayloadSerializerTest` (shared) with test events at v1→v2→v3 | Deterministic logic with several failure modes. |
| Rows persist and filter correctly: due, not processed, not dead-lettered; counters and timestamps written | `MongoOutboxEventStoreTest`, `JpaOutboxEventStoreTest` (Testcontainers) | Null handling and time comparisons in real queries only real databases prove. |
| Stored payloads of every version still read | Fixture test in `basket-core` and `catalog-core` | Compatibility is a property of the module's real registration and records. |
| Dispatchers translate events and wire the processor | `BasketOutboxDispatcherTest`, `CatalogOutboxDispatcherTest` (existing, adapted) | Wiring only; policy is proven once, in `shared`. |
| Assembled context still routes events into both outboxes | `ApplicationWiringTest` (existing) | Unchanged behaviour; it must stay green. |

## Delivery: four increments

The design above is delivered as four increments. Each one is its own branch and PR, merges on its own, and leaves `./mvnw clean verify` green. The cut was chosen with the user to avoid one large change. The design decisions are unchanged.

| # | Increment | Closes | Depends on |
|---|---|---|---|
| 1 | One `FakeOutboxEventStore`, in `shared`'s test-jar, on the **current** port | Backlog 11 | — |
| 2 | The port returns rows, reading moves to `shared`, and an unreadable row no longer stops the batch | The "whole outbox stops" part of Known Bug 2 | 1 |
| 3 | Retry, backoff and dead-letter | Known Bug 2 | 2 |
| 4 | Schema version, upcasters and payload fixtures | Known Bug 3 | 2 |

Why this order:
- **Fakes first.** Every port change has to rewrite every fake. With one fake left, increments 2 and 3 each change one fake instead of three. It also settles Backlog 11's real question, delete vs. flag-and-retain, by itself (decision 4: flag-and-retain).
- **Retry before versioning.** This swaps the original Phase 2 and Phase 3. Once increment 2 has merged, Known Bug 3 only matters at the next change to an event's shape (Backlog 8). Known Bug 2's endless retry exists today. Increments 3 and 4 depend only on 2, so if Backlog 8 becomes urgent, 4 can go first.
- **Retry and backoff stay together.** A fixed limit without backoff would dead-letter every in-flight event after a consumer outage of about 40 seconds (8 × 5s). That's the reason decision 2 rejected it.
- **Each increment closes its own live-list entries and its own docs.** A plan that merges in pieces has no single close-out to defer them to.
- **Increments 3 and 4 each edit `V1__init.sql`**, so each one needs `docker compose down -v` locally.

**Branches.** [feature-workflow.md](../../processes/feature-workflow.md) assumes one branch per plan. This plan has one branch per increment: `<prefix>/outbox-dead-letter-and-payload-compatibility-<n>`, where the prefix depends on what that increment closes. Increment 1 closes a Backlog item, so it uses `feature/`. Increments 2–4 close (part of) Known Bugs, so they use `fix/`. Each branch is cut from `main` after the previous increment has merged, so this plan file comes along with it.

## Testing rigor

- **Strict red → green:** unreadable-row isolation (2.3), the retry and dead-letter state machine (3.2), backoff arithmetic (3.1), the adapters' `fetchDue` filter (3.3), and the upcaster chain (4.2). This is new control flow or query logic with real regression risk, and a failing test first pins the interface down.
- **Tests alongside:**
  - fake consolidation (increment 1): test code only, behaviour-preserving, and the tests that use the fake are the safety net;
  - the serializer round-trip and the port refactor (2.1–2.2): behaviour-preserving, existing tests are the safety net;
  - dispatcher wiring (3.4);
  - schema-version persistence and fixture capture (4.3).
- **Refactor steps** (1.1–1.2, 2.1–2.2 and 4.1) follow [refactoring.md](../../processes/refactoring.md).

## Todo List

### Increment 1: One fake outbox store (closes Backlog 11)
No production code changes. The port stays `save(DomainEvent)` / `fetchUnprocessedEvents()` / `markAsProcessed(DomainEvent)`.
- [ ] 1.1. **Rewrite `shared`'s `FakeOutboxEventStore`** (`shared.infrastructure.fake`) as flag-and-retain: a `LinkedHashMap` of stored events plus a processed flag per event id. `fetchUnprocessedEvents()` returns the unflagged events **in save order**. Basket's copy uses a `HashMap`, which doesn't keep that order; increment 2's red test ("unreadable row saved *before* a valid one") depends on it. Don't add query methods that no current test needs.
- [ ] 1.2. **Publish and consume it.**
  - `shared/pom.xml` gains the `maven-jar-plugin` `test-jar` execution. Copy the four lines from `security/pom.xml`.
  - `basket-core` and `catalog-core` each add a `shared` dependency with `<type>test-jar</type>` and `<scope>test</scope>`, as `basket-core` already does for `security`.
  - Delete `basket.infrastructure.messaging.fake.FakeOutboxEventStore` and `products.infrastructure.messaging.fake.FakeOutboxEventStore`. Point `BasketOutboxDispatcherTest` and `CatalogOutboxDispatcherTest` at the shared one.
- [ ] 1.3. **`./mvnw clean verify`** is green. The full build matters here, not `-Dtest`: the test-jar only resolves through the reactor.
- [ ] 1.4. **Live lists:** remove Backlog 11 from [BACKLOG.md](../../../docs/BACKLOG.md). Move its "Do not fix the other duplicates SonarCloud reports" paragraph into [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) (decided with the user). It explains duplication that the architecture requires, so it belongs with the architecture, not with speculation.

### Increment 2: Reading moves to `shared`; an unreadable row doesn't stop the batch
- [ ] 2.1. **`OutboxPayloadSerializer` and `StoredOutboxEvent`** in `shared.infrastructure`, without versioning yet (`schemaVersion` always 1). `OutboxPayloadSerializerTest`: a round-trip, and an unknown type throwing a descriptive exception.
- [ ] 2.2. **The port returns rows.** `OutboxEventStore` becomes `save(StoredOutboxEvent)`, `fetchPending()` returning rows, and `markAsProcessed(UUID, Instant)`.
  - `OutboxEventSubscriber` serializes.
  - Both adapters store and return raw rows. `OutboxEventMapper` and `OutboxDocumentMapper` are deleted, and both module configs wire an `OutboxPayloadSerializer` bean.
  - The processor still reads **all rows before the loop**. This keeps today's behaviour on purpose, so 2.3 can go red.
  - The shared fake moves to the new port.
  - Adapter, subscriber, dispatcher and transactional tests are adjusted, and the full suite is green.
- [ ] 2.3. **Red:** `OutboxEventProcessorTest`: a row with an unreadable payload is saved before a valid one, and the valid one is still published. It fails, because reading happens outside the `try`. **Green:** move `serializer.read` inside the per-event `try`.
- [ ] 2.4. **Live lists:** rewrite [Known Bug 2](../../../docs/KNOWN_ISSUES.md) to say that a row that can't be read now only blocks itself, and that the endless retry is what remains. In Known Bug 3, drop the "wedges the whole outbox" sentence.

### Increment 3: Retry, backoff and dead-letter (closes Known Bug 2)
- [ ] 3.1. **Red → green:** `OutboxRetryPolicy` and `OutboxRetryPolicyTest`: first retry after `initialDelay`, doubling, capped at `maxDelay`.
- [ ] 3.2. **Red → green**, `OutboxEventProcessorTest` with a fixed `Clock`, one test at a time:
  - a failure records an attempt with `nextAttemptAt` from the policy and keeps the event pending;
  - a row that isn't due yet is not dispatched;
  - the `maxAttempts`-th failure dead-letters the event, and it is never fetched again;
  - a success after earlier failures marks the event processed.

  The port gains `fetchDue(Instant)`, `recordFailedAttempt` and `markAsDeadLettered`, replacing `fetchPending()`, and the fake implements them.
- [ ] 3.3. **Adapters:** Mongo fields and `$inc`/`$set` updates; JPA columns in `V1__init.sql`, entity fields and a repository query. **Red → green** adapter tests for `fetchDue`, covering due, not due, processed and dead-lettered rows. Tests alongside for the counters, `lastError` and timestamps being written.
- [ ] 3.4. **Wiring:** the `outboxClock` bean; the dispatchers build the processor with serializer, policy (8 attempts / 5s / 5 min) and clock; dispatcher tests adapted.
- [ ] 3.5. **`./mvnw clean verify`:** green, including `ArchitectureTest` and `ApplicationWiringTest`.
- [ ] 3.6. **Docs:** [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) gets the retry and dead-letter behaviour and the manual replay commands. [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) gets a worked-example row for the processor policy seam. [SETUP.md](../../../docs/SETUP.md) gets a note that `V1__init.sql` changed, so existing local volumes need `docker compose down -v`.
- [ ] 3.7. **Live lists:** remove Known Bug 2. Route the Deferred entries below (all of them relate to dead-lettering). Check [README.md](../../../README.md).

### Increment 4: Schema version and upcasters (closes Known Bug 3)
- [ ] 4.1. **`OutboxSchemaRegistry`** in `shared.infrastructure`, with a current version per type and upcasters registered per `(type, fromVersion)`. A bean in `SharedModuleConfig`. The serializer stamps the current version on `toStored`.
- [ ] 4.2. **Red:** `OutboxPayloadSerializerTest`: a test event registered at version 3, with upcasters 1→2 and 2→3, reads a version 1 payload into the current record; a missing upcaster throws a descriptive exception. **Green:** the upcaster loop in `read`.
- [ ] 4.3. **Adapters persist `schemaVersion`:** Mongo field, `V1__init.sql` column, adapter tests alongside.
  - Per-module registration helpers (`BasketOutboxEvents`, and the catalog equivalent) are called from the module configs.
  - Capture `BasketCheckedOut/v1.json` and `ProductCreated/v1.json`, and add a fixture test in `basket-core` and `catalog-core`.
- [ ] 4.4. **`./mvnw clean verify`:** green.
- [ ] 4.5. **Docs:** the compatibility policy in [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md). A worked-example row for the payload fixtures in [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md). Make sure SETUP.md's `down -v` note covers this change too.
- [ ] 4.6. **Live lists:** remove Known Bug 3. Update [Backlog 1](../../../docs/BACKLOG.md)'s "Realistic order", which names Known Bug 3 as the first step. Check [README.md](../../../README.md).

## Deferred
- **Dead-letter metric** (a Micrometer gauge such as `eshop.outbox.dead_lettered{module=…}`, needing a count query on the port). Decided with the user as an improvement → **Backlog**. It must not affect health status.
- **Replay operation** (an endpoint or admin command that requeues dead-lettered events). It needs authorization, which depends on Backlog 13's roles decision → **Backlog**.
- **Two instances polling the same outbox**: unchanged by this plan; already covered by [Known Bug 13](../../../docs/KNOWN_ISSUES.md)'s redelivery window. Nothing new to route.
- **Retry jitter**: not needed with a single scheduler per module. Dropped.
