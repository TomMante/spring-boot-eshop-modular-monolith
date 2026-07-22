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