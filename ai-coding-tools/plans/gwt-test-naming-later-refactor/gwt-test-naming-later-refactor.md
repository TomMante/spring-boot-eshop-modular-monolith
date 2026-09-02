# gwt-test-naming-later-refactor

**Status:** deferred — do not start until the blueprint/portfolio project is
explicitly marked finished.

## Why this waits

The suite already uses `givenX_whenY_thenZ` (and often a Gherkin `@DisplayName`).
Mixing styles mid-blueprint is worse than a noisy-but-consistent convention.
Finish the product shape first; rename tests as a dedicated pass after.

## Finding

- Keep GWT as structure (body AAA; `@Nested` = shared given; method = then).
- Stop encoding Given/When/Then in every method name.
- Nest only when a fixture is shared. Do not wrap a single test in Given + When
  types just to complete the grammar.
- Drop Gherkin `@DisplayName` unless the Java name is unreadable in reports.
- Cucumber stays for acceptance tests only, if ever.

## When to run

Only after the owner says the blueprint is done. Not during feature work.

## Steps (all tests-alongside / mechanical rename — no red/green)

1. Confirm the blueprint is finished; do not start otherwise.
2. Rewrite [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) so the
   later style is the current convention; remove the "until that pass" note.
3. Align [getting-started.md](../../processes/getting-started.md) and
   [rules-of-engagement.md](../../processes/rules-of-engagement.md) with that.
4. Rename tests module by module (catalog, basket, ordering, shared, outbox-*).
   Behavior unchanged; no assertion rewrites unless a name-split shows a test
   was covering two outcomes.
5. Run `mvn test -f SpringBoot-EshopModularMonolith/pom.xml`.
6. Delete this plan or mark it done.

## Out of scope

Changing what tests assert, adding Cucumber, or renaming tests before step 1.
