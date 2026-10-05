This file provides strict guidance for AI Agents (like WARP) working in this repository.

## 📚 Project Documentation
Before making architectural decisions, refactoring, or writing tests, consult the following documents in the `docs/` directory:
- [ARCHITECTURE.md](../../docs/ARCHITECTURE.md) - Core design (DDD, CQRS, VSA, Module Isolation, Shared Module Rules).
- [SPRING_MODULITH.md](../../docs/SPRING_MODULITH.md) - Which hand-built parts Spring Modulith provides out of the box, and where it does not fit. Awareness only; the project does not use it. Not yet reviewed by the author — verify a Spring Modulith claim before relying on it.
- [DESIGN_NOTES.md](../../docs/DESIGN_NOTES.md) - Options and open questions only. Nothing in it is a decision; do not treat it as direction.
- [KNOWN_ISSUES.md](../../docs/KNOWN_ISSUES.md) - Known defects and testing gaps in the current codebase.
- [BACKLOG.md](../../docs/BACKLOG.md) - Work deliberately postponed: missing capabilities and unmade decisions.
- [TESTING_CONVENTIONS.md](../../docs/TESTING_CONVENTIONS.md) - What to test and where (behavior seams, fakes at ports, when to narrow), plus current test naming (GWT in method names) and its intended later evolution.

**These last two are the only live lists of outstanding work.** Plan documents under `ai-coding-tools/plans/` are frozen history — a plan's *Deferred* section records what that increment decided, not what is still open. When asked what work remains, read the two files above; do not reconstruct an answer by scanning plans.

Use the [open-points](../skills/open-points/SKILL.md) skill to turn those two files into a prioritized checklist on demand, rather than re-deriving priority by hand each time.

## 🛠️ Build & Test Commands
The main Java application is in the `SpringBoot-EshopModularMonolith` directory,
which is also where the Maven wrapper lives. Run commands from there — the wrapper
pins the Maven version, so use `./mvnw`, not whatever `mvn` is on the PATH.

```bash
cd SpringBoot-EshopModularMonolith
```

- **Build:** `./mvnw clean install`
- **Test All:** `./mvnw test`
- **Test Single:** `./mvnw test -Dtest=TestClassName`
- **Coverage:** `./mvnw clean verify` (Generates Jacoco aggregate report).

From the repository root, add `-f SpringBoot-EshopModularMonolith/pom.xml` and call the
wrapper by path: `SpringBoot-EshopModularMonolith/mvnw clean verify -f SpringBoot-EshopModularMonolith/pom.xml`.

**When regenerating the wrapper** (`./mvnw wrapper:wrapper -Dtype=only-script -Dmaven=<version>`),
check `.mvn/wrapper/maven-wrapper.properties` afterwards: the plugin writes whatever mirror
the local `settings.xml` defines into `distributionUrl`, and that file is committed. It must
point at `https://repo.maven.apache.org/maven2/`.

## 🏗️ Core Constraints (Quick Reference)
- **Strict Module Isolation:** Modules act like independent microservices. They DO NOT share databases.
- **Cross-Module Communication:** State changes cross modules only as Spring `ApplicationEvents` through an outbox. Reading another module's current data goes through a query interface in that module's `contract` package, reached via a domain port and an adapter in the caller's infrastructure (e.g. basket's `CatalogProductACL`). Never call another module to change its state. See [ARCHITECTURE.md](../../docs/ARCHITECTURE.md#-cross-module-communication).
- **CQRS Separation:**
    - **Write:** Rich DDD aggregates.
    - **Read:** Return Contract DTOs directly from the DB. Bypasses the domain entirely.
- **Dependency Rules (`shared` module):** Domain and Contract are independent of each other and of Infrastructure. Enforced by `ArchitectureTest` in `starter`.
    - `shared.domain`: Pure, framework-agnostic. No infra/contract dependencies.
    - `shared.contract`: Public shapes (DTOs). No domain/infra dependencies.
    - `shared.infrastructure`: Spring/JPA concerns. Never depended on by domain/contract.

## 🧪 Testing Guidelines
- **What to Test (summary):** Test at the behavior seam — the narrowest entry point that is *stable* (its shape follows a requirement, not an implementation choice) and *complete* (exercising it produces the observable outcome). The unit under test is a behavior, not a class; which layer the seam sits in is a per-change question, so state it and justify it in the plan. Fake only at ports and assert outcomes, never that a collaborator was called. Narrow the seam when the logic behind it has more cases than you would enumerate at its setup cost — calculations, invariants that must hold regardless of caller, state machines, merge/dedup logic. Full criteria, including test-data guidance and worked examples, in [TESTING_CONVENTIONS.md](../../docs/TESTING_CONVENTIONS.md).
- **Behavior Over Lines:** Focus on testing business invariants and failure paths (e.g., error handlers, missing entities, domain event listeners firing) rather than line coverage.
- **Naming & Structure (summary):** Follow the current suite: Given/When/Then in the test method name. Do not mix in outcome-only names, and do not start a rename. The later style (GWT as `@Nested` structure, method = outcome only) is described in [TESTING_CONVENTIONS.md](../../docs/TESTING_CONVENTIONS.md) and planned in [gwt-test-naming-later-refactor](../plans/gwt-test-naming-later-refactor/gwt-test-naming-later-refactor.md).

# Interacting with Me
