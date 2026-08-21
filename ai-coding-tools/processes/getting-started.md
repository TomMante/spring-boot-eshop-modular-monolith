This file provides strict guidance for AI Agents (like WARP) working in this repository.

## 📚 Project Documentation
Before making architectural decisions, refactoring, or writing tests, consult the following documents in the `docs/` directory:
- [ARCHITECTURE.md](../../docs/ARCHITECTURE.md) - Core design (DDD, CQRS, VSA, Module Isolation, Shared Module Rules).
- [DEVELOPMENT.md](../../docs/DEVELOPMENT.md) - Active guidelines, validation, and read-model strategies.
- [TESTING_AND_BUGS.md](../../docs/TESTING_AND_BUGS.md) - Current testing gaps and known issues.
- [TESTING_CONVENTIONS.md](../../docs/TESTING_CONVENTIONS.md) - Current test naming (GWT in method names) and intended later evolution.

## 🛠️ Build & Test Commands
The main Java application is in the `SpringBoot-EshopModularMonolith` directory.
You must execute all Maven commands from within that directory OR use the `-f` flag.

- **Build:** `mvn clean install -f SpringBoot-EshopModularMonolith/pom.xml`
- **Test All:** `mvn test -f SpringBoot-EshopModularMonolith/pom.xml`
- **Test Single:** `mvn test -Dtest=TestClassName -f SpringBoot-EshopModularMonolith/pom.xml`
- **Coverage:** `mvn clean verify -f SpringBoot-EshopModularMonolith/pom.xml` (Generates Jacoco aggregate report).

## 🏗️ Core Constraints (Quick Reference)
- **Strict Module Isolation:** Modules act like independent microservices. They DO NOT share databases.
- **Cross-Module Communication:** Exclusively via Spring `ApplicationEvents`. No direct method calls between modules.
- **CQRS Separation:**
    - **Write:** Rich DDD aggregates.
    - **Read:** Return Contract DTOs directly from the DB. Bypasses the domain entirely.
- **Dependency Rules (`shared` module):** `Infrastructure` → `Domain` → `Contract`.
    - `shared.ddd`: Pure, framework-agnostic. No infra/contract dependencies.
    - `shared.contract`: Public shapes (DTOs). No domain/infra dependencies.
    - `shared.infrastructure`: Spring/JPA concerns. Never depended on by domain/contract.

## 🧪 Testing Guidelines
- **Domain Isolation:** Core domain aggregates (e.g., in `shared.ddd` or `domain/model`) MUST have their own isolated unit tests. Do not rely on Application/Handler layer tests for domain coverage.
- **Behavior Over Lines:** Focus on testing business invariants and failure paths (e.g., error handlers, missing entities, domain event listeners firing) rather than line coverage.
- **Naming & Structure (summary):** Follow the current suite: Given/When/Then in the test method name. Do not mix in outcome-only names, and do not start a rename. The later style (GWT as `@Nested` structure, method = outcome only) is described in [TESTING_CONVENTIONS.md](../../docs/TESTING_CONVENTIONS.md) and planned in [gwt-test-naming-later-refactor](../plans/gwt-test-naming-later-refactor/gwt-test-naming-later-refactor.md).

# Interacting with Me
