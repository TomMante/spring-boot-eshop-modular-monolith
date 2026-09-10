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
