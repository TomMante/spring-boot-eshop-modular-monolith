# Outbox Dispatcher Per-Event Isolation

## Context

[Known Bug 2](../../../docs/KNOWN_ISSUES.md) (basket module): `BasketOutboxDispatcher.dispatch()` publishes an event and only then calls `markAsProcessed`. `InMemoryMessageBus.publish` calls `ApplicationEventPublisher.publishEvent` synchronously, so a listener exception propagates straight out of `dispatch()` — the poisoned event is never marked processed (so it retries every 5s forever) and, because the loop stops there, every later event in that batch is silently skipped too. There is no dead-letter path and no retry limit; the HTTP caller that triggered the original write sees a 202 and never learns anything failed.

Investigated whether this shape exists elsewhere before scoping the fix: [CatalogOutboxDispatcher.java](../../../SpringBoot-EshopModularMonolith/modules/catalog/catalog-core/src/main/java/com/github/eshopmodularmonolith/catalog/products/infrastructure/persistence/jpa/CatalogOutboxDispatcher.java) has the identical loop shape (publish-then-markAsProcessed, no try/catch) — same defect. Ordering has no outbox dispatcher at all yet (`Order` never calls `addDomainEvent`, tracked separately as [Backlog item 5](../../../docs/BACKLOG.md), deliberately deferred pending a real consumer), so it isn't affected today — but it's a third instance of this exact shape waiting to happen the moment that trigger fires.

Known Bug 2's own Action Required frames two things: isolating each event so one failure doesn't stop the batch, and separately deciding long-term retry semantics (attempt counter + dead-letter state) for the poisoned event itself. Confirmed with the user: this plan does **isolation only** — the poisoned event still retries every 5 seconds indefinitely after this fix, it just no longer takes its neighbors down with it. Attempt-counting/dead-lettering would additionally touch the `OutboxEventStore` port and both persistence adapters — out of scope here, left as the narrowed remainder of Known Bug 2.

Raised during planning: since the fetch-loop-isolate-mark shape is identical in both existing dispatchers and would otherwise need to be hand-copied into every future one, this plan extracts it into a shared collaborator rather than duplicating the fix twice. Considered an abstract base class (template method) first; chose composition instead, since it's the idiom the rest of this codebase already uses for behavior sharing (`OutboxEventStore`, `MessageBus`, repositories are all interfaces injected into plain classes — no existing behavior-sharing class hierarchy) and it keeps `MessageBus` a direct, visible field of each dispatcher instead of something reached through a protected base-class accessor.

## Design decisions

- **New `OutboxEventProcessor` in `shared.infrastructure`** (same package as `MessageBus`, `OutboxEventStore`, `OutboxEventSubscriber`) — a plain, non-abstract class. It owns `OutboxEventStore` and implements the fetch-loop-isolate-mark logic once as `public void dispatchPendingEvents(EventPublisher publish)` — fetch, then per event: `try { publish.publish(event); outboxEventStore.markAsProcessed(event); } catch (Exception ex) { log and move on }`.
- **New `EventPublisher` functional interface** in `shared.infrastructure`: `void publish(DomainEvent event)`. This is the seam each module plugs its own "which event maps to what outgoing message" logic into, passed as a method reference.
- **`BasketOutboxDispatcher` and `CatalogOutboxDispatcher` hold an `OutboxEventProcessor` field** (constructed from their own `@Qualifier`-ed `OutboxEventStore`) alongside their existing `MessageBus` field — no inheritance, no change to Spring wiring or bean qualifiers. Each keeps its own `@Scheduled(fixedDelay = 5000) public void dispatch() { outboxEventProcessor.dispatchPendingEvents(this::publish); }` (same cadence as today); `publish(DomainEvent event)` becomes a private method holding exactly the `instanceof` check + mapping + `messageBus.publish(...)` call each already has today. No change to the public `dispatch()` contract either dispatcher's tests currently exercise.
- **Logging convention:** `private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class)` via `org.slf4j`, matching the existing pattern in [BasketCheckedOutHandler.java](../../../SpringBoot-EshopModularMonolith/modules/basket/basket-core/src/main/java/com/github/eshopmodularmonolith/basket/basket/application/features/checkoutbasket/BasketCheckedOutHandler.java). Log at `error` with the event's `eventId()` and `eventType()` — both available on every `DomainEvent`, so `OutboxEventProcessor` needs no per-event-type-specific fields.
- **Not adding retry/attempt-count/dead-letter state** — that's the explicitly deferred half of Known Bug 2, requiring an `OutboxEventStore` port change; recorded back into the doc, not silently dropped.

## Testing rigor

- **Isolation logic itself** (the try/catch loop, later `OutboxEventProcessor.dispatchPendingEvents`): strict TDD red → green, proven once via `BasketOutboxDispatcher` (new deterministic control-flow with real regression risk).
- **Extracting to the shared collaborator**: a behavior-preserving refactor — done once basket's isolation test is green, verified by basket's existing `BasketOutboxDispatcherTest` (both the original dispatch test and the new isolation test) staying green unchanged after the extraction.
- **Applying `OutboxEventProcessor` to `CatalogOutboxDispatcher`**: tests-alongside-implementation — this now mirrors an already-established, already-proven pattern (basket's), not new logic.

## Todo List

### 1. Prove per-event isolation in `BasketOutboxDispatcher` (TDD)
- [ ] Red first: in `BasketOutboxDispatcherTest`, save two `BasketCheckedOut` events to the (existing) `FakeOutboxEventStore`; stub `messageBus.publish(...)` to throw for the first event's translated `BasketCheckoutIntegrationEvent` and succeed for the second. Assert `dispatcher.dispatch()` does not throw, the second event is published and marked processed (absent from `fetchUnprocessedEvents()`), and the first remains in `fetchUnprocessedEvents()` (unprocessed, retried next run).
- [ ] Green: wrap the per-event work in `BasketOutboxDispatcher.dispatch()`'s loop in `try { ...; markAsProcessed(event); } catch (Exception ex) { logger.error(...); }`, with a `logger` field on the class.
- [ ] Full suite green.

### 2. Extract `OutboxEventProcessor` into `shared.infrastructure` (refactor)
- [ ] Add `EventPublisher` functional interface (`void publish(DomainEvent event)`) and `OutboxEventProcessor` (constructor takes `OutboxEventStore`; `public void dispatchPendingEvents(EventPublisher publish)` with the try/catch loop from Step 1).
- [ ] `BasketOutboxDispatcher`: add an `OutboxEventProcessor` field built from its own `OutboxEventStore`; keep the `MessageBus` field as-is; `dispatch()` becomes `@Scheduled(fixedDelay = 5000) public void dispatch() { outboxEventProcessor.dispatchPendingEvents(this::publish); }`; move the `BasketCheckedOut` instanceof/mapping/`messageBus.publish` line into a new private `publish(DomainEvent event)` method.
- [ ] `BasketOutboxDispatcherTest` from Step 1 stays green with no test changes — confirms the extraction preserved behavior.
- [ ] Full suite green.

### 3. Apply `OutboxEventProcessor` to `CatalogOutboxDispatcher`
- [ ] `CatalogOutboxDispatcher`: same shape — an `OutboxEventProcessor` field, `dispatch()` delegates via `this::publish`, the existing `ProductCreated` instanceof/mapping/`messageBus.publish` line moves into a private `publish(DomainEvent event)` method.
- [ ] `OutboxDispatcherTest`: add the same-shape isolation test as Step 1 (two `ProductCreated` events, first's `ProductCreatedMessage` publish throws, second succeeds) — mirrors an established pattern, written alongside the change rather than red-first.
- [ ] Full suite green.

## Verification
- `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green.
- Update [docs/KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) Known Bug 2: broaden **Location** to `OutboxEventProcessor` (shared) plus both concrete dispatchers, narrow **Issue**/**Impact** to reflect that a failure no longer stops the batch or blocks unrelated events, and narrow **Action Required** to just the still-open half — deciding attempt-counter/dead-letter semantics so a poisoned event doesn't retry forever with no visibility beyond a log line. Do not remove the entry; the bug is only partially closed.
- Checked [README.md](../../../README.md) — no mention of outbox/dispatch behavior, nothing to update.

## Explicitly deferred
- Attempt-counter / dead-letter state for a permanently-poisoned event — confirmed out of scope with the user; recorded as the narrowed remainder of Known Bug 2 rather than dropped.
