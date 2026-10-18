# Spring Modulith

> **Status: to review.** The author has not yet fully reviewed this comparison. Treat its Spring Modulith claims as unconfirmed until this note is removed.

[Spring Modulith](https://spring.io/projects/spring-modulith) is a Spring project for building modular monoliths. Several things this repository builds by hand — the transactional outbox, the module-boundary checks, module-scoped tests — are features Spring Modulith ships out of the box.

This project does **not** use Spring Modulith, and this document is **not** a plan to adopt it. It exists so a reader knows the framework is there, and can see which parts of this codebase it would replace and which it would not. Feature references are to Spring Modulith 2.x.

## Side by side

| Concern | This project | Spring Modulith |
|---|---|---|
| **Module boundaries** | `ArchitectureTest` (ArchUnit) fails the build if a module reaches past another module's `contract` package. It also checks layers *inside* a module (domain free of Spring and infrastructure). Maven's module split adds compile-time enforcement on top. | `ApplicationModules.verify()` (also ArchUnit) rejects access to another module's internal types and cyclic dependencies between modules. A `contract` package would be a *named interface*. Modulith does not check layers inside a module unless jMolecules is added, and has no compile-time enforcement. |
| **Outbox: what gets recorded** | The project stores **one outbox entry per event**, marked processed once the dispatcher has published it. | Modulith's Event Publication Registry stores **one entry per event-and-listener pair**, marked completed when that listener finishes. Two listeners on one event give two entries, each tracked separately. |
| **Outbox: when it is delivered** | A `@Scheduled` dispatcher per module (`BasketOutboxDispatcher`, `CatalogOutboxDispatcher`) polls the outbox every 5 seconds. | Modulith runs the listener (`@ApplicationModuleListener`) asynchronously right after the commit. A failed entry is not retried automatically: resubmitting on restart (`spring.modulith.events.republish-outstanding-events-on-restart`) and the staleness monitor are both off by default, and anything else goes through `IncompleteEventPublications` or `FailedEventPublications`, called by the application. |
| **Outbox: storage** | The project gives each module its own outbox, in that module's own database (MongoDB for basket, PostgreSQL for catalog), written in the same transaction as the module's state change. | Modulith keeps one registry per application, in one store (JPA, JDBC or MongoDB). |
| **Outbox: wiring** | The project declares outbox beans per module, explicitly (`@Qualifier("basketOutboxEventStore")` and so on). | Modulith needs no per-module beans: there is one registry, auto-configured. |
| **Poisoned events** | The project retries a failing event with exponential backoff (5 s doubling to a 5 min cap) and dead-letters it on its 8th failure, with a documented manual replay ([Architecture](ARCHITECTURE.md), *Retry and dead-letter*). | Modulith's registry has the building blocks: a publication lifecycle with `FAILED`, staleness detection, and (since 2.2) `ABANDONED` through an `AbandonPolicy`. None of them is active by default: `abandon-after-attempts` defaults to `-1`, so without a policy nothing is ever abandoned. A failing event is retried only when the application resubmits it, and dead-lettered only once a policy is configured. |
| **Payload versioning** | Every outbox row carries a schema version; older payloads are upcast on read, and captured fixtures prove they still read ([Architecture](ARCHITECTURE.md), *Payload versioning*). | Modulith does not solve this: its registry stores the serialized event keyed by its type, with no version. |
| **Duplicate delivery** | Ordering drops redeliveries through a unique index on `checkoutId`. | Modulith also delivers at least once, so its listeners must be idempotent too. |
| **Module tests** | `ApplicationWiringTest` loads the assembled context; modules otherwise use slice tests with hand-built configurations. | `@ApplicationModuleTest` boots a single module (optionally with its dependencies); the `Scenario` API and `PublishedEvents` assert on the events a module publishes. |
| **Diagrams** | A hand-written Mermaid diagram in the [README](../README.md). | Modulith's `Documenter` generates module diagrams from the code. |
| **Sending events to a broker** | Not built. Listed as a direction in [Design Notes](DESIGN_NOTES.md) (Kafka, RabbitMQ). | Modulith's event externalization (`@Externalized`) forwards events to Kafka, AMQP, JMS, SQS or SNS. |
| **Observability** | Not built. | Modulith provides module-level tracing through Micrometer. |

## Where Spring Modulith does not fit this project

**Per-module database isolation.** This project gives every module its own database on purpose (see [trade-offs](ARCHITECTURE.md#architectural-trade-offs)), and each module's outbox lives in that module's database so the outbox write shares the module's transaction. Spring Modulith's registry is a single store for the whole application. Basket commits to MongoDB and catalog to PostgreSQL, so one registry cannot share a transaction with both: whichever module's database the registry is not in would be back to a dual write — the exact problem this project's per-module outbox exists to prevent.

In a shared-schema modular monolith — the default [Architecture](ARCHITECTURE.md#architectural-trade-offs) recommends for production — that objection disappears, and Spring Modulith's registry could replace this project's outbox classes (`OutboxEventSubscriber`, `OutboxEventStore` and its adapters, `OutboxEventProcessor`, the per-module dispatchers). Delivery would change with them: listeners running right after the commit instead of a 5-second poll, and retries only where the application turns them on (see the rows above).
