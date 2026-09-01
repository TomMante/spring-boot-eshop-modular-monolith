# Architecture

## Architecture Highlights

- Modules are isolated as if they were microservices-in-waiting
- Each module can own its own database rather than sharing a schema (JPA/Postgres for some, MongoDB for others)
- Cross-module communication happens only through Spring ApplicationEvents, not direct calls or shared tables
- Domain is fully decoupled from infrastructure via ports/adapters

## Architectural Trade-offs

This project deliberately pushes module isolation further than most production systems would need:
- Each module can own its own database (JPA/Postgres for some, MongoDB for others) rather than sharing a schema
- Cross-module communication happens only through Spring ApplicationEvents, not direct calls or shared tables

This buys near-mechanical extraction into microservices later, at the cost of losing cross-module transactions and taking on real distributed-systems problems (e.g. dual writes, eventual consistency) inside what is still a single deployable.

In a real production system, I'd default to a shared-schema modular monolith with enforced module boundaries, and only isolate a module's storage when there's a concrete reason — e.g. a genuinely different data shape (graph/document data that doesn't fit relational), a compliance requirement for data isolation, or clear evidence a module needs independent scaling. Here, I isolated storage across the board on purpose, specifically to explore what that costs.

### Domain event publication
In this repository we can see how the repositories (JPA/Mongo) are in charge of broadcasting the event. This is not the ideal solution, as it breaks the Single Responsibility Principle.
Better solutions: 
1. Leverage the @DomainEvents annotation from Spring boot: this would require the outbox logic to be moved to the domain event lister.
2. Create a new AOP interceptor that deals with it: this would allow to have both the event and outbox to be published in the same place.
3. Explicit per-module wiring of the outbox to avoid cross-module bean conflicts. A future enhancement could be a `@EnableMongoOutbox` (or similar) custom annotation backed by an `ImportBeanDefinitionRegistrar` to automatically register module-prefixed outbox beans, avoiding conflicts by construction. Until several modules of the same type need an outbox, explicit per-module wiring is preferred for clarity.

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
| `placedAt` on an order | handler holds the `Clock`, `Order.create` receives the `Instant` | An aggregate must not read ambient time |

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

## 🧩 Shared module structure

The `shared` module contains reusable building blocks that are used across bounded contexts and vertical slices.  
It is divided into clear conceptual packages that reflect architectural responsibilities.

---

## 📦 shared.ddd

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
> Code in `shared.ddd` must not depend on infrastructure or contract code.

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
- Error/Problem response models

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

---

## 🧭 Architectural intent

This separation enforces:

- **Dependency direction:**  
  Infrastructure → Domain → Contract  
  (never the reverse)

- **Framework isolation:**  
  Spring and JPA are implementation details, not part of the model

- **API stability:**  
  Changing persistence technology does not affect API contracts

- **DDD correctness:**  
  Domain logic is expressed using explicit DDD building blocks

- **CQRS correctness:**  
  Queries return contract DTOs, not infrastructure or domain types

---

## 🧠 Mental model

| Layer | Think of it as | Example |
|-------|----------------|---------|
| shared.ddd | Domain primitives | `AggregateRoot`, `DomainEvent` |
| Domain | Business rules | `Product`, `Order`, `Money` |
| Contract | System boundary | `ProductDto`, `PaginatedResult`, `GetProductsResponse` |
| Infrastructure | Technical plumbing | `ProductEntity`, `AuditMetadata`, `JpaRepository` |