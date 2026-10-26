# Design Notes

Options and open questions being thought about. **Nothing here is a decision or a commitment** — some of it may never be worth doing, and saying so is part of the point.

Where things go when they stop being ideas:

- becomes a decision, and the work is real → [BACKLOG.md](BACKLOG.md)
- turns out to describe something broken or unverified → [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
- becomes how this project actually works → [ARCHITECTURE.md](ARCHITECTURE.md)

Neither of the first two should ever point back here. This file exists so speculation has a home that isn't the backlog — a backlog holding things nobody committed to stops being read.

## Read Model Strategy

The query side currently returns Contract DTOs straight from the database (see [ARCHITECTURE.md](ARCHITECTURE.md) for the CQRS split as built). DTOs are a data-transfer shape rather than a read model, so this is a convenience: it works, and it leaves no place to put a projection that differs from the write model's shape.

A fuller separation would give the read side its own persisted or derived shape, driven off the write side:

1. **Plain view.** No synchronization needed — a view is a convenience query, always current by construction.
2. **Materialized view.** Data is persisted, so it needs a sync strategy:
    1. *Domain event listener* — refreshes whenever a relevant event is published. Reactive; minimizes the window between a write and the read model reflecting it.
    2. *Scheduled sync* — refreshes every N seconds/minutes/hours. Simpler, but the read model is stale for that interval.

**Open question, and the reason this stays a note:** whether a study project of this size ever justifies it. The current approach costs nothing and has not yet been the thing that hurt. Worth revisiting if a read ever needs a shape the write model genuinely cannot produce.

## Where an End-to-End Test Would Sit

[assembled-context-wiring-test](../ai-coding-tools/plans/assembled-context-wiring-test/assembled-context-wiring-test.md) closes a gap in wiring/assembly — do the modules compose into a working context — deliberately kept separate from behaviour. The reasoning, worth keeping findable now that the plan itself is closed:

- A true end-to-end run (HTTP in, real Keycloak token, checkout through to a persisted order) answers a different question — does the flow work for a user — and would catch a wiring break only as a side effect, reporting it as a 500 or a missing order rather than naming the ambiguous bean. Diagnostically worse for the thing the wiring test exists to catch.
- It is also a heavier tier: a token round-trip against Keycloak, and an inherent multi-second wait on `BasketOutboxDispatcher`'s `fixedDelay` unless the dispatcher is invoked directly, at which point it is no longer really end-to-end.
- [TESTING_CONVENTIONS.md](TESTING_CONVENTIONS.md) already reserves Cucumber/`.feature` files for acceptance tests "if this project ever grows that far." Bruno covers the manual version of this today, outside `mvn test`, and caught real bugs the last time checkout changed shape.

**Open question:** whether an automated end-to-end test is ever worth its cost here, or whether the manual Bruno pass stays sufficient for a project this size. Not decided; nothing currently blocks on it.

## Whether the Coverage Aggregator Needs a Guard

`sonar/pom.xml` decides which modules appear in the Jacoco aggregate report, and therefore in SonarCloud, by
listing them as dependencies. That list is a hand-maintained duplicate of the root pom's `<modules>`, and nothing
compares the two. `outbox-mongo` was missing from it for as long as the module existed; the build stayed green and
the coverage badge simply reported a smaller project than the one that exists. It was found by reading the file,
not by anything failing.

The failure is silent and it flatters: a module omitted here is not reported as uncovered, it is excluded from the
ratio, so the number goes *up* when a module goes missing. `jacoco:report-aggregate` offers no way out — its scope
comes from dependencies by design, so the duplication cannot simply be deleted.

Both mechanical guards cost more than they save:

- A `maven-enforcer-plugin` custom rule needs its own plugin artifact to live in. The in-process alternative,
  `evaluateBeanshell`, would have to read every module's pom from BeanShell — and this build has already been
  bitten by a plugin's bundled library not coping with Java 25 bytecode (see [BACKLOG.md](BACKLOG.md), the
  test-scope guard).
- A script in the GitHub Actions workflow only fails in CI, and shell-plus-grep over XML stops matching the
  moment a pom is reformatted.
- A JUnit test comparing the two poms would fail locally as well as in CI, but means test code reaching across
  module boundaries by relative path to parse XML — a construct every later reader has to justify to themselves,
  and one that breaks on legitimate restructures.

**Open question, and why this stays a note:** this protects the accuracy of a metric, not the correctness of the
system. Forgetting the next module costs a wrong percentage on a badge until someone notices — against roughly
sixty lines of machinery, on a project that gains a module perhaps twice a year. The list is correct as of
2026-09-09 (verified: ten directories in `sonar/target/site/jacoco-aggregate/`, one per code-bearing module).
Worth revisiting if modules start arriving often, or if the coverage number ever gates something that matters.

## Making the JDK Requirement Fail Legibly

A clean checkout built with an older JDK fails at `catalog-contract` with:

```
Fatal error compiling: error: invalid target release: 25
```

The message never mentions Java 25, an installation, or what to do. The pin that makes it work on
the author's machine is `.java-version`, which is deliberately gitignored — it names a locally
installed jenv alias, so committing it would break anyone using a different version manager. That
exclusion is right; the unhelpful failure is the cost of it.

[SETUP.md](SETUP.md) now states the prerequisite first and quotes the error, which is the cheap half
of the fix. The other half would be making the build say it: `maven-enforcer-plugin`'s
`requireJavaVersion` with a custom message fails before the compiler does and can name the actual
requirement.

**Open question, and why this stays a note:** whether a study project wants an enforcer plugin in
the build for one error message a reader hits once. Documented-and-loud may be enough. Worth
revisiting if anyone actually reports the confusing failure, or if the project ever gains a
toolchains-based build.

## A Checkout Identity Owned by Basket

Ordering deduplicates redelivered checkout events with a unique `checkoutId` on `Order`: a redelivery collides on a unique sparse index and is skipped. As decided on 2026-09-14 in [one-order-per-checkout](../ai-coding-tools/plans/one-order-per-checkout/one-order-per-checkout.md), the ordering listener fills `checkoutId` from the integration event's `eventId`. That is sound as long as its premises hold: one checkout emits exactly one `BasketCheckedOut`, and its id is never regenerated — it is the outbox document's `_id`, and `BasketOutboxDispatcher` copies it into `BasketCheckoutIntegrationEvent` (pinned by `BasketOutboxDispatcherTest`). The decision "a checkout is identified by its event" is made in one place only, the listener.

The fuller shape: basket mints its own `checkoutId` in `ShoppingCart.checkout`, carries it on `BasketCheckedOut` and `BasketCheckoutIntegrationEvent`, and the listener reads that field instead. `Order`, `OrderDB`, the index and the dedup logic would not change.

What it would cost:

- `ShoppingCart`, `BasketCheckedOut`, `BasketCheckoutIntegrationEvent` and `BasketOutboxDispatcher`, plus their tests — three modules, and a change to the public contract between basket and ordering.
- A change to `BasketCheckedOut`'s shape, which outbox payloads already at rest do not have. Under the outbox's compatibility policy ([Architecture](ARCHITECTURE.md), *Payload versioning*) that is a version 2 with an upcaster from version 1, and the upcaster has to decide what `checkoutId` an old payload gets. Copying its `eventId` keeps dedup working for every event in flight at deploy time, and means the current identity survives as the compatibility path either way.

**Open question, and why this stays a note:** nothing would read the id today. `CheckoutBasketHandler` deletes the cart straight after checkout and `POST /basket/checkout` returns `202` with no body, so a basket-minted `checkoutId` would exist only on the event — a second random UUID, distinguishable from `eventId` only by its name. Worth revisiting when any of these appear:

- a second event about the same checkout (e.g. `CheckoutCancelled`), whose own `eventId` identifies nothing ordering can look up;
- basket keeping a checkout history;
- the customer receiving a checkout reference;
- anything that re-mints event ids for existing checkouts — a replay tool, or integration events getting ids of their own.

## Ownership Filtering as Infrastructure

Ordering scopes every read to the caller explicitly: `OrderQueryRepository` offers only `ordersVisibleTo(CurrentUser viewer, ...)` and `findVisibleTo(CurrentUser viewer, ...)`, so a handler that forgets the viewer does not compile. Decided on 2026-09-16 in [order-endpoints-scoped-to-customer](../ai-coding-tools/plans/order-endpoints-scoped-to-customer/order-endpoints-scoped-to-customer.md), with orders as the only owned resource — basket is keyed by username as its `@Id`, and catalog has no owner.

The alternative is to make ownership a property of the data and enforce it in one place, so no individual query can forget it. The shape worth growing into:

- A shared owned-resource type in `shared.infrastructure` carrying an `owner` field, with a common query fragment appended to each query — the same pattern `JpaAuditMetadata` already follows for audit fields.
- **Composition, not inheritance.** `OrderDB` and `ShoppingCartDB` are records, and a record cannot extend a class. Use an embedded component (the way `ProductDB` attaches `JpaAuditMetadata` with `@Embedded`) or an interface a record can implement, such as `interface OwnedResource { String owner(); }`.

What building it would run into:

- **The shared field is the cheap half.** Appending the filter does not generalize across this project's two engines. Hibernate's `@Filter` rewrites JPA queries automatically once enabled per session; Spring Data MongoDB has no equivalent hook, which leaves either a helper each query must remember to call — the forgetting problem again — or a custom repository base class. One shared field, two unrelated enforcement mechanisms.
- **`owner` is not `customer`.** `Order.customer` is domain vocabulary: who placed the order. `owner` is access control: whose data it is. Equal today, free to diverge — an agent placing an order on a customer's behalf, a household account. The domain should keep its own word.
- **Ambient identity has its own costs.** Reading the current user inside the repository, rather than passing it in, needs a `SecurityContext` in every adapter test and a fake that imitates it, plus a bypass for anything without one — the outbox dispatcher's scheduler thread, and back-office access.
- **A shared kernel is a coupling point.** It would be the one thing modules share beyond contracts, so a change to ownership semantics ripples through every module at once.
- **Multi-tenancy is a different problem.** If isolation ever means separate clients on shared infrastructure rather than per-user ownership, push it below the application — Postgres row-level security, Hibernate `@TenantId` — rather than hand-rolling this.

A related, smaller question is where the policy sits once roles exist. With the viewer passed to the port, a back-office role would add a branch inside `MongoOrderQueryRepository` and again inside `FakeOrderQueryRepository`: authorization policy in infrastructure, twice. A sealed `OrderVisibility` (`OnlyPlacedBy(customer)` / `AllOrders`) computed by the handler would keep the decision in the application layer and leave the adapters translating a data description.

**Open question, and why this stays a note:** with one owned resource and one kind of rule ("equals the caller"), explicit scoped ports already give the "cannot be forgotten" property without any of the above. Worth revisiting when:

- a second module gains data owned by a user;
- visibility stops meaning "equals the caller" — sharing, delegated access, roles;
- the back-office branch lands in the adapters and the two implementations start to drift.

## An Event-Sourced Payment Module

Raised on 2026-10-25 while planning [tokenized-card-payment](../ai-coding-tools/plans/tokenized-card-payment/tokenized-card-payment.md), which charges the card synchronously at checkout through a `PaymentGateway` port inside basket, and refunds the charge if basket's transaction then fails.

Payment is the one place in this project where event sourcing fits the domain rather than being imposed on it. It is a ledger: a charge is never undone, a refund is a new entry pointing at it, and the questions people ask are about history — why was this customer charged twice, was the refund sent, what did we know when the order was accepted.

**Why not now.**

- **The provider is already the ledger.** Stripe keeps the charge and refund records with their own ids and timestamps, and they are the record that counts. The shop only keeps a reference on the order. Event-sourcing a mirror of that adds no knowledge.
- **The lifecycle is two steps:** charge, and refund if our own transaction fails. Event sourcing pays off when an aggregate goes through many transitions whose order and reasons matter.
- **An audit trail does not need event sourcing.** Current state plus an append-only history gives the trail. Event sourcing earns its cost where the events *are* the state: new views rebuilt from old history, or "what was the state at time T?".

**What would make it worth it.** Each of these adds transitions; together they make a real lifecycle:

- authorize-then-capture: authorized → captured, cancelled, or expired after 7 days;
- provider webhooks (`charge.refunded`, `charge.dispute.created`, `payment_intent.payment_failed`), which are external events and append naturally to a stream;
- partial refunds, disputes, chargebacks;
- a refund that fails after a failed checkout. Today that leaves the customer charged with no order and only an ERROR log. A `Payment` aggregate recording `RefundFailed` and retried until `PaymentRefunded` is the natural fix, though a persisted payment record with plain state would also fix it, and is the cheaper first step.

**Where the boundaries would be.**

- **One aggregate in its own module:** `Payment`, one per checkout (id = `checkoutId`), with a stream of events such as `PaymentRequested`, `PaymentCharged`, `PaymentDeclined`, `RefundRequested`, `PaymentRefunded`, `RefundFailed`. Basket and ordering stay state-based and cannot tell the difference.
- **Stored events are not integration events.** The stream is internal and fine-grained. Other modules receive coarser integration events through the outbox, as they do now. Publishing the stored events directly would make the module's own history a public contract it could never refactor.
- **The event store can be the outbox.** Appending to the stream *is* the write, and a dispatcher publishes integration events from it, so there is no dual write. The existing `outbox-core` dispatching, retry and dead-letter handling would carry over.
- **Versioning becomes permanent.** Stored events are never deleted, so every shape ever written must stay readable through upcasters. The "correct the event in place" exception taken for `BasketCheckedOut` (pre-release, decision 4 of the plan above) would not be available.
- **Replay must not call out.** `apply(event)` rebuilds state from the stream every time the aggregate loads, possibly years later. Deciding a command may read anything (another module's contract query, the provider, the clock), but the outcome goes into the event as data — `PaymentCharged(amount = 998.00, chargeReference = …)`, never "charged the order's total". The same as "handlers supply, aggregates decide" in [ARCHITECTURE.md](ARCHITECTURE.md), made mandatory. Referencing other aggregates by id is fine.
- **Nothing in a stream that may have to be deleted.** Card data is already kept out by tokenization. Personal data (billing name, email) is the hard part: an immutable log conflicts with the right to erasure. Keep it out of events and reference a customer id instead, or encrypt it per customer and delete the key (crypto-shredding).

**Decisions it would reopen.**

- **A `payment` module instead of a port inside basket.** The payment record has to outlive the cart, which basket deletes at checkout.
- **The synchronous charge.** Modules never call each other to change state, so basket could not call a payment module to charge. Checkout would become a saga driven by events (`BasketCheckedOut` → `PaymentRequested` → `PaymentCharged` / `PaymentDeclined` → order placed or checkout failed), and the customer would lose the immediate yes or no from `POST /basket/checkout`. The alternative, allowing this one synchronous call, would be a written exception to the cross-module rule.

**On event sourcing only part of the application.** It is a per-aggregate (or per-context) persistence choice, not an architecture for the whole system, and this project's module isolation makes it cheaper than usual: each module already owns its storage, communicates by outbox events and contract queries, and has no cross-module transactions to lose. An event-sourced module answers queries from projections, which is the CQRS read side the project already separates. It is eventually consistent, as all cross-module data here already is.

**Open question, and why this stays a note:** the payment work as planned is a single synchronous charge with a refund as compensation, and the provider holds the authoritative record. Worth revisiting when webhooks, authorize-then-capture, or retried refunds arrive; as a showcase, an event-sourced payment context is defensible precisely because it can say why here and not in basket, catalog or ordering.

## A Central Audit Module

Raised on 2026-10-26, alongside the event-sourcing question above. Nothing records the history of anything today. A successful checkout leaves an order, and the payment provider keeps its own charge records, but a declined checkout leaves no trace in this system at all. What exists is incidental:

- `JpaAuditMetadata` records who created and last modified a catalog product: the latest change only, not a history. Mongo has no equivalent yet ([BACKLOG.md](BACKLOG.md), `MongoAuditMetadata`).
- The basket outbox holds every `BasketCheckedOut` with a timestamp, but nothing guarantees those rows are kept, and they were never meant to be read as a record.

A production shop would want the **full history** of checkouts, payments and data changes, in one place: when an auditor or a dispute asks, the cost is gathering evidence from five systems, not writing it.

**Three things get called "audit", with different answers.**

| Kind | Question | Where it comes from |
|---|---|---|
| Business history | What happened to this checkout or payment, in what order, and why? | The application, written on purpose |
| Security audit | Who logged in, who changed what as an admin, from where? | Keycloak's login and admin events; application access records |
| Technical logs | What did the code do? | Application logs. Not an audit record: sampled, rotated, not written atomically with the change |

**The shape: several producers, one audit module.** Capture has to differ, because the facts arise in different places — inside Keycloak, in each module's handlers and repositories, and in code that sees an outcome that changes no state. Storage and reading should be one place: an `audit` module with its own store and one query and export surface.

- **Modules send, the audit module stores.** Modules share no databases, so audit records cross the boundary the way every other fact does: through each module's outbox.
- **A generic envelope, not each module's integration events.** If the audit module consumed `BasketCheckoutIntegrationEvent`, `ProductUpdated` and the rest, it would depend on every module's contract and change with each of them. Instead, one shape in `shared.contract`, for example `AuditRecord(occurredAt, module, subjectType, subjectId, action, actor, correlationId, before, after, details)`. Each module translates its own facts into it; the audit module stores records it does not need to understand.
- **Full history needs every change to emit a record.** Today only checkout and catalog's product changes raise events; adding or removing a basket item raises nothing. Two ways to close that, and in practice both are needed:
  - *Generic, in each module's infrastructure:* a repository decorator or persistence listener that diffs before and after on every save and writes an `AuditRecord` into the module's outbox in the same transaction. [JaVers](https://javers.org/) can produce the diff and supports both JPA and MongoDB. Covers every change automatically, but records *what* changed, never *why*.
  - *Explicit, in handlers:* records for business outcomes, above all those that change no state — a declined card, an unreachable provider, a refund that failed. No generic mechanism can see these, because nothing is saved.
- **Security events come from outside.** Keycloak's events would reach the module through its event-listener extension point or by polling its admin API. Leaving them in Keycloak is also a valid scope decision: two sources for an auditor, not five.

**Storage, and where Elasticsearch fits.** Elasticsearch is strong at the reading side — search, filtering by time, user or subject, aggregations, and Kibana dashboards an auditor can use. As the store of record it is not transactional with the modules (the outbox already covers that) and documents are mutable by default; its *data streams* are append-only, with lifecycle policies for retention. Two shapes:

| Shape | Store of record | Elasticsearch |
|---|---|---|
| A. Database first | Append-only collection or table in the audit module; its database user can insert but not update or delete | A search index fed from it, rebuildable at any time |
| B. Elasticsearch only | Data streams | Store and search in one |

A is the cheaper start. The hard part of an audit trail is capturing every change reliably, not storing it; once capture works, storage sits behind the audit module's own port, and adding or switching to Elasticsearch changes one adapter. Elasticsearch also adds real weight to the compose stack (around a gigabyte of memory, plus Kibana for the UI).

**What makes it an audit log rather than a log.**

- Append-only: the application never updates or deletes entries.
- Written reliably: in the same transaction as the change, through the outbox, never best-effort afterwards.
- Says who: the user, or the service for scheduler-driven work such as the outbox dispatcher, plus a correlation id (`checkoutId` for checkouts and payments).
- Tamper-evident only if it is ever required, by chaining a hash of each entry into the next.

**Personal data is the constraint to settle first.** A central store with full before/after history from every module becomes the most sensitive store in the system.

- *Retention:* financial records usually have a legally required period, which depends on the country; other data should expire (data-stream lifecycle policies, or a TTL index).
- *Erasure:* an append-only store conflicts with the right to erasure, as with event sourcing above. Keep personal data out of `before` and `after` where possible (ids instead of names and emails), or encrypt it per customer and delete the key.
- Card data never enters it, which tokenization already guarantees.

**Considered and not chosen as the main mechanism.**

- *Audit columns* (created and modified by and at): only the last change.
- *Hibernate Envers:* full history, but JPA only, so catalog only.
- *Change data capture* (Debezium, MongoDB change streams): every row change with no application code, but state only, no intent and no failures, and real infrastructure to run.
- *Database-level auditing* (`pgAudit`; MongoDB auditing is an Enterprise feature): answers "who ran which query", a DBA concern rather than a business one.
- *Reconciliation against the payment provider* is not an alternative but a complement: a scheduled check that every provider charge has an order or a refund. It is how a shop catches a charge whose refund failed.

**Open question, and why this stays a note:** nobody has decided to build it, and its cost is mostly in capture — a generic envelope, a change-capturing mechanism in each module, and explicit records for outcomes that change no state — before the audit module itself stores anything. Worth turning into a Backlog entry when checkout and payment history is needed for real (disputes, an external audit), or when the first module needs a full history of its own data, such as catalog price changes.

## Directions Worth Exploring

Ideas carried over from the README's former "Extending the project" checklist. Each is a direction, not a question worked through yet; one that gets thought through earns its own section above.

- **Spring AI:** examples of how Spring AI could be used within this application.
- **RAG:** a retrieval-augmented generation example on top of the application's data.
- **API-level acceptance tests with Cucumber/Serenity:** executable scenarios against the HTTP API only.
- **Contract testing:** verifying the shapes modules exchange, so a change on one side cannot silently break the other.
- **Extract a module into a separate service:** one module moved out of the monolith, communicating over RabbitMQ.
- **Kafka as the communication layer:** replacing in-process events with Kafka topics.
- **Event sourcing with AxonFramework or KurrentDB:** persisting aggregates as event streams rather than current state. Payment, the likeliest candidate, is worked through in *An Event-Sourced Payment Module* above.
- **Performance testing:** exercising the application at scale.
- **Keycloak policy enforcer:** moving authorization decisions into Keycloak.
- **Roles in Keycloak vs. roles in the application:** where role definitions and checks should live.
- **The outbox as a Spring Boot starter:** auto-configuration (with `AutoConfiguration.imports`, conditional beans and configuration metadata) instead of each module declaring its outbox beans. The tension is that a Boot auto-configuration naturally creates one bean of each kind, while this project needs one outbox per module, each bound to that module's own database, template and transaction manager. So the starter would need a per-module registration that it expands into a store, a subscriber and a dispatcher. Spring Modulith's Event Publication Registry is the existing starter of this kind, and [SPRING_MODULITH.md](SPRING_MODULITH.md) explains why its single registry doesn't fit per-module databases.
- **[Mongock](https://mongock.io/) for MongoDB:** data migrations for the Mongo modules, as the counterpart to Flyway's schema migrations on Postgres.
- **Immutable aggregates:** making every aggregate immutable, for thread safety at the domain level.
- **Kustomize:** sample deployments of the `Kubernetes/` manifests built with Kustomize overlays.
- **GitOps with ArgoCD:** an example of deploying the application through ArgoCD.
