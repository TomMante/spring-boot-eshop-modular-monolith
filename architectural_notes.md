AI generated, needs to be reviewed and refined.

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


